package com.minhphu.mproute.service;

import com.minhphu.mproute.entity.Customer;
import com.minhphu.mproute.entity.Notification;
import com.minhphu.mproute.entity.Order;
import com.minhphu.mproute.entity.Route;
import com.minhphu.mproute.entity.RouteStop;
import com.minhphu.mproute.entity.enums.NotificationSendStatus;
import com.minhphu.mproute.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.format.DateTimeFormatter;

/**
 * Gui email bao gio giao du kien cho khach sau khi chot tuyen (CN-04-08, BR04-11, NTF-08).
 *
 * MOI LAN GUI DEU GHI MOT DONG VAO BANG notifications, ke ca khi khong gui duoc. Day khong
 * phai ghi log cho vui: BR04-12 bat buoc khach KHONG CO EMAIL van phai co ban ghi voi trang
 * thai NOT_SENT, de dieu phoi vien mo danh sach ra biet chinh xac phai goi dien cho ai. Bo
 * qua nhung khach do thi ho im lang khong duoc bao gi ma khong ai biet.
 *
 * BA TRANG THAI, moi cai mot tinh huong khac han nhau:
 *   SENT     — da day sang may chu SMTP
 *   NOT_SENT — khach khong co email, HOAC he thong chua cau hinh SMTP
 *   ERROR    — co email, co cau hinh, nhung gui hong (sai mat khau, mat mang)
 *
 * VI SAO DUNG ObjectProvider<JavaMailSender> chu khong tiem thang: Spring Boot chi tao bean
 * JavaMailSender khi trong application.properties co spring.mail.host. May chua khai bao SMTP
 * — dung la truong hop hien tai cua do an — thi bean do khong ton tai, va tiem thang la ung
 * dung khong khoi dong duoc. Phan tuyen la chuc nang loi, khong duoc chet chi vi chua cau hinh
 * email.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerNotificationService {

    private static final String EVENT_ROUTE_CONFIRMED = "NTF-08";

    private static final DateTimeFormatter DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HOUR_MINUTE = DateTimeFormatter.ofPattern("HH:mm");

    private final NotificationRepository notificationRepository;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    /** So thu da gui va so khach khong co email, de bao lai cho dieu phoi vien. */
    public record MailResult(int sentCount, int missingEmailCount, int errorCount) {}

    /**
     * Bao cho toan bo khach cua mot tuyen vua duoc chot.
     *
     * Goi ngay trong giao dich chot tuyen: khach phai biet gio giao cang som cang tot, va neu
     * viec chot bi hoan tac thi cac dong thong bao cung phai hoan tac theo — gui thu roi ma
     * tuyen lai khong ton tai la tinh huong te nhat.
     */
    @Transactional
    public MailResult notifyRouteConfirmed(Route route) {
        int sent = 0;
        int missingEmail = 0;
        int failed = 0;

        String trackingBaseUrl = trackingBaseUrl();

        for (RouteStop stop : route.getStops()) {
            Order order = stop.getOrder();
            Customer customer = order.getCustomer();

            String subject = "Đơn hàng " + order.getCode() + " sẽ được giao ngày "
                    + order.getDeliveryDate().format(DAY_MONTH_YEAR);
            String body = buildBody(order, stop, trackingBaseUrl);
            String link = trackingBaseUrl + "/track/" + order.getCode();

            // BR04-12 — khach khong co email van phai co ban ghi, de dieu phoi biet ma goi dien
            if (customer.getEmail() == null || customer.getEmail().isBlank()) {
                record(null, body, link, NotificationSendStatus.NOT_SENT);
                missingEmail++;
                continue;
            }

            NotificationSendStatus status = send(customer.getEmail(), subject, body);
            record(customer.getEmail(), body, link, status);

            switch (status) {
                case SENT -> sent++;
                case ERROR -> failed++;
                case NOT_SENT -> missingEmail++;
            }
        }

        log.info("Tuyen {}: gui {} thu, {} khach khong nhan duoc, {} thu loi",
                route.getCode(), sent, missingEmail, failed);
        return new MailResult(sent, missingEmail, failed);
    }

    // ==================== Ham rieng ====================

    /**
     * Noi dung thu. Van ban thuan chu khong phai HTML.
     *
     * Ly do: ten khach va dia chi la du lieu NGUOI DUNG NHAP. Ghep chung vao HTML thi phai
     * escape tung cho, quen mot cho la mo duong cho ma doc chay trong hop thu cua khach. Van
     * ban thuan thi khong co cho nao de ma chay ca, ma thu bao gio giao cung khong can dinh dang.
     */
    private String buildBody(Order order, RouteStop stop, String trackingBaseUrl) {
        StringBuilder body = new StringBuilder();
        body.append("Kính gửi ").append(order.getCustomer().getName()).append(",\n\n");
        body.append("Công ty Minh Phú xin thông báo lịch giao hàng cho đơn của quý khách:\n\n");
        body.append("  Mã đơn hàng : ").append(order.getCode()).append('\n');
        body.append("  Ngày giao   : ").append(order.getDeliveryDate().format(DAY_MONTH_YEAR)).append('\n');

        if (stop.getEstimatedArrival() != null) {
            body.append("  Giờ dự kiến : khoảng ")
                    .append(stop.getEstimatedArrival().format(HOUR_MINUTE)).append('\n');
        }
        body.append("  Địa chỉ giao: ").append(order.getDeliveryAddress()).append('\n');

        /*
         * CO Y KHONG ghi so tien, bien so xe hay ten tai xe vao thu. Email di qua nhieu may chu
         * trung gian va nam lai trong hop thu rat lau, nen no con de lo hon ca trang /track —
         * cung mot muc gioi han cua BR07-04 thi cang phai giu chat o day.
         */
        body.append("\nGiờ dự kiến có thể xê dịch tùy tình hình giao thông. ");
        body.append("Quý khách theo dõi tình trạng đơn tại:\n");
        body.append("  ").append(trackingBaseUrl).append("/track/").append(order.getCode()).append("\n\n");
        body.append("Trân trọng,\nCông ty Minh Phú");
        return body.toString();
    }

    /** Day thu sang may chu SMTP. Khong nem loi ra ngoai — chot tuyen khong duoc hong vi email. */
    private NotificationSendStatus send(String email, String subject, String body) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            /*
             * Chua khai bao spring.mail.host trong application.properties. Ghi log ca noi dung
             * thu de van kiem chung duoc chuc nang khi chua co SMTP that — den luc cau hinh
             * xong thi khong phai sua mot dong code nao.
             */
            log.info("Chua cau hinh SMTP, khong gui duoc thu toi {}. Noi dung:\n{}", email, body);
            return NotificationSendStatus.NOT_SENT;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            return NotificationSendStatus.SENT;
        } catch (Exception ex) {
            // Mat mang hay sai mat khau ung dung deu roi vao day. Tuyen van phai chot duoc
            log.warn("Khong gui duoc thu toi {}: {}", email, ex.getMessage());
            return NotificationSendStatus.ERROR;
        }
    }

    private void record(String email, String content, String link, NotificationSendStatus status) {
        Notification notification = new Notification();
        // recipient de null: nguoi nhan la khach hang chu khong phai tai khoan trong he thong
        notification.setRecipientEmail(email);
        notification.setEventCode(EVENT_ROUTE_CONFIRMED);
        notification.setContent(content);
        notification.setLink(link);
        notification.setSendStatus(status);
        notificationRepository.save(notification);
    }

    /**
     * Dia chi goc de dung duong dan tra cuu trong thu.
     *
     * Lay tu chinh request dang chay thay vi dat cung trong application.properties: do an chay
     * o localhost khi phat trien va co the doi cong khi demo, lay tu request thi luon dung ma
     * khong phai nho sua cau hinh.
     */
    private String trackingBaseUrl() {
        if (RequestContextHolder.getRequestAttributes() == null) {
            return "http://localhost:8080";   // goi tu ngoai request, vi du tu test
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    }
}
