package com.minhphu.mproute.service;

import com.minhphu.mproute.dto.PlannedStockMovement;
import com.minhphu.mproute.dto.StockLevel;
import com.minhphu.mproute.dto.StockSettlementPlan;
import com.minhphu.mproute.dto.StockShortage;
import com.minhphu.mproute.entity.Order;
import com.minhphu.mproute.entity.OrderItem;
import com.minhphu.mproute.entity.Product;
import com.minhphu.mproute.entity.StockMovement;
import com.minhphu.mproute.entity.User;
import com.minhphu.mproute.entity.enums.RecordStatus;
import com.minhphu.mproute.entity.enums.StockMovementType;
import com.minhphu.mproute.repository.OrderItemRepository;
import com.minhphu.mproute.repository.ProductRepository;
import com.minhphu.mproute.repository.StockMovementRepository;
import com.minhphu.mproute.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Nghiep vu ton kho thanh pham.
 *
 * <p>Lop nay lam phan doc ghi; toan bo phep tinh de sai nam o {@link StockSettlementPlanner}
 * va duoc test rieng bang JUnit thuan.
 *
 * <h2>Hai nguyen tac khong duoc pha</h2>
 *
 * <ol>
 *   <li><b>products.stock_boxes chi doi cung luc voi mot dong stock_movements.</b> Khong co
 *       duong nao khac sua con so do. Nho vay cong ca so luon ra dung con so dang luu,
 *       va {@link #reconcile(Long)} kiem duoc dieu nay bat cu luc nao.</li>
 *   <li><b>So kho chi ghi them.</b> Ghi sai thi ghi mot dong dao (xem {@link #reverse}),
 *       khong sua, khong xoa — xoa la vi pham CBR-17 va lam vo stock_after cua moi dong sau.</li>
 * </ol>
 *
 * <h2>Vi sao khong can chup anh so luong cu truoc khi sua don</h2>
 *
 * So thung mot don DANG cam ket khong lay tu danh sach dong hang cua don, ma
 * <b>dem lai tu so kho</b>: tong OUT tru tong RETURN cua chinh don do
 * ({@link #ledgerCommitmentOf}). Ba hau qua, ca ba deu co loi:
 *
 * <ul>
 *   <li>{@code OrderService.updateOrder} goi {@code order.getItems().clear()} truoc khi dung
 *       lai danh sach moi, nen so luong cu da mat khoi bo nho — neu doc tu don thi phai nho
 *       chup anh truoc dong clear() do, quen la so kho cong don thay vi bu tru.</li>
 *   <li>Goi hai lan khong hong gi: lan hai thay cam ket da bang 0 nen khong sinh dong nao.</li>
 *   <li>Huy mot don chua tung duyet cung khong hong gi: so kho khong co dong nao cho don do
 *       nen cam ket bang rong, khong hoan kho oan.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {

    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final StockSettlementPlanner planner;

    // ==================== Doc so ton cho man hinh ====================

    /** Ba con so ton kho cua moi san pham dang su dung, sap theo ma san pham. */
    @Transactional(readOnly = true)
    public List<StockLevel> levels() {
        Map<Long, Integer> committed = committedBoxesByProduct();

        return productRepository.findByStatus(RecordStatus.ACTIVE).stream()
                .sorted(Comparator.comparing(Product::getCode))
                .map(product -> new StockLevel(
                        product,
                        product.getStockBoxes(),
                        committed.getOrDefault(product.getId(), 0)))
                .toList();
    }

    /** So thung con ranh theo productId — form don hang dung de hien canh bao. */
    @Transactional(readOnly = true)
    public Map<Long, Integer> availableBoxesByProduct() {
        return productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getId, Product::getStockBoxes));
    }

    /**
     * So thung da cam ket theo productId: hang thuoc don da duyet nhung chua giao xong.
     *
     * Dem lai moi lan goi chu khong luu thanh cot rieng, nho vay khong bao gio lech khoi
     * bang orders. Chi goi o man hinh kho va form don hang, KHONG goi trong vong lap
     * thuat toan (luat vang so 4).
     */
    @Transactional(readOnly = true)
    public Map<Long, Integer> committedBoxesByProduct() {
        Map<Long, Integer> result = new HashMap<>();
        for (Object[] row : orderItemRepository.sumCommittedBoxesByProduct()) {
            Long productId = (Long) row[0];
            long boxes = ((Number) row[1]).longValue();
            result.put(productId, (int) boxes);
        }
        return result;
    }

    // ==================== Kiem truoc khi duyet (MSG-17) ====================

    /**
     * Kiem don co du hang khong, KHONG ghi gi xuong CSDL.
     *
     * Dung o hai cho khac nhau, cung mot cau tra loi:
     *   - form tao don: hien canh bao vang, van cho luu (don PENDING_APPROVAL chua giu hang)
     *   - bam Duyet don: chan cung
     *
     * Dat ten va kieu tra ve giong {@code OrderService.checkCapacity} de hai phep kiem
     * cua cung mot man hinh doc nhu nhau.
     *
     * @return thong bao MSG-17 khi thieu hang, rong khi du
     */
    @Transactional(readOnly = true)
    public Optional<String> checkAvailability(Order order) {
        return checkAvailability(order.getId(), requestedCommitmentOf(order));
    }

    /**
     * Ban dung tu FORM don hang, khi don chua luu nen chua co entity de doc dong hang.
     *
     * @param orderId null khi dang tao don moi — luc do khong co cam ket cu nao
     * @param requestedCommitment so thung muon cam ket, da gop theo san pham
     */
    @Transactional(readOnly = true)
    public Optional<String> checkAvailability(Long orderId, Map<Long, Integer> requestedCommitment) {
        StockSettlementPlan plan = planner.plan(
                availableBoxesByProduct(), ledgerCommitmentOf(orderId), requestedCommitment);

        if (plan.isFeasible()) {
            return Optional.empty();
        }
        return Optional.of(describeShortages(plan.shortages()));
    }

    /** Ghep danh sach thieu hang thanh cau MSG-17 cho nguoi dung doc. */
    private String describeShortages(List<StockShortage> shortages) {
        String detail = shortages.stream()
                .map(shortage -> {
                    Product product = productRepository.findById(shortage.productId()).orElseThrow();
                    return String.format("%s cần %d thùng, kho còn %d (thiếu %d)",
                            product.getName(), shortage.requiredBoxes(),
                            shortage.availableBoxes(), shortage.missingBoxes());
                })
                .collect(Collectors.joining("; "));

        return "Không đủ hàng trong kho: " + detail
                + ". Vui lòng giảm số thùng hoặc nhập thêm hàng trước khi duyệt.";
    }

    // ==================== Tru / hoan kho theo trang thai don ====================

    /**
     * Cam ket hang cho don (luc duyet don): sinh cac dong OUT.
     *
     * @return thong bao MSG-17 khi thieu hang — luc do KHONG ghi dong nao, don khong duyet duoc
     */
    @Transactional
    public Optional<String> commit(Order order, Long userId) {
        return settle(order, requestedCommitmentOf(order), userId, "Duyet don " + order.getCode());
    }

    /**
     * Nha toan bo hang dang cam ket (huy don, giao that bai, hoan don): sinh cac dong RETURN.
     *
     * Khong bao gio that bai: nha hang chi lam so ton tang nen khong co kha nang thieu.
     */
    @Transactional
    public void release(Order order, Long userId, String reason) {
        Optional<String> impossible = settle(order, Map.of(), userId, reason);
        // Nhanh nay khong the xay ra: requested rong nen moi delta deu <= 0, planner khong
        // sinh thieu hang duoc. Ghi log thay vi im lang de neu co thi con lan ra duoc
        impossible.ifPresent(message ->
                log.error("Nha hang cua don {} bao thieu hang, khong the xay ra: {}",
                        order.getCode(), message));
    }

    /**
     * Sua don da duyet: bu tru dung phan chenh lech so voi so kho dang ghi nhan.
     *
     * @return thong bao MSG-17 khi phan TANG THEM khong du hang
     */
    @Transactional
    public Optional<String> recommit(Order order, Long userId) {
        return settle(order, requestedCommitmentOf(order), userId, "Sua don " + order.getCode());
    }

    /**
     * Loi vao duy nhat cua moi phep tru/hoan kho theo don hang.
     *
     * Bon buoc: dem cam ket hien tai tu so kho, goi planner, neu thieu thi dung lai va
     * khong ghi gi, neu du thi ghi tung dong va cap nhat stock_boxes trong CUNG mot
     * giao dich. Mot @Transactional bao ca ham nen khong co kha nang ghi duoc nua chung.
     */
    private Optional<String> settle(Order order,
                                    Map<Long, Integer> requestedCommitment,
                                    Long userId,
                                    String note) {

        Map<Long, Integer> previousCommitment = ledgerCommitmentOf(order.getId());
        StockSettlementPlan plan =
                planner.plan(availableBoxesByProduct(), previousCommitment, requestedCommitment);

        if (!plan.isFeasible()) {
            return Optional.of(describeShortages(plan.shortages()));
        }

        User actor = findUser(userId);
        for (PlannedStockMovement planned : plan.movements()) {
            writeMovement(planned.productId(), planned.movementType(), planned.boxCount(),
                    order, null, note, actor);
        }

        if (!plan.movements().isEmpty()) {
            log.info("Ton kho: don {} ghi {} dong so kho ({})",
                    order.getCode(), plan.movements().size(), note);
        }
        return Optional.empty();
    }

    /**
     * So thung don nay DANG cam ket, dem tu so kho: tong OUT tru tong RETURN.
     *
     * Day la nguon su that ve "don nay dang giu bao nhieu hang", khong phai danh sach
     * dong hang cua don — xem phan giai thich o dau lop.
     */
    private Map<Long, Integer> ledgerCommitmentOf(Long orderId) {
        if (orderId == null) {
            return Map.of();
        }

        Map<Long, Integer> committed = new LinkedHashMap<>();
        for (StockMovement movement : stockMovementRepository.findByOrderId(orderId)) {
            Long productId = movement.getProduct().getId();
            // OUT lam tang phan don dang giu, RETURN lam giam — nguoc dau voi anh huong
            // len so ton, nen dao dau getSignedBoxCount()
            int heldDelta = -movement.getSignedBoxCount();
            committed.merge(productId, heldDelta, Integer::sum);
        }

        // Bo cac san pham da hoan het, de Map nay doc nhu "dang giu nhung gi"
        committed.values().removeIf(boxes -> boxes == 0);
        return committed;
    }

    /**
     * So thung don MUON cam ket, gop theo san pham.
     *
     * Phai gop vi mot san pham xuat hien tren hai dong chi tiet la hop le — nguoi dung
     * chon Bong tay trang o dong 1 va chon lai o dong 3. Khong gop thi dong sau ghi de
     * dong truoc va so kho tru thieu.
     */
    private Map<Long, Integer> requestedCommitmentOf(Order order) {
        Map<Long, Integer> requested = new LinkedHashMap<>();
        for (OrderItem line : order.getItems()) {
            requested.merge(line.getProduct().getId(), line.getBoxCount(), Integer::sum);
        }
        return requested;
    }

    // ==================== Nhap kho (IN) ====================

    /** Nhap kho: san xuat xong, hang vao kho. */
    @Transactional
    public void receive(Long productId, int boxCount, String note, Long userId) {
        if (boxCount <= 0) {
            throw new IllegalArgumentException("Số thùng nhập kho phải lớn hơn 0");
        }

        writeMovement(productId, StockMovementType.IN, boxCount, null, null, note, findUser(userId));
        log.info("Nhap kho san pham #{}: {} thung", productId, boxCount);
    }

    // ==================== Kiem ke (ADJUST) ====================

    /**
     * Kiem ke: nguoi dung go SO THUNG DEM DUOC TRONG KHO, he thong tu suy ra phan con ranh.
     *
     * Day la cho tinh te nhat cua ca phan ton kho. Nhan vien kho dem duoc 295 thung, nhung
     * 295 la so VAT LY — khong duoc dat stock_boxes = 295, vi trong 295 thung do co 220
     * thung da cam ket cho don. Phep tinh dung:
     *
     * <pre>
     *   con ranh dung ra phai la  =  dem duoc  -  da cam ket
     *                        75   =     295    -      220
     *   dang ghi nhan 80 -> lech -5 -> ghi ADJUST_DOWN 5
     * </pre>
     *
     * Dem duoc it hon so da cam ket la tinh huong that su co van de: hang da hua cho khach
     * nhung trong kho khong co du. Chan lai thay vi am tham dat ve 0, vi cach chua dung la
     * huy hoac giam don, khong phai sua so ton.
     *
     * @param countedPhysicalBoxes so thung dem duoc trong kho
     * @return thong bao khi khong can dieu chinh gi, rong khi da ghi dong dieu chinh
     */
    @Transactional
    public Optional<String> stocktake(Long productId, int countedPhysicalBoxes,
                                      String note, Long userId) {
        if (countedPhysicalBoxes < 0) {
            throw new IllegalArgumentException("Số thùng đếm được không thể là số âm");
        }
        if (note == null || note.isBlank()) {
            throw new IllegalArgumentException("Kiểm kê bắt buộc nhập lý do");
        }

        Product product = findProduct(productId);
        int committed = committedBoxesByProduct().getOrDefault(productId, 0);
        int shouldBeAvailable = countedPhysicalBoxes - committed;

        if (shouldBeAvailable < 0) {
            return Optional.of(String.format(
                    "Đếm được %d thùng nhưng đã cam kết %d thùng cho các đơn đã duyệt. "
                            + "Vui lòng hủy hoặc giảm đơn trước, không điều chỉnh tồn kho.",
                    countedPhysicalBoxes, committed));
        }

        int delta = shouldBeAvailable - product.getStockBoxes();
        if (delta == 0) {
            return Optional.of("Số đếm khớp với số hệ thống đang ghi nhận, không cần điều chỉnh.");
        }

        StockMovementType type =
                delta > 0 ? StockMovementType.ADJUST_UP : StockMovementType.ADJUST_DOWN;
        writeMovement(productId, type, Math.abs(delta), null, null, note, findUser(userId));

        log.info("Kiem ke san pham #{}: dem {} thung, cam ket {}, dieu chinh {}",
                productId, countedPhysicalBoxes, committed, delta);
        return Optional.empty();
    }

    // ==================== Hoan tac mot dong (but toan dao) ====================

    /**
     * Hoan tac mot dong so kho bang cach ghi MOT DONG DAO, khong xoa dong goc (CBR-17).
     *
     * Nguoi dung thay nhu da xoa, nhung van con dau vet ai ghi sai gi luc nao. Day la
     * nguyen tac but toan dao cua ke toan: so sach ghi sai thi khong boi di.
     *
     * Khong cho hoan tac OUT va RETURN: hai loai do do he thong sinh ra theo trang thai
     * don hang, dao bang tay se lam so kho noi khac voi bang orders. Muon dao thi di qua
     * nghiep vu don — huy don, bao giao that bai — roi so kho tu theo.
     */
    @Transactional
    public void reverse(Long movementId, String note, Long userId) {
        StockMovement original = stockMovementRepository.findById(movementId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dòng sổ kho"));

        if (original.getMovementType().isSystemGenerated()) {
            throw new IllegalStateException(
                    "Dòng do hệ thống ghi theo trạng thái đơn hàng, không hoàn tác trực tiếp. "
                            + "Vui lòng xử lý qua đơn hàng tương ứng.");
        }
        if (original.getReversalOf() != null) {
            throw new IllegalStateException("Không hoàn tác một dòng đảo");
        }
        if (stockMovementRepository.existsByReversalOfId(movementId)) {
            throw new IllegalStateException("Dòng này đã được hoàn tác trước đó");
        }
        if (note == null || note.isBlank()) {
            throw new IllegalArgumentException("Hoàn tác bắt buộc nhập lý do");
        }

        Product product = original.getProduct();
        int reversalSign = -original.getMovementType().getSign();
        int stockAfterReversal = product.getStockBoxes() + reversalSign * original.getBoxCount();

        if (stockAfterReversal < 0) {
            throw new IllegalStateException(String.format(
                    "Hoàn tác dòng này làm tồn kho âm (%d thùng). Hàng đã được cam kết cho đơn, "
                            + "vui lòng kiểm kê thay vì hoàn tác.", stockAfterReversal));
        }

        StockMovementType reversalType =
                reversalSign > 0 ? StockMovementType.ADJUST_UP : StockMovementType.ADJUST_DOWN;

        writeMovement(product.getId(), reversalType, original.getBoxCount(),
                null, original, note, findUser(userId));

        log.info("Hoan tac dong so kho #{} ({} {} thung cua san pham #{})",
                movementId, original.getMovementType(), original.getBoxCount(), product.getId());
    }

    // ==================== So kho ====================

    @Transactional(readOnly = true)
    public List<StockMovement> movementsOf(Long productId) {
        return stockMovementRepository.findByProductIdOrderByCreatedAtDescIdDesc(productId);
    }

    @Transactional(readOnly = true)
    public Page<StockMovement> searchMovements(Long productId, StockMovementType movementType,
                                               LocalDateTime from, LocalDateTime to,
                                               Pageable pageable) {
        return stockMovementRepository.search(productId, movementType, from, to, pageable);
    }

    @Transactional(readOnly = true)
    public List<StockMovement> searchAllMovements(Long productId, StockMovementType movementType,
                                                  LocalDateTime from, LocalDateTime to) {
        return stockMovementRepository.searchAll(productId, movementType, from, to);
    }

    /**
     * Doi chieu: cong ca so kho cua mot san pham co ra dung products.stock_boxes khong.
     *
     * Hai con so lech nhau la co loi trong code, khong phai loi nhap lieu. Day chinh la
     * cau tra loi cho "sao em biet so ton nay dung" khi van dap: khong tin con so dang
     * luu, cong lai ca so va doi chieu.
     *
     * @return rong khi khop, thong bao chenh lech khi lech
     */
    @Transactional(readOnly = true)
    public Optional<String> reconcile(Long productId) {
        Product product = findProduct(productId);
        int ledgerSum = stockMovementRepository.sumSignedBoxCount(productId);

        if (ledgerSum == product.getStockBoxes()) {
            return Optional.empty();
        }
        return Optional.of(String.format(
                "Sổ kho cộng ra %d thùng nhưng tồn kho đang ghi %d thùng (lệch %d)",
                ledgerSum, product.getStockBoxes(), ledgerSum - product.getStockBoxes()));
    }

    /** Doi chieu moi san pham mot luot — dung de kiem nhanh sau khi chay thu kich ban demo. */
    @Transactional(readOnly = true)
    public List<String> reconcileAll() {
        List<String> problems = new ArrayList<>();
        for (Product product : productRepository.findAll()) {
            reconcile(product.getId()).ifPresent(message ->
                    problems.add(product.getCode() + " " + product.getName() + ": " + message));
        }
        return problems;
    }

    // ==================== Ghi mot dong ====================

    /**
     * Cho DUY NHAT ghi vao stock_movements va sua products.stock_boxes.
     *
     * Hai viec do luon di cung nhau trong mot giao dich, khong co duong nao khac sua
     * stock_boxes. Nho vay bat bien "cong ca so ra dung con so dang luu" khong bao gio vo.
     */
    private void writeMovement(Long productId,
                               StockMovementType type,
                               int boxCount,
                               Order order,
                               StockMovement reversalOf,
                               String note,
                               User actor) {

        Product product = findProduct(productId);
        int stockAfter = product.getStockBoxes() + type.getSign() * boxCount;

        if (stockAfter < 0) {
            // Luoi an toan cuoi cung. Planner da chan thieu hang truoc do, den duoc day
            // nghia la co duong nao khac dang sua ton kho ma khong qua planner
            throw new IllegalStateException(String.format(
                    "Tồn kho của %s sẽ âm (%d thùng) sau phát sinh này",
                    product.getName(), stockAfter));
        }

        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setMovementType(type);
        movement.setBoxCount(boxCount);
        movement.setStockAfter(stockAfter);
        movement.setOrder(order);
        movement.setReversalOf(reversalOf);
        movement.setNote(note);
        movement.setCreatedBy(actor);
        movement.setCreatedAt(LocalDateTime.now());
        stockMovementRepository.save(movement);

        product.setStockBoxes(stockAfter);
        productRepository.save(product);
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));
    }

    /** NULL khi he thong tu ghi chu khong phai nguoi dung bam nut — giong StatusHistoryService. */
    private User findUser(Long userId) {
        return userId == null ? null : userRepository.findById(userId).orElse(null);
    }
}
