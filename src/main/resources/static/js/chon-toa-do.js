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
            var dongTen = document.createElement('div');
            dongTen.textContent = ketQua.displayName;

            // Hien ca toa do de soat bang mat truoc khi chon: khach noi thanh Ha Noi
            // phai vao khoang 20.9-21.1 va 105.7-105.9, lech xa la Nominatim doan sai
            var dongToaDo = document.createElement('div');
            dongToaDo.className = 'chu-phu';
            dongToaDo.textContent = lamTron7(ketQua.latitude) + ', ' + lamTron7(ketQua.longitude);

            nut.appendChild(dongTen);
            nut.appendChild(dongToaDo);

            nut.addEventListener('click', function () {
                datGhim(ketQua.latitude, ketQua.longitude, true);
                ghiToaDoVaoForm(ketQua.latitude, ketQua.longitude);
                anDanhSachKetQua();
                oTrangThai.textContent =
                    'Đã đặt ghim. Chưa đúng chỗ thì kéo ghim, hoặc bấm chỗ khác trên bản đồ.';
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
                oTrangThai.textContent = (cacKetQua.length === 1)
                    ? 'Tìm thấy 1 địa điểm — xem có đúng không rồi bấm vào dòng dưới:'
                    : 'Tìm thấy ' + cacKetQua.length + ' địa điểm, chọn một dòng:';
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
