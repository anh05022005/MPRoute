package com.minhphu.mproute.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minhphu.mproute.entity.Incident;
import com.minhphu.mproute.entity.RouteStop;
import com.minhphu.mproute.entity.enums.IncidentStatus;
import com.minhphu.mproute.entity.enums.SolutionType;
import com.minhphu.mproute.entity.enums.StopStatus;
import com.minhphu.mproute.repository.IncidentRepository;
import com.minhphu.mproute.security.CurrentUser;
import com.minhphu.mproute.service.IncidentService;
import com.minhphu.mproute.service.IncidentSolutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

/**
 * SCR-11 — Man hinh xu ly su co cua dieu phoi vien (CN-06-06 -> CN-06-12).
 *
 * Phan quyen giong man hinh phan tuyen: dieu phoi vien toan quyen, quan tri vien chi xem.
 * Tai xe khong vao duoc — ho bao su co tu man hinh cua ho, con viec chon phuong an la viec
 * cua nguoi ngoi o van phong nhin duoc toan bo doi xe.
 */
@Controller
@RequestMapping("/incidents")
@PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
@RequiredArgsConstructor
@Slf4j
public class IncidentController {

    private final IncidentRepository incidentRepository;
    private final IncidentService incidentService;
    private final IncidentSolutionService incidentSolutionService;
    private final ObjectMapper objectMapper;

    /** Mot ghim tren ban do su co. Chi chua thu can ve. */
    public record MapPoint(String name,
                           double latitude,
                           double longitude,
                           boolean depot,
                           String description) {}

    /** Danh sach su co, cai dang cho xu ly len dau. */
    @GetMapping
    public String list(Model model) {
        model.addAttribute("pendingIncidents",
                incidentRepository.findByStatusOrderByReportedAtDesc(IncidentStatus.PENDING));
        model.addAttribute("resolvedIncidents",
                incidentRepository.findByStatusOrderByReportedAtDesc(IncidentStatus.RESOLVED));
        return "incidents/incidents";
    }

    /**
     * CN-06-06 — man hinh xu ly mot su co: thong tin, vi tri xe tren ban do, diem chua giao,
     * va cac phuong an da xep hang.
     */
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Incident incident = incidentRepository.findById(id).orElseThrow(() ->
                new IllegalArgumentException("Không tìm thấy sự cố."));

        model.addAttribute("incident", incident);
        model.addAttribute("remainingStops", remainingStopsOf(incident));

        /*
         * Chi sinh phuong an cho su co CON DANG CHO. Su co da xu ly xong thi cac diem da co
         * nguoi nhan roi, sinh lai chi ra mot danh sach vo nghia va de bam nham lan hai.
         */
        if (incident.getStatus() == IncidentStatus.PENDING) {
            model.addAttribute("options", incidentSolutionService.generateOptions(incident));
        }

        model.addAttribute("mapPointsJson", buildMapPointsJson(incident));
        model.addAttribute("needsMap", true);
        model.addAttribute("needsIncidentJs", true);
        return "incidents/incident-detail";
    }

    /** CN-06-11 — dieu phoi vien chon mot phuong an de ap dung. */
    @PreAuthorize("hasRole('DISPATCHER')")
    @PostMapping("/{id}/apply")
    public String apply(@PathVariable Long id,
                        @RequestParam(required = false) SolutionType solutionType,
                        @RequestParam(required = false) Long targetVehicleId,
                        @RequestParam(required = false) Long targetRouteId,
                        @AuthenticationPrincipal CurrentUser currentUser,
                        RedirectAttributes redirectAttributes) {
        try {
            // Chi mot trong hai id co gia tri, tuy phuong an — truyen ca hai cho service tu chon
            Long targetId = (solutionType == SolutionType.A_RESCUE_VEHICLE)
                    ? targetVehicleId : targetRouteId;

            Incident incident = incidentService.applySolution(id, solutionType, targetId, currentUser.getId());
            redirectAttributes.addFlashAttribute("successMessage", String.format(
                    "Đã áp dụng phương án \"%s\". Khách bị ảnh hưởng đã được báo giờ giao mới.",
                    incident.getAppliedSolution().getDisplayName()));

        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/incidents/" + id;
    }

    // ==================== Ham rieng ====================

    private List<RouteStop> remainingStopsOf(Incident incident) {
        List<RouteStop> remaining = new ArrayList<>();
        for (RouteStop stop : incident.getRoute().getStops()) {
            if (stop.getStatus() == StopStatus.PENDING || stop.getStatus() == StopStatus.DELIVERING) {
                remaining.add(stop);
            }
        }
        return remaining;
    }

    /**
     * Ghim tren ban do: vi tri xe gap su co, kho, va cac diem chua giao.
     *
     * Vi tri xe ve bang o vuong dam giong kho de noi bat — day la thu dieu phoi vien nhin dau
     * tien de uoc luong xem dieu xe cuu ho tu kho ra co xa khong.
     */
    private String buildMapPointsJson(Incident incident) {
        List<MapPoint> points = new ArrayList<>();

        if (incident.getLatitude() != null && incident.getLongitude() != null) {
            points.add(new MapPoint("Vị trí sự cố",
                    incident.getLatitude(), incident.getLongitude(), true,
                    Boolean.TRUE.equals(incident.getEstimatedLocation())
                            ? "Vị trí ước lượng theo điểm giao gần nhất" : "Tọa độ lấy từ điện thoại tài xế"));
        }

        var depot = incident.getRoute().getDepot();
        points.add(new MapPoint(depot.getName(), depot.getLatitude(), depot.getLongitude(),
                true, "Kho xuất phát"));

        for (RouteStop stop : remainingStopsOf(incident)) {
            points.add(new MapPoint(stop.getOrder().getCustomer().getName(),
                    stop.getOrder().getLatitude(), stop.getOrder().getLongitude(),
                    false, "Chưa giao · " + stop.getOrder().getCode()));
        }

        try {
            return objectMapper.writeValueAsString(points);
        } catch (JsonProcessingException ex) {
            // Ban do rong con hon ca man hinh trang: phan phuong an ben duoi van dung duoc
            log.error("Khong dung duoc JSON cho ban do su co", ex);
            return "[]";
        }
    }
}
