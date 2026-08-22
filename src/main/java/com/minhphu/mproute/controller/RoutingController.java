package com.minhphu.mproute.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minhphu.mproute.entity.Depot;
import com.minhphu.mproute.entity.Order;
import com.minhphu.mproute.entity.enums.OrderStatus;
import com.minhphu.mproute.repository.OrderRepository;
import com.minhphu.mproute.service.DepotService;
import com.minhphu.mproute.service.optimization.DistanceMatrixService;
import com.minhphu.mproute.service.optimization.OptimizationPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * SCR-09 — Man hinh phan tuyen (CN-03-01, CN-03-02, CN-03-05).
 *
 * Tuan 5 moi lam ba phan dau: chon ngay, hien don APPROVED va cac diem giao len ban do,
 * va nut chuan bi ma tran khoang cach. Phan chay thuat toan (CN-03-06 tro di) lam o
 * tuan 8, sau khi thuat toan da chay duoc bang main() o tuan 6-7.
 *
 * Phan quyen giong man hinh don hang: dieu phoi vien toan quyen, quan tri vien chi xem
 * (docs/01 — ma tran phan quyen).
 */
@Controller
@RequestMapping("/routing")
@PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
@RequiredArgsConstructor
@Slf4j
public class RoutingController {

    private static final String CO_BAN_DO = "canBanDo";
    private static final String CO_JS_PHAN_TUYEN = "canJsPhanTuyen";

    private final OrderRepository orderRepository;
    private final DepotService depotService;
    private final DistanceMatrixService distanceMatrixService;
    private final ObjectMapper objectMapper;

    /** Mot ghim tren ban do. Chi chua thu can ve, khong day ca entity ra giao dien. */
    public record DiemTrenBanDo(String ten,
                                double viDo,
                                double kinhDo,
                                boolean laKho,
                                String moTa) {}

    @GetMapping
    public String screen(@RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate,
                         Model model) {

        // Mac dinh xem ngay mai — dung ngay ma don duoc tao san (BR02-06)
        LocalDate ngayGiao = (deliveryDate != null) ? deliveryDate : LocalDate.now().plusDays(1);

        // BR03-01: chi don DA DUYET moi duoc dua vao phan tuyen
        List<Order> cacDon = orderRepository.findByDeliveryDateAndStatus(ngayGiao, OrderStatus.APPROVED);

        double tongKg = cacDon.stream().mapToDouble(Order::getTotalWeight).sum();
        double tongM3 = cacDon.stream().mapToDouble(Order::getTotalVolume).sum();

        model.addAttribute("ngayGiao", ngayGiao);
        model.addAttribute("cacDon", cacDon);
        model.addAttribute("tongKg", tongKg);
        model.addAttribute("tongM3", tongM3);

        Depot kho = layKho(model);
        model.addAttribute("kho", kho);
        model.addAttribute("diemJson", dungJsonDiem(kho, cacDon));

        model.addAttribute(CO_BAN_DO, true);
        model.addAttribute(CO_JS_PHAN_TUYEN, true);
        return "routing/routing";
    }

    /**
     * CN-03-05 — nap truoc ma tran khoang cach cho tap diem cua ngay dang xem.
     *
     * Bam lan hai voi cung tap diem thi so o "lay moi" phai bang 0 va khong co dong
     * log nao goi OSRM — day chinh la cach chung minh bo nho dem hoat dong (CBR-12).
     */
    @PreAuthorize("hasRole('DISPATCHER')")
    @PostMapping("/prepare-matrix")
    public String prepareMatrix(@RequestParam
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate,
                                RedirectAttributes redirectAttributes) {

        List<Order> cacDon = orderRepository.findByDeliveryDateAndStatus(deliveryDate, OrderStatus.APPROVED);
        if (cacDon.isEmpty()) {
            redirectAttributes.addFlashAttribute("loi",
                    "Ngày này chưa có đơn nào đã duyệt nên chưa có điểm giao để tính khoảng cách.");
            return "redirect:/routing?deliveryDate=" + deliveryDate;
        }

        Depot kho = depotService.getActiveDepot();
        List<OptimizationPoint> cacDiem = dungDanhSachDiem(kho, cacDon);

        long batDau = System.currentTimeMillis();
        DistanceMatrixService.MaTranKhoangCach maTran = distanceMatrixService.napMaTran(cacDiem);
        long mili = System.currentTimeMillis() - batDau;

        int n = cacDiem.size();
        String thongBao = String.format(
                "Đã chuẩn bị ma trận %d×%d (%d ô) trong %d ms. "
                        + "Lấy từ bộ nhớ đệm: %d ô. Phải lấy mới: %d ô. Nguồn: %s.",
                n, n, n * n - n, mili,
                maTran.soOLayTuCache(), maTran.soOGoiMoi(), maTran.nguon().getDisplayName());

        if (maTran.dungXapXi()) {
            // MSG-09
            redirectAttributes.addFlashAttribute("loi",
                    "Không kết nối được OSRM. Đang dùng khoảng cách xấp xỉ (Haversine × hệ số). "
                            + "Tuyến sinh ra từ ma trận này sẽ được đánh dấu là xấp xỉ.");
        }
        redirectAttributes.addFlashAttribute("thongBao", thongBao);
        return "redirect:/routing?deliveryDate=" + deliveryDate;
    }

    // ==================== Ham rieng ====================

    /**
     * Doi don hang sang diem cua bai toan.
     *
     * KHO LUON DUNG DAU danh sach — quy uoc xuyen suot do an: chi so 0 trong ma tran
     * la kho. DistanceMatrixService dua vao dung thu tu nay de danh matrixIndex.
     */
    private List<OptimizationPoint> dungDanhSachDiem(Depot kho, List<Order> cacDon) {
        List<OptimizationPoint> cacDiem = new ArrayList<>();
        cacDiem.add(OptimizationPoint.depot(kho.getLatitude(), kho.getLongitude()));

        for (Order don : cacDon) {
            cacDiem.add(OptimizationPoint.cuaDon(
                    don.getId(),
                    don.getCustomer().getName(),
                    don.getLatitude(),
                    don.getLongitude(),
                    don.getTotalWeight(),
                    don.getTotalVolume()));
        }
        return cacDiem;
    }

    /**
     * Dung chuoi JSON cac ghim de JavaScript ve len ban do.
     *
     * Dung Jackson chu khong tu noi chuoi: ten khach hang co the chua dau nhay kep
     * ("Tap hoa "Ba Lan""), noi tay thi vo JSON va ca ban do trang.
     */
    private String dungJsonDiem(Depot kho, List<Order> cacDon) {
        List<DiemTrenBanDo> cacDiem = new ArrayList<>();

        if (kho != null) {
            cacDiem.add(new DiemTrenBanDo(kho.getName(), kho.getLatitude(), kho.getLongitude(),
                    true, kho.getAddress()));
        }

        for (Order don : cacDon) {
            int soThung = don.getItems().stream().mapToInt(dong -> dong.getBoxCount()).sum();
            // Locale.ROOT de dau thap phan luon la dau cham, dung quy uoc hien thi o docs/05
            String moTa = String.format(java.util.Locale.ROOT, "%d thùng · %.1f kg · %.3f m³",
                    soThung, don.getTotalWeight(), don.getTotalVolume());
            cacDiem.add(new DiemTrenBanDo(don.getCustomer().getName(),
                    don.getLatitude(), don.getLongitude(), false, moTa));
        }

        try {
            return objectMapper.writeValueAsString(cacDiem);
        } catch (JsonProcessingException loi) {
            // Khong de mot loi dinh dang lam hong ca man hinh: ban do rong, bang van con
            log.error("Khong dung duoc JSON cho ban do phan tuyen", loi);
            return "[]";
        }
    }

    /** Lay kho dang hoat dong. Chua khai bao kho thi bao ro chu khong nem loi 500. */
    private Depot layKho(Model model) {
        try {
            return depotService.getActiveDepot();
        } catch (RuntimeException loi) {
            model.addAttribute("loi",
                    "Chưa có kho nào đang hoạt động. Vào Danh mục → Kho để khai báo trước khi phân tuyến.");
            return null;
        }
    }
}
