package com.minhphu.mproute.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Trang thai gui thong bao.
 * Khach khong co email van ghi ban ghi voi trang thai NOT_SENT,
 * de dieu phoi vien biet ma chu dong goi dien (BR04-12).
 */
@Getter
@RequiredArgsConstructor
public enum NotificationSendStatus {

    NOT_SENT("Chưa gửi"),
    SENT("Đã gửi"),
    ERROR("Lỗi gửi");

    private final String displayName;
}
