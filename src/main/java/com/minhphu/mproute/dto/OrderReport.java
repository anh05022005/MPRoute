package com.minhphu.mproute.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Du lieu cua bao cao RPT-01 — thong ke don hang (CN-08-01).
 *
 * Bao cao nay tra loi cau "ky vua roi lam duoc bao nhieu": bao nhieu don, bao nhieu kg,
 * bao nhieu thung moi mat hang. Khac han RPT-03 — cai kia do HIEU QUA cua thuat toan,
 * cai nay do KHOI LUONG cong viec.
 *
 * Gop theo ngay, tuan hay thang la do nguoi xem chon. Cung mot du lieu goc, chi khac cach
 * gom: xem theo ngay de biet hom nao dot bien, xem theo thang de nhin xu the.
 */
public record OrderReport(Grouping grouping,
                          List<PeriodRow> rows,
                          List<ProductBoxCount> products,
                          Totals totals,
                          BarChart chart) {

    public boolean isEmpty() {
        return rows.isEmpty();
    }

    /** Ba cach gom du lieu, dung theo yeu cau "so don theo ngay/tuan/thang" cua CN-08-01. */
    public enum Grouping {

        DAY("Theo ngày"),
        WEEK("Theo tuần"),
        MONTH("Theo tháng");

        private final String displayName;

        Grouping(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Mot dong bang: mot ngay, mot tuan hoac mot thang tuy cach gom.
     *
     * label la chuoi da dinh dang san o tang service ("21/08/2026", "Tuần 34/2026",
     * "08/2026") — Thymeleaf chi viec do ra, khong phai biet dang gom kieu gi.
     */
    public record PeriodRow(String label,
                            long orderCount,
                            double totalWeight,
                            double totalVolume,
                            BigDecimal totalAmount) {
    }

    public record Totals(long orderCount,
                         double totalWeight,
                         double totalVolume,
                         BigDecimal totalAmount,
                         long boxCount) {
    }

    /**
     * Bieu do cot so don theo tung ky (CN-08-01).
     *
     * Toa do tinh san o tang service giong bieu do duong cua RPT-03, va cung mot ly do:
     * hai bieu do cua ca phan mem thi khong dang nap mot thu vien ve bieu do, va trang
     * phai ve duoc khi may khong co mang.
     */
    public record BarChart(List<Bar> bars, List<GridLine> gridLines, long maxValue) {

        /** Mot cot. x, y, width, height da o he toa do cua the SVG. */
        public record Bar(String label, long value, double x, double y,
                          double width, double height) {
        }

        public record GridLine(double y, String label) {
        }
    }
}
