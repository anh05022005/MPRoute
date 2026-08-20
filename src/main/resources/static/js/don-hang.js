/*
 * Man hinh don hang (CN-02-02, CN-02-03, CN-02-06).
 *
 * Ba viec:
 *   1. Chon khach -> tu dien dia chi, toa do, hinh thuc thanh toan (BR02-07)
 *   2. Go so thung -> tong kg / m3 / tien cap nhat NGAY, khong tai lai trang (CN-02-03)
 *   3. Bam "Sao chep" o trang chi tiet -> chep khoi tom tat de dan sang Zalo (CN-02-06)
 *
 * LUU Y: moi con so tinh o day chi de nguoi nhap NHIN TRUOC. Khi bam Luu, may chu
 * tinh lai tu dau bang don gia trong danh muc (BR02-04, BR02-13) — sua so trong
 * trinh duyet khong an thua.
 */
(function () {
    'use strict';

    // ==================== Dinh dang so kieu Viet Nam ====================

    var dinhDangTien = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 });

    function tien(so) {
        return dinhDangTien.format(Math.round(so)) + 'đ';
    }

    function kg(so) {
        return so.toFixed(1) + ' kg';
    }

    function m3(so) {
        return so.toFixed(3) + ' m³';
    }

    // ==================== Phan 1: form tao don ====================

    var form = document.getElementById('form-don-hang');
    if (form) {
        gan_ManHinhTaoDon();
    }

    function gan_ManHinhTaoDon() {
        var oKhach = document.getElementById('customerId');
        var oDiaChi = document.getElementById('address');
        var oViDo = document.getElementById('latitude');
        var oKinhDo = document.getElementById('longitude');
        var oThanhToan = document.getElementById('paymentMethod');
        var oGhiChuKhach = document.getElementById('ghi-chu-khach');
        var oGhiDe = document.getElementById('o-ghi-de-dia-chi');
        var khoiToaDo = document.getElementById('khoi-toa-do');
        var thanBang = document.getElementById('than-bang-chi-tiet');
        var nutThemDong = document.getElementById('nut-them-dong');

        // --- Chon khach hang thi dien san moi thu (BR02-07) ---
        oKhach.addEventListener('change', function () {
            var dong = oKhach.options[oKhach.selectedIndex];
            var diaChi = dong.getAttribute('data-dia-chi');

            if (!diaChi) {           // chon lai dong "— Chon khach hang —"
                oDiaChi.value = '';
                oViDo.value = '';
                oKinhDo.value = '';
                oGhiChuKhach.textContent = '';
                return;
            }

            oDiaChi.value = diaChi;
            oViDo.value = dong.getAttribute('data-vi-do');
            oKinhDo.value = dong.getAttribute('data-kinh-do');
            oThanhToan.value = dong.getAttribute('data-thanh-toan');

            /*
             * Gan .value bang JavaScript KHONG tu phat su kien change. Ma chon-toa-do.js
             * dua vao dung su kien do de di chuyen ghim, nen phai phat tay mot cai —
             * khong co dong nay thi ghim van nam o kho trong khi hai o toa do da doi.
             */
            oViDo.dispatchEvent(new Event('change'));

            // Ghi chu giao hang cua khach (BR01-07): "chi nhan buoi sang truoc 11h"
            var ghiChu = dong.getAttribute('data-ghi-chu');
            oGhiChuKhach.textContent = ghiChu ? 'Ghi chú của khách: ' + ghiChu : '';

            tinhLaiTatCa();      // doi hinh thuc thanh toan thi so tien phai thu doi theo
        });

        // --- Ghi de dia chi cho rieng don nay (BR02-08) ---
        oGhiDe.addEventListener('change', function () {
            var moKhoa = oGhiDe.checked;
            oDiaChi.readOnly = !moKhoa;
            oViDo.readOnly = !moKhoa;
            oKinhDo.readOnly = !moKhoa;
            khoiToaDo.classList.toggle('d-none', !moKhoa);

            if (moKhoa) {
                oDiaChi.focus();
                /*
                 * Leaflet do kich thuoc khung ban do luc khoi tao. Khung dang bi an thi
                 * kich thuoc do duoc la 0, hien ra sau se chi thay mot mang xam.
                 * Ban do tu goi invalidateSize khi cua so doi kich thuoc (tuy chon
                 * trackResize bat san), nen phat mot su kien resize la no ve lai dung.
                 */
                window.dispatchEvent(new Event('resize'));
            }
        });

        // Sau khi may chu tra ve form kem loi, giu nguyen trang thai da ghi de dia chi:
        // toa do khac toa do trong danh muc nghia la nguoi dung dang ghi de
        if (oDiaChi.value && oKhach.value) {
            var dongDangChon = oKhach.options[oKhach.selectedIndex];
            if (dongDangChon.getAttribute('data-dia-chi') !== oDiaChi.value) {
                oGhiDe.checked = true;
                oGhiDe.dispatchEvent(new Event('change'));
            }
        }

        // --- Cac dong chi tiet ---
        nutThemDong.addEventListener('click', function () {
            var dongMau = thanBang.querySelector('.dong-chi-tiet');
            var dongMoi = dongMau.cloneNode(true);

            dongMoi.querySelector('.o-san-pham').selectedIndex = 0;
            dongMoi.querySelector('.o-so-thung').value = '';
            thanBang.appendChild(dongMoi);

            danhLaiChiSo();
            tinhLaiTatCa();
        });

        /*
         * Bat su kien o than bang chu khong gan cho tung o: cac dong duoc them bang
         * JavaScript sau khi trang da tai xong, gan truc tiep thi dong moi khong co
         * su kien nao ca.
         */
        thanBang.addEventListener('change', tinhLaiTatCa);
        thanBang.addEventListener('input', tinhLaiTatCa);

        thanBang.addEventListener('click', function (sk) {
            if (!sk.target.classList.contains('nut-xoa-dong')) {
                return;
            }
            var cacDong = thanBang.querySelectorAll('.dong-chi-tiet');
            if (cacDong.length === 1) {
                // Xoa het thi khong con cho go. Doi lai thanh dong trong.
                cacDong[0].querySelector('.o-san-pham').selectedIndex = 0;
                cacDong[0].querySelector('.o-so-thung').value = '';
            } else {
                sk.target.closest('.dong-chi-tiet').remove();
            }
            danhLaiChiSo();
            tinhLaiTatCa();
        });

        oThanhToan.addEventListener('change', tinhLaiTatCa);

        /*
         * Danh lai chi so cho ten cac o nhap: items[0], items[1], ...
         * Xoa dong giua ma khong danh lai thi ten nhay so (items[0], items[2]),
         * Spring se hieu la co mot dong trong o giua.
         */
        function danhLaiChiSo() {
            var cacDong = thanBang.querySelectorAll('.dong-chi-tiet');
            for (var i = 0; i < cacDong.length; i++) {
                cacDong[i].querySelector('.o-san-pham').name = 'items[' + i + '].productId';
                cacDong[i].querySelector('.o-so-thung').name = 'items[' + i + '].boxCount';
            }
        }

        /** Tinh lai tung dong roi cong thanh tong (CN-02-03). */
        function tinhLaiTatCa() {
            var tongKg = 0;
            var tongM3 = 0;
            var tongTien = 0;

            var cacDong = thanBang.querySelectorAll('.dong-chi-tiet');
            for (var i = 0; i < cacDong.length; i++) {
                var dong = cacDong[i];
                var oSanPham = dong.querySelector('.o-san-pham');
                var chon = oSanPham.options[oSanPham.selectedIndex];
                var soThung = parseInt(dong.querySelector('.o-so-thung').value, 10);

                var kgMoiThung = parseFloat(chon.getAttribute('data-kg'));
                var m3MoiThung = parseFloat(chon.getAttribute('data-m3'));
                var giaMoiThung = parseFloat(chon.getAttribute('data-gia'));

                if (isNaN(kgMoiThung) || isNaN(soThung) || soThung <= 0) {
                    dong.querySelector('.o-kg').textContent = '—';
                    dong.querySelector('.o-m3').textContent = '—';
                    dong.querySelector('.o-gia').textContent = isNaN(giaMoiThung) ? '—' : tien(giaMoiThung);
                    dong.querySelector('.o-thanh-tien').textContent = '—';
                    continue;
                }

                var kgDong = kgMoiThung * soThung;
                var m3Dong = m3MoiThung * soThung;
                var tienDong = giaMoiThung * soThung;

                dong.querySelector('.o-kg').textContent = kg(kgDong);
                dong.querySelector('.o-m3').textContent = m3(m3Dong);
                dong.querySelector('.o-gia').textContent = tien(giaMoiThung);
                dong.querySelector('.o-thanh-tien').textContent = tien(tienDong);

                tongKg += kgDong;
                tongM3 += m3Dong;
                tongTien += tienDong;
            }

            document.getElementById('tong-kg').textContent = kg(tongKg);
            document.getElementById('tong-m3').textContent = m3(tongM3);
            document.getElementById('tong-tien').textContent = tien(tongTien);

            // BR02-14 / BR02-16: chi don tien mat moi phai thu tien khi giao
            var phaiThu = (oThanhToan.value === 'CASH') ? tongTien : 0;
            document.getElementById('tien-phai-thu').textContent = tien(phaiThu);
        }

        danhLaiChiSo();
        tinhLaiTatCa();

        gan_HopThoaiThemKhach(oKhach, oGhiChuKhach);
    }

    // ==================== Phan 2: hop thoai them khach nhanh (CN-02-10) ====================

    /*
     * Them khach ngay tren man hinh tao don, khong tai lai trang: don dang go do,
     * tai lai la mat het cac dong chi tiet vua nhap.
     *
     * Phan tim toa do o day viet rieng chu khong dung lai chon-toa-do.js, vi file do
     * gan cung vao ba o co id address / latitude / longitude cua chinh trang. Sua no
     * thanh dung chung duoc thi phai sua ca man hinh khach hang va man hinh kho dang
     * chay tot — khong dang doi lay may chuc dong.
     * Hop thoai nay CO Y khong co ban do ghim tay: chi tim theo dia chi hoac go tay
     * toa do. Muon ghim tay thi vao man hinh Khach hang.
     */
    function gan_HopThoaiThemKhach(oKhach, oGhiChuKhach) {
        var hopThoai = document.getElementById('hop-thoai-them-khach');
        if (!hopThoai) {
            return;
        }

        var formKhach = document.getElementById('form-them-khach');
        var khungLoi = document.getElementById('loi-them-khach');
        var nutTim = document.getElementById('kh-nut-tim-toa-do');
        var oTrangThai = document.getElementById('kh-trang-thai-tim');
        var khungKetQua = document.getElementById('kh-ket-qua-tim');
        var oDiaChi = document.getElementById('kh-address');
        var oViDo = document.getElementById('kh-latitude');
        var oKinhDo = document.getElementById('kh-longitude');

        function hienLoi(cacCau) {
            khungLoi.innerHTML = '';
            cacCau.forEach(function (cau) {
                var dong = document.createElement('div');
                dong.textContent = cau;   // textContent chu khong innerHTML
                khungLoi.appendChild(dong);
            });
            khungLoi.classList.remove('d-none');
        }

        function anLoi() {
            khungLoi.innerHTML = '';
            khungLoi.classList.add('d-none');
        }

        function anKetQuaTim() {
            khungKetQua.innerHTML = '';
            khungKetQua.classList.add('d-none');
        }

        // ---- Tim toa do theo dia chi (BR01-05, BR01-06) ----
        nutTim.addEventListener('click', function () {
            var diaChi = (oDiaChi.value || '').trim();
            if (diaChi === '') {
                oTrangThai.textContent = 'Nhập địa chỉ ở ô trên trước đã.';
                oDiaChi.focus();
                return;
            }

            oTrangThai.textContent = 'Đang tìm…';
            anKetQuaTim();

            fetch('/admin/geocode?q=' + encodeURIComponent(diaChi), {
                headers: { 'Accept': 'application/json' }
            })
                .then(function (traLoi) {
                    if (!traLoi.ok) {
                        throw new Error('HTTP ' + traLoi.status);
                    }
                    return traLoi.json();
                })
                .then(function (cacKetQua) {
                    if (!cacKetQua || cacKetQua.length === 0) {
                        oTrangThai.textContent =
                            'Không tìm thấy. Gõ tay hai ô tọa độ, hoặc thử tên đường ngắn hơn.';
                        return;
                    }
                    oTrangThai.textContent = 'Chọn một dòng để điền tọa độ:';
                    cacKetQua.forEach(function (ketQua) {
                        var nut = document.createElement('button');
                        nut.type = 'button';   // trong form thi mac dinh la nut submit
                        nut.className = 'list-group-item list-group-item-action py-1 small';

                        var dongTen = document.createElement('div');
                        dongTen.textContent = ketQua.displayName;

                        // Hien ca toa do de soat bang mat: noi thanh Ha Noi phai vao khoang
                        // 20.9-21.1 va 105.7-105.9, lech xa la Nominatim doan sai
                        var dongToaDo = document.createElement('div');
                        dongToaDo.className = 'chu-phu';
                        dongToaDo.textContent = Number(ketQua.latitude).toFixed(7)
                            + ', ' + Number(ketQua.longitude).toFixed(7);

                        nut.appendChild(dongTen);
                        nut.appendChild(dongToaDo);
                        nut.addEventListener('click', function () {
                            oViDo.value = Number(ketQua.latitude).toFixed(7);
                            oKinhDo.value = Number(ketQua.longitude).toFixed(7);
                            anKetQuaTim();
                            oTrangThai.textContent = 'Đã điền tọa độ. Sửa tay được nếu chưa đúng.';
                        });
                        khungKetQua.appendChild(nut);
                    });
                    khungKetQua.classList.remove('d-none');
                })
                .catch(function (loi) {
                    // Nuot loi co chu y: Nominatim chet khong duoc lam hong ca man hinh tao don
                    console.warn('Loi goi /admin/geocode:', loi);
                    oTrangThai.textContent = 'Không gọi được dịch vụ tìm tọa độ. Gõ tay hai ô tọa độ.';
                });
        });

        // ---- Gui form bang fetch, khong tai lai trang ----
        formKhach.addEventListener('submit', function (suKien) {
            suKien.preventDefault();
            anLoi();

            /*
             * URLSearchParams gui dang x-www-form-urlencoded, dung khuon ma @ModelAttribute
             * ben may chu doc duoc. FormData cung chay nhung gui dang multipart, khong can.
             * The an chua CSRF token do Thymeleaf tu chen nam trong form nen di theo luon.
             */
            var duLieu = new URLSearchParams(new FormData(formKhach));

            fetch(formKhach.action, {
                method: 'POST',
                headers: { 'Accept': 'application/json' },
                body: duLieu
            })
                .then(function (traLoi) {
                    return traLoi.json().then(function (than) {
                        return { ok: traLoi.ok, than: than };
                    });
                })
                .then(function (ketQua) {
                    if (!ketQua.ok) {
                        var loi = ketQua.than.loi || {};
                        var cacCau = Object.keys(loi).map(function (ten) { return loi[ten]; });
                        hienLoi(cacCau.length ? cacCau : ['Không thêm được khách hàng.']);
                        return;
                    }
                    themKhachVaoOChon(ketQua.than);
                })
                .catch(function (loi) {
                    console.warn('Loi goi /orders/quick-customer:', loi);
                    hienLoi(['Không gọi được máy chủ. Vui lòng thử lại.']);
                });
        });

        /** Them khach vua luu vao o chon roi CHON LUON cho don dang tao (CN-02-10). */
        function themKhachVaoOChon(khach) {
            var dong = document.createElement('option');
            dong.value = khach.id;
            dong.textContent = khach.name + ' — ' + khach.phone;
            dong.setAttribute('data-dia-chi', khach.address);
            dong.setAttribute('data-vi-do', khach.latitude);
            dong.setAttribute('data-kinh-do', khach.longitude);
            dong.setAttribute('data-thanh-toan', khach.paymentMethod);
            if (khach.deliveryNote) {
                dong.setAttribute('data-ghi-chu', khach.deliveryNote);
            }
            oKhach.appendChild(dong);
            oKhach.value = String(khach.id);

            // Phat su kien change de dung doan da co: tu dien dia chi, toa do, hinh thuc
            // thanh toan va tinh lai so tien phai thu
            oKhach.dispatchEvent(new Event('change'));

            formKhach.reset();
            anKetQuaTim();
            oTrangThai.textContent = '';
            bootstrap.Modal.getInstance(hopThoai).hide();

            // Trung so dien thoai chi la canh bao mem, khach van duoc luu
            if (khach.canhBao) {
                oGhiChuKhach.textContent =
                    (oGhiChuKhach.textContent ? oGhiChuKhach.textContent + ' · ' : '') + khach.canhBao;
            }
        }
    }

    // ==================== Phan 3: nut sao chep o trang chi tiet ====================

    var nutSaoChep = document.getElementById('nut-sao-chep');
    if (nutSaoChep) {
        nutSaoChep.addEventListener('click', function () {
            var vanBan = document.getElementById('khoi-tom-tat').textContent;

            /*
             * navigator.clipboard chi chay tren HTTPS hoac localhost. De an chay o
             * localhost nen dung duoc, nhung van co duong du phong bang mot o nhap
             * tam de neu bao ve tren may khac thi nut khong chet.
             */
            if (navigator.clipboard && navigator.clipboard.writeText) {
                navigator.clipboard.writeText(vanBan).then(baoDaChep);
                return;
            }
            var oTam = document.createElement('textarea');
            oTam.value = vanBan;
            document.body.appendChild(oTam);
            oTam.select();
            document.execCommand('copy');
            document.body.removeChild(oTam);
            baoDaChep();
        });
    }

    function baoDaChep() {
        var chuCu = nutSaoChep.textContent;
        nutSaoChep.textContent = 'Đã chép';
        setTimeout(function () {
            nutSaoChep.textContent = chuCu;
        }, 1500);
    }
})();
