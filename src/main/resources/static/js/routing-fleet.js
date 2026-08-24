/*
 * Doi chieu suc chua da chon voi nhu cau cua ngay, ngay tren man hinh phan tuyen (CN-03-03).
 *
 * DAY CHI LA TIEN NGHI, KHONG PHAI CHO KIEM THAT. May chu van kiem lai bang MSG-08
 * o RoutingService.checkTotalCapacity() — an nut di hay sua HTML trong trinh duyet deu
 * khong qua duoc (CBR-15). O day chi de dieu phoi vien biet ngay minh dang thieu bao nhieu
 * ma khong phai bam chay roi doc thong bao loi.
 *
 * Tach khoi routing-map.js vi hai viec khac han nhau: ben kia ve ban do, ben nay cong so.
 */
(function () {
    'use strict';

    var checkboxes = document.querySelectorAll('.vehicle-checkbox');
    var display = document.getElementById('fleet-capacity');
    if (checkboxes.length === 0 || !display) {
        return;   // trang nay khong phai man hinh phan tuyen
    }

    var demandWeight = readNumber('demand-weight');
    var demandVolume = readNumber('demand-volume');

    checkboxes.forEach(function (box) {
        box.addEventListener('change', updateSummary);
    });
    updateSummary();

    function updateSummary() {
        var weight = 0;
        var volume = 0;
        var count = 0;

        checkboxes.forEach(function (box) {
            if (box.checked) {
                weight += parseFloat(box.dataset.weight) || 0;
                volume += parseFloat(box.dataset.volume) || 0;
                count++;
            }
        });

        display.textContent = count + ' xe · '
            + format(weight, 0) + ' kg · ' + format(volume, 1) + ' m³';

        /*
         * Thieu KHOI LUONG hay thieu THE TICH deu phai bao — kiem mot cai la sai (CBR-03, CBR-04).
         * Hang cua Minh Phu co loai nhe ma cong kenh, doi xe du kg van co the thieu m3.
         */
        var missingWeight = demandWeight - weight;
        var missingVolume = demandVolume - volume;

        if (missingWeight > 0 || missingVolume > 0) {
            display.style.color = 'var(--danger)';
            display.textContent += ' — còn thiếu '
                + format(Math.max(missingWeight, 0), 1) + ' kg / '
                + format(Math.max(missingVolume, 0), 3) + ' m³';
        } else {
            display.style.color = 'var(--success)';
        }
    }

    /** Doc con so may chu dat san o thuoc tinh data-value, tranh phai doc lai chuoi da dinh dang. */
    function readNumber(elementId) {
        var element = document.getElementById(elementId);
        if (!element) {
            return 0;
        }
        return parseFloat(element.dataset.value) || 0;
    }

    /** Dinh dang theo quy uoc hien thi tieng Viet: dau phay thap phan, dau cham hang nghin. */
    function format(value, decimals) {
        return value.toLocaleString('vi-VN', {
            minimumFractionDigits: decimals,
            maximumFractionDigits: decimals
        });
    }
})();
