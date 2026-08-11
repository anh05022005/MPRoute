package com.minhphu.mproute.repository;

import com.minhphu.mproute.entity.Notification;
import com.minhphu.mproute.entity.enums.NotificationSendStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    long countByRecipientIdAndIsReadFalse(Long recipientId);

    // Cac thong bao chua gui duoc, de dieu phoi chu dong goi dien cho khach (BR04-12)
    List<Notification> findBySendStatus(NotificationSendStatus sendStatus);
}
