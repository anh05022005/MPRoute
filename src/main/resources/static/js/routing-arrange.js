/*
 * Keo tha chinh tay tren man hinh phan tuyen (CN-04-01 -> CN-04-05).
 *
 * DIEU QUAN TRONG NHAT PHAI HIEU VE FILE NAY: moi con so no tinh ra chi la XEM TRUOC.
 * Luc bam Luu, trinh duyet chi gui len THU TU DON — khong gui km, khong gui kg, khong gui
 * ETA. May chu tinh lai bang dung hai ham cua thuat toan (RouteDistanceCalculator va
 * EtaCalculator) roi moi ghi xuong CSDL. Neu tin con so cua trinh duyet thi bat ky ai mo
 * cong cu lap trinh vien ra cung ghi duoc mot tuyen 0 km vao bao cao chuong 4.
 *
 * Vay tai sao van tinh o day: BR04-03 doi "sau moi thay doi, tinh lai NGAY". Dieu phoi vien
 * keo mot don sang xe khac ma phai bam Luu roi doi tai trang moi biet dai them bao nhieu km
 * thi khong con la cong cu de thu phuong an nua.
 *
 * Cong thuc quang duong o day PHAI GIONG HET RouteDistanceCalculator.java, ke ca CHANG CUOI
 * VE KHO (CBR-01). Quen chang do thi con so xem truoc luon ngan hon con so that, va nguoi
 * dung se thay km "nhay" mot cai moi lan bam Luu.
 */
(function () {
    'use strict';

    var form = document.getElementById('arrange-form');
    var planBlock = document.getElementById('plan-data');
    if (!form || !planBlock) {
        return;   // trang nay khong phai man hinh phan tuyen, hoac dang xem bang vai tro chi doc
    }

    var plan = readPlan();
    if (!plan) {
        return;   // chua co tuyen nao thi khong co gi de keo
    }

    /** Chi so trong ma tran khoang cach, tra theo ma don. Kho luon la chi so 0. */
    var matrixIndexByOrderId = {};
    var demandByOrderId = {};
    plan.points.forEach(function (point) {
        matrixIndexByOrderId[point.orderId] = point.matrixIndex;
        demandByOrderId[point.orderId] = { weight: point.weight, volume: point.volume };
    });

    var DEPOT_INDEX = 0;

    var saveButton = document.getElementById('save-arrangement');
    var blocks = Array.prototype.slice.call(document.querySelectorAll('.route-block'));
    var draggedRow = null;
    var changed = false;

    // Ghi nho nut nao may chu khoa san TRUOC lan tinh lai dau tien, vi updateButtons se ghi de
    document.querySelectorAll('button[form="arrange-form"], #confirm-plan-button')
        .forEach(function (button) {
            button.dataset.lockedByServer = String(button.disabled);
        });

    blocks.forEach(setUpRoute);
    form.addEventListener('submit', fillFormBeforeSubmit);
    refreshEverything();

    function readPlan() {
        try {
            return JSON.parse(planBlock.textContent);
        } catch (ex) {
            console.warn('Du lieu ke hoach khong doc duoc:', ex);
            return null;
        }
    }

    // ==================== Keo tha ====================

    function setUpRoute(block) {
        var list = block.querySelector('.stop-list');
        if (!list || block.dataset.editable !== 'true') {
            return;   // tuyen da chot thi khong cho sua nua (CBR-10)
        }

        list.querySelectorAll('.stop-row').forEach(setUpRow);

        /*
         * Cho tha vao ca vung tbody chu khong chi vao tung dong: keo mot don sang tuyen dang
         * RONG thi khong con dong nao de tha len, ma do lai la thao tac hop le.
         */
        list.addEventListener('dragover', function (event) {
            if (!draggedRow) {
                return;
            }
            event.preventDefault();
            var below = rowBelowCursor(list, event.clientY);
            if (below) {
                list.insertBefore(draggedRow, below);
            } else {
                list.appendChild(draggedRow);
            }
        });

        list.addEventListener('drop', function (event) {
            event.preventDefault();
            changed = true;
            refreshEverything();
        });
    }

    function setUpRow(row) {
        row.addEventListener('dragstart', function (event) {
            draggedRow = row;
            row.classList.add('is-dragging');
            // Firefox khong bat dau keo neu khong dat du lieu vao dataTransfer
            event.dataTransfer.setData('text/plain', row.dataset.orderId);
            event.dataTransfer.effectAllowed = 'move';
        });

        row.addEventListener('dragend', function () {
            row.classList.remove('is-dragging');
            draggedRow = null;
            refreshEverything();
        });
    }

    /**
     * Dong nam ngay DUOI con tro. Chen truoc dong do la duoc dung cho.
     *
     * So voi TAM cua tung dong chu khong so voi mep tren: tha vao nua duoi cua mot dong thi
     * y nguoi dung la muon xuong duoi dong do.
     */
    function rowBelowCursor(list, cursorY) {
        var rows = Array.prototype.slice.call(list.querySelectorAll('.stop-row:not(.is-dragging)'));
        for (var i = 0; i < rows.length; i++) {
            var box = rows[i].getBoundingClientRect();
            if (cursorY < box.top + box.height / 2) {
                return rows[i];
            }
        }
        return null;
    }

    // ==================== Tinh lai (CN-04-01, CN-04-02, CN-04-03) ====================

    function refreshEverything() {
        var totalMeters = 0;
        var draftMeters = 0;
        var anyOverCapacity = false;

        blocks.forEach(function (block) {
            var result = refreshRoute(block);
            totalMeters += result.meters;
            // Tuyen da chot thuoc lan chay truoc, khong duoc cong vao phia so voi thuat toan
            if (block.dataset.editable === 'true') {
                draftMeters += result.meters;
            }
            if (result.overCapacity) {
                anyOverCapacity = true;
            }
        });

        updateTotals(totalMeters, draftMeters);
        updateButtons(anyOverCapacity);
    }

    function refreshRoute(block) {
        var rows = Array.prototype.slice.call(block.querySelectorAll('.stop-row'));
        var indexes = rows.map(function (row) {
            return matrixIndexByOrderId[row.dataset.orderId];
        });

        var meters = routeDistance(indexes);
        var etas = arrivalTimes(indexes);

        rows.forEach(function (row, position) {
            row.querySelector('.stop-sequence').textContent = position + 1;

            var previous = (position === 0) ? DEPOT_INDEX : indexes[position - 1];
            row.querySelector('.stop-leg').textContent = formatKm(distance(previous, indexes[position]));
            // Tuyen da chot giu ETA may chu da luu: xe co the roi kho muon hon gio ke hoach (docs/09 B7)
            if (block.dataset.editable === 'true') {
                row.querySelector('.stop-eta').textContent = etas[position];
            }
        });

        // Chang cuoi ve kho, hien rieng mot dong o tfoot (CBR-01)
        var returnLeg = block.querySelector('.return-leg');
        if (returnLeg) {
            returnLeg.textContent = rows.length === 0
                ? formatKm(0)
                : formatKm(distance(indexes[indexes.length - 1], DEPOT_INDEX));
        }

        var load = totalLoad(rows);
        updateRouteHeading(block, meters, load);
        return { meters: meters, overCapacity: showCapacityWarning(block, load) };
    }

    /**
     * Tong quang duong mot tuyen, don vi met.
     *
     * Ba phan, GIONG HET RouteDistanceCalculator.java:
     *   1. kho -> diem dau
     *   2. cac chang giua cac diem
     *   3. diem cuoi -> KHO      <-- cho hay bi quen nhat (CBR-01)
     */
    function routeDistance(indexes) {
        if (indexes.length === 0) {
            return 0;
        }

        var total = distance(DEPOT_INDEX, indexes[0]);
        for (var i = 0; i < indexes.length - 1; i++) {
            total += distance(indexes[i], indexes[i + 1]);
        }
        total += distance(indexes[indexes.length - 1], DEPOT_INDEX);
        return total;
    }

    function distance(from, to) {
        if (from == null || to == null) {
            return 0;
        }
        return plan.matrix[from][to];
    }

    /**
     * Gio du kien den tung diem, giong cong thuc cua EtaCalculator.java (CBR-13):
     *
     *     ETA(diem thu N) = gio xuat phat
     *                     + quang duong tich luy / toc do
     *                     + so cho dung da do xong x thoi gian boc do
     *
     * Chang 0 m (hai don cung mot khach dung canh nhau) khong tinh them lan boc do,
     * dung nhu EtaCalculator.java — lech nhau thi gio xem truoc khac gio luc Luu.
     */
    function arrivalTimes(indexes) {
        var startMinutes = departureMinutes();
        var cumulativeMeters = 0;
        var completedLocations = 0;
        var times = [];

        for (var i = 0; i < indexes.length; i++) {
            var previous = (i === 0) ? DEPOT_INDEX : indexes[i - 1];
            var legMeters = distance(previous, indexes[i]);
            cumulativeMeters += legMeters;
            if (i > 0 && legMeters > 0) {
                completedLocations++;
            }

            var travelMinutes = (cumulativeMeters / 1000) / plan.speedKmh * 60;
            // Den cho dung dau tien thi xe chua do hang o dau ca nen completedLocations = 0
            var loadingMinutes = completedLocations * plan.loadingMinutes;
            times.push(formatClock(startMinutes + travelMinutes + loadingMinutes));
        }
        return times;
    }

    function departureMinutes() {
        var parts = (plan.departureTime || '08:00').split(':');
        return parseInt(parts[0], 10) * 60 + parseInt(parts[1], 10);
    }

    function totalLoad(rows) {
        var weight = 0;
        var volume = 0;
        rows.forEach(function (row) {
            var demand = demandByOrderId[row.dataset.orderId];
            if (demand) {
                weight += demand.weight;
                volume += demand.volume;
            }
        });
        return { weight: weight, volume: volume };
    }

    function updateRouteHeading(block, meters, load) {
        var meta = block.querySelector('.route-meta');
        if (!meta) {
            return;
        }
        meta.textContent = formatKm(meters)
            + ' · ' + format(load.weight, 1) + '/' + format(numberOf(block, 'maxWeight'), 0) + ' kg'
            + ' · ' + format(load.volume, 3) + '/' + format(numberOf(block, 'maxVolume'), 1) + ' m³';
    }

    /**
     * CN-04-03 — keo don nang sang xe nho thi bao ngay bang MSG-14.
     *
     * Kiem CA HAI chieu kg va m3 (CBR-03, CBR-04): hang cua Minh Phu co loai nhe ma cong
     * kenh, tuyen day m3 truoc khi day kg la chuyen binh thuong.
     *
     * Day chi la canh bao cho de nhin. May chu van chan lai bang dung hai phep kiem nay o
     * RoutePlanService.applyManualArrangement (CBR-15).
     */
    function showCapacityWarning(block, load) {
        var warning = block.querySelector('.route-capacity-warning');
        if (!warning) {
            return false;
        }

        var maxWeight = numberOf(block, 'maxWeight');
        var maxVolume = numberOf(block, 'maxVolume');
        var messages = [];

        if (load.weight > maxWeight) {
            messages.push('vượt tải trọng ' + format(load.weight - maxWeight, 1) + ' kg');
        }
        if (load.volume > maxVolume) {
            messages.push('vượt thể tích ' + format(load.volume - maxVolume, 3) + ' m³');
        }

        var heading = block.querySelector('.route-heading');
        if (messages.length === 0) {
            warning.hidden = true;
            heading.classList.remove('is-over-capacity');
            return false;
        }

        warning.hidden = false;
        warning.textContent = 'Tuyến này ' + messages.join(' và ')
            + '. Kéo bớt đơn sang tuyến khác thì mới lưu và chốt được.';
        heading.classList.add('is-over-capacity');
        return true;
    }

    /**
     * totalMeters: moi tuyen trong ngay, ke ca tuyen da chot — dong "Tong quang duong".
     * draftMeters: chi tuyen nhap — cung tap tuyen ma lan chay thuat toan gan nhat sinh ra,
     * nen moi dem so duoc voi con so "Thuat toan tim ra" (CN-04-04).
     */
    function updateTotals(totalMeters, draftMeters) {
        var total = document.getElementById('total-distance');
        if (total) {
            total.textContent = formatKm(totalMeters);
        }

        var manual = document.getElementById('manual-distance');
        var gap = document.getElementById('manual-gap');
        if (!manual || !gap) {
            return;
        }

        manual.textContent = formatKm(draftMeters);

        // CN-04-04: chenh lech so voi quang duong thuat toan tim ra, luu tai thoi diem chay
        var automatic = parseFloat(gap.dataset.auto) || 0;
        var difference = draftMeters - automatic;
        if (Math.abs(difference) < 1) {
            gap.textContent = 'Bằng nhau';
            gap.style.color = 'var(--text-secondary)';
            return;
        }

        gap.textContent = (difference > 0 ? 'Dài hơn ' : 'Ngắn hơn ')
            + formatKm(Math.abs(difference));
        // Dai hon ket qua thuat toan la mot danh doi, khong phai loi — mau canh bao chu khong do
        gap.style.color = difference > 0 ? 'var(--warning)' : 'var(--success)';
    }

    function updateButtons(anyOverCapacity) {
        if (saveButton) {
            saveButton.disabled = !changed || anyOverCapacity;
        }

        /*
         * BR04-04 — vuot suc chua thi khoa ca nut Giao tuyen va nut Chot ke hoach. Khoa o day
         * chi de nguoi dung khoi mat cong bam; may chu van kiem lai het (CBR-15).
         */
        document.querySelectorAll('button[form="arrange-form"], #confirm-plan-button')
            .forEach(function (button) {
                if (button !== saveButton) {
                    // Nut may chu da khoa san (vd. tuyen chua giao tai xe) thi giu khoa,
                    // khong duoc vi het vuot suc chua ma mo lai
                    button.disabled = anyOverCapacity || button.dataset.lockedByServer === 'true';
                }
            });

        var hint = document.getElementById('arrange-hint');
        if (hint && changed && !anyOverCapacity) {
            hint.textContent = 'Đã đổi thứ tự nhưng chưa lưu. Bấm Lưu phương án chỉnh tay '
                + 'để máy chủ tính lại và ghi xuống.';
        }
    }

    // ==================== Gui len may chu ====================

    /**
     * Chen thu tu don vao form ngay truoc khi gui.
     *
     * Gui hai danh sach song song: routeIds[i] di voi orderIdsPerRoute[i]. Tuyen khong con don
     * nao van phai gui, voi chuoi rong — khong gui thi may chu thay thieu tuyen va tu choi ca
     * phuong an.
     *
     * NOI BANG DAU GACH NGANG CHU KHONG PHAI DAU PHAY: Spring tu dong tach chuoi co dau phay
     * khi bind vao List<String>, nen "5,6,7" cua mot tuyen se thanh ba phan tu rieng va hai
     * danh sach song song lech nhau ngay.
     */
    function fillFormBeforeSubmit() {
        form.querySelectorAll('input[name="routeIds"], input[name="orderIdsPerRoute"]')
            .forEach(function (input) {
                input.remove();
            });

        blocks.forEach(function (block) {
            if (block.dataset.editable !== 'true') {
                return;
            }
            var orderIds = Array.prototype.slice.call(block.querySelectorAll('.stop-row'))
                .map(function (row) {
                    return row.dataset.orderId;
                });

            form.appendChild(hiddenInput('routeIds', block.dataset.routeId));
            form.appendChild(hiddenInput('orderIdsPerRoute', orderIds.join('-')));
        });
    }

    function hiddenInput(name, value) {
        var input = document.createElement('input');
        input.type = 'hidden';
        input.name = name;
        input.value = value;
        return input;
    }

    // ==================== Dinh dang ====================

    function numberOf(block, key) {
        return parseFloat(block.dataset[key]) || 0;
    }

    function formatKm(meters) {
        return format(meters / 1000, 2) + ' km';
    }

    /** Quy uoc hien thi tieng Viet: dau phay thap phan, dau cham hang nghin (docs/05). */
    function format(value, decimals) {
        return value.toLocaleString('vi-VN', {
            minimumFractionDigits: decimals,
            maximumFractionDigits: decimals
        });
    }

    /*
     * Lam tron toi GIAY roi CAT phan giay, dung nhu may chu: EtaCalculator.java lam tron
     * offset theo giay, template hien HH:mm thi bo giay. Lam tron theo phut o day thi
     * 08:41:36 thanh 08:42 trong khi tai xe va email cua khach thay 08:41.
     */
    function formatClock(totalMinutes) {
        var wholeMinutes = Math.floor(Math.round(totalMinutes * 60) / 60);
        var hours = Math.floor(wholeMinutes / 60) % 24;
        var minutes = wholeMinutes % 60;
        return pad(hours) + ':' + pad(minutes);
    }

    function pad(value) {
        return (value < 10 ? '0' : '') + value;
    }
})();
