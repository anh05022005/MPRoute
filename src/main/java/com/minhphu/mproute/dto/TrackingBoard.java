package com.minhphu.mproute.dto;

import com.minhphu.mproute.entity.enums.RouteStatus;
import com.minhphu.mproute.entity.enums.StopStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Du lieu cho SCR-10 — man hinh theo doi tien do giao hang trong ngay cua dieu phoi.
 *
 * <p>Khac voi SCR-13 (/track) danh cho khach hang: o day nguoi xem da dang nhap va co vai tro
 * ADMIN hoac DISPATCHER, nen duoc thay day du bien so xe, ten tai xe, so tien phai thu — dung
 * nhung thu ma BR07-04 cam hien tren trang tra cuu cong khai.
 *
 * <p>Moi con so deu tinh san o tang service. Template chi do ra, khong lam phep tinh nao —
 * ke ca phep chia 1000 doi met sang ki lo met.
 */
public record TrackingBoard(LocalDate date,
                            Totals totals,
                            List<RouteRow> routes) {

    /**
     * Tong hop ca ngay.
     *
     * <p>{@code handled} la so diem dung da xu ly xong theo bat ky huong nao: giao duoc, giao
     * that bai, hoac hoan sang ngay khac. Do tien do phai dem ca ba, vi mot diem giao that bai
     * van la mot diem tai xe da den va da xu ly — khong con cho ai nua.
     *
     * <p>Con {@code delivered} dem rieng cac diem giao THANH CONG, vi day moi la con so dieu
     * phoi quan tam ve chat luong. Hai con so nay khac nhau, de lan lon la doc sai man hinh.
     */
    public record Totals(int routeCount,
                         int stopCount,
                         int delivered,
                         int failed,
                         int postponed,
                         int pending,
                         int handled,
                         double percentHandled) {
    }

    public record RouteRow(String code,
                           String vehiclePlate,
                           String driverName,
                           RouteStatus status,
                           double totalDistanceKm,
                           int stopCount,
                           int delivered,
                           int handled,
                           double percentHandled,
                           LocalDateTime departedAt,
                           List<StopRow> stops) {
    }

    /**
     * Mot diem dung. {@code failureReason} da doi sang chuoi hien thi tieng Viet o service,
     * de trong khi diem dung chua that bai.
     */
    public record StopRow(int sequence,
                          String orderCode,
                          String customerName,
                          String address,
                          StopStatus status,
                          LocalDateTime estimatedArrival,
                          LocalDateTime actualArrival,
                          String failureReason,
                          BigDecimal amountToCollect,
                          BigDecimal collectedAmount) {
    }
}
