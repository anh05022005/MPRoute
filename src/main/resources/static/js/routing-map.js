/*
 * Ban do man hinh phan tuyen (CN-03-02, CN-03-09).
 *
 * Ve ba thu, theo dung thu tu tu duoi len de lop tren khong bi che:
 *   1. DUONG DI cua tung tuyen, moi tuyen mot mau (CN-03-09)
 *   2. GHIM DANH SO cua cac diem da co tuyen, mau theo tuyen, so la thu tu giao
 *   3. Kho (o vuong dam) va cac don CHUA xep duoc tuyen (cham xam rong ruot)
 *
 * DIEM DE SAI NHAT: duong di phai lay mang geometry tu OSRM Route API, KHONG duoc noi
 * thang toa do cac khach voi nhau. May chu da lay san va da dao [kinh do, vi do] cua
 * GeoJSON thanh [vi do, kinh do] cua Leaflet, nen o day chi viec ve.
 *
 * Tuyen nao co approximate = true nghia la OSRM khong tra loi va may chu da lui ve doan
 * thang. Ve NET DUT cho truong hop do — nhin la biet ngay day khong phai duong that,
 * khong duoc ve giong het duong that roi de nguoi xem tuong nham (CBR-11).
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
    var UNASSIGNED_COLOR = '#8B97A5';

    var points = readJson('map-points-data');
    var routeLines = readJson('route-lines-data');

    var map = L.map('routing-map').setView(DEFAULT_CENTER, DEFAULT_ZOOM);
    // Nen OpenStreetMap chuan — vi sao khong dung CARTO, xem location-picker.js
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; OpenStreetMap'
    }).addTo(map);

    // Gom toa do cua moi thu da ve de phong ban do vua khit o cuoi
    var drawnCoordinates = [];

    drawRoutes();
    drawPoints();
    fitToEverything();

    /** Doc khoi JSON do may chu dat san trong trang. */
    function readJson(elementId) {
        var block = document.getElementById(elementId);
        if (!block) {
            return [];
        }
        try {
            return JSON.parse(block.textContent) || [];
        } catch (ex) {
            console.warn('Du lieu ban do khong doc duoc (' + elementId + '):', ex);
            return [];
        }
    }

    // ==================== CN-03-09 duong di tung tuyen ====================

    function drawRoutes() {
        routeLines.forEach(function (line) {
            drawPath(line);
            drawNumberedStops(line);
        });
    }

    function drawPath(line) {
        if (!line.path || line.path.length < 2) {
            return;
        }

        var polyline = L.polyline(line.path, {
            color: line.color,
            weight: 4,
            opacity: 0.85,
            // Net dut = duong nay chi la doan thang noi diem, khong phai duong di that
            dashArray: line.approximate ? '6, 6' : null
        }).addTo(map);

        polyline.bindTooltip(escapeHtml(line.code) + ' — ' + escapeHtml(line.licensePlate)
            + (line.approximate ? '<br>Đường thẳng xấp xỉ, OSRM không chạy' : ''),
            { sticky: true });

        line.path.forEach(function (coordinate) {
            drawnCoordinates.push(coordinate);
        });
    }

    /**
     * Ghim danh so thu tu giao, to mau theo tuyen.
     *
     * So tren ghim chinh la cot "#" cua bang ket qua ben canh, nho vay dieu phoi vien doi
     * chieu duoc bang voi ban do ma khong phai dem tay.
     */
    function drawNumberedStops(line) {
        (line.stops || []).forEach(function (stop) {
            var marker = L.marker([stop.latitude, stop.longitude], {
                icon: stopIcon(stop.sequence, line.color),
                zIndexOffset: 500
            }).addTo(map);

            var text = '<strong>' + escapeHtml(stop.customerName) + '</strong>'
                + '<br>' + escapeHtml(line.code) + ' · điểm ' + stop.sequence
                + '<br>' + escapeHtml(stop.orderCode);
            if (stop.estimatedArrival) {
                text += '<br>Dự kiến ' + escapeHtml(stop.estimatedArrival);
            }
            marker.bindTooltip(text, { direction: 'top' });

            drawnCoordinates.push([stop.latitude, stop.longitude]);
        });
    }

    /** Ghim tron co so ben trong, dung divIcon vi Leaflet khong ve san kieu nay. */
    function stopIcon(sequence, color) {
        return L.divIcon({
            className: '',
            html: '<div class="route-pin" style="background:' + color + '">' + sequence + '</div>',
            iconSize: [24, 24],
            iconAnchor: [12, 12]
        });
    }

    // ==================== CN-03-02 kho va don chua xep tuyen ====================

    function drawPoints() {
        points.forEach(function (point) {
            var marker = point.depot ? depotMarker(point) : unassignedMarker(point);
            marker.addTo(map);

            /*
             * Tooltip chu khong phai popup: re chuot la hien, khong phai bam.
             * Dieu phoi vien quet mat qua ca vung ban do de tim diem le loi, bam tung
             * cai mot thi cham.
             */
            marker.bindTooltip(tooltipHtml(point), { direction: 'top' });
            drawnCoordinates.push([point.latitude, point.longitude]);
        });
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

    /**
     * Don chua xep duoc vao tuyen nao: cham RONG RUOT mau xam.
     *
     * Co y ve khac han ghim danh so — chua co tuyen nghia la chua co ai di giao, dieu phoi
     * vien phai nhin ra ngay chu khong de lan vao dam diem da xep (CN-03-14).
     */
    function unassignedMarker(point) {
        return L.circleMarker([point.latitude, point.longitude], {
            radius: 7,
            color: routeLines.length > 0 ? UNASSIGNED_COLOR : CUSTOMER_COLOR,
            weight: 2,
            fillColor: '#fff',
            fillOpacity: routeLines.length > 0 ? 1 : 0.9
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

    function fitToEverything() {
        if (drawnCoordinates.length === 0) {
            return;
        }
        // Chua thua vien 30px de ghim o mep khong bi cat mat mot nua
        map.fitBounds(L.latLngBounds(drawnCoordinates), { padding: [30, 30] });
    }

    function escapeHtml(text) {
        var holder = document.createElement('div');
        holder.textContent = text == null ? '' : text;
        return holder.innerHTML;
    }
})();
