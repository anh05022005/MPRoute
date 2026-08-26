/*
 * Man hinh tai xe (CN-05-06 -> CN-05-09).
 *
 * Ba viec, deu nho:
 *   1. Lay toa do GPS cua trinh duyet va dien vao form truoc khi gui (BR05-08)
 *   2. Thu khac so phai thu thi mo o ghi ly do (BR05-12)
 *   3. Chon ly do that bai la "Khac" thi mo o mo ta (BR05-06)
 *
 * Hai viec sau chi la TIEN NGHI. May chu van chan lai bang dung hai luat do o
 * DeliveryService — tat JavaScript roi gui thang len van bi tu choi (CBR-15).
 */
(function () {
    'use strict';

    var forms = document.querySelectorAll('.needs-position');
    if (forms.length === 0) {
        return;   // trang nay khong phai man hinh tai xe
    }

    askForPosition();
    setUpCollectedAmount();
    setUpFailureReason();

    // ==================== BR05-08 toa do luc bam nut ====================

    /**
     * Hoi vi tri MOT LAN ngay khi mo trang roi dien san vao moi form.
     *
     * Hoi truoc chu khong hoi luc bam nut, vi hai ly do:
     *   - trinh duyet hoi quyen bang mot hop thoai, bat tai xe cho no giua luc bam "Da giao"
     *     la lam cham dung luc anh ta dang voi
     *   - lay toa do co the mat vai giay; bam nut xong ma phai cho thi tai xe se bam lan nua
     *
     * KHONG CHAN VIEC GUI khi khong lay duoc toa do. Trinh duyet co the tu choi quyen, dien
     * thoai co the dang trong ham, va tai xe thi van dang dung truoc cua hang can bao ket qua.
     * Toa do la thu tot-neu-co, khong phai dieu kien.
     */
    function askForPosition() {
        if (!navigator.geolocation) {
            return;
        }

        navigator.geolocation.getCurrentPosition(function (position) {
            forms.forEach(function (form) {
                setValue(form, '.position-latitude', position.coords.latitude);
                setValue(form, '.position-longitude', position.coords.longitude);
            });
        }, function (error) {
            // Nuot loi co chu dich, chi ghi log de con biet vi sao cot toa do trong
            console.warn('Khong lay duoc vi tri:', error.message);
        }, { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 });
    }

    function setValue(form, selector, value) {
        var input = form.querySelector(selector);
        if (input) {
            input.value = value;
        }
    }

    // ==================== BR05-12 thu khac so phai thu ====================

    function setUpCollectedAmount() {
        document.querySelectorAll('.collected-amount').forEach(function (input) {
            var box = input.closest('form').querySelector('.collection-note-box');
            var note = input.closest('form').querySelector('.collection-note');
            if (!box || !note) {
                return;
            }

            input.addEventListener('input', function () {
                var expected = parseFloat(input.dataset.expected) || 0;
                var actual = parseFloat(input.value);
                var different = !isNaN(actual) && actual !== expected;

                box.hidden = !different;
                // required chi bat khi o dang hien: o an ma required thi form khong gui duoc
                note.required = different;
            });
        });
    }

    // ==================== BR05-06 ly do "Khac" ====================

    function setUpFailureReason() {
        document.querySelectorAll('.failure-reason').forEach(function (select) {
            var box = select.closest('form').querySelector('.failure-note-box');
            var note = select.closest('form').querySelector('.failure-note');
            if (!box || !note) {
                return;
            }

            select.addEventListener('change', function () {
                var isOther = select.value === 'OTHER';
                box.hidden = !isOther;
                note.required = isOther;
            });
        });
    }
})();
