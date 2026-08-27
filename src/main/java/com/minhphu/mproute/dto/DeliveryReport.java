package com.minhphu.mproute.dto;

import com.minhphu.mproute.entity.enums.FailureReason;
import com.minhphu.mproute.entity.enums.OrderStatus;

import java.util.List;

/**
 * Du lieu cua bao cao RPT-02 — ty le giao thanh cong (CN-08-02).
 *
 * TY LE TINH THEO BR08-06:
 *
 *     ty le thanh cong = so don DELIVERED / tong don DA PHAN TUYEN
 *
 * Mau so la don DA PHAN TUYEN chu khong phai moi don trong ky. Don con dang cho duyet
 * hay da huy thi chua bao gio buoc len xe, dem chung vao mau so la tu ha ty le cua minh
 * xuong vi mot ly do khong lien quan gi toi viec giao hang.
 *
 * Phan tich ly do that bai doc tu route_stops — xem giai thich o FailureReasonCount.
 */
public record DeliveryReport(List<StatusRow> statusRows,
                             List<FailureRow> failureRows,
                             long routedOrderCount,
                             long deliveredCount,
                             long failedCount,
                             long postponedCount,
                             long inProgressCount,
                             long cancelledCount,
                             double successPercent,
                             double failurePercent,
                             double postponePercent,
                             DonutChart chart) {

    public boolean isEmpty() {
        return routedOrderCount == 0 && cancelledCount == 0;
    }

    /** Mot dong cua bang trang thai. share la phan tram tren tong don da phan tuyen. */
    public record StatusRow(OrderStatus status, long orderCount, double share, String styleClass) {
    }

    /** Mot dong cua bang phan tich that bai. share la phan tram tren tong so lan that bai. */
    public record FailureRow(FailureReason reason, long stopCount, double share) {
    }

    /**
     * Bieu do tron, ve bang mot vong tron duy nhat.
     *
     * CACH VE: moi mieng la mot the <circle> cung tam, cung ban kinh, chi khac stroke.
     * stroke-dasharray cat vien thanh "phan to mau" va "phan bo trong", con stroke-dashoffset
     * xoay diem bat dau cua mieng do ve dung cho. Cong lai thanh mot hinh tron day du.
     *
     * Vi sao khong ve bang the <path> voi cung tron: cung tron phai tinh sin/cos cho tung
     * diem dau cuoi va phai doi co cung lon hon nua duong tron, sai mot dau la mieng banh
     * lat nguoc. Cach dasharray chi can mot phep nhan, va khong co truong hop dac biet nao.
     */
    public record DonutChart(List<Slice> slices, double circumference) {

        public record Slice(String label, long value, double share, String styleClass,
                            double dashLength, double dashGap, double dashOffset) {
        }
    }
}
