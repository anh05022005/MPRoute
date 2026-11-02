/*
 * Hop xac nhan dung chung cho moi man hinh, thay cho confirm() cua trinh duyet.
 *
 * confirm() hien hop "localhost says" xau, khong dat duoc tieu de, khong to mau nut,
 * va noi dung chi la mot dong chu tron. Hop o day la modal Bootstrap dat san trong
 * layout.html, moi form tu khai noi dung bang thuoc tinh data-confirm-*:
 *
 *   <form ... data-confirm-message="Xe {subject} se chuyen sang Bao tri..."
 *             data-confirm-subject="29B-222.11"      chen vao cho {subject}, in dam
 *             data-confirm-title="Chuyen xe sang bao tri"
 *             data-confirm-button="Chuyen bao tri"   chu tren nut dong y
 *             data-confirm-variant="warning"         warning | success | danger | primary
 *             data-confirm-cancel="Khong huy">        chu tren nut huy, mac dinh "Huy"
 *
 * Form nao khong co data-confirm-message thi gui di binh thuong, file nay khong dung toi.
 */
(function () {
    'use strict';

    var dialogEl = document.getElementById('confirm-dialog');
    if (!dialogEl || typeof bootstrap === 'undefined') {
        return;
    }

    var dialog = bootstrap.Modal.getOrCreateInstance(dialogEl);
    var titleEl = document.getElementById('confirm-dialog-title');
    var messageEl = document.getElementById('confirm-dialog-message');
    var okButton = document.getElementById('confirm-dialog-ok');
    var cancelButton = document.getElementById('confirm-dialog-cancel');

    var BUTTON_CLASSES = {
        warning: 'btn-warning',
        success: 'btn-success',
        danger: 'btn-danger',
        primary: 'btn-primary'
    };

    var pendingForm = null;
    var pendingSubmitter = null;

    /*
     * Dung noi dung bang textContent chu khong bang innerHTML: subject la du lieu nguoi
     * dung nhap (ten khach, bien so), ghep thang vao HTML la mo cua cho XSS.
     */
    function renderMessage(template, subject) {
        messageEl.textContent = '';
        template.split('{subject}').forEach(function (part, index) {
            if (index > 0) {
                var strong = document.createElement('strong');
                strong.textContent = subject || '';
                messageEl.appendChild(strong);
            }
            messageEl.appendChild(document.createTextNode(part));
        });
    }

    // Bat su kien submit o muc document nen form nao tren trang cung duoc, ke ca form
    // nam trong khoi an hien (vi du khoi nhap ly do huy don)
    document.addEventListener('submit', function (event) {
        var form = event.target;
        if (!(form instanceof HTMLFormElement) || !form.dataset.confirmMessage) {
            return;
        }
        // Lan gui thu hai, sau khi da bam dong y trong hop: cho di that
        if (form.dataset.confirmed === 'true') {
            delete form.dataset.confirmed;
            return;
        }

        event.preventDefault();
        pendingForm = form;
        pendingSubmitter = event.submitter || null;

        titleEl.textContent = form.dataset.confirmTitle || 'Xác nhận';
        renderMessage(form.dataset.confirmMessage, form.dataset.confirmSubject);
        okButton.textContent = form.dataset.confirmButton || 'Đồng ý';
        okButton.className = 'btn ' + (BUTTON_CLASSES[form.dataset.confirmVariant] || 'btn-primary');
        cancelButton.textContent = form.dataset.confirmCancel || 'Hủy';
        okButton.disabled = false;

        dialog.show();
    });

    okButton.addEventListener('click', function () {
        if (!pendingForm) {
            return;
        }
        var form = pendingForm;
        var submitter = pendingSubmitter;
        pendingForm = null;
        pendingSubmitter = null;

        // Khoa nut ngay: bam hai lan lien tiep la gui hai request doi trang thai
        okButton.disabled = true;
        form.dataset.confirmed = 'true';
        dialog.hide();

        /*
         * requestSubmit chu khong phai submit(): submit() bo qua su kien submit va bo qua
         * kiem tra required cua trinh duyet. Truyen lai dung nut da bam de giu name/value
         * cua nut do neu co.
         */
        if (typeof form.requestSubmit === 'function') {
            if (submitter) {
                form.requestSubmit(submitter);
            } else {
                form.requestSubmit();
            }
        } else {
            form.submit();
        }
    });

    // Bam Huy hoac dong hop thi bo yeu cau dang cho
    dialogEl.addEventListener('hidden.bs.modal', function () {
        pendingForm = null;
        pendingSubmitter = null;
    });

    // Dat san con tro vao nut Huy: lo tay bam Enter thi khong thanh dong y
    dialogEl.addEventListener('shown.bs.modal', function () {
        cancelButton.focus();
    });
})();
