package com.minhphu.mproute.service;

import com.minhphu.mproute.entity.StatusHistory;
import com.minhphu.mproute.entity.User;
import com.minhphu.mproute.entity.enums.EntityType;
import com.minhphu.mproute.repository.StatusHistoryRepository;
import com.minhphu.mproute.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Ghi lich su chuyen trang thai (CBR-14).
 *
 * Dat rieng mot service vi CA don hang LAN tuyen deu ghi vao bang nay. Viet mot cho
 * de sau nay khong co chuyen man hinh tuyen quen ghi con man hinh don thi nho.
 *
 * Quen ghi thi trang tra cuu cong khai /track khong co du lieu de ve hanh trinh
 * don hang (CN-07-04, BR07-06) — day la loi im lang, chay van chay, chi den luc
 * mo trang tra cuu moi phat hien.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StatusHistoryService {

    private final StatusHistoryRepository statusHistoryRepository;
    private final UserRepository userRepository;

    /**
     * Ghi mot lan chuyen trang thai.
     *
     * @param trangThaiCu null khi day la ban ghi vua duoc tao (chua co trang thai truoc do)
     * @param nguoiThucHienId null khi he thong tu chuyen chu khong phai nguoi dung bam nut
     */
    @Transactional
    public void recordChange(EntityType entityType,
                    Long entityId,
                    String oldStatus,
                    String newStatus,
                    Long changedById,
                    String note) {

        StatusHistory entry = new StatusHistory();
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setOldStatus(oldStatus);
        entry.setNewStatus(newStatus);
        entry.setChangedAt(LocalDateTime.now());
        entry.setNote(note);

        if (changedById != null) {
            User changedBy = userRepository.findById(changedById).orElse(null);
            entry.setChangedBy(changedBy);
        }

        statusHistoryRepository.save(entry);
        log.info("Lich su trang thai: {} #{} tu {} sang {}",
                entityType, entityId, oldStatus, newStatus);
    }

    /** Hanh trinh cua mot don hang hoac mot tuyen, sap theo thoi gian tang dan. */
    @Transactional(readOnly = true)
    public List<StatusHistory> historyOf(EntityType entityType, Long entityId) {
        return statusHistoryRepository
                .findByEntityTypeAndEntityIdOrderByChangedAtAsc(entityType, entityId);
    }
}
