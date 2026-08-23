/*
 * Ban do man hinh phan tuyen (CN-03-02).
 *
 * Tuan 5 moi ve DIEM: kho la o vuong dam, khach la ghim tron, re chuot hien ten khach
 * va so thung. Ve DUONG DI cua tung tuyen la CN-03-09, lam o tuan 8 — luc do phai lay
 * mang geometry tu OSRM Route API chu KHONG duoc noi thang toa do cac khach voi nhau.
 *
 * File nay tach rieng khoi location-picker.js: hai man hinh dung ban do vao hai viec
 * khac han nhau. Ben kia la chon MOT toa do va ghi nguoc vao o nhap, ben nay la hien
 * NHIEU diem chi de nhin.
 */
(function () {
    'use strict';

    var container = document.getElementById('routing-map');
    if (!container) {
        return;   // trang nay khong co ban do phan tuyen
    }

    // Kho Phan Trong Tue — dung lam tam khi chua co diem nao
    var DEFAULT_CENTER = [20.9497, 105.8172];
    var DEFAULT_ZOOM = 12;

    var DEPOT_COLOR = '#0B2C4A';
    var CUSTOMER_COLOR = '#1E77BF';

    var points = readPoints();

    var map = L.map('routing-map').setView(DEFAULT_CENTER, DEFAULT_ZOOM);
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; OpenStreetMap'
    }).addTo(map);

    drawPoints();

    /** Doc khoi JSON do may chu dat san trong trang. */
    function readPoints() {
        var block = document.getElementById('map-points-data');
        if (!block) {
            return [];
        }
        try {
            return JSON.parse(block.textContent) || [];
        } catch (ex) {
            console.warn('Du lieu diem khong doc duoc:', ex);
            return [];
        }
    }

    function drawPoints() {
        if (points.length === 0) {
            return;
        }

        var coordinates = [];

        points.forEach(function (point) {
            var marker = point.depot ? depotMarker(point) : customerMarker(point);
            marker.addTo(map);

            /*
             * Tooltip chu khong phai popup: re chuot la hien, khong phai bam.
             * Dieu phoi vien quet mat qua ca vung ban do de tim diem le loi, bam tung
             * cai mot thi cham.
             */
            marker.bindTooltip(tooltipHtml(point), { direction: 'top' });
            coordinates.push([point.latitude, point.longitude]);
        });

        // Phong ban do vua khit tat ca cac diem, chua thua vien 30px cho ghim khong bi cat
        map.fitBounds(L.latLngBounds(coordinates), { padding: [30, 30] });
    }

    /** Kho ve thanh o vuong dam de phan biet ngay voi ghim khach (docs/05). */
    function depotMarker(point) {
        var icon = L.divIcon({
            className: '',
            html: '<div style="width:16px;height:16px;background:' + DEPOT_COLOR + ';'
                + 'border:2px solid #fff;box-shadow:0 0 3px rgba(0,0,0,.6);"></div>',
            iconSize: [16, 16],
            iconAnchor: [8, 8]
        });
        return L.marker([point.latitude, point.longitude], { icon: icon, zIndexOffset: 1000 });
    }

    /** Khach ve thanh cham tron, nho hon o vuong kho. */
    function customerMarker(point) {
        return L.circleMarker([point.latitude, point.longitude], {
            radius: 7,
            color: '#fff',
            weight: 2,
            fillColor: CUSTOMER_COLOR,
            fillOpacity: 0.9
        });
    }

    /*
     * Dung chuoi tooltip bang cach noi chuoi HTML, nhung ten khach do nguoi dung nhap
     * nen phai thay the cac ky tu dac biet truoc — khong lam la mo duong cho ma doc
     * chay ngay tren man hinh dieu phoi.
     */
    function tooltipHtml(point) {
        var title = '<strong>' + escapeHtml(point.name) + '</strong>';
        return point.description ? title + '<br>' + escapeHtml(point.description) : title;
    }

    function escapeHtml(text) {
        var holder = document.createElement('div');
        holder.textContent = text == null ? '' : text;
        return holder.innerHTML;
    }
})();
