package com.minhphu.mproute.security;

import com.minhphu.mproute.service.SystemParameterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Gioi han so lan tra cuu cong khai theo dia chi IP (BR07-09, CN-07-07).
 *
 * VI SAO TRANG NAY CAN GIOI HAN CON CAC TRANG KHAC THI KHONG: /track la duong DUY NHAT cua he
 * thong mo cho nguoi chua dang nhap, va no nhan vao so dien thoai. Khong chan thi mot script
 * quet lan luot cac dau so la doc duoc ten khach, dia chi giao va lich giao hang cua ca vung —
 * tung truy van deu hop le, cai sai la o TAN SUAT.
 *
 * Dem trong bo nho, khong dung Redis: do an chay tren mot may, mot tien trinh. Doi lai, khoi
 * dong lai ung dung la bo dem ve 0 — chap nhan duoc, vi day la rao chong quet hang loat chu
 * khong phai rao bao mat tinh vi. Han che nay ghi vao bao cao chu khong giau di.
 *
 * Cua so tinh theo TUNG PHUT TRON chu khong phai cua so truot: cach nay chi giu mot so dem cho
 * moi IP thay vi ca danh sach moc thoi gian, va sai lech toi da la cho qua gap doi han muc o
 * ranh giua hai phut. Voi muc dich chong quet thi doi do lay su don gian la dang.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LookupRateLimiter {

    /**
     * So IP toi da giu trong bo nho. Cham nguong thi don sach toan bo bang dem.
     *
     * Khong co chan nay thi moi IP moi la mot muc trong Map va khong bao gio duoc go ra —
     * mot dot quet tu hang chuc nghin dia chi la ung dung het bo nho, dung cai ma no dinh chan.
     */
    private static final int MAX_TRACKED_ADDRESSES = 10_000;

    private final SystemParameterService systemParameterService;

    /** Khoa la "IP@phut", nen sang phut moi la moi IP tu dong co so dem moi. */
    private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    /**
     * Ghi nhan mot lan tra cuu va cho biet co duoc phep di tiep khong.
     *
     * @return true neu con trong han muc, false neu da vuot
     */
    public boolean tryAcquire(String clientAddress) {
        int limit = systemParameterService.getInt(SystemParameterService.LOOKUP_RATE_LIMIT);
        if (limit <= 0) {
            return true;   // dat 0 nghia la tat gioi han
        }

        if (counters.size() > MAX_TRACKED_ADDRESSES) {
            counters.clear();
            log.warn("Bang dem tra cuu vuot {} dia chi, da don sach", MAX_TRACKED_ADDRESSES);
        }

        String key = clientAddress + "@" + Instant.now().getEpochSecond() / 60;
        int used = counters.computeIfAbsent(key, ignored -> new AtomicInteger()).incrementAndGet();

        if (used > limit) {
            log.warn("Dia chi {} tra cuu {} lan trong mot phut, vuot han muc {}",
                    clientAddress, used, limit);
            return false;
        }
        return true;
    }
}
