package com.minhphu.mproute.service;

import com.minhphu.mproute.dto.DepotForm;
import com.minhphu.mproute.entity.Depot;
import com.minhphu.mproute.repository.DepotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Nghiep vu danh muc kho (CN-01-06).
 *
 * Ca lop nay xoay quanh mot rang buoc duy nhat: BR01-10 — dung MOT kho active.
 * Ly do rang buoc do quan trong den vay: thuat toan phan tuyen lay kho active
 * lam diem c0. Co hai kho active thi khong biet lay cai nao; khong co cai nao
 * thi khong phan tuyen duoc.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DepotService {

    private final DepotRepository depotRepository;

    @Transactional(readOnly = true)
    public List<Depot> findAll() {
        return depotRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Depot getById(Long id) {
        return depotRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kho"));
    }

    /**
     * Kho dang hoat dong — diem c0 cua bai toan CVRP.
     *
     * Tu tuan 8 tro di, man hinh phan tuyen goi ham nay. Nem ngoai le chu khong
     * tra Optional rong: khong co kho thi khong co gi de phan tuyen ca, de lot
     * xuong duoi thanh NullPointerException kho truy hon nhieu.
     */
    @Transactional(readOnly = true)
    public Depot getActiveDepot() {
        return depotRepository.findByActiveTrue()
                .orElseThrow(() -> new IllegalStateException(
                        "Chưa có kho nào đang hoạt động. Vào Danh mục > Kho để đặt kho chính."));
    }

    /**
     * Con kho KHAC dang hoat dong hay khong.
     *
     * Controller dung ham nay de chan viec tat mat kho active cuoi cung.
     * id = null nghia la dang them moi, luc do moi kho hien co deu la "kho khac".
     */
    @Transactional(readOnly = true)
    public boolean hasAnotherActiveDepot(Long id) {
        return (id == null)
                ? depotRepository.existsByActiveTrue()
                : depotRepository.existsByActiveTrueAndIdNot(id);
    }

    @Transactional
    public Depot save(DepotForm form) {
        // id null la them moi, co id la sua ban ghi dang co
        Depot depot = (form.getId() == null) ? new Depot() : getById(form.getId());

        depot.setName(form.getName().trim());
        depot.setAddress(form.getAddress().trim());
        depot.setLatitude(form.getLatitude());
        depot.setLongitude(form.getLongitude());

        /*
         * He thong khong bao gio duoc roi vao trang thai KHONG CON kho nao active,
         * vi man hinh phan tuyen bat buoc phai co diem c0.
         *
         * Nen khi nguoi dung bo tich ma khong con kho khac dang active thi giu nguyen
         * active. Truong hop nay gom ca kho dau tien cua he thong (chua co kho nao khac)
         * lan kho active cuoi cung bi bo tich.
         *
         * Day la luoi an toan o tang service. Controller chan truoc va bao loi ro rang
         * cho nguoi dung (CBR-15) — khong dua vao mot minh cho nao.
         */
        boolean wantsActive = Boolean.TRUE.equals(form.getActive());
        boolean mustStayActive = !wantsActive && !hasAnotherActiveDepot(form.getId());
        depot.setActive(wantsActive || mustStayActive);

        Depot saved = depotRepository.save(depot);

        // BR01-10: dat kho nay active thi moi kho khac phai tat
        if (Boolean.TRUE.equals(saved.getActive())) {
            int deactivatedCount = depotRepository.deactivateOthers(saved.getId());
            if (deactivatedCount > 0) {
                log.info("Dat kho {} lam kho chinh, tat {} kho khac", saved.getName(), deactivatedCount);
            }
        }

        log.info("Luu kho {} ({}, {}) active={}",
                saved.getName(), saved.getLatitude(), saved.getLongitude(), saved.getActive());
        return saved;
    }

    /**
     * Dat mot kho lam kho chinh — nut rieng tren danh sach.
     *
     * Khong co ham "tat kho chinh": muon doi kho thi bat kho MOI len, kho cu tu tat.
     * Cach nay khong bao gio di qua trang thai trung gian khong co kho nao active.
     */
    @Transactional
    public Depot setActive(Long id) {
        Depot depot = getById(id);
        depot.setActive(true);
        depotRepository.save(depot);
        depotRepository.deactivateOthers(id);
        log.info("Kho {} tro thanh kho chinh", depot.getName());
        return depot;
    }
}
