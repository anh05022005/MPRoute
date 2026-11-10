/*
 * Man hinh don hang (CN-02-02, CN-02-03, CN-02-06).
 *
 * Ba viec:
 *   1. Go de tim va chon khach -> hien dia chi, tu dien hinh thuc thanh toan (BR02-07)
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
        var addressInput = document.getElementById('delivery-address');
        var paymentSelect = document.getElementById('paymentMethod');
        var deliveryNoteBox = document.getElementById('customer-delivery-note');
        var linesBody = document.getElementById('order-lines-body');
        var addLineButton = document.getElementById('add-line');

        /*
         * Hien dia chi va ghi chu giao hang cua khach dang chon. O dia chi chi de nhin:
         * may chu tu chep dia chi va toa do tu danh muc khi luu (BR02-07).
         */
        function showCustomerInfo() {
            var row = customerSelect.options[customerSelect.selectedIndex];
            addressInput.value = row.getAttribute('data-address') || '';

            // Ghi chu giao hang cua khach (BR01-07): "chi nhan buoi sang truoc 11h"
            var note = row.getAttribute('data-delivery-note');
            deliveryNoteBox.textContent = note ? 'Ghi chú của khách: ' + note : '';
        }

        // --- Chon khach hang thi dien san moi thu (BR02-07) ---
        customerSelect.addEventListener('change', function () {
            showCustomerInfo();

            var row = customerSelect.options[customerSelect.selectedIndex];
            var paymentMethod = row.getAttribute('data-payment-method');
            if (paymentMethod) {
                paymentSelect.value = paymentMethod;
            }
            recalculateAll();      // doi hinh thuc thanh toan thi so tien phai thu doi theo
        });

        // Mo man hinh sua don, hoac may chu tra form kem loi: khach da chon san nen phai tu
        // hien dia chi. Goi showCustomerInfo chu khong phat su kien change, vi change se
        // ghi de hinh thuc thanh toan nguoi dung da chon cho rieng don nay (BR02-15)
        showCustomerInfo();

        // --- Cac dong chi tiet ---
        addLineButton.addEventListener('click', function () {
            var templateRow = linesBody.querySelector('.order-line');
            var newRow = templateRow.cloneNode(true);

            newRow.querySelector('.line-product').selectedIndex = 0;
            newRow.querySelector('.line-box-count').value = '';
            delete newRow.dataset.capped;
            linesBody.appendChild(newRow);

            reindexRows();
            recalculateAll();
        });

        /*
         * Bat su kien o than bang chu khong gan cho tung o: cac dong duoc them bang
         * JavaScript sau khi trang da tai xong, gan truc tiep thi dong moi khong co
         * su kien nao ca.
         */
        linesBody.addEventListener('change', onLineEdited);
        linesBody.addEventListener('input', onLineEdited);

        function onLineEdited(event) {
            var row = event.target.closest('.order-line');
            if (row) {
                capBoxCountAtStock(row);
            }
            recalculateAll();
        }

        /*
         * Go qua so thung con trong kho thi keo ve dung bang so do, va danh dau dong de
         * hien loi nhac. Doi sang san pham it hang hon cung keo ve nhu vay. Day chi la
         * chan tren giao dien cho tien: duyet don van kiem lai ton kho that o may chu
         * (MSG-17), vi don cho duyet chua giu hang.
         */
        function capBoxCountAtStock(row) {
            var input = row.querySelector('.line-box-count');
            var productSelect = row.querySelector('.line-product');
            var stock = parseInt(productSelect.options[productSelect.selectedIndex].getAttribute('data-stock'), 10);
            var boxCount = parseInt(input.value, 10);

            if (!isNaN(stock) && !isNaN(boxCount) && boxCount > stock) {
                input.value = stock;
                row.dataset.capped = 'true';
            } else if (isNaN(stock) || isNaN(boxCount) || boxCount < stock) {
                // Bang dung so ton thi giu loi nhac: su kien change luc roi o nhap ban
                // ra sau input, xoa o day thi loi vua hien da bien mat
                delete row.dataset.capped;
            }
        }

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
        /**
         * Hien so thung con ranh duoi o chon san pham.
         *
         * CHI LA SO THAM KHAO, co y nhu vay. Don dang cho duyet chua giu hang, nen hai
         * nguoi tao hai don cung luc deu nhin thay cung mot con so va co the cong lai
         * vuot ton kho. Giu cho o buoc duyet don moi tru kho that va chan cung (MSG-17):
         * giu cho tu luc tao don thi phai nho nha cho khi don bi huy, va quen nha la nguon
         * sai so kinh dien cua moi he ton kho.
         *
         * Cung vi the o day khong tru phan cac dong khac trong cung don dang chiem.
         */
        function showStockHint(row, selectedOption, boxCount) {
            var hint = row.querySelector('.line-stock');
            if (!hint) {
                return;
            }

            var stock = parseInt(selectedOption.getAttribute('data-stock'), 10);
            if (isNaN(stock)) {
                hint.textContent = '';
                hint.className = 'line-stock text-sub d-block mt-1';
                return;
            }

            if (row.dataset.capped === 'true') {
                hint.textContent = (stock === 0)
                    ? 'Sản phẩm này đã hết hàng trong kho.'
                    : 'Chỉ còn ' + stock + ' thùng trong kho. Vui lòng nhập tối đa ' + stock + '.';
                hint.className = 'line-stock text-danger d-block mt-1';
            } else {
                hint.textContent = 'Còn rảnh ' + stock + ' thùng';
                hint.className = 'line-stock text-sub d-block mt-1';
            }
        }

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

                showStockHint(row, selected, boxCount);

                if (isNaN(weightPerBox) || isNaN(boxCount) || boxCount <= 0) {
                    // Chua du san pham + so thung thi de trong o, chi hien don gia neu da chon san pham
                    row.querySelector('.line-weight').textContent = '';
                    row.querySelector('.line-volume').textContent = '';
                    row.querySelector('.line-unit-price').textContent = isNaN(pricePerBox) ? '' : formatMoney(pricePerBox);
                    row.querySelector('.line-total').textContent = '';
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

        initCustomerSearch(customerSelect);
    }

    // ==================== Phan 2: o go de tim khach hang ====================

    /*
     * Bien o chon khach thanh o go de tim: danh muc co vai tram cua hang, cuon tim tung dong
     * trong danh sach tha xuong la cham. Go mot doan ten, so dien thoai hoac dia chi la loc;
     * go khong dau van ra ("van xuan" tim duoc "Van Xuan" co dau).
     *
     * KHONG thay the <select>: <select id="customerId"> van la o that gui len may chu va van
     * giu cac data-* de Phan 1 doc. O go chi doc danh sach tu <select>, chon xong thi gan
     * lai gia tri vao <select> roi phat su kien change — dien dia chi, hinh thuc thanh toan,
     * tinh lai tien chay dung nhu khi chon bang chuot. May chu khong biet co o go nay.
     */
    function initCustomerSearch(customerSelect) {
        var combo = document.getElementById('customer-combo');
        var input = document.getElementById('customer-search');
        var menu = document.getElementById('customer-options');
        if (!combo || !input || !menu) {
            return;
        }

        // Doc danh sach mot lan, bo dong rong "— Chon khach hang —"
        var customers = Array.prototype.slice.call(customerSelect.options)
            .filter(function (option) {
                return option.value !== '';
            })
            .map(function (option) {
                var address = option.getAttribute('data-address') || '';
                // Ten va SDT tach rieng de hien hai ben mot dong; thieu data-* thi lay chu cua option
                var name = option.getAttribute('data-name') || option.textContent.trim();
                var phone = option.getAttribute('data-phone') || '';
                return {
                    value: option.value,
                    name: name,
                    phone: phone,
                    address: address,
                    labelText: normalize(name + ' ' + phone),
                    searchText: normalize(name + ' ' + phone + ' ' + address)
                };
            });

        var matches = [];
        var activeIndex = -1;

        // Co JavaScript thi moi hien o go va an <select>; tat JavaScript van chon duoc bang <select>
        combo.hidden = false;
        customerSelect.hidden = true;
        var label = document.querySelector('label[for="customerId"]');
        if (label) {
            label.setAttribute('for', 'customer-search');
        }
        input.value = selectedLabel();

        // Bam vao o: boi den chu dang co de go la thay luon, va mo ca danh sach
        var justFocused = false;
        input.addEventListener('focus', function () {
            input.select();
            justFocused = true;
            render('');
        });
        // Chrome bo vung boi den ngay luc nha chuot sau cu bam lay focus — chan dung lan do
        input.addEventListener('mouseup', function (event) {
            if (justFocused) {
                event.preventDefault();
                justFocused = false;
            }
        });
        input.addEventListener('click', function () {
            if (menu.hidden) {
                render('');
            }
        });
        input.addEventListener('input', function () {
            render(input.value);
        });
        input.addEventListener('keydown', onKeyDown);

        /*
         * Roi o ma chua chon dong nao thi tra chu ve ten khach dang chon — khong de o go hien
         * mot doan chu trong khi khach that gui len lai la nguoi khac. Xoa trang roi roi o
         * nghia la bo chon khach.
         */
        input.addEventListener('blur', function () {
            close();
            if (input.value.trim() === '' && customerSelect.value !== '') {
                customerSelect.value = '';
                customerSelect.dispatchEvent(new Event('change'));
            }
            input.value = selectedLabel();
        });

        // Bam vao danh sach (ke ca thanh cuon) khong duoc lam o go mat focus truoc khi chon xong
        menu.addEventListener('mousedown', function (event) {
            event.preventDefault();
        });

        function render(query) {
            var phrase = normalize(query);
            var tokens = phrase.split(' ').filter(Boolean);
            matches = customers.filter(function (customer) {
                return tokens.every(function (token) {
                    return customer.searchText.indexOf(token) !== -1;
                });
            });

            /*
             * Xep dong khop trong TEN / SO DIEN THOAI len truoc dong chi khop nho dia chi.
             * Khong xep thi go "van xuan" ra "Cua hang Kim Ngan" dau tien vi dia chi la
             * "Le Van Luong, Thanh Xuan" — Enter se chon nham. sort cua trinh duyet la on
             * dinh nen trong cung mot bac van giu thu tu ten nhu danh muc.
             */
            matches.sort(function (a, b) {
                return rankOf(a) - rankOf(b);
            });

            function rankOf(customer) {
                if (customer.labelText.indexOf(phrase) !== -1) {
                    return 0;   // ca cum vua go nam lien trong ten hoac so dien thoai
                }
                var allInLabel = tokens.every(function (token) {
                    return customer.labelText.indexOf(token) !== -1;
                });
                return allInLabel ? 1 : 2;
            }

            menu.innerHTML = '';
            if (matches.length === 0) {
                var empty = document.createElement('div');
                empty.className = 'combo-empty';
                empty.textContent = 'Không tìm thấy khách hàng nào.';
                menu.appendChild(empty);
            }
            matches.forEach(function (customer) {
                var item = document.createElement('div');
                item.className = 'combo-option';
                item.id = 'customer-option-' + customer.value;
                item.setAttribute('role', 'option');

                // textContent chu khong innerHTML: ten khach do nguoi dung nhap
                var head = document.createElement('div');
                head.className = 'combo-option-head';
                var name = document.createElement('span');
                name.className = 'combo-option-name';
                name.textContent = customer.name;
                head.appendChild(name);
                if (customer.phone) {
                    var phone = document.createElement('span');
                    phone.className = 'combo-option-phone';
                    phone.textContent = customer.phone;
                    head.appendChild(phone);
                }
                item.appendChild(head);
                if (customer.address) {
                    var address = document.createElement('div');
                    address.className = 'combo-option-sub';
                    address.textContent = customer.address;
                    item.appendChild(address);
                }

                item.addEventListener('click', function () {
                    choose(customer);
                });
                menu.appendChild(item);
            });

            // Chua go gi thi to dong khach dang chon; da go thi to dong dau de Enter la chon
            activeIndex = query.trim() ? (matches.length ? 0 : -1) : indexOfSelected();
            highlight();
            menu.hidden = false;
            input.setAttribute('aria-expanded', 'true');
        }

        function onKeyDown(event) {
            switch (event.key) {
                case 'ArrowDown':
                    event.preventDefault();
                    if (menu.hidden) {
                        render('');
                    } else if (activeIndex < matches.length - 1) {
                        activeIndex++;
                        highlight();
                    }
                    break;
                case 'ArrowUp':
                    event.preventDefault();
                    if (activeIndex > 0) {
                        activeIndex--;
                        highlight();
                    }
                    break;
                case 'Enter':
                    // Enter trong o tim khong bao gio duoc gui ca form don hang di
                    event.preventDefault();
                    if (!menu.hidden && activeIndex >= 0) {
                        choose(matches[activeIndex]);
                    }
                    break;
                case 'Escape':
                    if (!menu.hidden) {
                        event.preventDefault();
                        close();
                        input.value = selectedLabel();
                        input.select();
                    }
                    break;
                case 'Tab':
                    // Da go chu va dang to mot dong thi Tab chon luon dong do roi moi sang o sau
                    if (!menu.hidden && activeIndex >= 0 && input.value !== selectedLabel()) {
                        choose(matches[activeIndex]);
                    }
                    break;
                default:
                    break;
            }
        }

        function choose(customer) {
            var changed = customerSelect.value !== customer.value;
            customerSelect.value = customer.value;
            input.value = customer.name;
            input.classList.remove('is-invalid');
            close();
            /*
             * Chi phat change khi doi sang khach KHAC: change se dat lai hinh thuc thanh toan
             * theo danh muc, chon lai dung khach cu thi khong duoc ghi de lua chon rieng cua
             * don nay (BR02-15).
             */
            if (changed) {
                customerSelect.dispatchEvent(new Event('change'));
            }
        }

        function highlight() {
            var items = menu.querySelectorAll('.combo-option');
            items.forEach(function (item, index) {
                item.classList.toggle('is-active', index === activeIndex);
            });
            var active = items[activeIndex];
            if (active) {
                active.scrollIntoView({ block: 'nearest' });
                input.setAttribute('aria-activedescendant', active.id);
            } else {
                input.removeAttribute('aria-activedescendant');
            }
        }

        function close() {
            menu.hidden = true;
            input.setAttribute('aria-expanded', 'false');
            input.removeAttribute('aria-activedescendant');
        }

        // O go chi hien ten khach: SDT da nam trong danh sach tha xuong, hien lai o day thua
        function selectedLabel() {
            if (!customerSelect.value) {
                return '';
            }
            var option = customerSelect.options[customerSelect.selectedIndex];
            return option.getAttribute('data-name') || option.textContent.trim();
        }

        function indexOfSelected() {
            for (var i = 0; i < matches.length; i++) {
                if (matches[i].value === customerSelect.value) {
                    return i;
                }
            }
            return -1;
        }
    }

    /**
     * Chuan hoa de so khop khong phan biet hoa thuong va dau tieng Viet. NFD tach dau ra
     * khoi chu cai roi xoa; rieng chu "đ" la mot chu cai rieng chu khong phai d + dau nen
     * phai doi tay. Gop moi khoang trang lien nhau thanh mot.
     */
    function normalize(text) {
        return (text || '')
            .normalize('NFD')
            .replace(/[̀-ͯ]/g, '')
            .replace(/đ/g, 'd')
            .replace(/Đ/g, 'D')
            .toLowerCase()
            .replace(/\s+/g, ' ')
            .trim();
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
