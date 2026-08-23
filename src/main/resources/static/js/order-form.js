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

    var currencyFormat = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 });

    function formatMoney(value) {
        return currencyFormat.format(Math.round(value)) + 'đ';
    }

    function formatWeight(value) {
        return value.toFixed(1) + ' kg';
    }

    function formatVolume(value) {
        return value.toFixed(3) + ' m³';
    }

    // ==================== Phan 1: form tao don ====================

    var form = document.getElementById('order-form');
    if (form) {
        initOrderForm();
    }

    function initOrderForm() {
        var customerSelect = document.getElementById('customerId');
        var addressInput = document.getElementById('address');
        var latitudeInput = document.getElementById('latitude');
        var longitudeInput = document.getElementById('longitude');
        var paymentSelect = document.getElementById('paymentMethod');
        var deliveryNoteBox = document.getElementById('customer-delivery-note');
        var overrideCheckbox = document.getElementById('override-address');
        var coordinatesBlock = document.getElementById('coordinates-block');
        var linesBody = document.getElementById('order-lines-body');
        var addLineButton = document.getElementById('add-line');

        // --- Chon khach hang thi dien san moi thu (BR02-07) ---
        customerSelect.addEventListener('change', function () {
            var row = customerSelect.options[customerSelect.selectedIndex];
            var address = row.getAttribute('data-address');

            if (!address) {           // chon lai dong "— Chon khach hang —"
                addressInput.value = '';
                latitudeInput.value = '';
                longitudeInput.value = '';
                deliveryNoteBox.textContent = '';
                return;
            }

            addressInput.value = address;
            latitudeInput.value = row.getAttribute('data-latitude');
            longitudeInput.value = row.getAttribute('data-longitude');
            paymentSelect.value = row.getAttribute('data-payment-method');

            /*
             * Gan .value bang JavaScript KHONG tu phat su kien change. Ma location-picker.js
             * dua vao dung su kien do de di chuyen ghim, nen phai phat tay mot cai —
             * khong co dong nay thi ghim van nam o kho trong khi hai o toa do da doi.
             */
            latitudeInput.dispatchEvent(new Event('change'));

            // Ghi chu giao hang cua khach (BR01-07): "chi nhan buoi sang truoc 11h"
            var note = row.getAttribute('data-delivery-note');
            deliveryNoteBox.textContent = note ? 'Ghi chú của khách: ' + note : '';

            recalculateAll();      // doi hinh thuc thanh toan thi so tien phai thu doi theo
        });

        // --- Ghi de dia chi cho rieng don nay (BR02-08) ---
        overrideCheckbox.addEventListener('change', function () {
            var unlocked = overrideCheckbox.checked;
            addressInput.readOnly = !unlocked;
            latitudeInput.readOnly = !unlocked;
            longitudeInput.readOnly = !unlocked;
            coordinatesBlock.classList.toggle('d-none', !unlocked);

            if (unlocked) {
                addressInput.focus();
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
        if (addressInput.value && customerSelect.value) {
            var selectedOption = customerSelect.options[customerSelect.selectedIndex];
            if (selectedOption.getAttribute('data-address') !== addressInput.value) {
                overrideCheckbox.checked = true;
                overrideCheckbox.dispatchEvent(new Event('change'));
            }
        }

        // --- Cac dong chi tiet ---
        addLineButton.addEventListener('click', function () {
            var templateRow = linesBody.querySelector('.order-line');
            var newRow = templateRow.cloneNode(true);

            newRow.querySelector('.line-product').selectedIndex = 0;
            newRow.querySelector('.line-box-count').value = '';
            linesBody.appendChild(newRow);

            reindexRows();
            recalculateAll();
        });

        /*
         * Bat su kien o than bang chu khong gan cho tung o: cac dong duoc them bang
         * JavaScript sau khi trang da tai xong, gan truc tiep thi dong moi khong co
         * su kien nao ca.
         */
        linesBody.addEventListener('change', recalculateAll);
        linesBody.addEventListener('input', recalculateAll);

        linesBody.addEventListener('click', function (event) {
            if (!event.target.classList.contains('remove-line')) {
                return;
            }
            var rows = linesBody.querySelectorAll('.order-line');
            if (rows.length === 1) {
                // Xoa het thi khong con cho go. Doi lai thanh dong trong.
                rows[0].querySelector('.line-product').selectedIndex = 0;
                rows[0].querySelector('.line-box-count').value = '';
            } else {
                event.target.closest('.order-line').remove();
            }
            reindexRows();
            recalculateAll();
        });

        paymentSelect.addEventListener('change', recalculateAll);

        /*
         * Danh lai chi so cho ten cac o nhap: items[0], items[1], ...
         * Xoa dong giua ma khong danh lai thi ten nhay so (items[0], items[2]),
         * Spring se hieu la co mot dong trong o giua.
         */
        function reindexRows() {
            var rows = linesBody.querySelectorAll('.order-line');
            for (var i = 0; i < rows.length; i++) {
                rows[i].querySelector('.line-product').name = 'items[' + i + '].productId';
                rows[i].querySelector('.line-box-count').name = 'items[' + i + '].boxCount';
            }
        }

        /** Tinh lai tung dong roi cong thanh tong (CN-02-03). */
        function recalculateAll() {
            var totalWeight = 0;
            var totalVolume = 0;
            var totalAmount = 0;

            var rows = linesBody.querySelectorAll('.order-line');
            for (var i = 0; i < rows.length; i++) {
                var row = rows[i];
                var productSelect = row.querySelector('.line-product');
                var selected = productSelect.options[productSelect.selectedIndex];
                var boxCount = parseInt(row.querySelector('.line-box-count').value, 10);

                var weightPerBox = parseFloat(selected.getAttribute('data-weight'));
                var volumePerBox = parseFloat(selected.getAttribute('data-volume'));
                var pricePerBox = parseFloat(selected.getAttribute('data-price'));

                if (isNaN(weightPerBox) || isNaN(boxCount) || boxCount <= 0) {
                    row.querySelector('.line-weight').textContent = '—';
                    row.querySelector('.line-volume').textContent = '—';
                    row.querySelector('.line-unit-price').textContent = isNaN(pricePerBox) ? '—' : formatMoney(pricePerBox);
                    row.querySelector('.line-total').textContent = '—';
                    continue;
                }

                var rowWeight = weightPerBox * boxCount;
                var rowVolume = volumePerBox * boxCount;
                var rowAmount = pricePerBox * boxCount;

                row.querySelector('.line-weight').textContent = formatWeight(rowWeight);
                row.querySelector('.line-volume').textContent = formatVolume(rowVolume);
                row.querySelector('.line-unit-price').textContent = formatMoney(pricePerBox);
                row.querySelector('.line-total').textContent = formatMoney(rowAmount);

                totalWeight += rowWeight;
                totalVolume += rowVolume;
                totalAmount += rowAmount;
            }

            document.getElementById('total-weight').textContent = formatWeight(totalWeight);
            document.getElementById('total-volume').textContent = formatVolume(totalVolume);
            document.getElementById('total-amount').textContent = formatMoney(totalAmount);

            // BR02-14 / BR02-16: chi don tien mat moi phai thu tien khi giao
            var amountToCollect = (paymentSelect.value === 'CASH') ? totalAmount : 0;
            document.getElementById('amount-to-collect').textContent = formatMoney(amountToCollect);
        }

        reindexRows();
        recalculateAll();

        initQuickCustomerDialog(customerSelect, deliveryNoteBox);
    }

    // ==================== Phan 2: hop thoai them khach nhanh (CN-02-10) ====================

    /*
     * Them khach ngay tren man hinh tao don, khong tai lai trang: don dang go do,
     * tai lai la mat het cac dong chi tiet vua nhap.
     *
     * Phan tim toa do o day viet rieng chu khong dung lai location-picker.js, vi file do
     * gan cung vao ba o co id address / latitude / longitude cua chinh trang. Sua no
     * thanh dung chung duoc thi phai sua ca man hinh khach hang va man hinh kho dang
     * chay tot — khong dang doi lay may chuc dong.
     * Hop thoai nay CO Y khong co ban do ghim tay: chi tim theo dia chi hoac go tay
     * toa do. Muon ghim tay thi vao man hinh Khach hang.
     */
    function initQuickCustomerDialog(customerSelect, deliveryNoteBox) {
        var modal = document.getElementById('quick-customer-modal');
        if (!modal) {
            return;
        }

        var customerForm = document.getElementById('quick-customer-form');
        var errorBox = document.getElementById('quick-customer-errors');
        var searchButton = document.getElementById('qc-geocode-search');
        var statusLabel = document.getElementById('qc-geocode-status');
        var resultsBox = document.getElementById('qc-geocode-results');
        var addressInput = document.getElementById('qc-address');
        var latitudeInput = document.getElementById('qc-latitude');
        var longitudeInput = document.getElementById('qc-longitude');

        function showErrors(messages) {
            errorBox.innerHTML = '';
            messages.forEach(function (message) {
                var row = document.createElement('div');
                row.textContent = message;   // textContent chu khong innerHTML
                errorBox.appendChild(row);
            });
            errorBox.classList.remove('d-none');
        }

        function hideErrors() {
            errorBox.innerHTML = '';
            errorBox.classList.add('d-none');
        }

        function hideResults() {
            resultsBox.innerHTML = '';
            resultsBox.classList.add('d-none');
        }

        // ---- Tim toa do theo dia chi (BR01-05, BR01-06) ----
        searchButton.addEventListener('click', function () {
            var address = (addressInput.value || '').trim();
            if (address === '') {
                statusLabel.textContent = 'Nhập địa chỉ ở ô trên trước đã.';
                addressInput.focus();
                return;
            }

            statusLabel.textContent = 'Đang tìm…';
            hideResults();

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
                        statusLabel.textContent =
                            'Không tìm thấy. Gõ tay hai ô tọa độ, hoặc thử tên đường ngắn hơn.';
                        return;
                    }
                    statusLabel.textContent = 'Chọn một dòng để điền tọa độ:';
                    results.forEach(function (result) {
                        var button = document.createElement('button');
                        button.type = 'button';   // trong form thi mac dinh la nut submit
                        button.className = 'list-group-item list-group-item-action py-1 small';

                        var nameLine = document.createElement('div');
                        nameLine.textContent = result.displayName;

                        // Hien ca toa do de soat bang mat: noi thanh Ha Noi phai vao khoang
                        // 20.9-21.1 va 105.7-105.9, lech xa la Nominatim doan sai
                        var coordinateLine = document.createElement('div');
                        coordinateLine.className = 'text-sub';
                        coordinateLine.textContent = Number(result.latitude).toFixed(7)
                            + ', ' + Number(result.longitude).toFixed(7);

                        button.appendChild(nameLine);
                        button.appendChild(coordinateLine);
                        button.addEventListener('click', function () {
                            latitudeInput.value = Number(result.latitude).toFixed(7);
                            longitudeInput.value = Number(result.longitude).toFixed(7);
                            hideResults();
                            statusLabel.textContent = 'Đã điền tọa độ. Sửa tay được nếu chưa đúng.';
                        });
                        resultsBox.appendChild(button);
                    });
                    resultsBox.classList.remove('d-none');
                })
                .catch(function (error) {
                    // Nuot loi co chu y: Nominatim chet khong duoc lam hong ca man hinh tao don
                    console.warn('Loi goi /admin/geocode:', error);
                    statusLabel.textContent = 'Không gọi được dịch vụ tìm tọa độ. Gõ tay hai ô tọa độ.';
                });
        });

        // ---- Gui form bang fetch, khong tai lai trang ----
        customerForm.addEventListener('submit', function (event) {
            event.preventDefault();
            hideErrors();

            /*
             * URLSearchParams gui dang x-www-form-urlencoded, dung khuon ma @ModelAttribute
             * ben may chu doc duoc. FormData cung chay nhung gui dang multipart, khong can.
             * The an chua CSRF token do Thymeleaf tu chen nam trong form nen di theo luon.
             */
            var payload = new URLSearchParams(new FormData(customerForm));

            fetch(customerForm.action, {
                method: 'POST',
                headers: { 'Accept': 'application/json' },
                body: payload
            })
                .then(function (response) {
                    return response.json().then(function (body) {
                        return { ok: response.ok, body: body };
                    });
                })
                .then(function (result) {
                    if (!result.ok) {
                        var error = result.body.errorMessage || {};
                        var messages = Object.keys(error).map(function (key) { return error[key]; });
                        showErrors(messages.length ? messages : ['Không thêm được khách hàng.']);
                        return;
                    }
                    addCustomerOption(result.body);
                })
                .catch(function (error) {
                    console.warn('Loi goi /orders/quick-customer:', error);
                    showErrors(['Không gọi được máy chủ. Vui lòng thử lại.']);
                });
        });

        /** Them khach vua luu vao o chon roi CHON LUON cho don dang tao (CN-02-10). */
        function addCustomerOption(customer) {
            var row = document.createElement('option');
            row.value = customer.id;
            row.textContent = customer.name + ' — ' + customer.phone;
            row.setAttribute('data-address', customer.address);
            row.setAttribute('data-latitude', customer.latitude);
            row.setAttribute('data-longitude', customer.longitude);
            row.setAttribute('data-payment-method', customer.paymentMethod);
            if (customer.deliveryNote) {
                row.setAttribute('data-delivery-note', customer.deliveryNote);
            }
            customerSelect.appendChild(row);
            customerSelect.value = String(customer.id);

            // Phat su kien change de dung doan da co: tu dien dia chi, toa do, hinh thuc
            // thanh toan va tinh lai so tien phai thu
            customerSelect.dispatchEvent(new Event('change'));

            customerForm.reset();
            hideResults();
            statusLabel.textContent = '';
            bootstrap.Modal.getInstance(modal).hide();

            // Trung so dien thoai chi la canh bao mem, khach van duoc luu
            if (customer.warning) {
                deliveryNoteBox.textContent =
                    (deliveryNoteBox.textContent ? deliveryNoteBox.textContent + ' · ' : '') + customer.warning;
            }
        }
    }

    // ==================== Phan 3: nut sao chep o trang chi tiet ====================

    var copyButton = document.getElementById('copy-summary');
    if (copyButton) {
        copyButton.addEventListener('click', function () {
            var text = document.getElementById('readback-summary').textContent;

            /*
             * navigator.clipboard chi chay tren HTTPS hoac localhost. De an chay o
             * localhost nen dung duoc, nhung van co duong du phong bang mot o nhap
             * tam de neu bao ve tren may khac thi nut khong chet.
             */
            if (navigator.clipboard && navigator.clipboard.writeText) {
                navigator.clipboard.writeText(text).then(showCopied);
                return;
            }
            var temp = document.createElement('textarea');
            temp.value = text;
            document.body.appendChild(temp);
            temp.select();
            document.execCommand('copy');
            document.body.removeChild(temp);
            showCopied();
        });
    }

    function showCopied() {
        var originalLabel = copyButton.textContent;
        copyButton.textContent = 'Đã chép';
        setTimeout(function () {
            copyButton.textContent = originalLabel;
        }, 1500);
    }
})();
