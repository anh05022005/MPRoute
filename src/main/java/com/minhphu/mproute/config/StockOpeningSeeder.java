package com.minhphu.mproute.config;

import com.minhphu.mproute.entity.Order;
import com.minhphu.mproute.entity.Product;
import com.minhphu.mproute.entity.StockMovement;
import com.minhphu.mproute.entity.enums.OrderStatus;
import com.minhphu.mproute.entity.enums.StockMovementType;
import com.minhphu.mproute.repository.OrderRepository;
import com.minhphu.mproute.repository.ProductRepository;
import com.minhphu.mproute.repository.StockMovementRepository;
import com.minhphu.mproute.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Khoi tao ton kho lan dau tu trang thai hien co cua CSDL — so du dau ky.
 *
 * <h2>Vi sao phai co lop rieng, chay sau cung</h2>
 *
 * Phan ton kho duoc them vao khi he thong da chay duoc mot thoi gian: CSDL da co san nam
 * san pham va hang tram don hang, trong do mot so don dang o trang thai da duyet nhung chua
 * giao xong — tuc la dang GIU hang trong kho. {@link HistorySeeder} cung ghi entity truc tiep
 * chu khong goi qua OrderService, nen tren may dung moi cac don do cung khong di qua phan
 * tru kho.
 *
 * Neu chi ghi ton dau ky cho tung san pham roi dung lai, con so se khong mach lac:
 *
 * <pre>
 *   ton dau ky      800
 *   da cam ket       39   (dem tu order_items cua don APPROVED/ROUTED/DELIVERING)
 *   trong kho       839   <- phong len, vi 39 thung kia chua tung bi tru
 * </pre>
 *
 * Nen lop nay lam HAI buoc: ghi ton dau ky bang so thung VAT LY trong kho, roi tru ngay phan
 * cac don dang giu. Ket qua mach lac:
 *
 * <pre>
 *   con ranh        761
 *   da cam ket       39
 *   trong kho       800   <- dung bang so dem duoc trong kho
 * </pre>
 *
 * Nho buoc hai, cac don cu hanh xu y het don moi: huy hoac giao that bai thi hoan dung so
 * thung do ve kho. Khong co buoc hai thi chung la mot nhom don dac biet khong tham gia ton
 * kho, va day la loai ngoai le se gay ra mot cau hoi kho tra loi khi van dap.
 *
 * <h2>Dieu kien chay</h2>
 *
 * Chi chay khi bang stock_movements RONG HOAN TOAN, tuc la ton kho chua tung duoc theo doi.
 * Mot dong roi thi khong bao gio dung vao nua, nen chay lai bao nhieu lan cung khong sinh
 * trung va khong de len so lieu da sua tay.
 *
 * San pham them moi qua giao dien ve sau khong duoc ghi ton dau ky — dung nhu vay, san pham
 * moi that su co 0 thung cho den khi co nguoi nhap kho.
 *
 * <p>Day chinh la cach moi he thong that di vao hoat dong: khong ai dung lai lich su muoi nam,
 * nguoi ta chot mot con so ngay khai truong roi ghi so tu do. Ghi trong docs/09.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@org.springframework.core.annotation.Order(4)   // sau HistorySeeder: can thay het don da duyet
public class StockOpeningSeeder implements CommandLineRunner {

    /**
     * So thung VAT LY trong kho luc bat dau theo doi, theo ma san pham (docs/08).
     *
     * Hai mat hang nhe va cong kenh de 800, ba mat hang nuoc de 500. Du de kich ban demo
     * chay muot, ma van du nho de dung duoc tinh huong thieu hang cho CN-03-10.
     */
    private static final Map<String, Integer> OPENING_PHYSICAL_BOXES = Map.of(
            "SP001", 800,
            "SP002", 800,
            "SP003", 500,
            "SP004", 500,
            "SP005", 500);

    /** Don o ba trang thai nay dang giu hang trong kho. */
    private static final List<OrderStatus> HOLDING_STOCK = List.of(
            OrderStatus.APPROVED, OrderStatus.ROUTED, OrderStatus.DELIVERING);

    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final OrderRepository orderRepository;
    private final StockService stockService;

    /*
     * @Transactional bao CA HAI buoc, va day la dieu bat buoc chu khong phai cho chac an.
     *
     * Hai buoc phu thuoc nhau: buoc 1 ghi ton dau ky, buoc 2 tru phan cac don dang giu.
     * Khong co giao dich chung thi buoc 2 chet giua duong se de lai trang thai nua voi —
     * so kho da co dong nen lan khoi dong sau dieu kien "so kho rong" khong con dung,
     * seeder bo qua luon, va cac don dang mo mai mai khong duoc tru kho. Hong im lang.
     *
     * Spring goi run() tu ben ngoai nen proxy giao dich co hieu luc (tu goi ham cua chinh
     * minh thi khong).
     */
    @Override
    @Transactional
    public void run(String... args) {
        if (stockMovementRepository.count() > 0) {
            return;
        }

        int seededProducts = openPhysicalStock();
        int committedOrders = commitOrdersAlreadyHoldingStock();

        log.info("Khoi tao ton kho: ton dau ky cho {} san pham, tru kho cho {} don dang giu hang",
                seededProducts, committedOrders);
    }

    /** Buoc 1: ghi ton dau ky bang so thung vat ly trong kho. */
    private int openPhysicalStock() {
        int count = 0;

        for (Product product : productRepository.findAll()) {
            Integer physicalBoxes = OPENING_PHYSICAL_BOXES.get(product.getCode());
            if (physicalBoxes == null) {
                continue;
            }

            product.setStockBoxes(physicalBoxes);
            productRepository.save(product);

            StockMovement opening = new StockMovement();
            opening.setProduct(product);
            opening.setMovementType(StockMovementType.IN);
            opening.setBoxCount(physicalBoxes);
            opening.setStockAfter(physicalBoxes);
            opening.setNote("Ton dau ky luc bat dau theo doi ton kho");
            // createdBy de null: he thong tu ghi, khong phai nguoi dung bam nut
            opening.setCreatedAt(LocalDateTime.now());
            stockMovementRepository.save(opening);

            count++;
        }
        return count;
    }

    /**
     * Buoc 2: tru kho cho cac don da duyet nhung chua giao xong.
     *
     * Dung chinh {@link StockService#commit} ma nghiep vu that dung, khong viet duong ghi
     * rieng — nho vay don cu va don moi co dung mot lich su so kho nhu nhau.
     */
    private int commitOrdersAlreadyHoldingStock() {
        int count = 0;

        for (Order order : orderRepository.findByStatusInWithItems(HOLDING_STOCK)) {
            // userId null: he thong tu tru luc khoi tao, khong ai bam nut
            var shortage = stockService.commit(order, null);

            if (shortage.isPresent()) {
                // Khong du hang ngay luc khoi tao nghia la so ton dau ky dat qua thap so voi
                // luong don dang mo. Bao to thay vi bo qua: de im thi so lieu lech ngay tu
                // dau va khong ai biet
                log.warn("Khoi tao ton kho: don {} khong tru duoc — {}",
                        order.getCode(), shortage.get());
                continue;
            }
            count++;
        }
        return count;
    }
}
