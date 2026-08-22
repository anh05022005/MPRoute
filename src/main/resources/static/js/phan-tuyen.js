/*
 * Ban do man hinh phan tuyen (CN-03-02).
 *
 * Tuan 5 moi ve DIEM: kho la o vuong dam, khach la ghim tron, re chuot hien ten khach
 * va so thung. Ve DUONG DI cua tung tuyen la CN-03-09, lam o tuan 8 — luc do phai lay
 * mang geometry tu OSRM Route API chu KHONG duoc noi thang toa do cac khach voi nhau.
 *
 * File nay tach rieng khoi chon-toa-do.js: hai man hinh dung ban do vao hai viec khac
 * han nhau. Ben kia la chon MOT toa do va ghi nguoc vao o nhap, ben nay la hien NHIEU
 * diem chi de nhin.
 */
(function () {
    'use strict';

    var oBanDo = document.getElementById('ban-do-phan-tuyen');
    if (!oBanDo) {
        return;   // trang nay khong co ban do phan tuyen
    }

    // Kho Phan Trong Tue — dung lam tam khi chua co diem nao
    var TAM_MAC_DINH = [20.9497, 105.8172];
    var MUC_PHONG_TO_XA = 12;

    var cacDiem = docDuLieuDiem();

    var banDo = L.map('ban-do-phan-tuyen').setView(TAM_MAC_DINH, MUC_PHONG_TO_XA);
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; OpenStreetMap'
    }).addTo(banDo);

    veCacDiem();

    /** Doc khoi JSON do may chu dat san trong trang. */
    function docDuLieuDiem() {
        var khoi = document.getElementById('du-lieu-diem');
        if (!khoi) {
            return [];
        }
        try {
            return JSON.parse(khoi.textContent) || [];
        } catch (loi) {
            console.warn('Du lieu diem khong doc duoc:', loi);
            return [];
        }
    }

    function veCacDiem() {
        if (cacDiem.length === 0) {
            return;
        }

        var cacToaDo = [];

        cacDiem.forEach(function (diem) {
            var ghim = diem.laKho ? ghimKho(diem) : ghimKhach(diem);
            ghim.addTo(banDo);

            /*
             * Tooltip chu khong phai popup: re chuot la hien, khong phai bam.
             * Dieu phoi vien quet mat qua ca vung ban do de tim diem le loi, bam tung
             * cai mot thi cham.
             */
            ghim.bindTooltip(tenVaMoTa(diem), { direction: 'top' });
            cacToaDo.push([diem.viDo, diem.kinhDo]);
        });

        // Phong ban do vua khit tat ca cac diem, chua thua vien 30px cho ghim khong bi cat
        banDo.fitBounds(L.latLngBounds(cacToaDo), { padding: [30, 30] });
    }

    /** Kho ve thanh o vuong dam de phan biet ngay voi ghim khach (docs/05). */
    function ghimKho(diem) {
        var bieuTuong = L.divIcon({
            className: '',
            html: '<div style="width:16px;height:16px;background:#0d3b66;'
                + 'border:2px solid #fff;box-shadow:0 0 3px rgba(0,0,0,.6);"></div>',
            iconSize: [16, 16],
            iconAnchor: [8, 8]
        });
        return L.marker([diem.viDo, diem.kinhDo], { icon: bieuTuong, zIndexOffset: 1000 });
    }

    /** Khach ve thanh cham tron, nho hon o vuong kho. */
    function ghimKhach(diem) {
        return L.circleMarker([diem.viDo, diem.kinhDo], {
            radius: 7,
            color: '#fff',
            weight: 2,
            fillColor: '#1f78b4',
            fillOpacity: 0.9
        });
    }

    /*
     * Dung chuoi tooltip bang cach noi chuoi HTML, nhung ten khach do nguoi dung nhap
     * nen phai thay the cac ky tu dac biet truoc — khong lam la mo duong cho ma doc
     * chay ngay tren man hinh dieu phoi.
     */
    function tenVaMoTa(diem) {
        var ten = '<strong>' + thoat(diem.ten) + '</strong>';
        return diem.moTa ? ten + '<br>' + thoat(diem.moTa) : ten;
    }

    function thoat(chuoi) {
        var o = document.createElement('div');
        o.textContent = chuoi == null ? '' : chuoi;
        return o.innerHTML;
    }
})();
