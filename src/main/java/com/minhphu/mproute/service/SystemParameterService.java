package com.minhphu.mproute.service;

import com.minhphu.mproute.entity.SystemParameter;
import com.minhphu.mproute.repository.SystemParameterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Nghiep vu tham so he thong (CN-01-08).
 *
 * Tham so luu dang cap khoa - gia tri, cot value la VARCHAR nen moi loai deu ve
 * duoc mot bang. Doi lai, kieu du lieu that KHONG duoc CSDL bao ve — nhap "abc"
 * vao AVERAGE_SPEED thi den tuan 8 moi no ra loi chia cho chu. Vi vay toan bo
 * viec kiem kieu nam o ham kiemGiaTri() duoi day.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SystemParameterService {

    // Ma tham so dung nhieu noi, dat hang so de khong go tay chuoi o moi cho
    public static final String AVERAGE_SPEED = "AVERAGE_SPEED";
    public static final String LOADING_TIME = "LOADING_TIME";
    public static final String DEPARTURE_TIME = "DEPARTURE_TIME";
    public static final String HAVERSINE_FACTOR = "HAVERSINE_FACTOR";
    public static final String OSRM_URL = "OSRM_URL";
    public static final String OSRM_TIMEOUT = "OSRM_TIMEOUT";
    public static final String PAGE_SIZE = "PAGE_SIZE";
    public static final String LOOKUP_RATE_LIMIT = "LOOKUP_RATE_LIMIT";

    private static final Pattern HH_MM_PATTERN = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");

    /** Mot dong trong bang tham so: ma, gia tri khoi tao, mo ta hien ra man hinh. */
    public record ParameterDefault(String code, String defaultValue, String description) {}

    /*
     * Thu tu trong danh sach nay chinh la thu tu hien tren man hinh.
     * Nam tham so dau anh huong THANG toi ket qua thuat toan, xep len tren.
     * Gia tri khoi tao lay tu docs/02-co-so-du-lieu.md muc 16.
     */
    private static final List<ParameterDefault> DEFAULTS = List.of(
            new ParameterDefault(AVERAGE_SPEED, "30",
                    "Tốc độ trung bình khi tính giờ đến dự kiến (km/h)"),
            new ParameterDefault(LOADING_TIME, "15",
                    "Thời gian bốc dỡ tại mỗi điểm giao (phút)"),
            new ParameterDefault(DEPARTURE_TIME, "08:00",
                    "Giờ xe xuất phát từ kho, dạng HH:mm"),
            /*
             * 1.45 la SO DO DUOC, khong phai so uoc chung. Do ngay 27/08/2026 tren 264 cap
             * diem that dang nam trong bang distance_matrix: lay tong quang duong OSRM chia
             * cho tong quang duong chim bay cua chinh cac cap do ra 1.453. Gia tri cu 1.3
             * la con so muon tu tai lieu, va no uoc luong THIEU quang duong that.
             *
             * Chia TONG cho TONG chu khong lay trung binh cac ty le: trung binh cac ty le
             * ra 1.499 vi cac cap diem gan nhau co ty le rat cao (ngo nho, duong mot chieu)
             * ma chung lai dong gop it vao tong quang duong. He so nay dung de uoc luong
             * TONG quang duong tuyen, nen phai lay theo tong.
             */
            new ParameterDefault(HAVERSINE_FACTOR, "1.45",
                    "Hệ số quy đổi từ đường chim bay sang đường đi thật, dùng khi OSRM không chạy"),
            new ParameterDefault(OSRM_URL, "http://localhost:5000",
                    "Địa chỉ máy chủ OSRM tính khoảng cách"),
            new ParameterDefault(OSRM_TIMEOUT, "10",
                    "Thời gian chờ tối đa khi gọi OSRM (giây)"),
            new ParameterDefault(PAGE_SIZE, "20",
                    "Số dòng mỗi trang trên các màn hình danh sách"),
            new ParameterDefault(LOOKUP_RATE_LIMIT, "20",
                    "Giới hạn tra cứu công khai (lần/phút/IP)")
    );

    private final SystemParameterRepository systemParameterRepository;

    /**
     * Tao du chin tham so neu thieu. Chay lai nhieu lan khong sao: da co thi bo qua,
     * khong ghi de, nen sua gia tri xong khoi dong lai khong bi tra ve mac dinh.
     */
    @Transactional
    public void seedDefaults() {
        for (ParameterDefault definition : DEFAULTS) {
            if (systemParameterRepository.findByCode(definition.code()).isPresent()) {
                continue;
            }
            SystemParameter parameter = new SystemParameter();
            parameter.setCode(definition.code());
            parameter.setValue(definition.defaultValue());
            parameter.setDescription(definition.description());
            systemParameterRepository.save(parameter);
            log.info("Da tao tham so {} = {}", definition.code(), definition.defaultValue());
        }
    }

    /** Danh sach tham so theo dung thu tu hien thi da dinh o MAC_DINH. */
    @Transactional(readOnly = true)
    public List<SystemParameter> findAllInDisplayOrder() {
        List<String> orderedCodes = DEFAULTS.stream().map(ParameterDefault::code).toList();
        /*
         * Chi hien nhung ma con dung. Tham so da bo (DEFAULT_ALGORITHM, docs/09 muc A7) van
         * con dong cu trong CSDL cua may da chay ban truoc — an di chu khong xoa, de khong
         * phai viet them buoc don du lieu.
         */
        return systemParameterRepository.findAll().stream()
                .filter(item -> orderedCodes.contains(item.getCode()))
                .sorted(Comparator.comparingInt(item -> orderedCodes.indexOf(item.getCode())))
                .toList();
    }

    @Transactional(readOnly = true)
    public String getValue(String code) {
        return systemParameterRepository.findByCode(code)
                .map(SystemParameter::getValue)
                .orElseThrow(() -> new IllegalStateException("Thiếu tham số hệ thống: " + code));
    }

    @Transactional(readOnly = true)
    public double getDouble(String code) {
        return Double.parseDouble(getValue(code));
    }

    @Transactional(readOnly = true)
    public int getInt(String code) {
        return Integer.parseInt(getValue(code));
    }

    /**
     * Kiem gia tri theo tung ma tham so.
     *
     * Tra ve null neu hop le, hoac cau bao loi tieng Viet neu sai.
     *
     * Vi sao phai kiem chan tren chu khong chi kiem "lon hon 0": AVERAGE_SPEED = 5000
     * khong lam chuong trinh chet, no chi lam moi ETA sai be bet ma khong bao gi ca.
     * Loi im lang kieu do kho phat hien hon nhieu so voi loi nem ngoai le.
     */
    public String validateValue(String code, String value) {
        String trimmed = (value == null) ? "" : value.trim();
        if (trimmed.isEmpty()) {
            return "Không được để trống";
        }

        return switch (code) {
            case AVERAGE_SPEED -> validateDecimal(trimmed, 1, 200, "Tốc độ phải là số trong khoảng 1 đến 200 km/h");
            case HAVERSINE_FACTOR -> validateDecimal(trimmed, 1, 3, "Hệ số phải là số trong khoảng 1.0 đến 3.0");
            case LOADING_TIME -> validateInteger(trimmed, 0, 240, "Thời gian bốc dỡ phải là số nguyên từ 0 đến 240 phút");
            case OSRM_TIMEOUT -> validateInteger(trimmed, 1, 120, "Thời gian chờ phải là số nguyên từ 1 đến 120 giây");
            case PAGE_SIZE -> validateInteger(trimmed, 5, 200, "Số dòng mỗi trang phải là số nguyên từ 5 đến 200");
            case LOOKUP_RATE_LIMIT -> validateInteger(trimmed, 1, 1000, "Giới hạn tra cứu phải là số nguyên từ 1 đến 1000");

            case DEPARTURE_TIME -> HH_MM_PATTERN.matcher(trimmed).matches()
                    ? null : "Giờ xuất phát phải có dạng HH:mm, ví dụ 08:00";

            // Chan ca hai loi thuong gap: go thieu giao thuc, va go du dau / o cuoi
            case OSRM_URL -> {
                if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                    yield "Địa chỉ phải bắt đầu bằng http:// hoặc https://";
                }
                yield trimmed.endsWith("/") ? "Địa chỉ không được kết thúc bằng dấu /" : null;
            }

            default -> null;
        };
    }

    private String validateDecimal(String trimmed, double min, double max, String errorMessage) {
        try {
            double number = Double.parseDouble(trimmed);
            return (number < min || number > max) ? errorMessage : null;
        } catch (NumberFormatException ex) {
            return errorMessage;
        }
    }

    private String validateInteger(String trimmed, int min, int max, String errorMessage) {
        try {
            int number = Integer.parseInt(trimmed);
            return (number < min || number > max) ? errorMessage : null;
        } catch (NumberFormatException ex) {
            return errorMessage;
        }
    }

    /**
     * Luu cac gia tri da sua. Chi ghi nhung ma DA CO san trong bang — ma la gui len
     * bi bo qua, khong tao ban ghi moi (CBR-15: du lieu tu ngoai gui vao khong duoc
     * tu sinh them tham so).
     */
    @Transactional
    public int updateValues(Map<String, String> newValues) {
        int updatedCount = 0;
        for (SystemParameter parameter : systemParameterRepository.findAll()) {
            String newValue = newValues.get(parameter.getCode());
            if (newValue == null || newValue.trim().equals(parameter.getValue())) {
                continue;
            }
            log.info("Doi tham so {}: {} -> {}", parameter.getCode(), parameter.getValue(), newValue.trim());
            parameter.setValue(newValue.trim());
            systemParameterRepository.save(parameter);
            updatedCount++;
        }
        return updatedCount;
    }
}
