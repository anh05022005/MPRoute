package com.minhphu.mproute.service;

import com.minhphu.mproute.dto.OrderForm;
import com.minhphu.mproute.dto.OrderItemForm;
import com.minhphu.mproute.dto.OrderTotals;
import com.minhphu.mproute.entity.Customer;
import com.minhphu.mproute.entity.Order;
import com.minhphu.mproute.entity.OrderItem;
import com.minhphu.mproute.entity.Product;
import com.minhphu.mproute.entity.Vehicle;
import com.minhphu.mproute.entity.enums.EntityType;
import com.minhphu.mproute.entity.enums.OrderStatus;
import com.minhphu.mproute.entity.enums.PaymentMethod;
import com.minhphu.mproute.entity.enums.PaymentStatus;
import com.minhphu.mproute.repository.CustomerRepository;
import com.minhphu.mproute.repository.OrderRepository;
import com.minhphu.mproute.repository.ProductRepository;
import com.minhphu.mproute.repository.UserRepository;
import com.minhphu.mproute.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Nghiep vu don hang (CN-02-01 den CN-02-06).
 *
 * Bon viec kho nam o day, deu la cho hoi dong hay hoi:
 *   1. Sinh ma don khong trung (BR02-01)
 *   2. Tu tinh kg / m3 / tien hang, CHEP don gia tai thoi diem tao don (BR02-04, BR02-13)
 *   3. Chan don khong xe nao cho noi (BR02-05) — kiem CA HAI rang buoc dong thoi
 *   4. Ghi status_history ngay khi tao don (CBR-14)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final StatusHistoryService statusHistoryService;

    @Transactional(readOnly = true)
    public Page<Order> search(LocalDate deliveryDate, OrderStatus status, Long customerId,
                              Pageable pageable) {
        return orderRepository.search(deliveryDate, status, customerId, pageable);
    }

    @Transactional(readOnly = true)
    public Order getById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));
    }

    // ==================== Sinh ma don (BR02-01) ====================

    /**
     * Sinh ma don dang MP + yyMMdd + 4 chu so thu tu. Vi du: MP2608040042.
     *
     * Sau chu so giua lay theo NGAY GIAO chu khong phai ngay tao don: dieu phoi vien
     * doc ma don qua dien thoai cho tai xe, ma chua trong no ngay phai giao thi de doi chieu.
     *
     * Cach lay so thu tu: dem so don da co cua ngay giao do roi cong 1. Neu ma tao ra
     * da ton tai thi tang tiep — truong hop nay xay ra khi mot don duoc SUA sang ngay giao
     * khac, luc do so dem khong con khop voi so thu tu lon nhat da phat.
     * Vong lap chay nhieu nhat vai vong, khong phai vong lap thuat toan.
     */
    @Transactional(readOnly = true)
    public String generateOrderCode(LocalDate deliveryDate) {
        String prefix = "MP" + deliveryDate.format(DateTimeFormatter.ofPattern("yyMMdd"));
        int sequence = (int) orderRepository.countByDeliveryDate(deliveryDate) + 1;

        while (sequence <= 9999) {
            String code = prefix + String.format("%04d", sequence);
            if (orderRepository.findByCode(code).isEmpty()) {
                return code;
            }
            sequence++;
        }
        // 9999 don mot ngay la khong tuong voi quy mo Minh Phu, nhung khong de im lang
        throw new IllegalStateException("Đã hết số thứ tự mã đơn cho ngày " + deliveryDate);
    }

    // ==================== Tu tinh kg / m3 / tien (BR02-04, BR02-13) ====================

    /**
     * Tinh ba con so tong tu cac dong chi tiet tren form.
     *
     * Don gia, kg moi thung, m3 moi thung deu lay tu DANH MUC tai thoi diem goi ham nay,
     * khong lay tu form gui len: nguoi dung sua duoc moi thu trong trinh duyet, chi
     * co danh muc trong CSDL la dang tin (BR02-04).
     */
    @Transactional(readOnly = true)
    public OrderTotals calculateTotals(OrderForm form) {
        double weight = 0;
        double volume = 0;
        BigDecimal amount = BigDecimal.ZERO;

        for (OrderItemForm line : form.getItems()) {
            if (line.isBlank()) {
                continue;
            }
            Product product = findProduct(line.getProductId());
            weight += product.getWeightPerBox() * line.getBoxCount();
            volume += product.getVolumePerBox() * line.getBoxCount();
            amount = amount.add(product.getUnitPrice().multiply(BigDecimal.valueOf(line.getBoxCount())));
        }

        // Lam tron dung so chu so thap phan cua cot trong CSDL (DECIMAL(10,2) va DECIMAL(10,3))
        // de con so hien tren man hinh khop het voi con so luu xuong
        return new OrderTotals(round(weight, 2), round(volume, 3), amount);
    }

    // ==================== Chan don vuot suc chua (BR02-05) ====================

    /**
     * Kiem don co xe nao cho noi khong.
     *
     * Dieu kien dung la ton tai MOT xe thoa CA HAI rang buoc cung luc (CBR-03, CBR-04):
     * xe A tai 1500 kg nhung thung nho, xe B thung to nhung chi tai 500 kg — don
     * 1200 kg / 8 m3 khong xe nao cho duoc, du "xe nao do" thoa tung dieu kien rieng le.
     * Kiem tach roi hai rang buoc la loi kinh dien cua bai toan nay.
     *
     * Khong loc theo trang thai xe: xe dang chay hay dang bao tri thi hom khac van chay,
     * cho nay hoi "don nay co bao gio giao duoc khong", khong phai "hom nay giao duoc khong".
     *
     * @return thong bao loi MSG-06 khi khong xe nao cho noi, rong khi hop le
     */
    @Transactional(readOnly = true)
    public Optional<String> checkCapacity(OrderTotals totals) {
        List<Vehicle> vehicles = vehicleRepository.findAll();

        if (vehicles.isEmpty()) {
            return Optional.of("Chưa khai báo phương tiện nào, không kiểm được sức chứa. "
                    + "Vui lòng thêm xe trong danh mục phương tiện trước.");
        }

        boolean hasVehicleThatFits = vehicles.stream()
                .anyMatch(vehicle -> vehicle.getMaxWeight() >= totals.weight()
                             && vehicle.getMaxVolume() >= totals.volume());
        if (hasVehicleThatFits) {
            return Optional.empty();
        }

        // Lay xe tai trong lon nhat lam moc de bao cho de hieu: don da khong vua xe nay
        // thi cung khong vua xe nao
        Vehicle largestVehicle = vehicles.stream()
                .max(Comparator.comparingDouble(Vehicle::getMaxWeight))
                .orElseThrow();

        return Optional.of(String.format(
                "Đơn hàng vượt sức chứa xe lớn nhất (%.2f kg / %.3f m³ so với %.2f kg / %.3f m³ "
                        + "của xe %s). Vui lòng tách đơn.",
                totals.weight(), totals.volume(),
                largestVehicle.getMaxWeight(), largestVehicle.getMaxVolume(), largestVehicle.getLicensePlate()));
    }

    // ==================== Tao don ====================

    /**
     * Tao don moi. Don luon vao he thong o trang thai PENDING_APPROVAL (BR02-09),
     * phai bam nut "Duyet don" rieng moi sang APPROVED — buoc do de soat lai
     * dia chi va toa do truoc khi don di vao thuat toan (BR02-10).
     */
    @Transactional
    public Order createOrder(OrderForm form, Long createdById) {
        Customer customer = customerRepository.findById(form.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng"));

        Order order = new Order();
        order.setCode(generateOrderCode(form.getDeliveryDate()));
        order.setCustomer(customer);
        order.setDeliveryDate(form.getDeliveryDate());
        order.setDeliveryAddress(form.getDeliveryAddress().trim());
        order.setLatitude(form.getLatitude());
        order.setLongitude(form.getLongitude());
        order.setPaymentMethod(form.getPaymentMethod());
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setStatus(OrderStatus.PENDING_APPROVAL);
        order.setNote(blankToNull(form.getNote()));

        if (createdById != null) {
            order.setCreatedBy(userRepository.findById(createdById).orElse(null));
        }

        for (OrderItem line : buildOrderItems(form)) {
            order.addItem(line);
        }
        applyTotals(order);

        Order saved = orderRepository.save(order);

        // CBR-14: moi lan chuyen trang thai deu ghi lai, ke ca lan dau tien
        statusHistoryService.recordChange(EntityType.ORDER, saved.getId(),
                null, OrderStatus.PENDING_APPROVAL.name(), createdById, "Tạo đơn hàng");

        log.info("Tao don {} cho khach {}: {} dong, {} kg, {} m3, {} d",
                saved.getCode(), customer.getName(), saved.getItems().size(),
                saved.getTotalWeight(), saved.getTotalVolume(), saved.getTotalAmount());
        return saved;
    }

    // ==================== CN-02-08 Sua don ====================

    /**
     * BR02-11 — chi don chua di vao thuat toan moi con sua hoac huy duoc.
     *
     * Tu ROUTED tro di, don da nam trong mot tuyen: doi so thung la doi khoi luong tuyen,
     * doi dia chi la doi ca thu tu diem dung ma tuyen thi da chot va tai xe co the dang
     * tren duong. Muon doi thi phai huy tuyen roi phan lai.
     */
    public boolean canEditOrCancel(Order order) {
        return order.getStatus() == OrderStatus.PENDING_APPROVAL
                || order.getStatus() == OrderStatus.APPROVED;
    }

    /**
     * Cap nhat mot don da co (CN-02-08).
     *
     * Ma don GIU NGUYEN ke ca khi doi ngay giao: khach da duoc doc ma qua dien thoai,
     * doi ma la ho tra cuu khong ra don cua minh nua. Day cung la ly do sinhMaDon phai
     * co vong lap kiem trung — sau lan doi ngay giao nay, so dem don trong ngay khong
     * con khop voi so thu tu lon nhat da phat cho ngay do.
     *
     * Trang thai cung giu nguyen: sua mot don DA DUYET khong lam no quay ve cho duyet
     * (docs/01 khong co canh nao di nguoc nhu vay).
     */
    @Transactional
    public Order updateOrder(OrderForm form, Long updatedById) {
        Order order = getById(form.getId());

        // Luoi an toan: Controller da chan roi, nhung ham nay cung phai tu bao ve minh
        if (!canEditOrCancel(order)) {
            throw new IllegalStateException("Không sửa được đơn ở trạng thái " + order.getStatus());
        }

        Customer customer = customerRepository.findById(form.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng"));

        order.setCustomer(customer);
        order.setDeliveryDate(form.getDeliveryDate());
        order.setDeliveryAddress(form.getDeliveryAddress().trim());
        order.setLatitude(form.getLatitude());
        order.setLongitude(form.getLongitude());
        order.setPaymentMethod(form.getPaymentMethod());
        order.setNote(blankToNull(form.getNote()));

        /*
         * Dung .clear() tren chinh danh sach dang co chu KHONG setItems(danh sach moi):
         * orphanRemoval bam vao dung the hien danh sach ma Hibernate dang theo doi,
         * thay bang danh sach khac thi cac dong cu khong bi xoa, don thanh ra cong don
         * hang cu va hang moi (BR02-03).
         */
        order.getItems().clear();
        for (OrderItem line : buildOrderItems(form)) {
            order.addItem(line);
        }
        applyTotals(order);

        Order saved = orderRepository.save(order);
        log.info("Sua don {} (nguoi sua #{}): {} dong, {} kg, {} m3, {} d",
                saved.getCode(), updatedById, saved.getItems().size(),
                saved.getTotalWeight(), saved.getTotalVolume(), saved.getTotalAmount());
        return saved;
    }

    // ==================== CN-02-07 Duyet don ====================

    /**
     * Duyet don: PENDING_APPROVAL -> APPROVED (BR02-09).
     *
     * Buoc nay khong phai thu tuc hanh chinh. No la cho nguoi that nhin lai dia chi va
     * toa do truoc khi don di vao thuat toan (BR02-10): toa do sai mot chut thi tuyen
     * van chay ra ket qua dep, chi la giao nham cho. Chi don APPROVED moi duoc dua vao
     * phan tuyen (BR03-01).
     */
    @Transactional
    public Order approveOrder(Long id, Long approvedById) {
        Order order = getById(id);

        if (order.getStatus() != OrderStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Chỉ duyệt được đơn đang chờ duyệt");
        }

        order.setStatus(OrderStatus.APPROVED);
        Order saved = orderRepository.save(order);

        statusHistoryService.recordChange(EntityType.ORDER, saved.getId(),
                OrderStatus.PENDING_APPROVAL.name(), OrderStatus.APPROVED.name(),
                approvedById, "Duyệt đơn");

        log.info("Duyet don {}", saved.getCode());
        return saved;
    }

    // ==================== CN-02-09 Huy don ====================

    /**
     * Huy don kem ly do (BR02-12).
     *
     * KHONG xoa ban ghi (CBR-17): don da huy van phai tra cuu duoc, va bao cao cuoi thang
     * can dem duoc bao nhieu don bi huy vi ly do gi. Ly do luu vao cancel_reason, dong
     * thoi ghi vao ghi chu cua lich su trang thai de trang tra cuu doc thang ra.
     */
    @Transactional
    public Order cancelOrder(Long id, String reason, Long cancelledById) {
        Order order = getById(id);

        if (!canEditOrCancel(order)) {
            throw new IllegalStateException("Không hủy được đơn ở trạng thái " + order.getStatus());
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Hủy đơn bắt buộc nhập lý do");
        }

        String previousStatus = order.getStatus().name();
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(reason.trim());
        Order saved = orderRepository.save(order);

        statusHistoryService.recordChange(EntityType.ORDER, saved.getId(),
                previousStatus, OrderStatus.CANCELLED.name(), cancelledById, reason.trim());

        log.info("Huy don {}: {}", saved.getCode(), reason.trim());
        return saved;
    }

    // ==================== CN-02-06 Khoi tom tat doc lai cho khach ====================

    /**
     * Dung doan van ban de dieu phoi vien DOC LAI cho khach qua dien thoai truoc khi cup may.
     *
     * Dung o tang service chu khong ghep chuoi trong file .html: doan nay co logic that
     * (duyet tung dong hang, dinh dang tien theo kieu Viet Nam), de trong template thi
     * thanh mot bieu thuc dai loang ngoang khong sua noi. Nguoi dung con bam nut sao chep
     * de dan sang Zalo nen phai la van ban thuan, khong phai bang HTML.
     */
    public String buildReadBackSummary(Order order) {
        StringBuilder summary = new StringBuilder();
        summary.append("Đơn hàng ").append(order.getCode()).append('\n');
        summary.append("Khách: ").append(order.getCustomer().getName())
                .append(" — ").append(order.getCustomer().getPhone()).append('\n');
        summary.append("Giao tới: ").append(order.getDeliveryAddress()).append('\n');
        summary.append("Ngày giao: ")
                .append(order.getDeliveryDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .append('\n');

        summary.append("Hàng:\n");
        for (OrderItem line : order.getItems()) {
            summary.append(" - ").append(line.getProduct().getName())
                    .append(": ").append(line.getBoxCount())
                    .append(' ').append(line.getProduct().getUnit()).append('\n');
        }

        summary.append("Tiền hàng: ").append(formatCurrency(order.getTotalAmount())).append('\n');
        summary.append("Thanh toán: ").append(order.getPaymentMethod().getDisplayName()).append('\n');
        summary.append("Phải thu khi giao: ").append(formatCurrency(order.getAmountToCollect()));
        return summary.toString();
    }

    /** Tien kieu Viet Nam: dau cham ngan nghin, khong phan thap phan. Vi du 3.720.000d */
    private String formatCurrency(BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("vi-VN"));
        symbols.setGroupingSeparator('.');
        return new DecimalFormat("#,##0", symbols).format(amount) + "đ";
    }

    // ==================== Ham dung chung ====================

    /**
     * Dung cac dong chi tiet tu form, CHEP gia va thong so tu danh muc (BR02-13).
     *
     * Chep chu khong tham chieu: neu chi luu product_id roi luc hien don moi tra sang
     * bang products lay gia, thi quan tri vien doi gia hom sau la don hang thang truoc
     * tu doi tien theo. Do la loi nghiep vu nghiem trong, hoi dong hay hoi dung cho nay.
     */
    private List<OrderItem> buildOrderItems(OrderForm form) {
        List<OrderItem> items = new ArrayList<>();

        for (OrderItemForm line : form.getItems()) {
            if (line.isBlank()) {
                continue;   // dong de trong tren man hinh, bo qua
            }
            Product product = findProduct(line.getProductId());
            int boxCount = line.getBoxCount();

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setBoxCount(boxCount);
            item.setLineWeight(round(product.getWeightPerBox() * boxCount, 2));
            item.setLineVolume(round(product.getVolumePerBox() * boxCount, 3));
            item.setUnitPrice(product.getUnitPrice());
            item.setLineTotal(product.getUnitPrice().multiply(BigDecimal.valueOf(boxCount)));
            items.add(item);
        }
        return items;
    }

    /**
     * Cong don ba con so tong tu cac dong DA GAN vao don, roi tinh so tien phai thu.
     *
     * BR02-14: chi don tien mat moi phai thu tien khi giao. Cong no va chuyen khoan
     * truoc deu bang 0 — tai xe cam to phieu ghi 0 dong thi khong thu nham cua khach.
     * BR02-16: doi hinh thuc thanh toan la con so nay tinh lai ngay, vi no duoc tinh
     * o day chu khong luu roi de do.
     */
    private void applyTotals(Order order) {
        double weight = 0;
        double volume = 0;
        BigDecimal amount = BigDecimal.ZERO;

        for (OrderItem line : order.getItems()) {
            weight += line.getLineWeight();
            volume += line.getLineVolume();
            amount = amount.add(line.getLineTotal());
        }

        order.setTotalWeight(round(weight, 2));
        order.setTotalVolume(round(volume, 3));
        order.setTotalAmount(amount);
        order.setAmountToCollect(order.getPaymentMethod() == PaymentMethod.CASH
                ? amount
                : BigDecimal.ZERO);
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm #" + id));
    }

    /** Lam tron ve dung so chu so thap phan cua cot trong CSDL. */
    private double round(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
