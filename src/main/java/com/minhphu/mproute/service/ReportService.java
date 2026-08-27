package com.minhphu.mproute.service;

import com.minhphu.mproute.dto.OptimizationReport;
import com.minhphu.mproute.entity.OptimizationResult;
import com.minhphu.mproute.repository.OptimizationResultRepository;
import com.minhphu.mproute.repository.RouteRepository;
import com.minhphu.mproute.service.optimization.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
 * Hien moi co RPT-03 — bao cao hieu qua toi uu (CN-08-03). RPT-01 va RPT-02 la muc
 * co the cat neu thieu thoi gian, con RPT-03 thi khong: no la cho duy nhat trong ca
 * phan mem tra loi bang so cau hoi "de tai giai quyet duoc gi".
 *
 * Lop nay KHONG tinh lai bat ky chi so nao. Moi con so deu lay tu bang
 * optimization_results da ghi luc chay thuat toan (CBR-18, BR08-02); viec cua no chi
 * la LOC dung dong va CONG lai.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    /** BR08-01 — khoang thoi gian toi da cua mot bao cao. */
    public static final int MAX_RANGE_DAYS = 365;

    private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd/MM");

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

    /** Cac cot chi so co the NULL voi ban ghi cu; bao cao coi nhu 0 chu khong no. */
    private double orZero(Double value) {
        return value == null ? 0 : value;
    }
}
