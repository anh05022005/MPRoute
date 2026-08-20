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
    public String sinhMaDon(LocalDate ngayGiao) {
        String tienTo = "MP" + ngayGiao.format(DateTimeFormatter.ofPattern("yyMMdd"));
        int thuTu = (int) orderRepository.countByDeliveryDate(ngayGiao) + 1;

        while (thuTu <= 9999) {
            String ma = tienTo + String.format("%04d", thuTu);
            if (orderRepository.findByCode(ma).isEmpty()) {
                return ma;
            }
            thuTu++;
        }
        // 9999 don mot ngay la khong tuong voi quy mo Minh Phu, nhung khong de im lang
        throw new IllegalStateException("Đã hết số thứ tự mã đơn cho ngày " + ngayGiao);
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
    public OrderTotals tinhTong(OrderForm form) {
        double kg = 0;
        double m3 = 0;
        BigDecimal tien = BigDecimal.ZERO;

        for (OrderItemForm dong : form.getItems()) {
            if (dong.laDongTrong()) {
                continue;
            }
            Product sp = laySanPham(dong.getProductId());
            kg += sp.getWeightPerBox() * dong.getBoxCount();
            m3 += sp.getVolumePerBox() * dong.getBoxCount();
            tien = tien.add(sp.getUnitPrice().multiply(BigDecimal.valueOf(dong.getBoxCount())));
        }

        // Lam tron dung so chu so thap phan cua cot trong CSDL (DECIMAL(10,2) va DECIMAL(10,3))
        // de con so hien tren man hinh khop het voi con so luu xuong
        return new OrderTotals(lamTron(kg, 2), lamTron(m3, 3), tien);
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
    public Optional<String> kiemSucChua(OrderTotals tong) {
        List<Vehicle> doiXe = vehicleRepository.findAll();

        if (doiXe.isEmpty()) {
            return Optional.of("Chưa khai báo phương tiện nào, không kiểm được sức chứa. "
                    + "Vui lòng thêm xe trong danh mục phương tiện trước.");
        }

        boolean coXeChoDuoc = doiXe.stream()
                .anyMatch(xe -> xe.getMaxWeight() >= tong.khoiLuong()
                             && xe.getMaxVolume() >= tong.theTich());
        if (coXeChoDuoc) {
            return Optional.empty();
        }

        // Lay xe tai trong lon nhat lam moc de bao cho de hieu: don da khong vua xe nay
        // thi cung khong vua xe nao
        Vehicle xeLonNhat = doiXe.stream()
                .max(Comparator.comparingDouble(Vehicle::getMaxWeight))
                .orElseThrow();

        return Optional.of(String.format(
                "Đơn hàng vượt sức chứa xe lớn nhất (%.2f kg / %.3f m³ so với %.2f kg / %.3f m³ "
                        + "của xe %s). Vui lòng tách đơn.",
                tong.khoiLuong(), tong.theTich(),
                xeLonNhat.getMaxWeight(), xeLonNhat.getMaxVolume(), xeLonNhat.getLicensePlate()));
    }

    // ==================== Tao don ====================

    /**
     * Tao don moi. Don luon vao he thong o trang thai PENDING_APPROVAL (BR02-09),
     * phai bam nut "Duyet don" rieng moi sang APPROVED — buoc do de soat lai
     * dia chi va toa do truoc khi don di vao thuat toan (BR02-10).
     */
    @Transactional
    public Order taoDon(OrderForm form, Long nguoiTaoId) {
        Customer khach = customerRepository.findById(form.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng"));

        Order don = new Order();
        don.setCode(sinhMaDon(form.getDeliveryDate()));
        don.setCustomer(khach);
        don.setDeliveryDate(form.getDeliveryDate());
        don.setDeliveryAddress(form.getDeliveryAddress().trim());
        don.setLatitude(form.getLatitude());
        don.setLongitude(form.getLongitude());
        don.setPaymentMethod(form.getPaymentMethod());
        don.setPaymentStatus(PaymentStatus.UNPAID);
        don.setStatus(OrderStatus.PENDING_APPROVAL);
        don.setNote(chuoiHoacNull(form.getNote()));

        if (nguoiTaoId != null) {
            don.setCreatedBy(userRepository.findById(nguoiTaoId).orElse(null));
        }

        for (OrderItem dong : dungCacDongChiTiet(form)) {
            don.addItem(dong);
        }
        apTongVaSoTienPhaiThu(don);

        Order daLuu = orderRepository.save(don);

        // CBR-14: moi lan chuyen trang thai deu ghi lai, ke ca lan dau tien
        statusHistoryService.ghi(EntityType.ORDER, daLuu.getId(),
                null, OrderStatus.PENDING_APPROVAL.name(), nguoiTaoId, "Tạo đơn hàng");

        log.info("Tao don {} cho khach {}: {} dong, {} kg, {} m3, {} d",
                daLuu.getCode(), khach.getName(), daLuu.getItems().size(),
                daLuu.getTotalWeight(), daLuu.getTotalVolume(), daLuu.getTotalAmount());
        return daLuu;
    }

    // ==================== CN-02-08 Sua don ====================

    /**
     * BR02-11 — chi don chua di vao thuat toan moi con sua hoac huy duoc.
     *
     * Tu ROUTED tro di, don da nam trong mot tuyen: doi so thung la doi khoi luong tuyen,
     * doi dia chi la doi ca thu tu diem dung ma tuyen thi da chot va tai xe co the dang
     * tren duong. Muon doi thi phai huy tuyen roi phan lai.
     */
    public boolean coTheSuaHoacHuy(Order don) {
        return don.getStatus() == OrderStatus.PENDING_APPROVAL
                || don.getStatus() == OrderStatus.APPROVED;
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
    public Order capNhatDon(OrderForm form, Long nguoiSuaId) {
        Order don = getById(form.getId());

        // Luoi an toan: Controller da chan roi, nhung ham nay cung phai tu bao ve minh
        if (!coTheSuaHoacHuy(don)) {
            throw new IllegalStateException("Không sửa được đơn ở trạng thái " + don.getStatus());
        }

        Customer khach = customerRepository.findById(form.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng"));

        don.setCustomer(khach);
        don.setDeliveryDate(form.getDeliveryDate());
        don.setDeliveryAddress(form.getDeliveryAddress().trim());
        don.setLatitude(form.getLatitude());
        don.setLongitude(form.getLongitude());
        don.setPaymentMethod(form.getPaymentMethod());
        don.setNote(chuoiHoacNull(form.getNote()));

        /*
         * Dung .clear() tren chinh danh sach dang co chu KHONG setItems(danh sach moi):
         * orphanRemoval bam vao dung the hien danh sach ma Hibernate dang theo doi,
         * thay bang danh sach khac thi cac dong cu khong bi xoa, don thanh ra cong don
         * hang cu va hang moi (BR02-03).
         */
        don.getItems().clear();
        for (OrderItem dong : dungCacDongChiTiet(form)) {
            don.addItem(dong);
        }
        apTongVaSoTienPhaiThu(don);

        Order daLuu = orderRepository.save(don);
        log.info("Sua don {} (nguoi sua #{}): {} dong, {} kg, {} m3, {} d",
                daLuu.getCode(), nguoiSuaId, daLuu.getItems().size(),
                daLuu.getTotalWeight(), daLuu.getTotalVolume(), daLuu.getTotalAmount());
        return daLuu;
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
    public Order duyetDon(Long id, Long nguoiDuyetId) {
        Order don = getById(id);

        if (don.getStatus() != OrderStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Chỉ duyệt được đơn đang chờ duyệt");
        }

        don.setStatus(OrderStatus.APPROVED);
        Order daLuu = orderRepository.save(don);

        statusHistoryService.ghi(EntityType.ORDER, daLuu.getId(),
                OrderStatus.PENDING_APPROVAL.name(), OrderStatus.APPROVED.name(),
                nguoiDuyetId, "Duyệt đơn");

        log.info("Duyet don {}", daLuu.getCode());
        return daLuu;
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
    public Order huyDon(Long id, String lyDo, Long nguoiHuyId) {
        Order don = getById(id);

        if (!coTheSuaHoacHuy(don)) {
            throw new IllegalStateException("Không hủy được đơn ở trạng thái " + don.getStatus());
        }
        if (lyDo == null || lyDo.isBlank()) {
            throw new IllegalArgumentException("Hủy đơn bắt buộc nhập lý do");
        }

        String trangThaiCu = don.getStatus().name();
        don.setStatus(OrderStatus.CANCELLED);
        don.setCancelReason(lyDo.trim());
        Order daLuu = orderRepository.save(don);

        statusHistoryService.ghi(EntityType.ORDER, daLuu.getId(),
                trangThaiCu, OrderStatus.CANCELLED.name(), nguoiHuyId, lyDo.trim());

        log.info("Huy don {}: {}", daLuu.getCode(), lyDo.trim());
        return daLuu;
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
    public String tomTatDeDocLai(Order don) {
        StringBuilder tomTat = new StringBuilder();
        tomTat.append("Đơn hàng ").append(don.getCode()).append('\n');
        tomTat.append("Khách: ").append(don.getCustomer().getName())
                .append(" — ").append(don.getCustomer().getPhone()).append('\n');
        tomTat.append("Giao tới: ").append(don.getDeliveryAddress()).append('\n');
        tomTat.append("Ngày giao: ")
                .append(don.getDeliveryDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .append('\n');

        tomTat.append("Hàng:\n");
        for (OrderItem dong : don.getItems()) {
            tomTat.append(" - ").append(dong.getProduct().getName())
                    .append(": ").append(dong.getBoxCount())
                    .append(' ').append(dong.getProduct().getUnit()).append('\n');
        }

        tomTat.append("Tiền hàng: ").append(dinhDangTien(don.getTotalAmount())).append('\n');
        tomTat.append("Thanh toán: ").append(don.getPaymentMethod().getDisplayName()).append('\n');
        tomTat.append("Phải thu khi giao: ").append(dinhDangTien(don.getAmountToCollect()));
        return tomTat.toString();
    }

    /** Tien kieu Viet Nam: dau cham ngan nghin, khong phan thap phan. Vi du 3.720.000d */
    private String dinhDangTien(BigDecimal soTien) {
        DecimalFormatSymbols kyHieu = new DecimalFormatSymbols(Locale.forLanguageTag("vi-VN"));
        kyHieu.setGroupingSeparator('.');
        return new DecimalFormat("#,##0", kyHieu).format(soTien) + "đ";
    }

    // ==================== Ham dung chung ====================

    /**
     * Dung cac dong chi tiet tu form, CHEP gia va thong so tu danh muc (BR02-13).
     *
     * Chep chu khong tham chieu: neu chi luu product_id roi luc hien don moi tra sang
     * bang products lay gia, thi quan tri vien doi gia hom sau la don hang thang truoc
     * tu doi tien theo. Do la loi nghiep vu nghiem trong, hoi dong hay hoi dung cho nay.
     */
    private List<OrderItem> dungCacDongChiTiet(OrderForm form) {
        List<OrderItem> ketQua = new ArrayList<>();

        for (OrderItemForm dong : form.getItems()) {
            if (dong.laDongTrong()) {
                continue;   // dong de trong tren man hinh, bo qua
            }
            Product sp = laySanPham(dong.getProductId());
            int soThung = dong.getBoxCount();

            OrderItem item = new OrderItem();
            item.setProduct(sp);
            item.setBoxCount(soThung);
            item.setLineWeight(lamTron(sp.getWeightPerBox() * soThung, 2));
            item.setLineVolume(lamTron(sp.getVolumePerBox() * soThung, 3));
            item.setUnitPrice(sp.getUnitPrice());
            item.setLineTotal(sp.getUnitPrice().multiply(BigDecimal.valueOf(soThung)));
            ketQua.add(item);
        }
        return ketQua;
    }

    /**
     * Cong don ba con so tong tu cac dong DA GAN vao don, roi tinh so tien phai thu.
     *
     * BR02-14: chi don tien mat moi phai thu tien khi giao. Cong no va chuyen khoan
     * truoc deu bang 0 — tai xe cam to phieu ghi 0 dong thi khong thu nham cua khach.
     * BR02-16: doi hinh thuc thanh toan la con so nay tinh lai ngay, vi no duoc tinh
     * o day chu khong luu roi de do.
     */
    private void apTongVaSoTienPhaiThu(Order don) {
        double kg = 0;
        double m3 = 0;
        BigDecimal tien = BigDecimal.ZERO;

        for (OrderItem dong : don.getItems()) {
            kg += dong.getLineWeight();
            m3 += dong.getLineVolume();
            tien = tien.add(dong.getLineTotal());
        }

        don.setTotalWeight(lamTron(kg, 2));
        don.setTotalVolume(lamTron(m3, 3));
        don.setTotalAmount(tien);
        don.setAmountToCollect(don.getPaymentMethod() == PaymentMethod.CASH
                ? tien
                : BigDecimal.ZERO);
    }

    private Product laySanPham(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm #" + id));
    }

    /** Lam tron ve dung so chu so thap phan cua cot trong CSDL. */
    private double lamTron(double giaTri, int soChuSo) {
        return BigDecimal.valueOf(giaTri).setScale(soChuSo, RoundingMode.HALF_UP).doubleValue();
    }

    private String chuoiHoacNull(String giaTri) {
        return (giaTri == null || giaTri.isBlank()) ? null : giaTri.trim();
    }
}
