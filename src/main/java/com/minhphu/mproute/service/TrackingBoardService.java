package com.minhphu.mproute.service;

import com.minhphu.mproute.dto.TrackingBoard;
import com.minhphu.mproute.entity.Route;
import com.minhphu.mproute.entity.RouteStop;
import com.minhphu.mproute.entity.enums.RouteStatus;
import com.minhphu.mproute.entity.enums.StopStatus;
import lombok.RequiredArgsConstructor;
import com.minhphu.mproute.repository.RouteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Dung du lieu cho SCR-10 — man hinh theo doi tien do giao hang cua dieu phoi.
 *
 * <p>Tach rieng khoi {@link TrackingService}: lop kia phuc vu trang tra cuu CONG KHAI va phai
 * tuan thu mot loat quy tac che giau thong tin (BR07-04, BR07-08). Tron hai muc dich vao mot
 * lop la som muon cung co nguoi goi nham ham khong che giau gi cho trang cong khai.
 */
@Service
@RequiredArgsConstructor
public class TrackingBoardService {

    /**
     * Chi theo doi nhung tuyen da thuc su buoc vao ngay lam viec.
     *
     * <p>Bo DRAFT vi tuyen nhap la ke hoach dang soan tren man hinh phan tuyen, chua chot, chua
     * ai chay — dua vao day thi tien do ca ngay bi keo xuong boi mot thu khong ai dinh giao.
     * Bo CANCELLED vi tuyen huy khong con la viec phai lam.
     */
    private static final List<RouteStatus> LIVE_STATUSES = List.of(
            RouteStatus.CONFIRMED,
            RouteStatus.IN_PROGRESS,
            RouteStatus.INCIDENT,
            RouteStatus.COMPLETED);

    private static final double METERS_PER_KM = 1000.0;

    private final RouteRepository routeRepository;

    @Transactional(readOnly = true)
    public TrackingBoard board(LocalDate date) {
        // Dung lai truy van san co cua man hinh phan tuyen: no da JOIN FETCH san xe, tai xe,
        // diem dung, don va khach. Viet truy van moi o day chi tao them mot cho co the sinh
        // van de N+1 ma khong duoc gi
        List<Route> routes = routeRepository.findWithStopsByDeliveryDateAndStatusIn(date, LIVE_STATUSES);

        List<TrackingBoard.RouteRow> rows = routes.stream()
                .sorted(Comparator.comparing(Route::getCode))
                .map(this::toRow)
                .toList();

        return new TrackingBoard(date, totalsOf(rows), rows);
    }

    private TrackingBoard.RouteRow toRow(Route route) {
        List<TrackingBoard.StopRow> stops = route.getStops().stream()
                .sorted(Comparator.comparing(RouteStop::getSequence))
                .map(this::toStopRow)
                .toList();

        int delivered = countOf(stops, StopStatus.DELIVERED);
        int handled = delivered
                + countOf(stops, StopStatus.FAILED)
                + countOf(stops, StopStatus.POSTPONED);

        return new TrackingBoard.RouteRow(
                route.getCode(),
                route.getVehicle().getLicensePlate(),
                // Tuyen da chot van co the chua gan tai xe (CN-04-06 lam o buoc rieng)
                route.getDriver() == null ? null : route.getDriver().getFullName(),
                route.getStatus(),
                route.getTotalDistance() / METERS_PER_KM,
                stops.size(),
                delivered,
                handled,
                percentOf(handled, stops.size()),
                route.getDepartedAt(),
                stops);
    }

    private TrackingBoard.StopRow toStopRow(RouteStop stop) {
        return new TrackingBoard.StopRow(
                stop.getSequence(),
                stop.getOrder().getCode(),
                stop.getOrder().getCustomer().getName(),
                // Dia chi giao lay tu DON chu khong tu ho so khach: don cho phep sua dia chi
                // giao rieng cho lan giao do (CN-02-03)
                stop.getOrder().getDeliveryAddress(),
                stop.getStatus(),
                stop.getEstimatedArrival(),
                stop.getActualArrival(),
                stop.getFailureReason() == null ? null : stop.getFailureReason().getDisplayName(),
                stop.getOrder().getAmountToCollect(),
                stop.getCollectedAmount());
    }

    private TrackingBoard.Totals totalsOf(List<TrackingBoard.RouteRow> rows) {
        List<TrackingBoard.StopRow> allStops = rows.stream()
                .flatMap(row -> row.stops().stream())
                .toList();

        int delivered = countOf(allStops, StopStatus.DELIVERED);
        int failed = countOf(allStops, StopStatus.FAILED);
        int postponed = countOf(allStops, StopStatus.POSTPONED);
        int handled = delivered + failed + postponed;

        return new TrackingBoard.Totals(
                rows.size(),
                allStops.size(),
                delivered,
                failed,
                postponed,
                allStops.size() - handled,
                handled,
                percentOf(handled, allStops.size()));
    }

    private int countOf(List<TrackingBoard.StopRow> stops, StopStatus status) {
        return (int) stops.stream().filter(stop -> stop.status() == status).count();
    }

    /** Ngay chua co tuyen nao thi mau so bang 0 — tra ve 0 chu khong de phep chia no ra NaN. */
    private double percentOf(int part, int total) {
        return total == 0 ? 0 : part * 100.0 / total;
    }
}
