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
import java.util.Locale;

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

    private static final String NEEDS_MAP = "needsMap";
    private static final String NEEDS_ROUTING_JS = "needsRoutingJs";

    private final OrderRepository orderRepository;
    private final DepotService depotService;
    private final DistanceMatrixService distanceMatrixService;
    private final ObjectMapper objectMapper;

    /** Mot ghim tren ban do. Chi chua thu can ve, khong day ca entity ra giao dien. */
    public record MapPoint(String name,
                           double latitude,
                           double longitude,
                           boolean depot,
                           String description) {}

    @GetMapping
    public String screen(@RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate,
                         Model model) {

        // Mac dinh xem ngay mai — dung ngay ma don duoc tao san (BR02-06)
        LocalDate targetDate = (deliveryDate != null) ? deliveryDate : LocalDate.now().plusDays(1);

        // BR03-01: chi don DA DUYET moi duoc dua vao phan tuyen
        List<Order> orders = orderRepository.findByDeliveryDateAndStatus(targetDate, OrderStatus.APPROVED);

        double totalWeight = orders.stream().mapToDouble(Order::getTotalWeight).sum();
        double totalVolume = orders.stream().mapToDouble(Order::getTotalVolume).sum();

        model.addAttribute("deliveryDate", targetDate);
        model.addAttribute("orders", orders);
        model.addAttribute("totalWeight", totalWeight);
        model.addAttribute("totalVolume", totalVolume);

        Depot depot = findActiveDepot(model);
        model.addAttribute("depot", depot);
        model.addAttribute("mapPointsJson", buildMapPointsJson(depot, orders));

        model.addAttribute(NEEDS_MAP, true);
        model.addAttribute(NEEDS_ROUTING_JS, true);
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

        List<Order> orders = orderRepository.findByDeliveryDateAndStatus(deliveryDate, OrderStatus.APPROVED);
        if (orders.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ngày này chưa có đơn nào đã duyệt nên chưa có điểm giao để tính khoảng cách.");
            return "redirect:/routing?deliveryDate=" + deliveryDate;
        }

        Depot depot = depotService.getActiveDepot();
        List<OptimizationPoint> points = buildOptimizationPoints(depot, orders);

        long startedAt = System.currentTimeMillis();
        DistanceMatrixService.DistanceMatrix matrix = distanceMatrixService.loadMatrix(points);
        long elapsedMs = System.currentTimeMillis() - startedAt;

        int size = points.size();
        String message = String.format(
                "Đã chuẩn bị ma trận %d×%d (%d ô) trong %d ms. "
                        + "Lấy từ bộ nhớ đệm: %d ô. Phải lấy mới: %d ô. Nguồn: %s.",
                size, size, size * size - size, elapsedMs,
                matrix.cacheHits(), matrix.cacheMisses(), matrix.source().getDisplayName());

        if (matrix.approximate()) {
            // MSG-09
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Không kết nối được OSRM. Đang dùng khoảng cách xấp xỉ (Haversine × hệ số). "
                            + "Tuyến sinh ra từ ma trận này sẽ được đánh dấu là xấp xỉ.");
        }
        redirectAttributes.addFlashAttribute("successMessage", message);
        return "redirect:/routing?deliveryDate=" + deliveryDate;
    }

    // ==================== Ham rieng ====================

    /**
     * Doi don hang sang diem cua bai toan.
     *
     * KHO LUON DUNG DAU danh sach — quy uoc xuyen suot do an: chi so 0 trong ma tran
     * la kho. DistanceMatrixService dua vao dung thu tu nay de danh matrixIndex.
     */
    private List<OptimizationPoint> buildOptimizationPoints(Depot depot, List<Order> orders) {
        List<OptimizationPoint> points = new ArrayList<>();
        points.add(OptimizationPoint.depot(depot.getLatitude(), depot.getLongitude()));

        for (Order order : orders) {
            points.add(OptimizationPoint.forOrder(
                    order.getId(),
                    order.getCustomer().getName(),
                    order.getLatitude(),
                    order.getLongitude(),
                    order.getTotalWeight(),
                    order.getTotalVolume()));
        }
        return points;
    }

    /**
     * Dung chuoi JSON cac ghim de JavaScript ve len ban do.
     *
     * Dung Jackson chu khong tu noi chuoi: ten khach hang co the chua dau nhay kep
     * ("Tap hoa "Ba Lan""), noi tay thi vo JSON va ca ban do trang.
     */
    private String buildMapPointsJson(Depot depot, List<Order> orders) {
        List<MapPoint> mapPoints = new ArrayList<>();

        if (depot != null) {
            mapPoints.add(new MapPoint(depot.getName(), depot.getLatitude(), depot.getLongitude(),
                    true, depot.getAddress()));
        }

        for (Order order : orders) {
            int boxCount = order.getItems().stream().mapToInt(item -> item.getBoxCount()).sum();
            // Locale.ROOT de dau thap phan luon la dau cham, dung quy uoc hien thi o docs/05
            String description = String.format(Locale.ROOT, "%d thùng · %.1f kg · %.3f m³",
                    boxCount, order.getTotalWeight(), order.getTotalVolume());
            mapPoints.add(new MapPoint(order.getCustomer().getName(),
                    order.getLatitude(), order.getLongitude(), false, description));
        }

        try {
            return objectMapper.writeValueAsString(mapPoints);
        } catch (JsonProcessingException ex) {
            // Khong de mot loi dinh dang lam hong ca man hinh: ban do rong, bang van con
            log.error("Khong dung duoc JSON cho ban do phan tuyen", ex);
            return "[]";
        }
    }

    /** Lay kho dang hoat dong. Chua khai bao kho thi bao ro chu khong nem loi 500. */
    private Depot findActiveDepot(Model model) {
        try {
            return depotService.getActiveDepot();
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage",
                    "Chưa có kho nào đang hoạt động. Vào Danh mục → Kho để khai báo trước khi phân tuyến.");
            return null;
        }
    }
}
