package com.minhphu.mproute.repository;

import com.minhphu.mproute.entity.StatusHistory;
import com.minhphu.mproute.entity.enums.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {

    /**
     * Nguon du lieu ve HANH TRINH DON HANG tren trang tra cuu cong khai (CN-07-04).
     * Sap theo thoi gian tang dan de khach doc tu tren xuong (BR07-06).
     * Index idx_status_history_entity phuc vu dung truy van nay.
     */
    List<StatusHistory> findByEntityTypeAndEntityIdOrderByChangedAtAsc(
            EntityType entityType, Long entityId);
}
