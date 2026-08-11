package com.minhphu.mproute.entity;

import com.minhphu.mproute.entity.enums.NotificationSendStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Thong bao trong he thong (cho nguoi dung) hoac email (cho khach hang).
 *
 * Khach khong co email VAN ghi ban ghi voi sendStatus = NOT_SENT,
 * de dieu phoi vien biet ma chu dong goi dien (BR04-12).
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // NULL khi gui cho khach hang qua email thay vi cho nguoi dung trong he thong
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id")
    private User recipient;

    @Column(name = "recipient_email", length = 100)
    private String recipientEmail;

    // Ma su kien NTF-01 den NTF-09, xem docs/01-nghiep-vu.md
    @Column(name = "event_code", nullable = false, length = 20)
    private String eventCode;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @Column(name = "link", length = 255)
    private String link;

    // Dat ten is_read chu khong phai read cho ro day la co boolean
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "send_status", nullable = false, length = 20)
    private NotificationSendStatus sendStatus = NotificationSendStatus.NOT_SENT;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
