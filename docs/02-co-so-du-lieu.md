# 02 — Cơ sở dữ liệu

Hệ quản trị: **MySQL 8**. Charset `utf8mb4`, collation `utf8mb4_unicode_ci`.

Chuỗi kết nối bắt buộc có tham số encoding:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/mproute?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

`show-sql=true` giữ nguyên trong suốt quá trình làm — nhìn được câu SQL thật mà JPA sinh ra, rất hữu ích khi cần giải thích trước hội đồng.

---

## Quy ước đặt tên

**Toàn bộ định danh trong code và cơ sở dữ liệu viết bằng tiếng Anh.** Chỉ chuỗi hiển thị ra giao diện mới là tiếng Việt có dấu, vì người dùng cuối là chủ tạp hóa và tài xế.

| Đối tượng | Quy ước | Ví dụ |
|---|---|---|
| Bảng | snake_case, **số nhiều** | `orders`, `order_items`, `route_stops` |
| Cột | snake_case, số ít | `total_weight`, `delivery_date` |
| Khóa ngoại | `<bảng số ít>_id` | `customer_id`, `vehicle_id` |
| Entity | PascalCase, **số ít** | `Order`, `OrderItem`, `RouteStop` |
| Thuộc tính Java | camelCase | `totalWeight`, `deliveryDate` |
| Enum | PascalCase, giá trị UPPER_SNAKE_CASE | `OrderStatus.PENDING_APPROVAL` |

**Vì sao tên bảng để số nhiều:** `ORDER` và `ROUTE` là từ khóa của SQL. Đặt bảng là `order` thì mọi câu truy vấn phải bọc dấu backtick, rất dễ sinh lỗi. Số nhiều tránh được hoàn toàn va chạm này, và cũng là quy ước phổ biến nhất trong thực tế.

Entity `Order` ánh xạ sang bảng `orders` qua `@Table(name = "orders")`.

---

## Quy ước kiểu dữ liệu

| Loại dữ liệu | Kiểu MySQL | Kiểu Java | Lý do |
|---|---|---|---|
| Khóa chính | `BIGINT AUTO_INCREMENT` | `Long` | |
| Tiền | `DECIMAL(15,2)` | `BigDecimal` | **Không dùng double cho tiền** |
| Khối lượng (kg) | `DECIMAL(10,2)` | `Double` | |
| Thể tích (m³) | `DECIMAL(10,3)` | `Double` | 3 chữ số vì giá trị nhỏ |
| Tọa độ | `DECIMAL(10,7)` | `Double` | 7 chữ số ≈ độ chính xác 1 cm |
| Khoảng cách | `DOUBLE` (đơn vị mét) | `Double` | **Lưu bằng mét, chia 1000 khi hiển thị km** |
| Trạng thái | `VARCHAR(30)` | `enum` Java | Dùng `@Enumerated(EnumType.STRING)` |
| Thời điểm | `DATETIME` | `LocalDateTime` | |
| Ngày | `DATE` | `LocalDate` | |

**Lưu khoảng cách bằng mét** để tránh sai số cộng dồn khi tính tổng nhiều chặng.

---

## Sơ đồ quan hệ

```
users ─┬─< drivers ──< routes
       └─< orders (created_by)

depots ──< routes

products ──< order_items >── orders >── customers

vehicles ──< routes ──< route_stops >── orders

routes ──< incidents

orders ──< status_history

distance_matrix     (bang doc lap, tra theo toa do)
optimization_results (bang doc lap, luu theo ngay giao)
system_parameters   (bang doc lap)
notifications >── users
```

---

## Chi tiết từng bảng

### 1. `users` — người dùng hệ thống

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGINT | PK, AI | |
| `username` | VARCHAR(50) | NOT NULL, UNIQUE | |
| `password` | VARCHAR(100) | NOT NULL | Băm BCrypt |
| `full_name` | VARCHAR(100) | NOT NULL | |
| `email` | VARCHAR(100) | | |
| `role` | VARCHAR(20) | NOT NULL | `ADMIN`, `DISPATCHER`, `DRIVER` |
| `status` | VARCHAR(20) | NOT NULL | `ACTIVE`, `LOCKED` |
| `failed_login_attempts` | INT | DEFAULT 0 | Khóa tạm sau 5 lần sai |
| `locked_until` | DATETIME | | Thời điểm hết khóa tạm |
| `created_at` | DATETIME | NOT NULL | |

### 2. `depots` — kho

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGINT | PK, AI | |
| `name` | VARCHAR(100) | NOT NULL | |
| `address` | VARCHAR(255) | NOT NULL | |
| `latitude` | DECIMAL(10,7) | NOT NULL | |
| `longitude` | DECIMAL(10,7) | NOT NULL | |
| `active` | BOOLEAN | NOT NULL DEFAULT TRUE | Chỉ một bản ghi được TRUE |

Dùng từ **depot** thay vì *warehouse* vì đây là thuật ngữ chuẩn của bài toán VRP — trùng với `c0` trong `03-thuat-toan.md` và với tài liệu CVRPLIB.

### 3. `products` — sản phẩm

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGINT | PK, AI | |
| `code` | VARCHAR(20) | NOT NULL, UNIQUE | Ví dụ `SP001` |
| `name` | VARCHAR(200) | NOT NULL | |
| `unit` | VARCHAR(20) | NOT NULL DEFAULT 'Thung' | |
| `weight_per_box` | DECIMAL(10,2) | NOT NULL, > 0 | kg — **đầu vào thuật toán** |
| `volume_per_box` | DECIMAL(10,3) | NOT NULL, > 0 | m³ — **đầu vào thuật toán** |
| `unit_price` | DECIMAL(15,2) | NOT NULL DEFAULT 0 | Giá một thùng |
| `status` | VARCHAR(20) | NOT NULL | `ACTIVE`, `INACTIVE` |
| `note` | VARCHAR(500) | | |

### 4. `vehicles` — phương tiện

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGINT | PK, AI | |
| `license_plate` | VARCHAR(20) | NOT NULL, UNIQUE | |
| `vehicle_type` | VARCHAR(50) | NOT NULL | |
| `max_weight` | DECIMAL(10,2) | NOT NULL, > 0 | kg — **ràng buộc thuật toán** |
| `max_volume` | DECIMAL(10,3) | NOT NULL, > 0 | m³ — **ràng buộc thuật toán** |
| `status` | VARCHAR(20) | NOT NULL | `AVAILABLE`, `IN_USE`, `MAINTENANCE` |
| `note` | VARCHAR(500) | | |

### 5. `drivers` — tài xế

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGINT | PK, AI | |
| `user_id` | BIGINT | FK → `users.id`, UNIQUE | |
| `full_name` | VARCHAR(100) | NOT NULL | |
| `phone` | VARCHAR(15) | NOT NULL | 10 chữ số, bắt đầu bằng 0 |
| `license_number` | VARCHAR(30) | | Số bằng lái |
| `default_vehicle_id` | BIGINT | FK → `vehicles.id`, NULL được | Xe thường lái |
| `status` | VARCHAR(20) | NOT NULL | `ACTIVE`, `INACTIVE` |

### 6. `customers` — khách hàng

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGINT | PK, AI | |
| `name` | VARCHAR(200) | NOT NULL | Tên cửa hàng/đại lý |
| `contact_person` | VARCHAR(100) | | |
| `phone` | VARCHAR(15) | NOT NULL | |
| `email` | VARCHAR(100) | | Dùng gửi thông báo ETA |
| `address` | VARCHAR(255) | NOT NULL | |
| `latitude` | DECIMAL(10,7) | NOT NULL | **Bắt buộc, CBR-07** |
| `longitude` | DECIMAL(10,7) | NOT NULL | |
| `payment_method` | VARCHAR(30) | NOT NULL | `CREDIT`, `CASH`, `PREPAID` |
| `delivery_note` | VARCHAR(500) | | "Chỉ nhận buổi sáng", "Xe lớn không vào được ngõ" |
| `status` | VARCHAR(20) | NOT NULL | `ACTIVE`, `INACTIVE` |

Index: `idx_customers_phone` trên `phone` (phục vụ tra cứu).

### 7. `orders` — đơn hàng

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGINT | PK, AI | |
| `code` | VARCHAR(20) | NOT NULL, UNIQUE | `MP` + yyMMdd + 4 số |
| `customer_id` | BIGINT | FK → `customers.id`, NOT NULL | |
| `delivery_date` | DATE | NOT NULL | |
| `delivery_address` | VARCHAR(255) | NOT NULL | Chép từ khách, cho phép ghi đè |
| `latitude` | DECIMAL(10,7) | NOT NULL | Chép hoặc geocode lại |
| `longitude` | DECIMAL(10,7) | NOT NULL | |
| `total_weight` | DECIMAL(10,2) | NOT NULL | Hệ thống tính, không sửa tay |
| `total_volume` | DECIMAL(10,3) | NOT NULL | Hệ thống tính |
| `total_amount` | DECIMAL(15,2) | NOT NULL DEFAULT 0 | Tổng tiền hàng |
| `payment_method` | VARCHAR(30) | NOT NULL | Điền sẵn từ khách, sửa được |
| `amount_to_collect` | DECIMAL(15,2) | NOT NULL DEFAULT 0 | Theo BR02-14 |
| `payment_status` | VARCHAR(30) | NOT NULL | `UNPAID`, `PAID`, `DEBT` |
| `status` | VARCHAR(30) | NOT NULL | Xem `01-nghiep-vu.md` |
| `cancel_reason` | VARCHAR(500) | | |
| `note` | VARCHAR(500) | | |
| `created_by_id` | BIGINT | FK → `users.id` | |
| `created_at` | DATETIME | NOT NULL | |

Index: `idx_orders_date_status` trên (`delivery_date`, `status`) — truy vấn nóng nhất, dùng mỗi lần phân tuyến.

### 8. `order_items` — chi tiết đơn hàng

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGINT | PK, AI | |
| `order_id` | BIGINT | FK → `orders.id`, NOT NULL | ON DELETE CASCADE |
| `product_id` | BIGINT | FK → `products.id`, NOT NULL | |
| `box_count` | INT | NOT NULL, > 0 | Số thùng |
| `line_weight` | DECIMAL(10,2) | NOT NULL | `box_count × weight_per_box` tại thời điểm tạo |
| `line_volume` | DECIMAL(10,3) | NOT NULL | `box_count × volume_per_box` |
| `unit_price` | DECIMAL(15,2) | NOT NULL | **Chép từ danh mục tại thời điểm tạo — BR02-13** |
| `line_total` | DECIMAL(15,2) | NOT NULL | `box_count × unit_price` |

Chép `unit_price`, `line_weight`, `line_volume` thay vì tham chiếu là chủ ý. Nếu chỉ tham chiếu tới `products`, khi quản trị viên đổi giá hoặc đổi khối lượng thùng thì đơn hàng cũ sẽ tự đổi theo — đó là lỗi nghiệp vụ nghiêm trọng. **Hội đồng hay hỏi đúng chỗ này.**

### 9. `routes` — tuyến giao hàng

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGINT | PK, AI | |
| `code` | VARCHAR(20) | NOT NULL, UNIQUE | `TT` + yyMMdd + 2 số |
| `depot_id` | BIGINT | FK → `depots.id`, NOT NULL | |
| `vehicle_id` | BIGINT | FK → `vehicles.id`, NOT NULL | |
| `driver_id` | BIGINT | FK → `drivers.id`, NULL khi còn DRAFT | |
| `delivery_date` | DATE | NOT NULL | |
| `total_distance` | DOUBLE | NOT NULL | Mét, **gồm cả chặng về kho** |
| `total_weight` | DECIMAL(10,2) | NOT NULL | |
| `total_volume` | DECIMAL(10,3) | NOT NULL | |
| `algorithm` | VARCHAR(30) | NOT NULL | `NN`, `NN_2OPT`, `MANUAL` |
| `execution_time_ms` | BIGINT | | Thời gian chạy thuật toán |
| `approximate_distance` | BOOLEAN | NOT NULL DEFAULT FALSE | TRUE khi dùng Haversine |
| `status` | VARCHAR(30) | NOT NULL | |
| `departed_at` | DATETIME | | Khi tài xế xác nhận nhận hàng |
| `completed_at` | DATETIME | | |
| `created_by_id` | BIGINT | FK → `users.id` | |
| `created_at` | DATETIME | NOT NULL | |

### 10. `route_stops` — điểm dừng

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGINT | PK, AI | |
| `route_id` | BIGINT | FK → `routes.id`, NOT NULL | ON DELETE CASCADE |
| `order_id` | BIGINT | FK → `orders.id`, NOT NULL, UNIQUE | Một đơn chỉ ở một tuyến — CBR-02 |
| `sequence` | INT | NOT NULL | Bắt đầu từ 1 |
| `leg_distance` | DOUBLE | NOT NULL | Mét, từ điểm trước tới điểm này |
| `estimated_arrival` | DATETIME | | ETA dự kiến |
| `actual_arrival` | DATETIME | | Khi tài xế cập nhật |
| `updated_latitude` | DECIMAL(10,7) | | GPS lúc tài xế bấm nút |
| `updated_longitude` | DECIMAL(10,7) | | |
| `status` | VARCHAR(30) | NOT NULL | `PENDING`, `DELIVERING`, `DELIVERED`, `FAILED`, `POSTPONED` |
| `failure_reason` | VARCHAR(50) | | Enum theo BR05-06 |
| `failure_note` | VARCHAR(500) | | |
| `collected_amount` | DECIMAL(15,2) | DEFAULT 0 | Số tiền đã thu |
| `collection_note` | VARCHAR(500) | | Khi thu lệch |

Unique key: `uk_route_sequence` trên (`route_id`, `sequence`).

### 11. `distance_matrix` — bảng cache khoảng cách

| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| `id` | BIGINT | PK, AI | |
| `from_latitude` | DECIMAL(10,7) | NOT NULL | |
| `from_longitude` | DECIMAL(10,7) | NOT NULL | |
| `to_latitude` | DECIMAL(10,7) | NOT NULL | |
| `to_longitude` | DECIMAL(10,7) | NOT NULL | |
| `distance_meters` | DOUBLE | NOT NULL | Mét |
| `duration_seconds` | INT | | OSRM cũng trả về thời gian |
| `source` | VARCHAR(20) | NOT NULL | `OSRM`, `HAVERSINE` |
| `updated_at` | DATETIME | NOT NULL | |

Unique key: `uk_coordinate_pair` trên (`from_latitude`, `from_longitude`, `to_latitude`, `to_longitude`).

**Lưu theo tọa độ chứ không theo id thực thể**, vì đầu và cuối có thể là kho hoặc khách hàng — hai loại thực thể khác nhau. Lưu theo tọa độ thì thống nhất.

**Bảng này là bảo hiểm khi demo.** Nếu hôm bảo vệ OSRM không chạy được, dữ liệu đã cache vẫn cho hệ thống hoạt động bình thường.

### 12. `optimization_results` — chỉ số mỗi lần chạy

Lưu chỉ số tại thời điểm chạy, phục vụ báo cáo (CBR-18).

| Cột | Kiểu | Mô tả |
|---|---|---|
| `id` | BIGINT | PK, AI |
| `delivery_date` | DATE | NOT NULL |
| `order_count` | INT | Số đơn được phân tuyến |
| `route_count` | INT | Số tuyến sinh ra |
| `vehicle_count` | INT | Số xe sử dụng |
| `baseline_a_distance` | DOUBLE | Mốc A — mỗi đơn một chuyến khứ hồi |
| `baseline_b_distance` | DOUBLE | Mốc B — cùng phân xe, thứ tự theo thứ tự nhận đơn |
| `optimized_distance` | DOUBLE | Sau tối ưu |
| `reduction_a_meters` | DOUBLE | |
| `reduction_a_percent` | DOUBLE | |
| `reduction_b_meters` | DOUBLE | |
| `reduction_b_percent` | DOUBLE | |
| `weight_utilization` | DOUBLE | Trung bình % sử dụng tải trọng |
| `volume_utilization` | DOUBLE | Trung bình % sử dụng thể tích |
| `algorithm` | VARCHAR(30) | |
| `execution_time_ms` | BIGINT | |
| `approximate_distance` | BOOLEAN | |
| `created_at` | DATETIME | |

**Hai mốc so sánh là điểm quan trọng** — xem `07-thuc-nghiem.md` để hiểu vì sao cần cả hai.

### 13. `incidents` — sự cố

| Cột | Kiểu | Mô tả |
|---|---|---|
| `id` | BIGINT | PK, AI |
| `route_id` | BIGINT | FK → `routes.id`, NOT NULL |
| `driver_id` | BIGINT | FK → `drivers.id` |
| `incident_type` | VARCHAR(30) | `VEHICLE_BREAKDOWN`, `TRAFFIC_JAM`, `ACCIDENT`, `OTHER` |
| `severity` | VARCHAR(30) | `CAN_CONTINUE`, `CANNOT_CONTINUE` |
| `description` | VARCHAR(500) | |
| `delay_minutes` | INT | Chỉ dùng khi `CAN_CONTINUE` |
| `latitude` | DECIMAL(10,7) | Vị trí xe lúc báo |
| `longitude` | DECIMAL(10,7) | |
| `estimated_location` | BOOLEAN | TRUE nếu không lấy được GPS |
| `reported_at` | DATETIME | NOT NULL |
| `applied_solution` | VARCHAR(30) | `A_RESCUE_VEHICLE`, `B_INSERT_OTHER_ROUTE`, `C_POSTPONE`, `MANUAL` |
| `solution_note` | VARCHAR(500) | |
| `extra_distance` | DOUBLE | Km phát sinh, lưu bằng mét |
| `resolved_at` | DATETIME | |
| `resolved_by_id` | BIGINT | FK → `users.id` |
| `status` | VARCHAR(30) | `PENDING`, `RESOLVED` |

### 14. `status_history` — lịch sử chuyển trạng thái

| Cột | Kiểu | Mô tả |
|---|---|---|
| `id` | BIGINT | PK, AI |
| `entity_type` | VARCHAR(30) | `ORDER`, `ROUTE` |
| `entity_id` | BIGINT | NOT NULL |
| `old_status` | VARCHAR(30) | |
| `new_status` | VARCHAR(30) | NOT NULL |
| `changed_at` | DATETIME | NOT NULL |
| `changed_by_id` | BIGINT | FK → `users.id`, NULL nếu hệ thống tự chuyển |
| `note` | VARCHAR(500) | |

Index: `idx_status_history_entity` trên (`entity_type`, `entity_id`).

Bảng này chính là nguồn dữ liệu để vẽ **hành trình đơn hàng** trên trang tra cứu.

### 15. `notifications` — thông báo

| Cột | Kiểu | Mô tả |
|---|---|---|
| `id` | BIGINT | PK, AI |
| `recipient_id` | BIGINT | FK → `users.id`, NULL nếu gửi khách qua email |
| `recipient_email` | VARCHAR(100) | Dùng khi gửi cho khách |
| `event_code` | VARCHAR(20) | `NTF-01` … `NTF-09` |
| `content` | VARCHAR(1000) | |
| `link` | VARCHAR(255) | Đường dẫn để bấm vào |
| `is_read` | BOOLEAN | DEFAULT FALSE |
| `send_status` | VARCHAR(20) | `NOT_SENT`, `SENT`, `ERROR` |
| `created_at` | DATETIME | NOT NULL |

Đặt `is_read` chứ không phải `read` để tránh nhầm với từ khóa và để rõ đây là cờ boolean.

### 16. `system_parameters` — tham số hệ thống

| Cột | Kiểu | Mô tả |
|---|---|---|
| `id` | BIGINT | PK, AI |
| `code` | VARCHAR(50) | NOT NULL, UNIQUE |
| `value` | VARCHAR(255) | NOT NULL |
| `description` | VARCHAR(500) | |

Giá trị khởi tạo:

| `code` | `value` | Mô tả |
|---|---|---|
| `AVERAGE_SPEED` | `30` | km/h |
| `LOADING_TIME` | `15` | phút mỗi điểm |
| `DEPARTURE_TIME` | `08:00` | |
| `HAVERSINE_FACTOR` | `1.3` | |
| `DEFAULT_ALGORITHM` | `OR_TOOLS` | Bộ giải chính. Đổi sang `NN_2OPT` để chạy bản tự cài đặt |
| `OSRM_URL` | `http://localhost:5000` | |
| `OSRM_TIMEOUT` | `10` | giây |
| `PAGE_SIZE` | `20` | Số dòng mỗi trang |
| `LOOKUP_RATE_LIMIT` | `20` | lần/phút/IP |

---

## Danh sách enum Java cần tạo

Đặt trong package `entity.enums`. **18 enum.**

```
Role                  : ADMIN, DISPATCHER, DRIVER
UserStatus            : ACTIVE, LOCKED
RecordStatus          : ACTIVE, INACTIVE          <- products, customers, drivers
OrderStatus           : PENDING_APPROVAL, APPROVED, ROUTED, DELIVERING,
                        DELIVERED, FAILED, POSTPONED, CANCELLED
RouteStatus           : DRAFT, CONFIRMED, IN_PROGRESS, INCIDENT, COMPLETED, CANCELLED
StopStatus            : PENDING, DELIVERING, DELIVERED, FAILED, POSTPONED
VehicleStatus         : AVAILABLE, IN_USE, MAINTENANCE
PaymentMethod         : CREDIT, CASH, PREPAID
PaymentStatus         : UNPAID, PAID, DEBT
FailureReason         : CUSTOMER_ABSENT, CUSTOMER_REFUSED, WRONG_ADDRESS,
                        DAMAGED_GOODS, OTHER
IncidentType          : VEHICLE_BREAKDOWN, TRAFFIC_JAM, ACCIDENT, OTHER
IncidentSeverity      : CAN_CONTINUE, CANNOT_CONTINUE
IncidentStatus        : PENDING, RESOLVED                 <- bang incidents
SolutionType          : A_RESCUE_VEHICLE, B_INSERT_OTHER_ROUTE, C_POSTPONE, MANUAL
EntityType            : ORDER, ROUTE                      <- bang status_history
NotificationSendStatus: NOT_SENT, SENT, ERROR             <- bang notifications
AlgorithmType         : OR_TOOLS, NN, NN_2OPT, MANUAL
DistanceSource        : OSRM, HAVERSINE
```

Bốn enum có dấu `<-` là các cột trạng thái nằm rải trong bảng, rất dễ bỏ sót khi tạo entity vì chúng không nằm trong nhóm trạng thái chính.

`RecordStatus` dùng chung cho `products`, `customers` và `drivers` — cả ba đều chỉ cần ACTIVE/INACTIVE nên không tách riêng.

Mỗi enum có thêm trường `displayName` để đổ ra giao diện tiếng Việt có dấu:

```java
@Getter
@RequiredArgsConstructor
public enum OrderStatus {

    PENDING_APPROVAL("Chờ duyệt"),
    APPROVED("Đã duyệt"),
    ROUTED("Đã phân tuyến"),
    DELIVERING("Đang giao"),
    DELIVERED("Đã giao"),
    FAILED("Giao thất bại"),
    POSTPONED("Hoãn"),
    CANCELLED("Đã hủy");

    private final String displayName;
}
```

**Giá trị enum lưu vào DB là chuỗi tiếng Anh** (`@Enumerated(EnumType.STRING)`), còn `displayName` chỉ dùng để hiển thị. Đây là cách tách bạch giữa dữ liệu và giao diện.

### Bẫy bắt buộc phải biết: `@Enumerated(STRING)` sinh ra `ENUM` chứ không phải `VARCHAR`

Hibernate 6 **mặc định ánh xạ enum Java sang kiểu `ENUM` native của MySQL**, không phải `VARCHAR` như đặc tả ở trên. Phải đánh dấu **từng trường** để ép về `VARCHAR`:

```java
@Enumerated(EnumType.STRING)
@JdbcTypeCode(SqlTypes.VARCHAR)          // <- bat buoc, khong duoc quen
@Column(name = "status", nullable = false, length = 20)
private OrderStatus status;
```

Hai lý do phải làm việc này:

1. **Thêm giá trị enum mới sẽ hỏng.** Cột `ENUM('ACTIVE','INACTIVE')` chốt cứng danh sách ngay trong kiểu dữ liệu. Thêm một hằng số vào enum Java rồi lưu xuống sẽ bị MySQL từ chối, mà `ddl-auto=update` **không tự ALTER kiểu enum**. Lỗi xảy ra lúc chạy, không lúc biên dịch.
2. **MySQL lưu `ENUM` theo chỉ số bên trong, không theo chuỗi.** Hibernate xếp các giá trị theo thứ tự bảng chữ cái. Thêm một giá trị mới làm thứ tự đổi, nếu cột bị tạo lại thì dữ liệu cũ bị đọc sai nghĩa.

Đặt tham số toàn cục **không có tác dụng** — đã thử `hibernate.type.prefer_native_enum_types=false` trong `application.properties`, Hibernate 6.6 vẫn sinh `ENUM`. Chỉ annotation trên từng trường mới ăn.

Cách kiểm nhanh sau khi tạo entity:

```sql
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'mproute' AND DATA_TYPE = 'enum';
```

Truy vấn này phải trả về **rỗng**. Còn dòng nào là còn trường quên `@JdbcTypeCode`.

### Hệ quả còn lại: Hibernate vẫn sinh CHECK constraint

Kể cả sau khi ép về `VARCHAR`, Hibernate vẫn sinh thêm ràng buộc kiểm tra:

```sql
send_status varchar(20) not null check (send_status in ('NOT_SENT','SENT','ERROR'))
```

MySQL 8 thi hành ràng buộc này thật. Thêm một hằng số mới vào enum Java rồi lưu xuống sẽ bị chặn:

```
ERROR 3819: Check constraint 'users_chk_2' is violated
```

**Không có tham số nào của Hibernate 6.6 tắt được ràng buộc này** (đã tìm trong jar). Nhưng giữ lại vẫn tốt hơn kiểu `ENUM` native, vì cách hỏng khác hẳn nhau:

| | Kiểu `ENUM` native | `VARCHAR` + CHECK |
|---|---|---|
| Cách hỏng | MySQL lưu theo **chỉ số**; đổi thứ tự giá trị làm dữ liệu cũ bị đọc **sai nghĩa, âm thầm** | Chèn giá trị lạ thì **báo lỗi ngay**, không dữ liệu nào bị hiểu sai |
| Chẩn đoán | Rất khó | Thông báo chỉ đúng bảng và cột |

**Cách xử lý khi thêm giá trị enum mới:**

- Giai đoạn đang phát triển (chưa có dữ liệu thật) → xóa và tạo lại database, nhanh nhất:
  ```sql
  DROP DATABASE mproute;
  CREATE DATABASE mproute CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  ```
- Khi đã có dữ liệu seed (từ tuần 11) → gỡ ràng buộc cũ rồi để Hibernate tạo lại:
  ```sql
  ALTER TABLE users DROP CHECK users_chk_2;
  ```

---

## Thứ tự tạo entity

Tạo theo thứ tự này để tránh lỗi khóa ngoại:

1. `User`, `SystemParameter`, `DistanceMatrixEntry` (không phụ thuộc gì)
2. `Depot`, `Product`, `Vehicle`
3. `Driver` (phụ thuộc User, Vehicle)
4. `Customer`
5. `Order` (phụ thuộc Customer, User)
6. `OrderItem` (phụ thuộc Order, Product)
7. `Route` (phụ thuộc Depot, Vehicle, Driver)
8. `RouteStop` (phụ thuộc Route, Order)
9. `Incident`, `StatusHistory`, `Notification`, `OptimizationResult`

Để `ddl-auto=update` tự sinh bảng, không cần viết script `CREATE TABLE` thủ công. Chỉ viết script `INSERT` cho dữ liệu mẫu — xem `08-du-lieu-mau.md`.
