package com.minhphu.mproute.repository;

import com.minhphu.mproute.entity.Driver;
import com.minhphu.mproute.entity.enums.RecordStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    // Tai xe dang nhap: tra ho so tai xe tu tai khoan de loc tuyen cua chinh ho (BR05-01)
    Optional<Driver> findByUserId(Long userId);

    List<Driver> findByStatus(RecordStatus status);

    /**
     * Man hinh danh muc: tim theo ho ten hoac so dien thoai, loc them trang thai (CN-01-03).
     * Truyen null de bo qua dieu kien. Dung tham so, khong noi chuoi SQL.
     *
     * LEFT JOIN FETCH de nap san tai khoan va xe mac dinh trong CUNG mot cau truy van.
     * Khong co no thi luc Thymeleaf doc tx.user.username va tx.defaultVehicle.licensePlate,
     * moi dong trong bang lai sinh them hai cau SELECT (van de N+1).
     * Dung LEFT chu khong dung INNER vi hai quan he nay deu duoc phep NULL.
     */
    @Query(value = """
            SELECT d FROM Driver d
            LEFT JOIN FETCH d.user
            LEFT JOIN FETCH d.defaultVehicle
            WHERE (:keyword IS NULL
                   OR LOWER(d.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR d.phone LIKE CONCAT('%', :keyword, '%'))
              AND (:status IS NULL OR d.status = :status)
            """,
            countQuery = """
            SELECT COUNT(d) FROM Driver d
            WHERE (:keyword IS NULL
                   OR LOWER(d.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR d.phone LIKE CONCAT('%', :keyword, '%'))
              AND (:status IS NULL OR d.status = :status)
            """)
    Page<Driver> search(@Param("keyword") String keyword,
                        @Param("status") RecordStatus status,
                        Pageable pageable);

    /**
     * Danh sach id tai khoan DA gan cho tai xe, tru tai xe dang sua (excludeId).
     *
     * Cot user_id dat unique nen mot tai khoan chi thuoc ve mot tai xe. Danh sach nay
     * dung de loai cac tai khoan da co chu ra khoi o chon, khong de nguoi dung chon
     * roi moi bao loi.
     */
    @Query("""
            SELECT d.user.id FROM Driver d
            WHERE d.user IS NOT NULL
              AND (:excludeId IS NULL OR d.id <> :excludeId)
            """)
    List<Long> findLinkedUserIds(@Param("excludeId") Long excludeId);
}
