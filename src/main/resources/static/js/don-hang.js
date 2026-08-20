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
    }

    // ==================== Phan 2: nut sao chep o trang chi tiet ====================

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
