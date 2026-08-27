# 10 — Quy ước kỹ thuật

Công nghệ, cấu trúc thư mục và quy ước đặt tên của hệ thống. Đọc file này cùng với
`00-tong-quan.md` trước khi đọc code.

---

## Bối cảnh

Đồ án tốt nghiệp cử nhân Công nghệ thông tin, **một người làm trong 12 tuần**. Không phải
dự án thương mại. Mọi quyết định kỹ thuật trong file này đều cân theo ràng buộc đó: chọn
cách đơn giản nhất chạy được, không chọn cách "đúng chuẩn enterprise".

Hệ thống quản lý và tối ưu tuyến đường giao hàng cho Công ty Minh Phú — doanh nghiệp nhỏ
sản xuất và phân phối hàng tiêu dùng (bông tẩy trang, khăn rửa mặt dùng một lần, nước giặt,
nước lau sàn, nước rửa tay) tại Hà Nội, kho đặt ở đường Phan Trọng Tuệ.

---

## Ba điều bị cấm

Cả ba đều **không phải quy tắc phong cách** — chúng là đúng/sai, hoặc là điều kiện ăn điểm.

| Cấm | Vì sao |
|---|---|
| **Import OR-Tools vào `optimization/heuristic/`** | Package đó là bản tự cài đặt dùng làm mốc so sánh ở chương thực nghiệm — lẫn thư viện vào là mất ý nghĩa so sánh. Bộ giải OR-Tools nằm riêng ở `optimization/ortools/` |
| **`float`** ở bất kỳ đâu | Tiền dùng `BigDecimal`, tọa độ và khoảng cách dùng `double`. `float` sai số quá lớn cho cả hai |
| **Gọi OSRM / Repository trong vòng lặp thuật toán** | 2-opt gọi hàm khoảng cách hàng nghìn lần, mỗi lần một HTTP request là treo chương trình. Ma trận phải nạp sẵn vào bộ nhớ, thuật toán chỉ tra `matrix[i][j]` |

Ngoài ba điều trên, viết code như một lập trình viên Java bình thường: **Lombok, Stream API,
lambda, method reference, `Optional`, `var`, record, annotation đầy đủ — dùng thoải mái ở mọi
nơi, kể cả trong `service/optimization/`.**

---

## Ba lỗi kinh điển phải tự kiểm

1. **Chặng cuối về kho.** Tổng quãng đường tuyến phải gồm cả `kho → điểm đầu` **và**
   `điểm cuối → kho` (CBR-01). Đây là lỗi kinh điển nhất của đồ án VRP.
2. **Hai ràng buộc sức chứa kiểm đồng thời.** Khối lượng (kg) và thể tích (m³), không được
   chỉ kiểm một (CBR-03, CBR-04).
3. **Ghi `status_history`** mỗi lần chuyển trạng thái đơn hoặc tuyến (CBR-14).

Ba lỗi hay gặp khác:

4. **Encoding tiếng Việt.** Chuỗi kết nối MySQL phải có `?useUnicode=true&characterEncoding=UTF-8`.
5. **Ẩn nút trên giao diện mà quên chặn ở server.** Mọi endpoint phải kiểm tra quyền phía
   server (CBR-15) — hội đồng sẽ gõ thẳng địa chỉ vào thanh địa chỉ để thử.
6. **Xóa cứng bản ghi đã phát sinh dữ liệu nghiệp vụ.** Chỉ chuyển `INACTIVE` (CBR-17).

---

## Phải giảng lại được phần thuật toán

Sinh viên bảo vệ trực tiếp trước hội đồng: họ mở file ra, chỉ vào một dòng và hỏi "dòng này
làm gì, tại sao viết thế này". `service/optimization/` là chỗ bị hỏi sâu nhất.

Code kiểu gì cũng được, nhưng nếu một chuỗi Stream lồng nhau mà lúc bị hỏi không tách ra
giải thích từng bước được, thì nên viết lại đoạn đó rõ hơn — không phải vì quy tắc, mà vì
điểm vấn đáp.

---

## Công nghệ

| Thành phần | Lựa chọn |
|---|---|
| Ngôn ngữ | Java 21 |
| Framework | Spring Boot 3.x |
| Thư viện Spring | Web, Data JPA, Security, Thymeleaf, Validation, Mail |
| Cơ sở dữ liệu | MySQL 8 |
| Giao diện | Thymeleaf + Bootstrap 5 + Leaflet |
| Tính khoảng cách | OSRM chạy Docker cục bộ, dự phòng Haversine |
| Geocoding | Nominatim |
| Build | Maven |
| Chạy | localhost, **không deploy** |

**Có dùng:** Lombok (giảm getter/setter thủ công).

**Có dùng:** OR-Tools `9.15.6755` làm bộ giải CVRP chính (`com.google.ortools:ortools-java`).
Kéo theo thư viện native của cả 5 nền tảng, khoảng 113 MB trong `~/.m2` — chỉ bản
`ortools-win32-x86-64` được nạp lúc chạy. Không loại bớt các nền tảng kia vì không đáng
đánh đổi lấy rủi ro build gãy khó hiểu khi đổi máy.

**Không dùng:** React/Vue, jsprit, OptaPlanner, Redis, Kafka, Docker Compose cho
ứng dụng (chỉ dùng Docker cho OSRM).

Lưu ý kỹ thuật: JDK trên máy phát triển là 23, mà từ JDK 21 `javac` không còn tự chạy
annotation processor trên classpath. `pom.xml` đã khai báo `annotationProcessorPaths` cho
Lombok — **đừng xóa đoạn đó**, xóa là Lombok im lặng không sinh getter.

---

## Cấu trúc thư mục

```
src/main/java/com/minhphu/mproute/
├── MpRouteApplication.java
├── config/          SecurityConfig, WebConfig, DataSeeder
├── entity/          Cac lop anh xa bang du lieu
├── repository/      Interface ke thua JpaRepository
├── service/         Logic nghiep vu
│   └── optimization/       Rieng phan thuat toan toi uu
├── controller/      Nhan request, tra ve ten view
├── security/        CustomUserDetailsService, CurrentUser, xu ly dang nhap
├── dto/             Doi tuong truyen du lieu giua cac lop
└── util/            Ham tien ich dung chung

src/main/resources/
├── templates/       File .html Thymeleaf, chia thu muc theo chuc nang
├── static/
│   ├── css/style.css
│   └── js/
├── application.properties
└── data/            File du lieu mau, file benchmark Augerat
```

---

## Quy ước đặt tên

**Mọi định danh viết bằng tiếng Anh**: tên bảng, tên cột, entity, biến, method, giá trị enum,
URL. Bảng `orders` thì entity là `Order`, cột `total_weight` thì thuộc tính là `totalWeight`.

| Đối tượng | Quy ước | Ví dụ |
|---|---|---|
| Bảng CSDL | snake_case tiếng Anh, **số nhiều** | `orders`, `order_items` |
| Cột CSDL | snake_case | `total_weight`, `delivery_date` |
| Entity | PascalCase tiếng Anh, **số ít** | `Order`, `OrderItem` |
| Thuộc tính Java | camelCase | `totalWeight`, `deliveryDate` |
| Repository | `<Entity>Repository` | `OrderRepository` |
| Service | `<Nghiệp vụ>Service` | `RoutingService` |
| Controller | `<Nghiệp vụ>Controller` | `RoutingController` |
| Template | kebab-case theo thư mục | `templates/orders.html` |
| URL | kebab-case tiếng Anh | `/orders`, `/driver/my-route` |
| Enum | UPPER_SNAKE_CASE | `ROUTED`, `PENDING_APPROVAL` |

**Tên bảng để số nhiều** (`orders`, `routes`) vì `ORDER` và `ROUTE` là từ khóa SQL. Entity thì
số ít, ánh xạ qua `@Table(name = "orders")`.

Dùng **`depot`** thay vì *warehouse* cho kho — đây là thuật ngữ chuẩn của bài toán VRP, trùng
với `c0` trong `03-thuat-toan.md` và với tài liệu CVRPLIB.

Ngoại lệ: giữ nguyên tiếng Anh cho các từ đã thành thuật ngữ — `Controller`, `Service`,
`Repository`, `Entity`, `ETA`, `OSRM`.

### Ngôn ngữ trong code

- **Mọi định danh viết bằng tiếng Anh, không có ngoại lệ**: tên class, method, biến cục bộ,
  field, tên file, class CSS, biến JavaScript, tên bảng, tên cột, giá trị enum, URL.
  `tinhTongQuangDuong()` phải là `calculateTotalDistance()`, `.thanh-tieu-de` phải là
  `.app-header`. Đây là thứ người ngoài nhìn vào đánh giá đầu tiên.
- **Comment viết tiếng Việt không dấu.** Định danh nói *cái gì*, comment nói *tại sao* — và
  phần "tại sao" là thứ cần diễn đạt trôi chảy nhất, nên viết bằng tiếng mẹ đẻ là hợp lý.
  Lúc hội đồng chỉ vào một dòng thì comment ngay trên đó đã là câu trả lời sẵn.
- **Comment trả lời *tại sao*, không kể lại *cái gì*.** `// tang bien dem` đặt trên `i++` là rác.
  Comment có giá trị là chỗ giải thích vì sao tổng quãng đường phải cộng thêm chặng cuối về kho,
  hoặc vì sao phải đối xứng hóa ma trận trước khi chạy 2-opt.
- **Commit message viết tiếng Anh** — xem mục "Quy ước Git" bên dưới.
- **Chuỗi hiển thị ra giao diện là tiếng Việt có dấu bình thường**, vì người dùng cuối là chủ
  tạp hóa và tài xế. Mỗi enum có trường `displayName` để đổ ra giao diện — dữ liệu lưu
  `PENDING_APPROVAL`, màn hình hiện "Chờ duyệt".
- Phần `service/optimization/` vẫn phải **giảng lại được bằng tiếng Việt khi vấn đáp**. Comment
  tiếng Anh không cản trở việc đó — hội đồng hỏi bạn hiểu thuật toán đến đâu, không hỏi comment
  viết bằng tiếng gì.

---

## Kiểu dữ liệu

| Loại | Kiểu Java |
|---|---|
| Tiền | `BigDecimal` |
| Khối lượng, thể tích, tọa độ | `Double` |
| Khoảng cách | `Double`, **lưu bằng mét**, chia 1000 khi hiển thị |
| Trạng thái | `enum` với `@Enumerated(EnumType.STRING)` |
| Thời điểm / Ngày | `LocalDateTime` / `LocalDate` |

---

## Kiểm thử

Đồ án **không đặt mục tiêu phủ 80% code**. Viết test tràn lan sẽ ăn mất thời gian của tuần 6–7
vốn là trái tim đồ án. Mục tiêu là có đủ test để trả lời câu hỏi "em kiểm thử thế nào" và để
chắc chắn thuật toán đúng.

**14 test bắt buộc**, đặt trong `src/test/java/.../optimization/`. Mười test cho bản tự cài đặt:

| Test | Kiểm cái gì |
|---|---|
| `totalDistanceIncludesReturnLegToDepot` | Tuyến 3 điểm, tổng phải gồm chặng cuối về kho |
| `haversineMatchesHandCalculatedDistances` | So với giá trị tính tay của hai tọa độ đã biết |
| `nearestNeighborNeverExceedsWeightCapacity` | Sinh dữ liệu nặng, mọi tuyến ≤ tải trọng |
| `nearestNeighborNeverExceedsVolumeCapacity` | Sinh dữ liệu cồng kềnh, mọi tuyến ≤ thể tích |
| `nearestNeighborAssignsEveryPointWhenCapacityAllows` | Không sót điểm nào |
| `nearestNeighborReportsUnassignedPointsWhenCapacityRunsOut` | Trả về danh sách chưa gán, không im lặng bỏ qua |
| `twoOptNeverIncreasesTotalDistance` | Sau 2-opt luôn ≤ trước 2-opt |
| `twoOptPreservesPointSet` | Số điểm và tổng kg/m³ không đổi |
| `etaOfFirstStopMatchesHandCalculation` | Khớp với kết quả tính tay |
| `cheapestInsertionPicksCorrectPosition` | Ví dụ nhỏ tính tay được |

Bốn test cho bộ giải chính:

| Test | Kiểm cái gì |
|---|---|
| `orToolsSolvesSmallCvrpToHandCalculatedOptimum` | Bài 4 điểm 2 xe, ra đúng 8000 m tính tay. **Đã viết, đã xanh** |
| `orToolsRespectsBothCapacityDimensions` | Dữ liệu vừa nặng vừa cồng kềnh, kiểm cả hai Dimension |
| `orToolsDropsPointsWhenCapacityIsInsufficient` | `addDisjunction` chạy đúng, trả `unassignedPoints` chứ không `null` |
| `bothSolversCoverTheSamePointSet` | Hai bộ giải trên cùng dữ liệu phải phủ đúng tập điểm như nhau |

Cách viết:

- **Dữ liệu cứng, tính tay được.** Đừng sinh ngẫu nhiên rồi so với chính code — như vậy không
  chứng minh được gì.
- Ghi ngay trong comment **kết quả mong đợi tính tay ra sao**. Hội đồng hỏi "sao em biết 13.3 km
  là đúng" thì có sẵn câu trả lời.
- Test thuật toán **không cần Spring context** — dùng JUnit thuần, chạy nhanh, không cần MySQL.
- Không mock lung tung. `HaversineDistanceCalculator` chạy thật được thì dùng thật.

Ngoài 10 test trên chỉ viết thêm khi vừa sửa một lỗi khó (viết test chặn lỗi đó tái phát), hoặc
khi một quy tắc nghiệp vụ dễ sai và hay dùng (ví dụ BR02-14 tính `amount_to_collect`). Không
viết test cho controller, không viết test cho CRUD, không dựng Testcontainers.

```
mvnw.cmd test                                    # chay toan bo
mvnw.cmd test -Dtest=NearestNeighborSolverTest   # chay mot lop
```

Tuần 6–7 phải có `AlgorithmRunner` với `main()` chạy được **không cần khởi động web, không cần
MySQL, không cần OSRM**. Đây vừa là cách gỡ lỗi nhanh, vừa là công cụ chạy benchmark Augerat
ở tuần 10.

---

## Bảo mật

Mức độ vừa đủ cho đồ án, nhưng những điểm dưới đây hội đồng hay hỏi và dễ mất điểm.

| Mã | Nội dung |
|---|---|
| CBR-15 | **Kiểm tra quyền ở phía server cho mọi endpoint.** Ẩn nút trên giao diện là chưa đủ |
| CBR-16 | Mật khẩu băm **BCrypt**. Tuyệt đối không lưu văn bản thuần, kể cả trong `DataSeeder` |
| CBR-17 | Bản ghi đã phát sinh dữ liệu nghiệp vụ **không xóa cứng**, chỉ chuyển `INACTIVE` |
| BR07-09 | `/track` giới hạn 20 lần/phút/IP |
| BR07-04 | Trang tra cứu công khai **không được lộ**: giá trị đơn hàng, biển số xe, khách khác cùng tuyến, các điểm giao khác |
| BR07-08 | Không tìm thấy đơn → MSG-11 chung chung, không nói rõ nguyên nhân |

Danh sách kiểm khi soát code:

- [ ] Không có mật khẩu, khóa API, chuỗi kết nối thật nào hard-code trong file `.java`
- [ ] Mọi truy vấn dùng JPA hoặc `@Query` có tham số — **không nối chuỗi SQL**
- [ ] Mọi controller có `@PreAuthorize` hoặc được chặn trong `SecurityConfig`
- [ ] Thymeleaf dùng `th:text` (tự escape), không dùng `th:utext` với dữ liệu người dùng nhập
- [ ] Form có CSRF token (Spring Security bật mặc định — đừng tắt đi cho tiện)
- [ ] Đường dẫn công khai đúng và chỉ đúng: `/track`, `/login`, `/css/**`, `/js/**`
- [ ] Tài xế đăng nhập chỉ thấy tuyến của chính mình (BR05-01) — kiểm ở tầng service
- [ ] Sai mật khẩu 5 lần → khóa 15 phút, thông báo MSG-12 không nói rõ sai tên hay sai mật khẩu

Kiểm nhanh: đăng nhập vai trò `DRIVER`, gõ `/admin/products` → phải ra **trang 403**, không phải
trang trắng hay lỗi 500 (CN-00-04).

---

## Quy ước Git

Lịch sử commit sạch giúp khôi phục được khi code hỏng gần ngày bảo vệ, và chứng minh với hội
đồng là đồ án được làm dần trong 12 tuần.

1. **Commit sau mỗi chức năng chạy được**, đừng dồn cả tuần vào một commit.
2. **Không commit khi code chưa biên dịch được.** Chạy `mvnw.cmd compile` trước.
3. Không commit: `target/`, `.idea/`, file `.osm.pbf` của OSRM (rất nặng), mật khẩu MySQL thật.

Định dạng — Conventional Commits:

```
<loai>(<pham vi>): <mo ta ngan> (<ma chuc nang>)
```

Mô tả viết **tiếng Anh, thể mệnh lệnh** (`add`, `fix`, `remove` — không phải `added`, `adds`),
chữ thường sau dấu hai chấm, **không chấm cuối câu**, gói trong 72 ký tự. Mã chức năng để trong
ngoặc ở cuối để `git log --oneline` đối chiếu thẳng được với `04-chuc-nang.md` — đây chính là
cách các đội thật gắn commit với mã ticket.

| Loại | Khi nào |
|---|---|
| `feat` | Thêm chức năng người dùng thấy được |
| `fix` | Sửa lỗi |
| `refactor` | Đổi cấu trúc code, **hành vi không đổi** |
| `test` | Thêm hoặc sửa test |
| `docs` | Chỉ đụng `docs/` hoặc comment |
| `perf` | Tối ưu tốc độ |
| `build` | `pom.xml`, `application.properties`, cấu hình chạy |
| `chore` | Việc lặt vặt: `.gitignore`, công cụ, dọn file |

Phạm vi — tên module bằng tiếng Anh chữ thường, trùng tên package hoặc tên màn hình:
`auth` · `product` · `vehicle` · `driver` · `customer` · `depot` · `order` · `routing` ·
`optimization` · `tracking` · `incident` · `report` · `ui` · `db`. Bỏ trống cũng được khi việc
trải rộng cả dự án.

Phần thân commit chỉ viết khi **lý do không nhìn ra được từ diff** — trả lời "vì sao", không kể
lại "làm gì". Chỗ này chính là câu trả lời sẵn khi hội đồng hỏi "vì sao em sửa dòng này".

Ví dụ:

```
feat(routing): add distance matrix cache backed by MySQL (CN-03-05)
fix(optimization): include final leg back to depot in total distance (CBR-01)
refactor(order): extract capacity check into OrderService
test(optimization): cover both capacity constraints in NearestNeighborSolver
perf(routing): preload distance matrix before running 2-opt
docs: update progress after finishing OSRM integration
```

Đánh tag ở các mốc sống còn: `t7-thuat-toan-chay-duoc`, `t8-phan-tuyen-tren-web`, `t11-ban-demo`.
**Trước ngày bảo vệ, tag một bản chắc chắn chạy được** và đừng động vào nữa.

Làm thẳng trên nhánh chính là đủ. Chỉ tạo nhánh khi thử một hướng có thể phải bỏ đi.

---

## Thứ tự làm việc

Bám theo `06-ke-hoach.md`. Tóm tắt:

1. Dựng project, cấu hình DB, tạo entity (tuần 2)
2. CRUD danh mục + đăng nhập phân quyền (tuần 3–4)
3. Đơn hàng (tuần 4–5)
4. Tích hợp OSRM + Leaflet + bảng ma trận khoảng cách (tuần 5)
5. **Thuật toán — chạy console trước, chưa gắn web** (tuần 6–7)
6. Gắn thuật toán vào màn hình phân tuyến (tuần 8)
7. Giao diện tài xế + tra cứu khách hàng (tuần 9)
8. Xử lý sự cố (tuần 9–10)
9. Báo cáo + thực nghiệm benchmark (tuần 10)
10. Seed dữ liệu, chạy thử, quay video (tuần 11)

**Tuần 6–7 là trái tim đồ án.** Nếu tuần 5 mà CRUD chưa xong thì cắt bớt CRUD, không được lùi
lịch thuật toán.

Làm từng bước nhỏ: xong một chức năng thì dừng, kiểm lại, rồi mới đi tiếp. Không tự ý thêm
chức năng ngoài `04-chuc-nang.md` — phạm vi đã được cân nhắc kỹ theo thời gian còn lại.

---

## Danh sách file tài liệu

| File | Nội dung |
|---|---|
| `00-tong-quan.md` | Bối cảnh, vấn đề, phạm vi, ngoài phạm vi |
| `01-nghiep-vu.md` | Tác nhân, quy tắc nghiệp vụ, trạng thái, phân quyền |
| `02-co-so-du-lieu.md` | Toàn bộ bảng, cột, khóa, script tạo bảng |
| `03-thuat-toan.md` | Đặc tả thuật toán chi tiết, interface, mã giả |
| `04-chuc-nang.md` | Danh sách chức năng kèm tiêu chí hoàn thành |
| `05-giao-dien.md` | Màn hình, bảng màu, quy ước hiển thị |
| `06-ke-hoach.md` | Kế hoạch 12 tuần chia thành công việc nhỏ |
| `07-thuc-nghiem.md` | Cách đo và chứng minh hiệu quả thuật toán |
| `08-du-lieu-mau.md` | Dữ liệu seed để demo |
| `09-thay-doi-so-voi-ban-dau.md` | Đã sửa gì so với đặc tả gốc và vì sao — dùng khi vấn đáp |
| `10-quy-uoc-ky-thuat.md` | File này: công nghệ, cấu trúc thư mục, quy ước đặt tên, kiểm thử, bảo mật, Git |
| `TIEN-DO.md` | Trạng thái hiện tại, việc tiếp theo, nợ kỹ thuật. **Nạp vào mọi phiên nên phải giữ ngắn** |
| `NHAT-KY.md` | Nhật ký từng tuần: đã làm gì, vì sao làm thế, số liệu chạy thật. Chỉ mở khi cần tra |
