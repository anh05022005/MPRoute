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

    var oBanDo = document.getElementById('ban-do');
    if (!oBanDo) {
        return; // trang nay khong co ban do, khong lam gi ca
    }

    var oViDo = document.getElementById('latitude');
    var oKinhDo = document.getElementById('longitude');
    var oDiaChi = document.getElementById('address');
    var nutTim = document.getElementById('nut-tim-toa-do');
    var khungKetQua = document.getElementById('ket-qua-tim');
    var oTrangThai = document.getElementById('trang-thai-tim');

    // Kho Phan Trong Tue - lay lam tam khi chua co toa do nao
    var TAM_MAC_DINH = [20.9497, 105.8172];
    var MUC_PHONG_TO_XA = 12;
    var MUC_PHONG_TO_GAN = 16;

    // Cot latitude/longitude trong CSDL la DECIMAL(10,7), lam tron dung 7 chu so
    // thap phan de so hien tren man hinh khop het voi so luu xuong
    function lamTron7(so) {
        return Number(so).toFixed(7);
    }

    function docToaDoDangCo() {
        var viDo = parseFloat(oViDo.value);
        var kinhDo = parseFloat(oKinhDo.value);
        if (isNaN(viDo) || isNaN(kinhDo)) {
            return null;
        }
        return [viDo, kinhDo];
    }

    var toaDoDangCo = docToaDoDangCo();

    var banDo = L.map('ban-do').setView(
        toaDoDangCo || TAM_MAC_DINH,
        toaDoDangCo ? MUC_PHONG_TO_GAN : MUC_PHONG_TO_XA);

    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; OpenStreetMap'
    }).addTo(banDo);

    var ghim = null;

    function datGhim(viDo, kinhDo, phongTo) {
        if (ghim === null) {
            ghim = L.marker([viDo, kinhDo], { draggable: true }).addTo(banDo);
            // Keo ghim xong thi cap nhat lai hai o
            ghim.on('dragend', function () {
                var viTri = ghim.getLatLng();
                ghiToaDoVaoForm(viTri.lat, viTri.lng);
            });
        } else {
            ghim.setLatLng([viDo, kinhDo]);
        }
        if (phongTo) {
            banDo.setView([viDo, kinhDo], MUC_PHONG_TO_GAN);
        }
    }

    function ghiToaDoVaoForm(viDo, kinhDo) {
        oViDo.value = lamTron7(viDo);
        oKinhDo.value = lamTron7(kinhDo);
    }

    if (toaDoDangCo) {
        datGhim(toaDoDangCo[0], toaDoDangCo[1], false);
    }

    // ---- Cach 2: bam thang vao ban do de ghim tay (BR01-06) ----
    banDo.on('click', function (suKien) {
        datGhim(suKien.latlng.lat, suKien.latlng.lng, false);
        ghiToaDoVaoForm(suKien.latlng.lat, suKien.latlng.lng);
        anDanhSachKetQua();
    });

    // ---- Cach 3: go tay vao hai o thi ban do nhay theo ----
    function theoDoiGoTay() {
        var toaDo = docToaDoDangCo();
        if (toaDo) {
            datGhim(toaDo[0], toaDo[1], true);
        }
    }
    oViDo.addEventListener('change', theoDoiGoTay);
    oKinhDo.addEventListener('change', theoDoiGoTay);

    // ---- Cach 1: tim theo dia chi ----
    function anDanhSachKetQua() {
        khungKetQua.innerHTML = '';
        khungKetQua.classList.add('d-none');
    }

    function hienDanhSachKetQua(cacKetQua) {
        khungKetQua.innerHTML = '';
        cacKetQua.forEach(function (ketQua) {
            var nut = document.createElement('button');
            nut.type = 'button'; // lai la nut trong form, phai chan submit
            nut.className = 'list-group-item list-group-item-action py-1 small';
            // textContent chu khong phai innerHTML: chuoi nay do Nominatim tra ve,
            // khong duoc dung no de dung the HTML
            nut.textContent = ketQua.displayName;
            nut.addEventListener('click', function () {
                datGhim(ketQua.latitude, ketQua.longitude, true);
                ghiToaDoVaoForm(ketQua.latitude, ketQua.longitude);
                anDanhSachKetQua();
                oTrangThai.textContent = 'Đã chọn. Kéo ghim để chỉnh lại nếu chưa đúng.';
            });
            khungKetQua.appendChild(nut);
        });
        khungKetQua.classList.remove('d-none');
    }

    nutTim.addEventListener('click', function () {
        var diaChi = (oDiaChi.value || '').trim();
        if (diaChi === '') {
            oTrangThai.textContent = 'Nhập địa chỉ ở ô trên trước đã.';
            oDiaChi.focus();
            return;
        }

        anDanhSachKetQua();
        oTrangThai.textContent = 'Đang tìm...';
        nutTim.disabled = true;

        fetch('/admin/geocode?q=' + encodeURIComponent(diaChi), {
            headers: { 'Accept': 'application/json' }
        })
            .then(function (phanHoi) {
                if (!phanHoi.ok) {
                    throw new Error('HTTP ' + phanHoi.status);
                }
                return phanHoi.json();
            })
            .then(function (cacKetQua) {
                if (!cacKetQua || cacKetQua.length === 0) {
                    // BR01-06: khong co ket qua thi bat ghim tay
                    oTrangThai.textContent =
                        'Không tìm thấy địa chỉ này. Bấm vào bản đồ để ghim tay.';
                    return;
                }
                if (cacKetQua.length === 1) {
                    // Chi mot ket qua thi chon luon, khong bat bam them mot lan nua
                    datGhim(cacKetQua[0].latitude, cacKetQua[0].longitude, true);
                    ghiToaDoVaoForm(cacKetQua[0].latitude, cacKetQua[0].longitude);
                    oTrangThai.textContent = 'Đã tìm thấy. Kéo ghim để chỉnh lại nếu chưa đúng.';
                    return;
                }
                oTrangThai.textContent =
                    'Tìm thấy ' + cacKetQua.length + ' địa điểm, chọn một dòng:';
                hienDanhSachKetQua(cacKetQua);
            })
            .catch(function (loi) {
                // Mat mang hay Nominatim chan deu roi vao day. Khong duoc de man hinh
                // dung hinh — nguoi dung phai ghim tay duoc de con luu ho so
                oTrangThai.textContent =
                    'Không gọi được dịch vụ tìm tọa độ. Bấm vào bản đồ để ghim tay.';
                console.warn('Loi goi /admin/geocode:', loi);
            })
            .finally(function () {
                nutTim.disabled = false;
            });
    });
})();
