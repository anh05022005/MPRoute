package com.minhphu.mproute.service;

import com.minhphu.mproute.entity.SystemParameter;
import com.minhphu.mproute.entity.enums.AlgorithmType;
import com.minhphu.mproute.repository.SystemParameterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    public static final String DEFAULT_ALGORITHM = "DEFAULT_ALGORITHM";
    public static final String OSRM_URL = "OSRM_URL";
    public static final String OSRM_TIMEOUT = "OSRM_TIMEOUT";
    public static final String PAGE_SIZE = "PAGE_SIZE";
    public static final String LOOKUP_RATE_LIMIT = "LOOKUP_RATE_LIMIT";

    private static final Pattern GIO_PHUT = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");

    /** Mot dong trong bang tham so: ma, gia tri khoi tao, mo ta hien ra man hinh. */
    public record ParameterDefault(String code, String defaultValue, String description) {}

    /*
     * Thu tu trong danh sach nay chinh la thu tu hien tren man hinh.
     * Nam tham so dau anh huong THANG toi ket qua thuat toan, xep len tren.
     * Gia tri khoi tao lay tu docs/02-co-so-du-lieu.md muc 16.
     */
    private static final List<ParameterDefault> MAC_DINH = List.of(
            new ParameterDefault(AVERAGE_SPEED, "30",
                    "Tốc độ trung bình khi tính giờ đến dự kiến (km/h)"),
            new ParameterDefault(LOADING_TIME, "15",
                    "Thời gian bốc dỡ tại mỗi điểm giao (phút)"),
            new ParameterDefault(DEPARTURE_TIME, "08:00",
                    "Giờ xe xuất phát từ kho, dạng HH:mm"),
            new ParameterDefault(HAVERSINE_FACTOR, "1.3",
                    "Hệ số quy đổi từ đường chim bay sang đường đi thật, dùng khi OSRM không chạy"),
            new ParameterDefault(DEFAULT_ALGORITHM, AlgorithmType.OR_TOOLS.name(),
                    "Thuật toán chọn sẵn trên màn hình phân tuyến"),
            new ParameterDefault(OSRM_URL, "http://localhost:5000",
                    "Địa chỉ máy chủ OSRM tính khoảng cách"),
            new ParameterDefault(OSRM_TIMEOUT, "10",
                    "Thời gian chờ tối đa khi gọi OSRM (giây)"),
            new ParameterDefault(PAGE_SIZE, "20",
                    "Số dòng mỗi trang trên các màn hình danh sách"),
            new ParameterDefault(LOOKUP_RATE_LIMIT, "20",
                    "Giới hạn tra cứu công khai (lần/phút/IP) — BR07-09")
    );

    private final SystemParameterRepository systemParameterRepository;

    /**
     * Tao du chin tham so neu thieu. Chay lai nhieu lan khong sao: da co thi bo qua,
     * khong ghi de, nen sua gia tri xong khoi dong lai khong bi tra ve mac dinh.
     */
    @Transactional
    public void seedDefaults() {
        for (ParameterDefault macDinh : MAC_DINH) {
            if (systemParameterRepository.findByCode(macDinh.code()).isPresent()) {
                continue;
            }
            SystemParameter thamSo = new SystemParameter();
            thamSo.setCode(macDinh.code());
            thamSo.setValue(macDinh.defaultValue());
            thamSo.setDescription(macDinh.description());
            systemParameterRepository.save(thamSo);
            log.info("Da tao tham so {} = {}", macDinh.code(), macDinh.defaultValue());
        }
    }

    /** Danh sach tham so theo dung thu tu hien thi da dinh o MAC_DINH. */
    @Transactional(readOnly = true)
    public List<SystemParameter> findAllInDisplayOrder() {
        List<String> thuTu = MAC_DINH.stream().map(ParameterDefault::code).toList();
        List<SystemParameter> danhSach = new ArrayList<>(systemParameterRepository.findAll());
        // Tham so la nhung ma khong co trong MAC_DINH thi xep xuong cuoi
        danhSach.sort(Comparator.comparingInt(ts -> {
            int viTri = thuTu.indexOf(ts.getCode());
            return (viTri < 0) ? Integer.MAX_VALUE : viTri;
        }));
        return danhSach;
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
    public String kiemGiaTri(String code, String value) {
        String giaTri = (value == null) ? "" : value.trim();
        if (giaTri.isEmpty()) {
            return "Không được để trống";
        }

        return switch (code) {
            case AVERAGE_SPEED -> kiemSoThuc(giaTri, 1, 200, "Tốc độ phải là số trong khoảng 1 đến 200 km/h");
            case HAVERSINE_FACTOR -> kiemSoThuc(giaTri, 1, 3, "Hệ số phải là số trong khoảng 1.0 đến 3.0");
            case LOADING_TIME -> kiemSoNguyen(giaTri, 0, 240, "Thời gian bốc dỡ phải là số nguyên từ 0 đến 240 phút");
            case OSRM_TIMEOUT -> kiemSoNguyen(giaTri, 1, 120, "Thời gian chờ phải là số nguyên từ 1 đến 120 giây");
            case PAGE_SIZE -> kiemSoNguyen(giaTri, 5, 200, "Số dòng mỗi trang phải là số nguyên từ 5 đến 200");
            case LOOKUP_RATE_LIMIT -> kiemSoNguyen(giaTri, 1, 1000, "Giới hạn tra cứu phải là số nguyên từ 1 đến 1000");

            case DEPARTURE_TIME -> GIO_PHUT.matcher(giaTri).matches()
                    ? null : "Giờ xuất phát phải có dạng HH:mm, ví dụ 08:00";

            case DEFAULT_ALGORITHM -> laAlgorithmTypeHopLe(giaTri)
                    ? null : "Thuật toán không hợp lệ";

            // Chan ca hai loi thuong gap: go thieu giao thuc, va go du dau / o cuoi
            case OSRM_URL -> {
                if (!giaTri.startsWith("http://") && !giaTri.startsWith("https://")) {
                    yield "Địa chỉ phải bắt đầu bằng http:// hoặc https://";
                }
                yield giaTri.endsWith("/") ? "Địa chỉ không được kết thúc bằng dấu /" : null;
            }

            default -> null;
        };
    }

    private boolean laAlgorithmTypeHopLe(String giaTri) {
        for (AlgorithmType loai : AlgorithmType.values()) {
            if (loai.name().equals(giaTri)) {
                return true;
            }
        }
        return false;
    }

    private String kiemSoThuc(String giaTri, double min, double max, String thongBao) {
        try {
            double so = Double.parseDouble(giaTri);
            return (so < min || so > max) ? thongBao : null;
        } catch (NumberFormatException loi) {
            return thongBao;
        }
    }

    private String kiemSoNguyen(String giaTri, int min, int max, String thongBao) {
        try {
            int so = Integer.parseInt(giaTri);
            return (so < min || so > max) ? thongBao : null;
        } catch (NumberFormatException loi) {
            return thongBao;
        }
    }

    /**
     * Luu cac gia tri da sua. Chi ghi nhung ma DA CO san trong bang — ma la gui len
     * bi bo qua, khong tao ban ghi moi (CBR-15: du lieu tu ngoai gui vao khong duoc
     * tu sinh them tham so).
     */
    @Transactional
    public int updateValues(Map<String, String> giaTriMoi) {
        int soSua = 0;
        for (SystemParameter thamSo : systemParameterRepository.findAll()) {
            String moi = giaTriMoi.get(thamSo.getCode());
            if (moi == null || moi.trim().equals(thamSo.getValue())) {
                continue;
            }
            log.info("Doi tham so {}: {} -> {}", thamSo.getCode(), thamSo.getValue(), moi.trim());
            thamSo.setValue(moi.trim());
            systemParameterRepository.save(thamSo);
            soSua++;
        }
        return soSua;
    }
}
