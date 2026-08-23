/*
 * Chon toa do cho form khach hang va form kho (CN-01-05).
 *
 * Ba cach dat toa do, du ca ba deu ghi vao cung hai o latitude/longitude:
 *   1. Bam "Tim toa do" -> goi /admin/geocode -> chon mot dong trong danh sach (BR01-05)
 *   2. Bam thang vao ban do, hoac keo ghim (BR01-06 - khi Nominatim khong ra gi)
 *   3. Go tay vao hai o - van luon lam duoc, ban do se nhay theo
 */
(function () {
    'use strict';

    var mapContainer = document.getElementById('location-map');
    if (!mapContainer) {
        return; // trang nay khong co ban do, khong lam gi ca
    }

    var latitudeInput = document.getElementById('latitude');
    var longitudeInput = document.getElementById('longitude');
    var addressInput = document.getElementById('address');
    var searchButton = document.getElementById('geocode-search');
    var resultsBox = document.getElementById('geocode-results');
    var statusLabel = document.getElementById('geocode-status');

    // Kho Phan Trong Tue - lay lam tam khi chua co toa do nao
    var DEFAULT_CENTER = [20.9497, 105.8172];
    var ZOOM_FAR = 12;
    var ZOOM_NEAR = 16;

    // Cot latitude/longitude trong CSDL la DECIMAL(10,7), lam tron dung 7 chu so
    // thap phan de so hien tren man hinh khop het voi so luu xuong
    function round7(value) {
        return Number(value).toFixed(7);
    }

    function readCoordinates() {
        var latitude = parseFloat(latitudeInput.value);
        var longitude = parseFloat(longitudeInput.value);
        if (isNaN(latitude) || isNaN(longitude)) {
            return null;
        }
        return [latitude, longitude];
    }

    var currentCoordinates = readCoordinates();

    var map = L.map('location-map').setView(
        currentCoordinates || DEFAULT_CENTER,
        currentCoordinates ? ZOOM_NEAR : ZOOM_FAR);

    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; OpenStreetMap'
    }).addTo(map);

    var marker = null;

    function placeMarker(latitude, longitude, zoomIn) {
        if (marker === null) {
            marker = L.marker([latitude, longitude], { draggable: true }).addTo(map);
            // Keo ghim xong thi cap nhat lai hai o
            marker.on('dragend', function () {
                var position = marker.getLatLng();
                writeCoordinates(position.lat, position.lng);
            });
        } else {
            marker.setLatLng([latitude, longitude]);
        }
        if (zoomIn) {
            map.setView([latitude, longitude], ZOOM_NEAR);
        }
    }

    function writeCoordinates(latitude, longitude) {
        latitudeInput.value = round7(latitude);
        longitudeInput.value = round7(longitude);
    }

    if (currentCoordinates) {
        placeMarker(currentCoordinates[0], currentCoordinates[1], false);
    }

    // ---- Cach 2: bam thang vao ban do de ghim tay (BR01-06) ----
    map.on('click', function (event) {
        placeMarker(event.latlng.lat, event.latlng.lng, false);
        writeCoordinates(event.latlng.lat, event.latlng.lng);
        hideResults();
    });

    // ---- Cach 3: go tay vao hai o thi ban do nhay theo ----
    function onManualEdit() {
        var coordinates = readCoordinates();
        if (coordinates) {
            placeMarker(coordinates[0], coordinates[1], true);
        }
    }
    latitudeInput.addEventListener('change', onManualEdit);
    longitudeInput.addEventListener('change', onManualEdit);

    // ---- Cach 1: tim theo dia chi ----
    function hideResults() {
        resultsBox.innerHTML = '';
        resultsBox.classList.add('d-none');
    }

    function showResults(results) {
        resultsBox.innerHTML = '';
        results.forEach(function (result) {
            var button = document.createElement('button');
            button.type = 'button'; // lai la nut trong form, phai chan submit
            button.className = 'list-group-item list-group-item-action py-1 small';

            // textContent chu khong phai innerHTML: chuoi nay do Nominatim tra ve,
            // khong duoc dung no de dung the HTML
            var nameLine = document.createElement('div');
            nameLine.textContent = result.displayName;

            // Hien ca toa do de soat bang mat truoc khi chon: khach noi thanh Ha Noi
            // phai vao khoang 20.9-21.1 va 105.7-105.9, lech xa la Nominatim doan sai
            var coordinateLine = document.createElement('div');
            coordinateLine.className = 'text-sub';
            coordinateLine.textContent = round7(result.latitude) + ', ' + round7(result.longitude);

            button.appendChild(nameLine);
            button.appendChild(coordinateLine);

            button.addEventListener('click', function () {
                placeMarker(result.latitude, result.longitude, true);
                writeCoordinates(result.latitude, result.longitude);
                hideResults();
                statusLabel.textContent =
                    'Đã đặt ghim. Chưa đúng chỗ thì kéo ghim, hoặc bấm chỗ khác trên bản đồ.';
            });
            resultsBox.appendChild(button);
        });
        resultsBox.classList.remove('d-none');
    }

    searchButton.addEventListener('click', function () {
        var address = (addressInput.value || '').trim();
        if (address === '') {
            statusLabel.textContent = 'Nhập địa chỉ ở ô trên trước đã.';
            addressInput.focus();
            return;
        }

        hideResults();
        statusLabel.textContent = 'Đang tìm...';
        searchButton.disabled = true;

        fetch('/admin/geocode?q=' + encodeURIComponent(address), {
            headers: { 'Accept': 'application/json' }
        })
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('HTTP ' + response.status);
                }
                return response.json();
            })
            .then(function (results) {
                if (!results || results.length === 0) {
                    // BR01-06: khong co ket qua thi bat ghim tay
                    statusLabel.textContent =
                        'Không tìm thấy địa chỉ này. Bấm vào bản đồ để ghim tay.';
                    return;
                }

                /*
                 * LUON hien danh sach, ke ca khi chi co MOT ket qua, va khong tu di
                 * chuyen ban do truoc khi nguoi dung bam chon.
                 *
                 * Truoc day co doan "mot ket qua thi chon luon cho do phai bam", da bo.
                 * Ly do: Nominatim o Viet Nam thuong chi doc duoc den muc TEN DUONG chu
                 * khong ra so nha, nen "ket qua duy nhat" rat hay la mot dia diem khac han.
                 * Vi du that: "91 Chua Boc, Dong Da" tra ve dung mot ket qua la
                 * "Cau vuot Tay Son". Tu nhay toi do khien nguoi dung tuong man hinh hong,
                 * va nguy hiem hon la ho co the luu nham toa do do ma khong biet.
                 */
                statusLabel.textContent = (results.length === 1)
                    ? 'Tìm thấy 1 địa điểm — xem có đúng không rồi bấm vào dòng dưới:'
                    : 'Tìm thấy ' + results.length + ' địa điểm, chọn một dòng:';
                showResults(results);
            })
            .catch(function (error) {
                // Mat mang hay Nominatim chan deu roi vao day. Khong duoc de man hinh
                // dung hinh — nguoi dung phai ghim tay duoc de con luu ho so
                statusLabel.textContent =
                    'Không gọi được dịch vụ tìm tọa độ. Bấm vào bản đồ để ghim tay.';
                console.warn('Loi goi /admin/geocode:', error);
            })
            .finally(function () {
                searchButton.disabled = false;
            });
    });
})();
