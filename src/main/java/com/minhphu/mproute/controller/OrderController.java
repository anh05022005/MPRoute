package com.minhphu.mproute.controller;

import com.minhphu.mproute.dto.OrderForm;
import com.minhphu.mproute.dto.OrderTotals;
import com.minhphu.mproute.entity.Customer;
import com.minhphu.mproute.entity.Order;
import com.minhphu.mproute.entity.Product;
import com.minhphu.mproute.entity.enums.EntityType;
import com.minhphu.mproute.entity.enums.OrderStatus;
import com.minhphu.mproute.entity.enums.PaymentMethod;
import com.minhphu.mproute.entity.enums.RecordStatus;
import com.minhphu.mproute.repository.CustomerRepository;
import com.minhphu.mproute.repository.ProductRepository;
import com.minhphu.mproute.security.CurrentUser;
import com.minhphu.mproute.service.OrderService;
import com.minhphu.mproute.service.StatusHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * SCR-07 / SCR-08 — Danh sach va tao don hang (CN-02-01 den CN-02-06).
 *
 * Phan quyen nguoc voi cac man hinh danh muc (docs/01 — ma tran phan quyen):
 * don hang la viec cua DIEU PHOI VIEN, quan tri vien chi xem. Vi vay lop nay mo cho
 * ca hai vai tro, con cac duong GHI deu co @PreAuthorize("hasRole('DISPATCHER')").
 */
@Controller
@RequestMapping("/orders")
@PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
@RequiredArgsConstructor
public class OrderController {

    private static final int SO_DONG_MOI_TRANG = 15;

    // Bao layout.html nap Leaflet va JavaScript tinh tong cho man hinh nay
    private static final String CO_BAN_DO = "canBanDo";
    private static final String CO_JS_DON_HANG = "canJsDonHang";

    private static final String VIEW_FORM = "orders/order-form";

    // MSG-07 trong docs/01
    private static final String MSG_07 = "Không sửa/hủy được đơn đã phân tuyến.";

    /**
     * Cau bao khi don khong con sua/huy duoc nua (BR02-11).
     *
     * Don da huy cung roi vao nhanh nay nhung ly do khac han, bao nham thanh
     * "don da phan tuyen" thi nguoi dung di tim tuyen khong co that.
     */
    private String thongBaoKhongSuaDuoc(Order don) {
        return (don.getStatus() == OrderStatus.CANCELLED)
                ? "Đơn này đã hủy, không sửa hay hủy lại được."
                : MSG_07;
    }

    private final OrderService orderService;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final StatusHistoryService statusHistoryService;

    @ModelAttribute("cacTrangThaiDon")
    public OrderStatus[] cacTrangThaiDon() {
        return OrderStatus.values();
    }

    @ModelAttribute("cacHinhThucThanhToan")
    public PaymentMethod[] cacHinhThucThanhToan() {
        return PaymentMethod.values();
    }

    /**
     * Bang tra "APPROVED" -> "Đã duyệt" cho bang lich su trang thai.
     *
     * Cot status_history.new_status luu dang CHUOI chu khong phai enum, vi bang do dung
     * chung cho ca don hang lan tuyen (docs/02). Vay nen muon hien ten tieng Viet thi
     * phai tu tra bang, khong goi thang duoc .displayName.
     */
    @ModelAttribute("tenTrangThaiDon")
    public Map<String, String> tenTrangThaiDon() {
        return Arrays.stream(OrderStatus.values())
                .collect(Collectors.toMap(Enum::name, OrderStatus::getDisplayName));
    }

    // ==================== CN-02-01 Danh sach don hang ====================

    @GetMapping
    public String list(@RequestParam(required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate,
                       @RequestParam(required = false) OrderStatus status,
                       @RequestParam(required = false) Long customerId,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {

        // Don moi nhat len dau: dieu phoi vien vao man hinh nay chu yeu de xem don vua nhap
        var pageable = PageRequest.of(page, SO_DONG_MOI_TRANG,
                Sort.by("deliveryDate").descending().and(Sort.by("id").descending()));
        Page<Order> trang = orderService.search(deliveryDate, status, customerId, pageable);

        model.addAttribute("trang", trang);
        model.addAttribute("deliveryDate", deliveryDate);
        model.addAttribute("status", status);
        model.addAttribute("customerId", customerId);
        model.addAttribute("cacKhachHang", khachHangDangDung());
        return "orders/orders";
    }

    // ==================== CN-02-06 Xem mot don ====================

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Order don = orderService.getById(id);
        model.addAttribute("don", don);
        model.addAttribute("tomTat", orderService.tomTatDeDocLai(don));
        model.addAttribute("lichSu", statusHistoryService.lichSuCua(EntityType.ORDER, id));
        // Quyet dinh hien hay an ba nut Sua / Duyet / Huy. Giao dien an nut la de nhin,
        // cho chan that nam o cac duong POST ben duoi (CBR-15)
        model.addAttribute("coTheSuaHoacHuy", orderService.coTheSuaHoacHuy(don));
        model.addAttribute(CO_JS_DON_HANG, true);
        return "orders/order-detail";
    }

    // ==================== CN-02-02 Tao don hang ====================

    @PreAuthorize("hasRole('DISPATCHER')")
    @GetMapping("/create")
    public String createForm(Model model) {
        // BR02-06: ngay giao mac dinh la ngay mai — da dat san trong OrderForm
        model.addAttribute("form", new OrderForm());
        napDuLieuChoForm(model);
        return VIEW_FORM;
    }

    // ==================== CN-02-08 Sua don ====================

    @PreAuthorize("hasRole('DISPATCHER')")
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model,
                           RedirectAttributes redirectAttributes) {
        Order don = orderService.getById(id);

        /*
         * BR02-11 chan o PHIA MAY CHU (CBR-15). Nut Sua tren man hinh da bi an khi don
         * da phan tuyen, nen den duoc day nghia la co nguoi go thang dia chi vao thanh
         * dia chi — dung thu hoi dong hay lam nhat.
         */
        if (!orderService.coTheSuaHoacHuy(don)) {
            redirectAttributes.addFlashAttribute("loi", thongBaoKhongSuaDuoc(don));
            return "redirect:/orders/" + id;
        }

        model.addAttribute("form", OrderForm.from(don));
        napDuLieuChoForm(model);
        return VIEW_FORM;
    }

    /** Dung chung cho ca tao moi va sua: hai luong kiem y het nhau, chi khac cau lenh luu. */
    @PreAuthorize("hasRole('DISPATCHER')")
    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("form") OrderForm form,
                       BindingResult binding,
                       @AuthenticationPrincipal CurrentUser nguoiDung,
                       Model model,
                       RedirectAttributes redirectAttributes) {

        boolean dangSua = (form.getId() != null);

        if (dangSua) {
            Order donDangCo = orderService.getById(form.getId());
            if (!orderService.coTheSuaHoacHuy(donDangCo)) {
                redirectAttributes.addFlashAttribute("loi", thongBaoKhongSuaDuoc(donDangCo));
                return "redirect:/orders/" + form.getId();
            }
        }

        kiemQuyTacNghiepVu(form, binding);

        if (binding.hasErrors()) {
            napDuLieuChoForm(model);
            return VIEW_FORM;
        }

        Order daLuu = dangSua
                ? orderService.capNhatDon(form, nguoiDung.getId())
                : orderService.taoDon(form, nguoiDung.getId());

        redirectAttributes.addFlashAttribute("thongBao", dangSua
                ? "Đã lưu thay đổi cho đơn " + daLuu.getCode() + "."
                : "Đã tạo đơn hàng " + daLuu.getCode() + ". Đơn đang chờ duyệt.");
        // CN-02-06: ve thang trang chi tiet, o do co khoi tom tat de doc lai cho khach
        return "redirect:/orders/" + daLuu.getId();
    }

    // ==================== CN-02-07 Duyet don ====================

    @PreAuthorize("hasRole('DISPATCHER')")
    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                          @AuthenticationPrincipal CurrentUser nguoiDung,
                          RedirectAttributes redirectAttributes) {
        Order don = orderService.getById(id);

        if (don.getStatus() != OrderStatus.PENDING_APPROVAL) {
            redirectAttributes.addFlashAttribute("loi",
                    "Đơn này không ở trạng thái chờ duyệt nên không duyệt lại được.");
            return "redirect:/orders/" + id;
        }

        Order daDuyet = orderService.duyetDon(id, nguoiDung.getId());
        redirectAttributes.addFlashAttribute("thongBao",
                "Đã duyệt đơn " + daDuyet.getCode() + ". Đơn đã vào danh sách chờ phân tuyến.");
        return "redirect:/orders/" + id;
    }

    // ==================== CN-02-09 Huy don ====================

    @PreAuthorize("hasRole('DISPATCHER')")
    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id,
                         @RequestParam(required = false) String cancelReason,
                         @AuthenticationPrincipal CurrentUser nguoiDung,
                         RedirectAttributes redirectAttributes) {
        Order don = orderService.getById(id);

        if (!orderService.coTheSuaHoacHuy(don)) {
            redirectAttributes.addFlashAttribute("loi", thongBaoKhongSuaDuoc(don));
            return "redirect:/orders/" + id;
        }

        // BR02-12: ly do la bat buoc. O nhap co thuoc tinh required, nhung day moi la cho chan that
        if (cancelReason == null || cancelReason.isBlank()) {
            redirectAttributes.addFlashAttribute("loi",
                    "Hủy đơn bắt buộc nhập lý do. Đơn chưa bị hủy.");
            return "redirect:/orders/" + id;
        }
        if (cancelReason.trim().length() > 500) {
            redirectAttributes.addFlashAttribute("loi", "Lý do hủy tối đa 500 ký tự.");
            return "redirect:/orders/" + id;
        }

        Order daHuy = orderService.huyDon(id, cancelReason, nguoiDung.getId());
        redirectAttributes.addFlashAttribute("thongBao",
                "Đã hủy đơn " + daHuy.getCode() + ". Đơn vẫn được giữ lại để tra cứu.");
        return "redirect:/orders/" + id;
    }

    // ==================== Kiem quy tac nghiep vu ====================

    /**
     * Nhung quy tac Bean Validation khong dien ta duoc, phai kiem bang tay.
     *
     * Tat ca deu kiem o PHIA MAY CHU (CBR-15): giao dien da chan san bang JavaScript
     * va thuoc tinh min cua o nhap, nhung nguoi gui du lieu thang bang cong cu ngoai
     * trinh duyet thi nhung cai do khong con tac dung.
     */
    private void kiemQuyTacNghiepVu(OrderForm form, BindingResult binding) {

        // BR02-06: ngay giao khong duoc nho hon hom nay
        if (form.getDeliveryDate() != null && form.getDeliveryDate().isBefore(LocalDate.now())) {
            binding.rejectValue("deliveryDate", "ngayGiaoQuaKhu",
                    "Ngày giao không được nhỏ hơn hôm nay.");
        }

        long soDongHopLe = form.getItems().stream()
                .filter(dong -> !dong.laDongTrong())
                .count();

        // Dong da chon san pham thi so thung bat buoc > 0 — bo qua im lang cho nay
        // se lam don thieu hang ma nguoi nhap khong biet
        boolean coDongSaiSoThung = form.getItems().stream()
                .anyMatch(dong -> dong.getProductId() != null
                        && (dong.getBoxCount() == null || dong.getBoxCount() <= 0));

        if (coDongSaiSoThung) {
            binding.reject("soThungKhongHopLe",
                    "Có dòng đã chọn sản phẩm nhưng số thùng để trống hoặc không lớn hơn 0.");

        } else if (soDongHopLe == 0) {
            // BR02-02: it nhat mot dong chi tiet. Chi bao cau nay khi khong co dong nao
            // dang go do — neu khong nguoi dung nhan hai cau loi cho cung mot cai sai
            binding.reject("thieuDongChiTiet",
                    "Đơn hàng phải có ít nhất một dòng chi tiết. "
                            + "Vui lòng chọn sản phẩm và nhập số thùng lớn hơn 0.");
        }

        if (form.getCustomerId() != null) {
            kiemGhiDeDiaChi(form, binding);
        }

        // BR02-05: chan don khong xe nao cho noi. Kiem sau cung vi phai co dong chi tiet
        // hop le thi tinh tong moi co nghia
        if (soDongHopLe > 0 && !coDongSaiSoThung) {
            OrderTotals tong = orderService.tinhTong(form);
            orderService.kiemSucChua(tong)
                    .ifPresent(thongBao -> binding.reject("vuotSucChua", thongBao));
        }
    }

    /**
     * BR02-07 / BR02-08 — ghi de dia chi cho rieng don thi phai xac dinh lai toa do.
     *
     * Ly do: dia chi chi la chuoi chu, THUAT TOAN chi doc toa do. Doi chu ma giu nguyen
     * toa do cu thi don van duoc giao ve dung dia chi cu, ma man hinh lai hien dia chi moi —
     * sai kieu nguy hiem nhat vi khong ai nhin ra.
     */
    private void kiemGhiDeDiaChi(OrderForm form, BindingResult binding) {
        Customer khach = customerRepository.findById(form.getCustomerId()).orElse(null);
        if (khach == null) {
            binding.rejectValue("customerId", "khongCoKhach", "Không tìm thấy khách hàng đã chọn.");
            return;
        }

        boolean daSuaDiaChi = form.getDeliveryAddress() != null
                && !form.getDeliveryAddress().trim().equalsIgnoreCase(khach.getAddress().trim());
        boolean giuNguyenToaDo = Objects.equals(form.getLatitude(), khach.getLatitude())
                && Objects.equals(form.getLongitude(), khach.getLongitude());

        if (daSuaDiaChi && giuNguyenToaDo) {
            binding.rejectValue("deliveryAddress", "thieuToaDoMoi",
                    "Đã sửa địa chỉ giao cho riêng đơn này thì phải xác định lại tọa độ: "
                            + "bấm \"Tìm tọa độ từ địa chỉ\" hoặc ghim thẳng lên bản đồ.");
        }
    }

    // ==================== Ham dung chung ====================

    /** Do danh sach khach, san pham va cac co bao layout nap thu vien vao model. */
    private void napDuLieuChoForm(Model model) {
        model.addAttribute("cacKhachHang", khachHangDangDung());
        model.addAttribute("cacSanPham", sanPhamDangDung());
        model.addAttribute(CO_BAN_DO, true);
        model.addAttribute(CO_JS_DON_HANG, true);
    }

    // Chi khach dang su dung moi hien trong o chon (CBR-17: khach ngung dung van con
    // trong CSDL de don cu khong mat thong tin, nhung khong dat don moi duoc nua)
    private List<Customer> khachHangDangDung() {
        return customerRepository.findByStatus(RecordStatus.ACTIVE).stream()
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .toList();
    }

    private List<Product> sanPhamDangDung() {
        return productRepository.findByStatus(RecordStatus.ACTIVE).stream()
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .toList();
    }
}
