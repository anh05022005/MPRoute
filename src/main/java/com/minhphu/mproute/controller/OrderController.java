package com.minhphu.mproute.controller;

import com.minhphu.mproute.dto.CustomerForm;
import com.minhphu.mproute.dto.OrderForm;
import com.minhphu.mproute.dto.OrderItemForm;
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
import com.minhphu.mproute.service.CustomerService;
import com.minhphu.mproute.service.OrderService;
import com.minhphu.mproute.service.StatusHistoryService;
import com.minhphu.mproute.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    private static final int PAGE_SIZE = 15;

    // Bao layout.html nap JavaScript tinh tong cho man hinh nay
    private static final String NEEDS_ORDER_JS = "needsOrderJs";

    private static final String VIEW_FORM = "orders/order-form";

    // MSG-07 trong docs/01
    private static final String MSG_07 = "Không sửa/hủy được đơn đã phân tuyến.";

    /**
     * Cau bao khi don khong con sua/huy duoc nua (BR02-11).
     *
     * Don da huy cung roi vao nhanh nay nhung ly do khac han, bao nham thanh
     * "don da phan tuyen" thi nguoi dung di tim tuyen khong co that.
     */
    private String notEditableMessage(Order order) {
        return (order.getStatus() == OrderStatus.CANCELLED)
                ? "Đơn này đã hủy, không sửa hay hủy lại được."
                : MSG_07;
    }

    private final OrderService orderService;
    private final CustomerService customerService;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final StatusHistoryService statusHistoryService;
    private final StockService stockService;

    @ModelAttribute("orderStatuses")
    public OrderStatus[] orderStatuses() {
        return OrderStatus.values();
    }

    @ModelAttribute("paymentMethods")
    public PaymentMethod[] paymentMethods() {
        return PaymentMethod.values();
    }

    /**
     * Bang tra "APPROVED" -> "Đã duyệt" cho bang lich su trang thai.
     *
     * Cot status_history.new_status luu dang CHUOI chu khong phai enum, vi bang do dung
     * chung cho ca don hang lan tuyen (docs/02). Vay nen muon hien ten tieng Viet thi
     * phai tu tra bang, khong goi thang duoc .displayName.
     */
    @ModelAttribute("orderStatusLabels")
    public Map<String, String> orderStatusLabels() {
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
        var pageable = PageRequest.of(page, PAGE_SIZE,
                Sort.by("deliveryDate").descending().and(Sort.by("id").descending()));
        Page<Order> pageResult = orderService.search(deliveryDate, status, customerId, pageable);

        model.addAttribute("pageResult", pageResult);
        model.addAttribute("deliveryDate", deliveryDate);
        model.addAttribute("status", status);
        model.addAttribute("customerId", customerId);
        model.addAttribute("customers", activeCustomers());
        return "orders/orders";
    }

    // ==================== CN-02-06 Xem mot don ====================

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Order order = orderService.getById(id);
        model.addAttribute("order", order);
        model.addAttribute("readBackSummary", orderService.buildReadBackSummary(order));
        model.addAttribute("statusHistory", statusHistoryService.historyOf(EntityType.ORDER, id));
        // Quyet dinh hien hay an ba nut Sua / Duyet / Huy. Giao dien an nut la de nhin,
        // cho chan that nam o cac duong POST ben duoi (CBR-15)
        model.addAttribute("canEditOrCancel", orderService.canEditOrCancel(order));
        model.addAttribute(NEEDS_ORDER_JS, true);
        return "orders/order-detail";
    }

    // ==================== CN-02-02 Tao don hang ====================

    @PreAuthorize("hasRole('DISPATCHER')")
    @GetMapping("/create")
    public String createForm(Model model) {
        // BR02-06: ngay giao mac dinh la ngay mai — da dat san trong OrderForm
        model.addAttribute("form", new OrderForm());
        loadFormData(model);
        return VIEW_FORM;
    }

    // ==================== CN-02-08 Sua don ====================

    @PreAuthorize("hasRole('DISPATCHER')")
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model,
                           RedirectAttributes redirectAttributes) {
        Order order = orderService.getById(id);

        /*
         * BR02-11 chan o PHIA MAY CHU (CBR-15). Nut Sua tren man hinh da bi an khi don
         * da phan tuyen, nen den duoc day nghia la co nguoi go thang dia chi vao thanh
         * dia chi — dung thu hoi dong hay lam nhat.
         */
        if (!orderService.canEditOrCancel(order)) {
            redirectAttributes.addFlashAttribute("errorMessage", notEditableMessage(order));
            return "redirect:/orders/" + id;
        }

        model.addAttribute("form", OrderForm.from(order));
        loadFormData(model);
        return VIEW_FORM;
    }

    /** Dung chung cho ca tao moi va sua: hai luong kiem y het nhau, chi khac cau lenh luu. */
    @PreAuthorize("hasRole('DISPATCHER')")
    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("form") OrderForm form,
                       BindingResult binding,
                       @AuthenticationPrincipal CurrentUser currentUser,
                       Model model,
                       RedirectAttributes redirectAttributes) {

        boolean editing = (form.getId() != null);

        if (editing) {
            Order existingOrder = orderService.getById(form.getId());
            if (!orderService.canEditOrCancel(existingOrder)) {
                redirectAttributes.addFlashAttribute("errorMessage", notEditableMessage(existingOrder));
                return "redirect:/orders/" + form.getId();
            }
        }

        validateBusinessRules(form, binding);

        if (binding.hasErrors()) {
            loadFormData(model);
            return VIEW_FORM;
        }

        Order saved = editing
                ? orderService.updateOrder(form, currentUser.getId())
                : orderService.createOrder(form, currentUser.getId());

        redirectAttributes.addFlashAttribute("successMessage", editing
                ? "Đã lưu thay đổi cho đơn " + saved.getCode() + "."
                : "Đã tạo đơn hàng " + saved.getCode() + ". Đơn đang chờ duyệt.");
        // CN-02-06: ve thang trang chi tiet, o do co khoi tom tat de doc lai cho khach
        return "redirect:/orders/" + saved.getId();
    }

    // ==================== CN-02-07 Duyet don ====================

    @PreAuthorize("hasRole('DISPATCHER')")
    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                          @AuthenticationPrincipal CurrentUser currentUser,
                          RedirectAttributes redirectAttributes) {
        Order order = orderService.getById(id);

        if (order.getStatus() != OrderStatus.PENDING_APPROVAL) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Đơn này không ở trạng thái chờ duyệt nên không duyệt lại được.");
            return "redirect:/orders/" + id;
        }

        /*
         * MSG-17 — khong du hang trong kho thi khong duyet.
         *
         * Chan o day de nguoi dung nhan mot cau thong bao tu te thay vi trang 500.
         * approveOrder() cung tu kiem lai va nem loi: hai lop, giong cach lam voi phan
         * quyen (CBR-15), sua nham mot ben thi ben con lai van giu.
         *
         * Day moi la cho chan cung. Luc TAO don chi canh bao, vi don PENDING_APPROVAL
         * chua giu hang — khach goi dat truoc trong khi xuong san xuat con dang chay la
         * chuyen binh thuong, chan ngay luc do thi khong ghi noi don.
         */
        Optional<String> shortage = stockService.checkAvailability(order);
        if (shortage.isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", shortage.get());
            return "redirect:/orders/" + id;
        }

        Order approved = orderService.approveOrder(id, currentUser.getId());
        redirectAttributes.addFlashAttribute("successMessage",
                "Đã duyệt đơn " + approved.getCode() + ". Đơn đã vào danh sách chờ phân tuyến.");
        return "redirect:/orders/" + id;
    }

    // ==================== CN-02-09 Huy don ====================

    @PreAuthorize("hasRole('DISPATCHER')")
    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id,
                         @RequestParam(required = false) String cancelReason,
                         @AuthenticationPrincipal CurrentUser currentUser,
                         RedirectAttributes redirectAttributes) {
        Order order = orderService.getById(id);

        if (!orderService.canEditOrCancel(order)) {
            redirectAttributes.addFlashAttribute("errorMessage", notEditableMessage(order));
            return "redirect:/orders/" + id;
        }

        // BR02-12: ly do la bat buoc. O nhap co thuoc tinh required, nhung day moi la cho chan that
        if (cancelReason == null || cancelReason.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Hủy đơn bắt buộc nhập lý do. Đơn chưa bị hủy.");
            return "redirect:/orders/" + id;
        }
        if (cancelReason.trim().length() > 500) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lý do hủy tối đa 500 ký tự.");
            return "redirect:/orders/" + id;
        }

        Order cancelled = orderService.cancelOrder(id, cancelReason, currentUser.getId());
        redirectAttributes.addFlashAttribute("successMessage",
                "Đã hủy đơn " + cancelled.getCode() + ". Đơn vẫn được giữ lại để tra cứu.");
        return "redirect:/orders/" + id;
    }

    // ==================== CN-02-10 Them khach ngay trong man tao don ====================

    /**
     * Them mot khach hang moi tu hop thoai tren man hinh tao don.
     *
     * NGOAI LE CO CHU Y CUA MA TRAN PHAN QUYEN (docs/01: danh muc la "chi xem" voi dieu
     * phoi vien). Ly do: khach goi dat hang lan dau thi don dang go do, khong the bat ho
     * cho toi khi quan tri vien ranh de them khach. Xem docs/09 muc A4.
     *
     * Ngoai le duoc bo HEP HET MUC:
     *   - chi THEM MOI, khong sua: id bi ep ve null truoc khi luu, gui id len khong an thua
     *   - chi qua duong nay, dieu phoi vien van khong vao duoc /admin/customers de sua hay
     *     ngung dung khach dang co
     *   - trang thai ep ACTIVE, khong cho tu chon
     *
     * Tra JSON chu khong tra ten view: man hinh dang go do don, tai lai trang la mat het.
     */
    @PreAuthorize("hasRole('DISPATCHER')")
    @PostMapping("/quick-customer")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> quickAddCustomer(
            @Valid @ModelAttribute CustomerForm form, BindingResult binding) {

        if (binding.hasErrors()) {
            // Gom loi theo ten o nhap de JavaScript to do dung o do trong hop thoai
            Map<String, String> fieldErrors = new HashMap<>();
            binding.getFieldErrors()
                    .forEach(fe -> fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
            return ResponseEntity.badRequest().body(Map.of("errorMessage", fieldErrors));
        }

        form.setId(null);
        form.setStatus(RecordStatus.ACTIVE);

        // Trung so dien thoai chi CANH BAO, khong chan — giong CN-01-04, vi thuc te
        // hai cua hang cung mot chu dung chung mot so
        boolean phoneTaken = customerService.isPhoneTaken(form.getPhone(), null);
        Customer saved = customerService.save(form);

        // LinkedHashMap de giu thu tu truong, doc JSON tra ve trong tab Network cho de
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", saved.getId());
        payload.put("name", saved.getName());
        payload.put("phone", saved.getPhone());
        payload.put("address", saved.getAddress());
        payload.put("latitude", saved.getLatitude());
        payload.put("longitude", saved.getLongitude());
        payload.put("paymentMethod", saved.getPaymentMethod().name());
        payload.put("deliveryNote", saved.getDeliveryNote());
        if (phoneTaken) {
            payload.put("warning", "Số điện thoại này đã dùng cho một khách hàng khác. "
                    + "Khách vẫn được thêm, nhưng trang tra cứu công khai sẽ khó phân biệt.");
        }
        return ResponseEntity.ok(payload);
    }

    // ==================== Kiem quy tac nghiep vu ====================

    /**
     * Nhung quy tac Bean Validation khong dien ta duoc, phai kiem bang tay.
     *
     * Tat ca deu kiem o PHIA MAY CHU (CBR-15): giao dien da chan san bang JavaScript
     * va thuoc tinh min cua o nhap, nhung nguoi gui du lieu thang bang cong cu ngoai
     * trinh duyet thi nhung cai do khong con tac dung.
     */
    private void validateBusinessRules(OrderForm form, BindingResult binding) {

        // BR02-06: ngay giao khong duoc nho hon hom nay
        if (form.getDeliveryDate() != null && form.getDeliveryDate().isBefore(LocalDate.now())) {
            binding.rejectValue("deliveryDate", "deliveryDateInPast",
                    "Ngày giao không được nhỏ hơn hôm nay.");
        }

        long validLineCount = form.getItems().stream()
                .filter(line -> !line.isBlank())
                .count();

        // Dong da chon san pham thi so thung bat buoc > 0 — bo qua im lang cho nay
        // se lam don thieu hang ma nguoi nhap khong biet
        boolean hasInvalidBoxCount = form.getItems().stream()
                .anyMatch(line -> line.getProductId() != null
                        && (line.getBoxCount() == null || line.getBoxCount() <= 0));

        if (hasInvalidBoxCount) {
            binding.reject("invalidBoxCount",
                    "Có dòng đã chọn sản phẩm nhưng số thùng để trống hoặc không lớn hơn 0.");

        } else if (validLineCount == 0) {
            // BR02-02: it nhat mot dong chi tiet. Chi bao cau nay khi khong co dong nao
            // dang go do — neu khong nguoi dung nhan hai cau loi cho cung mot cai sai
            binding.reject("noOrderLines",
                    "Đơn hàng phải có ít nhất một dòng chi tiết. "
                            + "Vui lòng chọn sản phẩm và nhập số thùng lớn hơn 0.");
        }

        if (form.getCustomerId() != null && !customerRepository.existsById(form.getCustomerId())) {
            binding.rejectValue("customerId", "customerNotFound", "Không tìm thấy khách hàng đã chọn.");
        }

        // BR02-05: chan don khong xe nao cho noi. Kiem sau cung vi phai co dong chi tiet
        // hop le thi tinh tong moi co nghia
        if (validLineCount > 0 && !hasInvalidBoxCount) {
            OrderTotals totals = orderService.calculateTotals(form);
            orderService.checkCapacity(totals)
                    .ifPresent(message -> binding.reject("capacityExceeded", message));

            validateStockForApprovedOrder(form, binding);
        }
    }

    /**
     * MSG-17 khi SUA mot don DA DUYET: don dang giu hang, tang so thung len thi phai co
     * du hang cho phan tang them.
     *
     * Chi kiem voi don da duyet. Don con cho duyet chua giu hang nen sua thoai mai, phan
     * chan cung nam o buoc bam Duyet don.
     *
     * Kiem o day chu khong de updateOrder() nem loi, de nguoi dung nhan loi ngay tren form
     * va khong mat nhung gi vua go.
     */
    private void validateStockForApprovedOrder(OrderForm form, BindingResult binding) {
        if (form.getId() == null) {
            return;
        }

        Order existing = orderService.getById(form.getId());
        if (existing.getStatus() != OrderStatus.APPROVED) {
            return;
        }

        // Gop theo san pham: chon cung mot san pham o hai dong la hop le, khong gop thi
        // dong sau ghi de dong truoc va phep kiem bo sot phan chenh
        Map<Long, Integer> requested = new LinkedHashMap<>();
        for (OrderItemForm line : form.getItems()) {
            if (!line.isBlank()) {
                requested.merge(line.getProductId(), line.getBoxCount(), Integer::sum);
            }
        }

        stockService.checkAvailability(form.getId(), requested)
                .ifPresent(message -> binding.reject("stockShortage", message));
    }

    // ==================== Ham dung chung ====================

    /** Do danh sach khach, san pham va cac co bao layout nap thu vien vao model. */
    private void loadFormData(Model model) {
        model.addAttribute("customers", activeCustomers());
        model.addAttribute("products", activeProducts());
        // So thung con ranh theo productId, de form hien ngay canh o chon san pham —
        // dieu phoi vien dang nghe dien thoai can tra loi "hang con khong" tai cho
        model.addAttribute("availableStock", stockService.availableBoxesByProduct());
        model.addAttribute(NEEDS_ORDER_JS, true);
    }

    // Chi khach dang su dung moi hien trong o chon (CBR-17: khach ngung dung van con
    // trong CSDL de don cu khong mat thong tin, nhung khong dat don moi duoc nua)
    private List<Customer> activeCustomers() {
        return customerRepository.findByStatus(RecordStatus.ACTIVE).stream()
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .toList();
    }

    private List<Product> activeProducts() {
        return productRepository.findByStatus(RecordStatus.ACTIVE).stream()
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .toList();
    }
}
