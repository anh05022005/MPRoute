package com.minhphu.mproute.entity;

import com.minhphu.mproute.entity.enums.EntityType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Lich su chuyen trang thai cua don hang va tuyen.
 *
 * MOI lan chuyen trang thai deu phai ghi vao day kem thoi diem va nguoi thuc hien (CBR-14).
 * Quen ghi thi trang tra cuu cong khai khong co du lieu de ve hanh trinh don hang.
 *
 * Khong dung khoa ngoai vi bang nay tro toi HAI bang khac nhau (orders va routes),
 * phai dung cap (entityType, entityId) de xac dinh ban ghi goc.
 */
@Entity
@Table(
        name = "status_history",
        indexes = @Index(name = "idx_status_history_entity", columnList = "entity_type, entity_id")
)
@Getter
@Setter
@NoArgsConstructor
public class StatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "entity_type", nullable = false, length = 30)
    private EntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    // Luu dang chuoi vi cot nay chua gia tri cua OrderStatus lan RouteStatus
    @Column(name = "old_status", length = 30)
    private String oldStatus;

    @Column(name = "new_status", nullable = false, length = 30)
    private String newStatus;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    // NULL khi he thong tu chuyen trang thai chu khong phai nguoi dung thao tac
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_id")
    private User changedBy;

    @Column(name = "note", length = 500)
    private String note;
}
