package com.minhphu.mproute.dto;

import com.minhphu.mproute.entity.enums.AlgorithmType;

import java.time.LocalDate;
import java.util.List;

/**
 * Du lieu cua bao cao RPT-03 — bao cao hieu qua toi uu (CN-08-03).
 *
 * DAY LA BAO CAO QUAN TRONG NHAT CUA DO AN, va la muc "tuyet doi khong cat" cua
 * docs/04. No tra loi bang so cho cau hoi ma hoi dong chac chan se hoi:
 * "de tai nay giup Minh Phu giam duoc bao nhieu".
 *
 * Moi con so o day lay TU BANG optimization_results, tuc la chi so da luu tai
 * thoi diem chay va khong bao gio tinh lai (CBR-18, BR08-02). Khong tinh lai la
 * co chu dich: khoang cach OSRM, danh muc xe va du lieu don deu doi theo thoi
 * gian, tinh lai hom nay se ra con so khac han voi luc chay that.
 *
 * BA VIEC LOC — tuong ung CN-08-04:
 *  - Ngay ma MOI tuyen deu da huy khong duoc tinh (BR08-04): hom do khong co
 *    chuyen hang nao thuc su chay.
 *  - Lan chay dung khoang cach xap xi Haversine tach ra bang rieng, khong tinh
 *    vao trung binh (BR08-03). Con so xap xi lech toi hai chuc phan tram, tron
 *    vao trung binh la lam hong ca bao cao.
 *  - Moi ngay chi lay LAN CHAY GAN NHAT. Chay lai ba lan trong ngay thi ca ba
 *    dong deu la so lieu that, nhung chi lan cuoi la ke hoach thuc su duoc chot.
 */
public record OptimizationReport(

        LocalDate fromDate,
        LocalDate toDate,

        /** Cac ngay dung khoang cach that, la nguon cua dong trung binh. */
        List<DayRow> rows,

        /** Cac ngay dung khoang cach xap xi — hien rieng, co ghi chu (BR08-03). */
        List<DayRow> approximateRows,

        /** Ngay bi loai vi moi tuyen deu da huy (BR08-04). Hien ra de bao cao trung thuc. */
        List<LocalDate> cancelledDates,

        PeriodTotals totals,

        Chart chart) {

    public boolean isEmpty() {
        return rows.isEmpty() && approximateRows.isEmpty();
    }

    /**
     * Mot dong cua bang — la mot ngay giao, lay tu lan chay gan nhat cua ngay do.
     *
     * Khoang cach giu nguyen don vi MET nhu trong CSDL, chia 1000 luc hien thi.
     */
    public record DayRow(LocalDate deliveryDate,
                         int orderCount,
                         int routeCount,
                         double baselineADistance,
                         double baselineBDistance,
                         double optimizedDistance,
                         double reductionAMeters,
                         double reductionAPercent,
                         double reductionBMeters,
                         double reductionBPercent,
                         int tripsSaved,
                         AlgorithmType algorithm,
                         Long executionTimeMs,
                         boolean approximate) {
    }

    /**
     * Dong trung binh toan ky.
     *
     * PHAN TRAM TINH TU TONG QUANG DUONG chu khong phai trung binh cong cac phan
     * tram cua tung ngay. Hai cach ra hai so khac nhau: trung binh cong coi ngay
     * 3 don va ngay 15 don nang nhu nhau, con tinh tu tong thi ngay nhieu don co
     * trong so lon hon — dung nhu y nghia "ca ky giam bao nhieu". Neu hoi dong hoi
     * "so nay tinh the nao" thi day la cau tra loi.
     */
    public record PeriodTotals(int dayCount,
                               int orderCount,
                               int routeCount,
                               double baselineADistance,
                               double baselineBDistance,
                               double optimizedDistance,
                               double reductionAMeters,
                               double reductionAPercent,
                               double reductionBMeters,
                               double reductionBPercent,
                               int tripsSaved,
                               double weightUtilization,
                               double volumeUtilization) {
    }

    /**
     * Bieu do duong ve bang SVG, toa do tinh san o tang service.
     *
     * Vi sao ve tay bang SVG chu khong nap Chart.js: bieu do nay chi co hai duong
     * va vai chuc diem, ma them mot thu vien la them 200 KB phai tai ve, phai giai
     * thich khi van dap, va phai chay duoc luc mat mang. SVG thi trinh duyet nao
     * cung ve duoc san.
     */
    public record Chart(List<Column> columns,
                        String pointsA,
                        String pointsB,
                        List<GridLine> gridLines,
                        double maxPercent) {

        /** Mot ngay tren truc ngang. Toa do da doi sang he toa do cua the SVG. */
        public record Column(String label, double x, double yA, double yB) {
        }

        /** Mot duong ke ngang kem nhan phan tram ben trai. */
        public record GridLine(double y, String label) {
        }
    }
}
