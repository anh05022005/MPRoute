package com.minhphu.mproute.config;

import com.minhphu.mproute.entity.Customer;
import com.minhphu.mproute.entity.Depot;
import com.minhphu.mproute.entity.Driver;
import com.minhphu.mproute.entity.Product;
import com.minhphu.mproute.entity.User;
import com.minhphu.mproute.entity.Vehicle;
import com.minhphu.mproute.entity.enums.PaymentMethod;
import com.minhphu.mproute.entity.enums.RecordStatus;
import com.minhphu.mproute.entity.enums.VehicleStatus;
import com.minhphu.mproute.repository.CustomerRepository;
import com.minhphu.mproute.repository.DepotRepository;
import com.minhphu.mproute.repository.DriverRepository;
import com.minhphu.mproute.repository.ProductRepository;
import com.minhphu.mproute.repository.UserRepository;
import com.minhphu.mproute.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Nap danh muc nen theo dung bang so lieu trong docs/08-du-lieu-mau.md:
 * mot kho, nam san pham, bon xe, ba tai xe, muoi lam khach hang.
 *
 * <p>Vi sao phai co lop nay: kho git chua ma nguon chu khong chua du lieu. Truoc day
 * danh muc duoc nhap tay qua giao dien trong tuan 3-5 nen no chi ton tai trong MySQL
 * cua mot may. Dung mot may khac la moi man hinh deu trong, va cach chua duy nhat la
 * bung mot ban mysqldump mang theo. Co lop nay thi chi can clone va chay.
 *
 * <p>Thu tu chen KHONG duoc doi. {@link HistorySeeder} lay danh sach khach bang
 * findByStatus roi xao tron bang Random co hat giong co dinh, ma findByStatus khong co
 * ORDER BY nen MySQL tra ve theo thu tu khoa chinh. Doi thu tu chen o day la doi ID,
 * doi ID la doi ket qua xao tron, va muoi ngay du lieu sinh ra se khac di — keo theo
 * moi con so da viet trong luan van khong con dung nua.
 *
 * <p>Chay sau {@link DataSeeder} (Order 1) vi tai xe tro toi tai khoan dang nhap, va
 * truoc {@link HistorySeeder} (Order 3) vi don hang tro toi khach va san pham.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@org.springframework.core.annotation.Order(2)
public class CatalogSeeder implements CommandLineRunner {

    private final DepotRepository depotRepository;
    private final ProductRepository productRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        seedDepot();
        seedProducts();
        // Xe phai co truoc tai xe: moi tai xe tro toi mot xe mac dinh
        seedVehicles();
        seedDrivers();
        seedCustomers();
    }

    // ==================== KHO ====================

    private void seedDepot() {
        if (depotRepository.existsByActiveTrue()) {
            return;
        }

        Depot depot = new Depot();
        depot.setName("Kho Minh Phú");
        depot.setAddress("Đường Phan Trọng Tuệ, Thanh Trì, Hà Nội");
        depot.setLatitude(20.9497);
        depot.setLongitude(105.8172);
        depot.setActive(true);

        depotRepository.save(depot);
        log.info("Da tao kho mau: {}", depot.getName());
    }

    // ==================== SAN PHAM ====================

    /**
     * Nam san pham co doi trong luong va the tich co tinh tuong phan: bong tay trang
     * 6 kg nhung 0,085 m3 (nhe ma cong kenh), nuoc lau san 24,8 kg nhung 0,031 m3
     * (nang ma gon). Day chinh la du lieu chung minh vi sao phai kiem ca hai rang buoc
     * suc chua chu khong chi kiem khoi luong (CBR-03, CBR-04).
     */
    private void seedProducts() {
        seedProduct("SP001", "Bông tẩy trang", 6.0, 0.085, 480_000);
        seedProduct("SP002", "Khăn rửa mặt dùng 1 lần", 5.5, 0.072, 520_000);
        seedProduct("SP003", "Nước giặt 3.5L", 21.5, 0.028, 720_000);
        seedProduct("SP004", "Nước lau sàn 2L", 24.8, 0.031, 380_000);
        seedProduct("SP005", "Nước rửa tay 500ml", 14.2, 0.024, 450_000);
    }

    private void seedProduct(String code, String name, double weightPerBox, double volumePerBox,
                             long unitPrice) {
        if (productRepository.existsByCode(code)) {
            return;
        }

        Product product = new Product();
        product.setCode(code);
        product.setName(name);
        product.setUnit("Thùng");
        product.setWeightPerBox(weightPerBox);
        product.setVolumePerBox(volumePerBox);
        // Tien luon dung BigDecimal. valueOf(long) chu khong phai new BigDecimal(double),
        // vi khoi tao tu double keo theo sai so nhi phan cua chinh so double do
        product.setUnitPrice(BigDecimal.valueOf(unitPrice));
        product.setStatus(RecordStatus.ACTIVE);

        productRepository.save(product);
    }

    // ==================== PHUONG TIEN ====================

    /**
     * Xe thu tu de o trang thai MAINTENANCE co y: man hinh phan tuyen phai loai xe khong
     * san sang ra khoi danh sach chon, va luc demo can co san mot xe de chi vao cho do.
     */
    private void seedVehicles() {
        seedVehicle("29H-123.45", "Xe tải 1.5 tấn", 1500.0, 12.0, VehicleStatus.AVAILABLE);
        seedVehicle("29C-678.90", "Xe tải 500kg", 500.0, 6.0, VehicleStatus.AVAILABLE);
        seedVehicle("29F-555.66", "Xe tải 1 tấn", 1000.0, 8.0, VehicleStatus.AVAILABLE);
        seedVehicle("29B-222.11", "Xe tải 800kg", 800.0, 7.0, VehicleStatus.MAINTENANCE);
    }

    private void seedVehicle(String licensePlate, String vehicleType, double maxWeight,
                             double maxVolume, VehicleStatus status) {
        if (vehicleRepository.existsByLicensePlate(licensePlate)) {
            return;
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setLicensePlate(licensePlate);
        vehicle.setVehicleType(vehicleType);
        vehicle.setMaxWeight(maxWeight);
        vehicle.setMaxVolume(maxVolume);
        vehicle.setStatus(status);

        vehicleRepository.save(vehicle);
    }

    // ==================== TAI XE ====================

    /**
     * Ho ten o day phai TRUNG voi ho ten cua tai khoan dang nhap tuong ung trong
     * {@link DataSeeder}. Man hinh tai xe lay ten tu tai khoan, man hinh phan tuyen lay
     * ten tu ho so tai xe — lech nhau thi luc demo hai man hinh hien hai ten khac nhau
     * cho cung mot nguoi.
     */
    private void seedDrivers() {
        seedDriver("taixe01", "Nguyễn Văn Hùng", "0987654321", "29H-123.45");
        seedDriver("taixe02", "Lê Văn Nam", "0987654322", "29C-678.90");
        seedDriver("taixe03", "Phạm Văn Tú", "0987654323", "29F-555.66");
    }

    private void seedDriver(String username, String fullName, String phone,
                            String defaultVehiclePlate) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            // DataSeeder chay truoc nen khong bao gio roi vao day. Neu roi vao that thi
            // bo qua con hon la de ung dung chet luc khoi dong
            log.warn("Khong tim thay tai khoan {} nen bo qua ho so tai xe", username);
            return;
        }
        if (driverRepository.findByUserId(user.getId()).isPresent()) {
            return;
        }

        Driver driver = new Driver();
        driver.setUser(user);
        driver.setFullName(fullName);
        driver.setPhone(phone);
        driver.setDefaultVehicle(vehicleRepository.findByLicensePlate(defaultVehiclePlate)
                .orElse(null));
        driver.setStatus(RecordStatus.ACTIVE);

        driverRepository.save(driver);
    }

    // ==================== KHACH HANG ====================

    /**
     * Muoi lam vi tri co that trong noi thanh Ha Noi, cach kho Phan Trong Tue tu 5 den
     * 15 km. Rai rac du de thuat toan co viec lam: neu don dulieu tap trung mot cho thi
     * moi cach sap xep deu cho ket qua gan bang nhau va phan thuc nghiem khong chung
     * minh duoc gi.
     *
     * <p>Ba khach cuoi bang co ghi chu giao hang de demo truong nay duoc dung that.
     */
    private void seedCustomers() {
        List<CustomerSpec> specs = List.of(
                new CustomerSpec("Tạp hóa Minh Anh", "Ngô Thị Minh", "0912345001",
                        "24 Láng Hạ, Đống Đa", 21.0148, 105.8125, PaymentMethod.CREDIT, null),
                new CustomerSpec("Siêu thị Hồng Hà", "Trần Thị Bình", "0912345002",
                        "8 Thái Hà, Đống Đa", 21.0106, 105.8215, PaymentMethod.CREDIT,
                        "Chỉ nhận hàng buổi sáng trước 11h"),
                new CustomerSpec("Cửa hàng Bảo Châu", "Lê Văn Bảo", "0912345003",
                        "91 Chùa Bộc, Đống Đa", 21.0067, 105.8290, PaymentMethod.CASH, null),
                new CustomerSpec("Đại lý Thanh Tâm", "Phạm Thanh Tâm", "0912345004",
                        "12 Trần Duy Hưng, Cầu Giấy", 21.0102, 105.7963, PaymentMethod.PREPAID, null),
                new CustomerSpec("Tạp hóa Đức Lợi", "Vũ Đức Lợi", "0912345005",
                        "55 Nguyễn Trãi, Thanh Xuân", 20.9955, 105.8055, PaymentMethod.CASH, null),
                new CustomerSpec("Cửa hàng Kim Ngân", "Đỗ Kim Ngân", "0912345006",
                        "7 Lê Văn Lương, Thanh Xuân", 21.0025, 105.8010, PaymentMethod.CREDIT, null),
                new CustomerSpec("Siêu thị An Phát", "Hoàng Văn An", "0912345007",
                        "145 Giải Phóng, Hai Bà Trưng", 20.9895, 105.8412, PaymentMethod.CREDIT, null),
                new CustomerSpec("Tạp hóa Bình Minh", "Nguyễn Thị Hoa", "0912345008",
                        "30 Trương Định, Hoàng Mai", 20.9820, 105.8450, PaymentMethod.CASH, null),
                new CustomerSpec("Đại lý Phú Quý", "Trần Văn Quý", "0912345009",
                        "88 Nguyễn Xiển, Thanh Xuân", 20.9835, 105.8005, PaymentMethod.CREDIT, null),
                new CustomerSpec("Cửa hàng Hải Yến", "Lý Hải Yến", "0912345010",
                        "15 Kim Giang, Thanh Xuân", 20.9760, 105.8130, PaymentMethod.CASH, null),
                new CustomerSpec("Tạp hóa Thu Hà", "Bùi Thu Hà", "0912345011",
                        "60 Định Công, Hoàng Mai", 20.9788, 105.8320, PaymentMethod.CREDIT, null),
                new CustomerSpec("Siêu thị Tân Việt", "Đặng Tân", "0912345012",
                        "22 Ngọc Hồi, Thanh Trì", 20.9420, 105.8425, PaymentMethod.CREDIT, null),
                new CustomerSpec("Cửa hàng Vạn Xuân", "Ngô Văn Xuân", "0912345013",
                        "5 Cầu Bươu, Thanh Trì", 20.9585, 105.7955, PaymentMethod.CASH,
                        "Ngõ nhỏ, xe tải trên 1 tấn không vào được"),
                new CustomerSpec("Đại lý Hồng Phúc", "Phan Hồng Phúc", "0912345014",
                        "40 Tựu Liệt, Thanh Trì", 20.9520, 105.8280, PaymentMethod.CREDIT, null),
                new CustomerSpec("Tạp hóa Mai Lan", "Trịnh Mai Lan", "0912345015",
                        "18 Linh Đàm, Hoàng Mai", 20.9668, 105.8280, PaymentMethod.CASH,
                        "Gọi trước 15 phút khi tới"));

        for (CustomerSpec spec : specs) {
            seedCustomer(spec);
        }
    }

    private void seedCustomer(CustomerSpec spec) {
        // So dien thoai la thu duy nhat chac chan khong trung giua muoi lam khach nay,
        // nen dung no lam moc nhan biet da nap hay chua
        if (!customerRepository.findByPhone(spec.phone()).isEmpty()) {
            return;
        }

        Customer customer = new Customer();
        customer.setName(spec.name());
        customer.setContactPerson(spec.contactPerson());
        customer.setPhone(spec.phone());
        customer.setAddress(spec.address());
        customer.setLatitude(spec.latitude());
        customer.setLongitude(spec.longitude());
        customer.setPaymentMethod(spec.paymentMethod());
        customer.setDeliveryNote(spec.deliveryNote());
        customer.setStatus(RecordStatus.ACTIVE);

        customerRepository.save(customer);
    }

    /**
     * Gom mot dong cua bang khach hang trong docs/08. Dat rieng thanh record de danh
     * sach o tren doc duoc thang nhu doc bang trong tai lieu, khong phai dem tham so.
     *
     * <p>Khong dat email: docs/08 khong quy dinh, va CustomerNotificationService bo qua
     * khach khong co email nen chuc nang van chay binh thuong. Muon demo gui thu that
     * thi dien email vao mot khach qua giao dien truoc khi quay video.
     */
    private record CustomerSpec(String name,
                                String contactPerson,
                                String phone,
                                String address,
                                double latitude,
                                double longitude,
                                PaymentMethod paymentMethod,
                                String deliveryNote) {
    }
}
