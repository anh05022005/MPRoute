package com.minhphu.mproute.service;

import com.minhphu.mproute.dto.DailyOrderCount;
import com.minhphu.mproute.dto.DeliveryReport;
import com.minhphu.mproute.dto.FailureReasonCount;
import com.minhphu.mproute.dto.OptimizationReport;
import com.minhphu.mproute.dto.OrderReport;
import com.minhphu.mproute.dto.OrderStatusCount;
import com.minhphu.mproute.dto.ProductBoxCount;
import com.minhphu.mproute.entity.OptimizationResult;
import com.minhphu.mproute.repository.OptimizationResultRepository;
import com.minhphu.mproute.entity.enums.FailureReason;
import com.minhphu.mproute.entity.enums.OrderStatus;
import com.minhphu.mproute.repository.OrderRepository;
import com.minhphu.mproute.repository.RouteRepository;
import com.minhphu.mproute.repository.RouteStopRepository;
import com.minhphu.mproute.service.optimization.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Dung so lieu cho cac bao cao (nhom 8).
 *
 * Ba bao cao, tra loi ba cau khac han nhau:
 *
 *   RPT-01 (CN-08-01) — LAM DUOC BAO NHIEU: so don, so kg, so thung moi mat hang
 *   RPT-02 (CN-08-02) — GIAO CO TRON KHONG: ty le thanh cong va ly do that bai
 *   RPT-03 (CN-08-03) — THUAT TOAN GIUP DUOC GI: muc giam so voi hai moc
 *
 * RPT-03 la bao cao quan trong nhat va la muc "tuyet doi khong cat" cua docs/04.
 *
 * KHAC NHAU O NGUON DU LIEU, va cho nay quan trong:
 *   - RPT-03 doc bang optimization_results, la chi so DA LUU luc chay thuat toan va
 *     KHONG BAO GIO tinh lai (CBR-18, BR08-02) — khoang cach, danh muc xe, du lieu don
 *     deu doi theo thoi gian, tinh lai hom nay se ra con so khac han luc chay that.
 *   - RPT-01 va RPT-02 gop tu don hang va diem dung, tuc tinh tai thoi diem xem bao cao.
 *     Duoc phep vi chung dem SO LUONG chu khong do khoang cach: mot don da giao thi mai
 *     mai la mot don da giao, khong phu thuoc vao ma tran khoang cach hom nay.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    /** BR08-01 — khoang thoi gian toi da cua mot bao cao. */
    public static final int MAX_RANGE_DAYS = 365;

    private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MONTH_YEAR = DateTimeFormatter.ofPattern("MM/yyyy");

    /** Ban kinh vong tron cua bieu do RPT-02, trong he toa do viewBox "0 0 240 240". */
    private static final double DONUT_RADIUS = 80;

    /*
     * Khung ve cua bieu do duong, tinh theo he toa do cua the SVG (viewBox "0 0 960 220").
     * Truc y cua SVG huong XUONG, nen phan tram cang lon thi y cang NHO — cho nay rat de
     * nham dau khi doc ham chartOf().
     */
    private static final double CHART_LEFT = 54;
    private static final double CHART_RIGHT = 944;
    private static final double CHART_TOP = 16;
    private static final double CHART_BOTTOM = 186;

    /** So khoang giua cac duong ke ngang: 4 khoang thanh 5 duong (0%, 25%, ... 100%). */
    private static final int GRID_INTERVALS = 4;

    /** Truc dung luon lam tron len boi so cua 10 cho de doc, va khong bao gio nho hon 10%. */
    private static final double AXIS_STEP_PERCENT = 10;

    /** Toi da bay nhieu nhan ngay tren truc ngang, nhieu hon thi chu chong len nhau. */
    private static final int MAX_X_LABELS = 12;

    private final OptimizationResultRepository optimizationResultRepository;
    private final RouteRepository routeRepository;
    private final OrderRepository orderRepository;
    private final RouteStopRepository routeStopRepository;

    /**
     * RPT-03 — bang hieu qua toi uu theo ngay, kem dong trung binh toan ky (CN-08-03).
     *
     * Ba buoc: lay lan chay gan nhat cua tung ngay, loc theo CN-08-04, roi cong lai.
     */
    @Transactional(readOnly = true)
    public OptimizationReport optimizationReport(LocalDate fromDate, LocalDate toDate) {

        List<OptimizationResult> allRuns =
                optimizationResultRepository.findByDeliveryDateBetweenOrderByDeliveryDateAsc(fromDate, toDate);

        /*
         * Ngay nao con it nhat mot tuyen chua huy thi moi duoc tinh (BR08-04). Hoi mot lan
         * cho ca khoang roi tra trong bo nho, khong hoi lai CSDL trong vong lap.
         */
        Set<LocalDate> datesWithLiveRoutes =
                new HashSet<>(routeRepository.findDatesWithLiveRoutes(fromDate, toDate));

        List<OptimizationResult> latestRunPerDay = latestRunPerDay(allRuns);

        List<OptimizationReport.DayRow> rows = new ArrayList<>();
        List<OptimizationReport.DayRow> approximateRows = new ArrayList<>();
        List<LocalDate> cancelledDates = new ArrayList<>();

        for (OptimizationResult run : latestRunPerDay) {
            if (!datesWithLiveRoutes.contains(run.getDeliveryDate())) {
                cancelledDates.add(run.getDeliveryDate());
                continue;
            }
            OptimizationReport.DayRow row = toRow(run);
            if (row.approximate()) {
                approximateRows.add(row);
            } else {
                rows.add(row);
            }
        }

        return new OptimizationReport(fromDate, toDate, rows, approximateRows, cancelledDates,
                totalsOf(rows, latestRunPerDay), chartOf(rows));
    }

    /**
     * Moi ngay giu DUNG MOT dong — lan chay gan nhat.
     *
     * Chay lai phan tuyen ba lan trong ngay thi bang co ba dong, ca ba deu la so lieu that.
     * Nhung chi lan cuoi moi la ke hoach thuc su duoc chot va dem giao, nen bao cao lay lan
     * cuoi. Lay theo id lon nhat chu khong theo created_at: hai lan chay cach nhau vai giay
     * co the trung created_at den tung giay, con id thi khong bao gio trung.
     */
    private List<OptimizationResult> latestRunPerDay(List<OptimizationResult> allRuns) {
        Map<LocalDate, OptimizationResult> latestByDate = new LinkedHashMap<>();
        for (OptimizationResult run : allRuns) {
            OptimizationResult kept = latestByDate.get(run.getDeliveryDate());
            if (kept == null || run.getId() > kept.getId()) {
                latestByDate.put(run.getDeliveryDate(), run);
            }
        }
        List<OptimizationResult> latest = new ArrayList<>(latestByDate.values());
        latest.sort(Comparator.comparing(OptimizationResult::getDeliveryDate));
        return latest;
    }

    /**
     * Doi mot ban ghi chi so thanh mot dong bang.
     *
     * "So chuyen tiet kiem" = so don - so tuyen. Y nghia: moc A la moi don mot chuyen khu
     * hoi rieng, tuc 12 don thi 12 luot xe ra khoi kho; sau toi uu chi con dung bang so
     * tuyen. Day la con so de hieu nhat voi nguoi khong lam ky thuat — nghe "giam 44%" thi
     * mo ho, nghe "12 chuyen con 3 chuyen" thi hieu ngay.
     */
    private OptimizationReport.DayRow toRow(OptimizationResult run) {
        return new OptimizationReport.DayRow(
                run.getDeliveryDate(),
                run.getOrderCount(),
                run.getRouteCount(),
                run.getBaselineADistance(),
                run.getBaselineBDistance(),
                run.getOptimizedDistance(),
                orZero(run.getReductionAMeters()),
                orZero(run.getReductionAPercent()),
                orZero(run.getReductionBMeters()),
                orZero(run.getReductionBPercent()),
                run.getOrderCount() - run.getRouteCount(),
                run.getAlgorithm(),
                run.getExecutionTimeMs(),
                Boolean.TRUE.equals(run.getApproximateDistance()));
    }

    /**
     * Dong trung binh toan ky, chi tinh tren cac ngay hop le (da bo ngay huy va ngay dung
     * khoang cach xap xi — CN-08-04).
     *
     * Ty le lap day lay tu chinh cac ban ghi goc chu khong tu DayRow, vi hai cot do khong
     * xuat hien tren bang theo ngay nhung van can cho dong tong ket.
     */
    private OptimizationReport.PeriodTotals totalsOf(List<OptimizationReport.DayRow> rows,
                                                     List<OptimizationResult> latestRunPerDay) {
        if (rows.isEmpty()) {
            return new OptimizationReport.PeriodTotals(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        int orderCount = 0;
        int routeCount = 0;
        int tripsSaved = 0;
        double baselineA = 0;
        double baselineB = 0;
        double optimized = 0;
        Set<LocalDate> countedDates = new HashSet<>();
        for (OptimizationReport.DayRow row : rows) {
            orderCount += row.orderCount();
            routeCount += row.routeCount();
            tripsSaved += row.tripsSaved();
            baselineA += row.baselineADistance();
            baselineB += row.baselineBDistance();
            optimized += row.optimizedDistance();
            countedDates.add(row.deliveryDate());
        }

        double weightUtilization = 0;
        double volumeUtilization = 0;
        int utilizationDays = 0;
        for (OptimizationResult run : latestRunPerDay) {
            if (countedDates.contains(run.getDeliveryDate())) {
                weightUtilization += orZero(run.getWeightUtilization());
                volumeUtilization += orZero(run.getVolumeUtilization());
                utilizationDays++;
            }
        }
        if (utilizationDays > 0) {
            weightUtilization /= utilizationDays;
            volumeUtilization /= utilizationDays;
        }

        return new OptimizationReport.PeriodTotals(
                rows.size(), orderCount, routeCount,
                baselineA, baselineB, optimized,
                baselineA - optimized, MetricsService.reductionPercent(baselineA, optimized),
                baselineB - optimized, MetricsService.reductionPercent(baselineB, optimized),
                tripsSaved, weightUtilization, volumeUtilization);
    }

    /**
     * Toa do cua bieu do duong: muc giam so voi moc A va so voi moc B qua tung ngay.
     *
     * Ve muc GIAM chu khong ve quang duong: quang duong len xuong theo so don cua tung
     * ngay nen duong bieu do chi phan anh hom do nhieu hang hay it hang. Muc giam thi bo
     * duoc yeu to do ra, cho thay dung thu can xem la thuat toan on dinh den dau.
     */
    private OptimizationReport.Chart chartOf(List<OptimizationReport.DayRow> rows) {
        if (rows.isEmpty()) {
            return new OptimizationReport.Chart(List.of(), "", "", List.of(), AXIS_STEP_PERCENT);
        }

        double maxPercent = AXIS_STEP_PERCENT;
        for (OptimizationReport.DayRow row : rows) {
            maxPercent = Math.max(maxPercent, Math.max(row.reductionAPercent(), row.reductionBPercent()));
        }
        // Lam tron len boi so cua 10 de nhan truc dung doc cho gon: 44,9% -> truc cao 50%
        maxPercent = Math.ceil(maxPercent / AXIS_STEP_PERCENT) * AXIS_STEP_PERCENT;

        int labelStep = (int) Math.ceil((double) rows.size() / MAX_X_LABELS);

        List<OptimizationReport.Chart.Column> columns = new ArrayList<>();
        StringBuilder pointsA = new StringBuilder();
        StringBuilder pointsB = new StringBuilder();

        for (int i = 0; i < rows.size(); i++) {
            OptimizationReport.DayRow row = rows.get(i);

            // Mot ngay duy nhat thi khong co doan thang nao, dat diem vao giua cho can
            double x = (rows.size() == 1)
                    ? (CHART_LEFT + CHART_RIGHT) / 2
                    : CHART_LEFT + i * (CHART_RIGHT - CHART_LEFT) / (rows.size() - 1);

            double yA = yOf(row.reductionAPercent(), maxPercent);
            double yB = yOf(row.reductionBPercent(), maxPercent);

            String label = (i % labelStep == 0) ? row.deliveryDate().format(DAY_MONTH) : "";
            columns.add(new OptimizationReport.Chart.Column(label, x, yA, yB));

            appendPoint(pointsA, x, yA);
            appendPoint(pointsB, x, yB);
        }

        List<OptimizationReport.Chart.GridLine> gridLines = new ArrayList<>();
        for (int i = 0; i <= GRID_INTERVALS; i++) {
            double percent = maxPercent * i / GRID_INTERVALS;
            gridLines.add(new OptimizationReport.Chart.GridLine(
                    yOf(percent, maxPercent),
                    String.format(Locale.ROOT, "%.0f%%", percent)));
        }

        return new OptimizationReport.Chart(columns, pointsA.toString(), pointsB.toString(),
                gridLines, maxPercent);
    }

    /** Doi phan tram thanh toa do dung cua SVG. Phan tram cang lon thi y cang nho. */
    private double yOf(double percent, double maxPercent) {
        double clamped = Math.max(0, Math.min(percent, maxPercent));
        return CHART_BOTTOM - (clamped / maxPercent) * (CHART_BOTTOM - CHART_TOP);
    }

    /** Locale.ROOT de dau thap phan luon la dau CHAM — SVG khong doc duoc dau phay. */
    private void appendPoint(StringBuilder points, double x, double y) {
        if (points.length() > 0) {
            points.append(' ');
        }
        points.append(String.format(Locale.ROOT, "%.1f,%.1f", x, y));
    }


    // ==================== RPT-01 Thong ke don hang (CN-08-01) ====================

    /**
     * RPT-01 — so don theo ngay/tuan/thang, kem so thung theo tung san pham (CN-08-01).
     *
     * Bao cao nay do KHOI LUONG cong viec, khac han RPT-03 do HIEU QUA thuat toan. Voi
     * Minh Phu thi cot "so thung moi mat hang" moi la cai dung de len ke hoach san xuat.
     */
    @Transactional(readOnly = true)
    public OrderReport orderReport(LocalDate fromDate, LocalDate toDate,
                                   OrderReport.Grouping grouping) {

        List<DailyOrderCount> daily = orderRepository.countOrdersByDay(fromDate, toDate);
        List<ProductBoxCount> products = orderRepository.countBoxesByProduct(fromDate, toDate);

        List<OrderReport.PeriodRow> rows = groupByPeriod(daily, grouping);

        long orderCount = 0;
        double totalWeight = 0;
        double totalVolume = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderReport.PeriodRow row : rows) {
            orderCount += row.orderCount();
            totalWeight += row.totalWeight();
            totalVolume += row.totalVolume();
            totalAmount = totalAmount.add(row.totalAmount());
        }

        long boxCount = 0;
        for (ProductBoxCount product : products) {
            boxCount += orZeroLong(product.boxCount());
        }

        OrderReport.Totals totals =
                new OrderReport.Totals(orderCount, totalWeight, totalVolume, totalAmount, boxCount);
        return new OrderReport(grouping, rows, products, totals, barChartOf(rows));
    }

    /**
     * Gom cac ngay thanh ngay / tuan / thang.
     *
     * CSDL da gop san theo NGAY, o day chi gom tiep. Vi sao khong gom tuan trong SQL: ham
     * lay so tuan la ham rieng cua tung he quan tri (MySQL co WEEK(), Postgres dung
     * EXTRACT), viet vao JPQL la khoa chat do an vao MySQL. Mot khoang toi da 365 ngay thi
     * cung chi 365 dong, gom trong bo nho khong ton gi.
     *
     * Tuan tinh theo chuan ISO — tuan bat dau tu THU HAI. Dung WeekFields.ISO chu khong dung
     * mac dinh cua may: mac dinh doi theo Locale, may cai tieng Anh My thi tuan bat dau tu
     * Chu nhat va so tuan lech di mot.
     */
    private List<OrderReport.PeriodRow> groupByPeriod(List<DailyOrderCount> daily,
                                                      OrderReport.Grouping grouping) {

        // LinkedHashMap de thu tu ky giu nguyen theo chieu thoi gian nhu cau truy van tra ve
        Map<String, OrderReport.PeriodRow> byPeriod = new LinkedHashMap<>();

        for (DailyOrderCount day : daily) {
            String label = periodLabel(day.day(), grouping);
            OrderReport.PeriodRow current = byPeriod.get(label);

            long orders = day.orderCount() + (current == null ? 0 : current.orderCount());
            double weight = orZero(day.totalWeight()) + (current == null ? 0 : current.totalWeight());
            double volume = orZero(day.totalVolume()) + (current == null ? 0 : current.totalVolume());
            BigDecimal amount = (day.totalAmount() == null ? BigDecimal.ZERO : day.totalAmount())
                    .add(current == null ? BigDecimal.ZERO : current.totalAmount());

            byPeriod.put(label, new OrderReport.PeriodRow(label, orders, weight, volume, amount));
        }
        return new ArrayList<>(byPeriod.values());
    }

    private String periodLabel(LocalDate day, OrderReport.Grouping grouping) {
        return switch (grouping) {
            case DAY -> day.format(DAY_MONTH_YEAR);
            case WEEK -> "Tuần " + day.get(WeekFields.ISO.weekOfWeekBasedYear())
                    + "/" + day.get(WeekFields.ISO.weekBasedYear());
            case MONTH -> day.format(MONTH_YEAR);
        };
    }

    /**
     * Bieu do cot so don theo tung ky (CN-08-01).
     *
     * Cot ve tu duoi len: y la DINH cot, height la chieu cao. Truc y cua SVG huong xuong
     * nen dinh cot cang cao thi y cang nho — cung cho de nham dau nhu bieu do duong.
     */
    private OrderReport.BarChart barChartOf(List<OrderReport.PeriodRow> rows) {
        if (rows.isEmpty()) {
            return new OrderReport.BarChart(List.of(), List.of(), 0);
        }

        long maxValue = 0;
        for (OrderReport.PeriodRow row : rows) {
            maxValue = Math.max(maxValue, row.orderCount());
        }
        if (maxValue == 0) {
            maxValue = 1;
        }

        double slotWidth = (CHART_RIGHT - CHART_LEFT) / rows.size();
        double barWidth = Math.min(46, slotWidth * 0.62);
        int labelStep = (int) Math.ceil((double) rows.size() / MAX_X_LABELS);

        List<OrderReport.BarChart.Bar> bars = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            OrderReport.PeriodRow row = rows.get(i);
            double height = (double) row.orderCount() / maxValue * (CHART_BOTTOM - CHART_TOP);
            double centre = CHART_LEFT + slotWidth * (i + 0.5);
            String label = (i % labelStep == 0) ? row.label() : "";
            bars.add(new OrderReport.BarChart.Bar(label, row.orderCount(),
                    centre - barWidth / 2, CHART_BOTTOM - height, barWidth, height));
        }

        List<OrderReport.BarChart.GridLine> gridLines = new ArrayList<>();
        for (int i = 0; i <= GRID_INTERVALS; i++) {
            double value = (double) maxValue * i / GRID_INTERVALS;
            double y = CHART_BOTTOM - (value / maxValue) * (CHART_BOTTOM - CHART_TOP);
            gridLines.add(new OrderReport.BarChart.GridLine(
                    y, String.format(Locale.ROOT, "%.0f", value)));
        }
        return new OrderReport.BarChart(bars, gridLines, maxValue);
    }

    // ==================== RPT-02 Ty le giao thanh cong (CN-08-02) ====================

    /**
     * RPT-02 — ty le giao thanh cong va phan tich ly do that bai (CN-08-02).
     *
     * TY LE TINH THEO BR08-06: so don DELIVERED chia TONG DON DA PHAN TUYEN. Mau so khong
     * phai moi don trong ky — don cho duyet hay da huy thi chua bao gio len xe, dem chung
     * vao la tu ha ty le cua minh xuong vi mot ly do khong lien quan gi toi viec giao hang.
     */
    @Transactional(readOnly = true)
    public DeliveryReport deliveryReport(LocalDate fromDate, LocalDate toDate) {

        Map<OrderStatus, Long> countByStatus = new LinkedHashMap<>();
        for (OrderStatusCount row : orderRepository.countOrdersByStatus(fromDate, toDate)) {
            countByStatus.put(row.status(), row.orderCount());
        }

        long delivered = countOf(countByStatus, OrderStatus.DELIVERED);
        long failed = countOf(countByStatus, OrderStatus.FAILED);
        long postponed = countOf(countByStatus, OrderStatus.POSTPONED);
        long inProgress = countOf(countByStatus, OrderStatus.ROUTED)
                + countOf(countByStatus, OrderStatus.DELIVERING);
        long cancelled = countOf(countByStatus, OrderStatus.CANCELLED);

        // BR08-06 — mau so la don DA phan tuyen, tuc tu ROUTED tro di
        long routed = delivered + failed + postponed + inProgress;

        List<DeliveryReport.StatusRow> statusRows = List.of(
                statusRow(OrderStatus.DELIVERED, delivered, routed, "slice-delivered"),
                statusRow(OrderStatus.FAILED, failed, routed, "slice-failed"),
                statusRow(OrderStatus.POSTPONED, postponed, routed, "slice-postponed"),
                statusRow(OrderStatus.ROUTED, inProgress, routed, "slice-in-progress"));

        List<FailureReasonCount> failures = routeStopRepository.countFailuresByReason(fromDate, toDate);
        long failureTotal = 0;
        for (FailureReasonCount failure : failures) {
            failureTotal += failure.stopCount();
        }
        List<DeliveryReport.FailureRow> failureRows = new ArrayList<>();
        for (FailureReasonCount failure : failures) {
            failureRows.add(new DeliveryReport.FailureRow(failure.reason(), failure.stopCount(),
                    percentOf(failure.stopCount(), failureTotal)));
        }

        return new DeliveryReport(statusRows, failureRows, routed,
                delivered, failed, postponed, inProgress, cancelled,
                percentOf(delivered, routed),
                percentOf(failed, routed),
                percentOf(postponed, routed),
                donutChartOf(statusRows));
    }

    private DeliveryReport.StatusRow statusRow(OrderStatus status, long count, long total,
                                               String styleClass) {
        return new DeliveryReport.StatusRow(status, count, percentOf(count, total), styleClass);
    }

    /**
     * Bieu do tron ve bang stroke-dasharray tren cac vong tron chong len nhau.
     *
     * Moi mieng la mot vong tron day du, nhung vien cua no bi cat thanh hai doan: mot doan
     * TO MAU dai bang phan tram cua mieng, mot doan BO TRONG la phan con lai. Dash offset
     * xoay diem bat dau cua doan to mau ve dung cho no phai nam.
     *
     * Offset mang dau AM va cong don: SVG chay dash nguoc chieu voi chieu ve, nen muon
     * mieng thu hai bat dau ngay sau mieng thu nhat thi phai lui offset di dung do dai
     * mieng thu nhat. Dat dau duong la cac mieng chong len nhau.
     */
    private DeliveryReport.DonutChart donutChartOf(List<DeliveryReport.StatusRow> statusRows) {

        double circumference = 2 * Math.PI * DONUT_RADIUS;

        List<DeliveryReport.DonutChart.Slice> slices = new ArrayList<>();
        double consumed = 0;
        for (DeliveryReport.StatusRow row : statusRows) {
            if (row.orderCount() == 0) {
                continue;   // mieng 0% ve ra chi la mot cham mau vo nghia
            }
            double dashLength = circumference * row.share() / 100.0;
            slices.add(new DeliveryReport.DonutChart.Slice(
                    row.status().getDisplayName(), row.orderCount(), row.share(), row.styleClass(),
                    dashLength, circumference - dashLength, -consumed));
            consumed += dashLength;
        }
        return new DeliveryReport.DonutChart(slices, circumference);
    }

    private long countOf(Map<OrderStatus, Long> countByStatus, OrderStatus status) {
        Long count = countByStatus.get(status);
        return count == null ? 0 : count;
    }

    /** Phan tram, tra ve 0 khi mau so bang 0 chu khong chia cho 0. */
    private double percentOf(long part, long total) {
        return total <= 0 ? 0 : (double) part / total * 100.0;
    }

    private long orZeroLong(Long value) {
        return value == null ? 0 : value;
    }

    /** Cac cot chi so co the NULL voi ban ghi cu; bao cao coi nhu 0 chu khong no. */
    private double orZero(Double value) {
        return value == null ? 0 : value;
    }
}
