/*
 * Bao loi nhap lieu bang dong chu do ngay duoi o nhap, thay cho bong bong cua trinh duyet.
 *
 * Bong bong mac dinh ("Value must be less than or equal to 5000", "Please fill out this
 * field") hien tieng Anh, tu tat sau vai giay, va trong khac han loi do may chu tra ve.
 * File nay tat bong bong do (noValidate) roi tu kiem lai dung cac thuoc tinh san co tren
 * o nhap — required, min, max, step, type="email" — va hien cau tieng Viet theo cung kieu
 * loi cua may chu: vien do (.is-invalid) + dong chu do (.invalid-feedback) cua Bootstrap.
 *
 * Cau bao loi ghep tu nhan cua o, nen o nao viet dung nhan la co cau dung, khong phai
 * khai them gi:  "Vui long nhap tai trong toi da tu 300 den 5.000 kg".
 *
 * Day chi la chan truoc cho do mat mot lan gui di. Cho chan that van la @Valid o may chu —
 * ai tat JavaScript hoac gui thang request thi may chu van bat lai.
 */
(function () {
    'use strict';

    function formatNumber(value) {
        return Number(value).toLocaleString('vi-VN');
    }

    // yyyy-MM-dd -> dd/MM/yyyy cho giong cach hien ngay o moi cho khac
    function formatDate(value) {
        var parts = value.split('-');
        return parts.length === 3 ? parts[2] + '/' + parts[1] + '/' + parts[0] : value;
    }

    function lowerFirst(text) {
        return text ? text.charAt(0).toLowerCase() + text.slice(1) : text;
    }

    /*
     * Ten o de ghep vao cau bao loi. Thu lan luot: nhan gan bang for, nhan dung cung khoi
     * (man tai xe, hop nhap kho khong dat id cho o), tieu de cot neu o nam trong bang.
     * Bo dau * va phan trong ngoac "(bat buoc)", "(d)" vi doc len trong cau bi thua.
     */
    function labelOf(control) {
        var label = control.id ? document.querySelector('label[for="' + control.id + '"]') : null;
        if (!label) {
            var block = (control.closest('.input-group') || control).parentElement;
            label = block ? block.querySelector('label') : null;
        }
        var text = label ? label.textContent : '';
        if (!text) {
            var cell = control.closest('td');
            var header = cell ? cell.closest('table').querySelectorAll('thead th')[cell.cellIndex] : null;
            text = header ? header.textContent : (control.getAttribute('aria-label') || '');
        }
        return text.replace('*', '').replace(/\(.*?\)/g, '').replace(/\s+/g, ' ').trim();
    }

    function unitOf(control) {
        var group = control.closest('.input-group');
        var unit = group ? group.querySelector('.input-group-text') : null;
        return unit ? ' ' + unit.textContent.trim() : '';
    }

    // Tra ve cau bao loi, hoac null neu o hop le
    function messageFor(control) {
        if (!control.willValidate) {
            return null;
        }
        var validity = control.validity;
        var name = lowerFirst(labelOf(control)) || 'thông tin này';
        var isChoice = control.tagName === 'SELECT' || control.type === 'date';
        var verb = isChoice ? 'Vui lòng chọn ' : 'Vui lòng nhập ';

        // Trinh duyet coi "   " la da nhap; may chu thi @NotBlank — chan luon o day
        var blank = control.required && typeof control.value === 'string' && control.value.trim() === ''
            && control.type !== 'file';
        if (validity.valueMissing || blank) {
            return verb + name;
        }
        if (validity.badInput) {
            return control.type === 'date' ? 'Ngày không hợp lệ' : 'Vui lòng nhập một số hợp lệ';
        }
        if (validity.typeMismatch && control.type === 'email') {
            return 'Email không đúng định dạng';
        }
        if (validity.rangeUnderflow || validity.rangeOverflow) {
            var min = control.getAttribute('min');
            var max = control.getAttribute('max');
            if (control.type === 'date') {
                return validity.rangeUnderflow
                    ? verb + name + ' từ ' + formatDate(min) + ' trở đi'
                    : verb + name + ' đến ' + formatDate(max) + ' là muộn nhất';
            }
            var unit = unitOf(control);
            if (min !== null && max !== null) {
                return verb + name + ' từ ' + formatNumber(min) + ' đến ' + formatNumber(max) + unit;
            }
            return validity.rangeUnderflow
                ? verb + name + ' từ ' + formatNumber(min) + unit + ' trở lên'
                : verb + name + ' tối đa ' + formatNumber(max) + unit;
        }
        if (validity.stepMismatch) {
            // Buoc le (0,1 kg; 0,001 m3) chi la goi y cho nut tang giam, may chu tu lam tron.
            // Chi bat khi buoc la so nguyen: so thung phai nguyen, tien thu tron nghin
            var step = Number(control.getAttribute('step'));
            if (step === 1) {
                return verb + name + ' là số nguyên';
            }
            if (step > 1) {
                return verb + name + ' là bội số của ' + formatNumber(step);
            }
            return null;
        }
        if (validity.patternMismatch) {
            return control.title || 'Không đúng định dạng';
        }
        return validity.valid ? null : 'Giá trị không hợp lệ';
    }

    /*
     * Dong chu do dat ngay sau o, hoac sau ca nhom o + don vi (input-group), hoac sau ca
     * hang o + nut neu o nam chung hang voi nut (trang tra cuu). d-block vi Bootstrap chi
     * tu hien .invalid-feedback khi no dung ngay sau o .is-invalid.
     */
    function anchorOf(control) {
        var anchor = control.closest('.input-group') || control;
        var parent = anchor.parentElement;
        if (parent && parent.tagName !== 'FORM' && parent.tagName !== 'TD'
                && getComputedStyle(parent).display === 'flex' && parent.children.length > 1) {
            anchor = parent;
        }
        return anchor;
    }

    /*
     * Loi cu do may chu tra ve (th:errors) bo luon khi nguoi dung bat dau sua o do, hoac khi
     * may o nay bao loi moi. Xoa han chu khong an: may dong loi cua o chon co lop d-block,
     * ma d-block cua Bootstrap thang thuoc tinh hidden.
     */
    function removeServerFeedback(block) {
        block.querySelectorAll('.invalid-feedback:not(.client-feedback)').forEach(function (el) {
            el.remove();
        });
    }

    function clearError(control) {
        if (!control.classList || !control.classList.contains('is-invalid')) {
            return;
        }
        control.classList.remove('is-invalid');
        if (control.clientFeedback) {
            control.clientFeedback.remove();
            control.clientFeedback = null;
        }
        var block = anchorOf(control).parentElement;
        if (block) {
            removeServerFeedback(block);
            block.querySelectorAll('.form-text').forEach(function (el) { el.hidden = false; });
        }
    }

    function showError(control, message) {
        clearError(control);
        var anchor = anchorOf(control);
        var block = anchor.parentElement;
        if (block) {
            removeServerFeedback(block);
            // Goi y "Tu 300 den 5.000 kg" an di cho khoi lap voi cau bao loi
            block.querySelectorAll('.form-text').forEach(function (el) { el.hidden = true; });
        }
        var feedback = document.createElement('div');
        feedback.className = 'invalid-feedback client-feedback d-block';
        feedback.textContent = message;
        anchor.insertAdjacentElement('afterend', feedback);
        control.clientFeedback = feedback;
        control.classList.add('is-invalid');
    }

    function prepare(root) {
        root.querySelectorAll('form').forEach(function (form) {
            form.noValidate = true;
        });
    }

    /*
     * Bat o pha capture cua document: chay TRUOC moi xu ly submit khac (hop xac nhan
     * confirm-dialog.js, kiem ly do huy tuyen...). Form sai thi dung han o day, hop xac
     * nhan khong bat len hoi "co chac khong" cho mot form chua nhap xong.
     */
    document.addEventListener('submit', function (event) {
        var form = event.target;
        var firstInvalid = null;
        Array.prototype.forEach.call(form.elements, function (control) {
            var message = messageFor(control);
            if (message) {
                showError(control, message);
                firstInvalid = firstInvalid || control;
            } else if (control.clientFeedback) {
                // Chi go loi do file nay hien. Loi may chu bao ma trinh duyet khong tu kiem
                // duoc (vi du bien so da ton tai) phai giu nguyen den khi nguoi dung sua o do
                clearError(control);
            }
        });
        if (firstInvalid) {
            event.preventDefault();
            event.stopImmediatePropagation();
            firstInvalid.focus();
        }
    }, true);

    // Bat dau sua la bo to do ngay, khong doi bam Luu lan nua moi biet da dung
    ['input', 'change'].forEach(function (type) {
        document.addEventListener(type, function (event) {
            if (event.target.classList && event.target.classList.contains('is-invalid')) {
                clearError(event.target);
            }
        });
    });

    prepare(document);
})();
