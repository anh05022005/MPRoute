package com.minhphu.mproute.repository;

import com.minhphu.mproute.entity.Depot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DepotRepository extends JpaRepository<Depot, Long> {

    // Chi mot kho duoc danh dau active (BR01-10), phan tuyen luon xuat phat tu kho nay
    Optional<Depot> findByActiveTrue();

    List<Depot> findAllByOrderByNameAsc();

    /** Con kho nao KHAC dang hoat dong khong. Dung de khong tat mat kho cuoi cung. */
    boolean existsByActiveTrueAndIdNot(Long id);

    boolean existsByActiveTrue();

    /**
     * Tat co active cua moi kho khac (BR01-10).
     *
     * Dung mot cau UPDATE thay vi nap het ve roi sua tung ban ghi: viec nay phai
     * xay ra gon trong mot cau lenh, khong de lot trang thai co hai kho cung active.
     *
     * clearAutomatically = true de cac thuc the dang nam trong bo nho khong con giu
     * gia tri active cu sau khi cau UPDATE chay thang xuong CSDL.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Depot d SET d.active = false WHERE d.id <> :keepId")
    int deactivateOthers(@Param("keepId") Long keepId);
}
