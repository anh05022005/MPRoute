# 03 — Đặc tả thuật toán

**Đây là phần lõi của đồ án.** Toàn bộ điểm số phụ thuộc vào phần này. Mọi thứ khác chỉ là hỗ trợ.

## Một bộ giải: OR-Tools

Hệ thống giải bài toán phân tuyến bằng **OR-Tools** (Google), nằm ở `optimization/ortools/`.

Ban đầu đồ án cài thêm hai bộ giải tự viết là **Nearest Neighbor** và **Nearest Neighbor + 2-opt**
để làm mốc so sánh. Cả hai **đã bỏ** (`09` mục A7): OR-Tools cho tuyến ngắn hơn rõ rệt, còn giữ
bản tự viết chỉ để so sánh thì phải bảo trì gấp đôi mà sản phẩm không dùng tới. Chất lượng của
OR-Tools giờ được đo bằng hai mốc A, B (mục 11) và bằng benchmark Augerat có lời giải tối ưu
đã công bố (`07-thuc-nghiem.md`).

`optimization/heuristic/` chỉ còn `CheapestInsertion` cho màn xử lý sự cố (mục 10).

Nguyên tắc bắt buộc khi viết code phần này:

- **Không gọi OSRM hay truy vấn DB bên trong vòng lặp thuật toán.** Ma trận nạp sẵn vào bộ nhớ trước. Hàm chi phí của OR-Tools bị gọi hàng chục nghìn lần mỗi lần giải.
- Cách viết code thì bình thường — Stream API, lambda, Lombok đều dùng được.
- **Phải giảng lại được từng bước bằng tiếng Việt.** Đây là chỗ hội đồng hỏi sâu nhất: họ chỉ vào một dòng và hỏi "dòng này làm gì". Với OR-Tools, câu hỏi sẽ là "em cấu hình cái gì và vì sao" — xem mục 5.

---

## 1. Mô hình bài toán

Bài toán: **CVRP — Capacitated Vehicle Routing Problem**, với **hai loại ràng buộc sức chứa**.

| Ký hiệu | Ý nghĩa |
|---|---|
| `c0` | Kho (depot) |
| `C = {c1, ..., cn}` | Tập điểm giao |
| `V = {v1, ..., vm}` | Tập xe khả dụng |
| `D[i][j]` | Khoảng cách từ điểm i tới điểm j, đơn vị mét |
| `w(ci)` | Khối lượng đơn hàng tại điểm ci, kg |
| `u(ci)` | Thể tích đơn hàng tại điểm ci, m³ |
| `W(vk)` | Tải trọng tối đa của xe vk, kg |
| `U(vk)` | Thể tích thùng xe vk, m³ |

**Hàm mục tiêu:** cực tiểu hóa tổng quãng đường của tất cả tuyến.

**Ràng buộc:**
1. Mỗi điểm giao thuộc đúng một tuyến
2. Với mọi tuyến r gán cho xe v: `tổng w của các điểm trong r ≤ W(v)`
3. Với mọi tuyến r gán cho xe v: `tổng u của các điểm trong r ≤ U(v)`
4. Mỗi tuyến bắt đầu và kết thúc tại `c0`

### Vì sao cần hai ràng buộc?

Đây là câu hỏi hội đồng chắc chắn sẽ hỏi. Câu trả lời nằm ở đặc thù hàng hóa Minh Phú:

| Sản phẩm | kg/thùng | m³/thùng | Nhận xét |
|---|---|---|---|
| Bông tẩy trang | 6.0 | 0.085 | Nhẹ nhưng cồng kềnh |
| Khăn rửa mặt | 5.5 | 0.072 | Nhẹ, cồng kềnh |
| Nước giặt 3.5L | 21.5 | 0.028 | Nặng, gọn |
| Nước lau sàn 2L | 24.8 | 0.031 | Nặng, gọn |

Xe chở toàn bông tẩy trang sẽ **đầy chỗ trước khi đầy tải**. Xe chở toàn nước giặt thì ngược lại. Nếu chỉ ràng buộc một trong hai, kết quả sẽ sai trong thực tế.

---

## 2. Kiến trúc code

Dùng **Strategy pattern** cho tính khoảng cách (OSRM hoặc Haversine) và cho bộ giải
(`RoutingAlgorithm`). Đổi nguồn khoảng cách không phải sửa một dòng nào của bộ giải.

```
service/optimization/
├── OptimizationPoint.java              Lop du lieu mot diem trong bai toan
├── OptimizationVehicle.java            Lop du lieu mot xe
├── OptimizationRoute.java              Lop du lieu ket qua mot tuyen
├── RoutingResult.java                  Toan bo ket qua mot lan chay
│
├── DistanceCalculator.java             INTERFACE
├── OsrmDistanceCalculator.java         Cai dat goi OSRM
├── HaversineDistanceCalculator.java    Cai dat cong thuc Haversine
├── DistanceMatrixService.java          Nap ma tran, co cache DB
│
├── RouteDistanceCalculator.java        Tong quang duong, CO chang ve kho (muc 8)
├── EtaCalculator.java                  Gio den du kien (muc 9)
├── MetricsService.java                 Moc A, moc B, gap (muc 11)
│
├── RoutingAlgorithm.java               INTERFACE cua bo giai
├── ortools/
│   └── OrToolsSolver.java              Bo giai duy nhat, dich ket qua ve RoutingResult
├── heuristic/
│   └── CheapestInsertion.java          Dung cho xu ly su co
│
├── AlgorithmRunner.java                Chay console, khong can web/MySQL/OSRM
├── BenchmarkRunner.java                Chay bo Augerat
├── VrpFileReader.java / VrpInstance    Doc file .vrp cua CVRPLIB
```

**Cheapest Insertion vẫn là code tự viết** dù OR-Tools là bộ giải chính. Lý do: xử lý sự cố
(UC06) cần chèn một điểm vào tuyến **đang chạy dở**, biết xe đang ở đâu và không được chèn
vào quá khứ. Dựng lại ràng buộc đó trong OR-Tools tốn công hơn là viết thẳng vòng lặp 15 dòng.

---

## 3. Các lớp dữ liệu

### `OptimizationPoint`

```java
public class OptimizationPoint {
    private Long orderId;      // null neu la kho
    private String name;      // "KHO" hoac ten khach hang
    private double latitude;
    private double longitude;
    private double weight;    // kg, = 0 neu la kho
    private double volume;      // m3, = 0 neu la kho
    private boolean isDepot;
    // Dung @Getter @Setter cua Lombok
}
```

### `OptimizationVehicle`

```java
public class OptimizationVehicle {
    private Long vehicleId;
    private String licensePlate;
    private double maxWeight;
    private double maxVolume;
}
```

### `OptimizationRoute`

```java
public class OptimizationRoute {
    private OptimizationVehicle vehicle;
    private List<OptimizationPoint> points;  // KHONG chua kho, kho la ngam dinh
    private double totalDistance;         // met, DA gom chang ve kho
    private double totalWeight;
    private double totalVolume;
}
```

**Chú ý:** `points` chỉ chứa các điểm giao, không chứa kho. Kho là ngầm định ở đầu và cuối. Cách này đơn giản hơn khi kiểm tra ràng buộc, nhưng **hàm tính tổng quãng đường phải tự cộng chặng kho→điểm đầu và điểm cuối→kho**.

### `RoutingResult`

```java
public class RoutingResult {
    private List<OptimizationRoute> routes;
    private List<OptimizationPoint> unassignedPoints; // khi khong du suc chua
    private double optimizedDistance;
    private double baselineADistance;
    private double baselineBDistance;
    private long executionTimeMs;
    private String algorithm;
}
```

---

## 4. Tính khoảng cách

### Interface

```java
public interface DistanceCalculator {
    // Tra ve khoang cach giua hai diem, don vi met
    double khoangCach(OptimizationPoint diemA, OptimizationPoint diemB);

    // Ten nguon du lieu, de ghi vao bang ket qua
    String sourceName();
}
```

### `HaversineDistanceCalculator`

Công thức đường chim bay trên mặt cầu, nhân hệ số quy đổi để xấp xỉ đường đi thật.

```
R = 6371000 met (ban kinh Trai Dat)

phi1 = radian(latitude1)
phi2 = radian(latitude2)
deltaPhi    = radian(latitude2 - latitude1)
deltaLambda = radian(longitude2 - longitude1)

a = sin(deltaPhi/2)^2 + cos(phi1) * cos(phi2) * sin(deltaLambda/2)^2
c = 2 * atan2(can(a), can(1-a))

khoangCachChimBay = R * c
khoangCachThuc    = khoangCachChimBay * conversionFactor   // mac dinh 1.3
```

Cài đặt này **luôn phải có** kể cả khi OSRM chạy tốt — nó là phương án dự phòng bắt buộc theo CBR-11, và cũng là ví dụ Strategy pattern để nói khi vấn đáp.

### `OsrmDistanceCalculator`

Gọi OSRM Table API lấy cả ma trận một lần thay vì từng cặp:

```
GET http://localhost:5000/table/v1/driving/
    {lon1},{lat1};{lon2},{lat2};...?annotations=distance,duration
```

Trả về JSON có mảng `distances[i][j]` đơn vị mét.

**Giới hạn:** OSRM Table API mặc định giới hạn khoảng 100 điểm mỗi lần gọi. Với quy mô đồ án là đủ. Nếu vượt, chia thành nhiều lần gọi.

### `DistanceMatrixService` — lớp quan trọng nhất về hiệu năng

Trách nhiệm: nạp toàn bộ ma trận vào bộ nhớ **một lần** trước khi thuật toán chạy.

```
HAM loadMatrix(points):
    n = so luong diem (gom ca kho)
    matrix = mang 2 chieu n x n

    B1. Voi moi cap (i, j):
            Tra bang distance_matrix trong DB theo toa do
            Neu co  -> dien vao matrix[i][j]
            Neu chua-> danh dau la can lay moi

    B2. Neu con cap chua co:
            Goi OSRM Table API mot lan cho toan bo diem
            Neu OSRM loi -> dung HaversineDistanceCalculator, dat co dungXapXi = true
            Luu ket qua moi vao bang distance_matrix

    B3. DOI XUNG HOA ma tran:
            VOI moi cap (i, j) voi i < j:
                trungBinh   = (matrix[i][j] + matrix[j][i]) / 2
                matrix[i][j] = trungBinh
                matrix[j][i] = trungBinh

    B4. TRA VE matrix
```

### Bước B3 — vì sao đối xứng hóa

OSRM trả về ma trận **bất đối xứng**: `D[i][j] ≠ D[j][i]` vì đường một chiều, cấm rẽ trái, dải phân cách.

Bước này sinh ra để phục vụ 2-opt: công thức độ lệch bốn cạnh của 2-opt chỉ đúng khi ma trận
đối xứng. Ví dụ kiểm chứng bằng số (ma trận bất đối xứng, đảo đoạn hai điểm đầu):

```
Cong thuc delta = -9  ->  thuat toan ket luan "cai thien, nhan dao doan"
Tuyen truoc: 30  ->  tuyen sau: 49   =>  thuc te DAI THEM 19
```

2-opt đã bỏ và OR-Tools nhận được ma trận bất đối xứng, nhưng **vẫn giữ bước đối xứng hóa**
để các số km đã đo ở `07-thuc-nghiem.md` và các tuyến đã lưu so sánh được với lần chạy mới.
Bỏ bước này là đổi thước đo giữa chừng, phải đo lại toàn bộ chương 4.

Ghi vào báo cáo: *hệ thống lấy trung bình cộng hai chiều của ma trận OSRM. Hạn chế: cách này
làm mất một phần thông tin về đường một chiều.*

**Quy tắc tuyệt đối: không được gọi OSRM hay truy vấn DB bên trong vòng lặp của thuật toán.** Hàm chi phí của OR-Tools bị gọi hàng chục nghìn lần — mỗi lần một HTTP request thì chương trình sẽ treo. Bộ giải chỉ được tra mảng `matrix[i][j]` trong bộ nhớ.

---

## 5. Bộ giải chính — OR-Tools

Thư viện: `com.google.ortools:ortools-java:9.15.6755`. Phần dùng đến là **Routing Library** —
bộ giải chuyên cho họ bài toán VRP, xây trên nền lập trình ràng buộc.

Đã chạy thử trên chính máy phát triển (Windows 11, JDK 23): nạp được thư viện native, giải
bài CVRP 4 điểm 2 xe ra **đúng đáp số tính tay 8000 m** trong 1,5 giây. Xem
`src/test/java/.../optimization/OrToolsSmokeTest.java`.

### 5.1 Ánh xạ mô hình

| Trong bài toán của ta | Trong OR-Tools |
|---|---|
| Kho `c0` | `depot = 0` — chỉ số điểm xuất phát và kết thúc |
| Điểm giao `c1..cn` | Các node 1..n của `RoutingIndexManager` |
| Ma trận `D[i][j]` | Hàm chi phí đăng ký bằng `registerTransitCallback` |
| Ràng buộc khối lượng `W(v)` | Một `Dimension` tên `"KhoiLuong"` |
| Ràng buộc thể tích `U(v)` | Một `Dimension` tên `"TheTich"` |
| Điểm không gán được | `addDisjunction` với tiền phạt |

**Hai ràng buộc sức chứa = hai Dimension riêng** (CBR-03, CBR-04). Đây là chỗ dễ sai nhất:
gần như mọi ví dụ OR-Tools trên mạng chỉ có một dimension `"Capacity"`. Khai một cái là
xe chở toàn bông tẩy trang sẽ đầy thùng mà hệ thống vẫn tưởng còn chỗ.

### 5.2 Khung code

```java
Loader.loadNativeLibraries();

RoutingIndexManager quanLyChiSo = new RoutingIndexManager(soDiem, soXe, CHI_SO_KHO);
RoutingModel moHinh = new RoutingModel(quanLyChiSo);

// --- Chi phi: tra thang vao ma tran da nap san trong bo nho ---
int hamKhoangCach = moHinh.registerTransitCallback((tu, den) ->
        (long) Math.round(matrix[quanLyChiSo.indexToNode(tu)][quanLyChiSo.indexToNode(den)]));
moHinh.setArcCostEvaluatorOfAllVehicles(hamKhoangCach);

// --- Rang buoc 1: khoi luong ---
int hamKhoiLuong = moHinh.registerUnaryTransitCallback(
        tu -> khoiLuongGam[quanLyChiSo.indexToNode(tu)]);
moHinh.addDimensionWithVehicleCapacity(hamKhoiLuong, 0, sucChuaGam, true, "KhoiLuong");

// --- Rang buoc 2: the tich ---
int hamTheTich = moHinh.registerUnaryTransitCallback(
        tu -> theTichCm3[quanLyChiSo.indexToNode(tu)]);
moHinh.addDimensionWithVehicleCapacity(hamTheTich, 0, sucChuaCm3, true, "TheTich");

// --- Cho phep bo diem khi khong du suc chua, thay vi tra ve null ---
for (int diem = 1; diem < soDiem; diem++) {
    moHinh.addDisjunction(new long[]{quanLyChiSo.nodeToIndex(diem)}, TIEN_PHAT_BO_DIEM);
}

RoutingSearchParameters thamSo = main.defaultRoutingSearchParameters().toBuilder()
        .setFirstSolutionStrategy(FirstSolutionStrategy.Value.PATH_CHEAPEST_ARC)
        .setLocalSearchMetaheuristic(LocalSearchMetaheuristic.Value.GUIDED_LOCAL_SEARCH)
        .setTimeLimit(Duration.newBuilder().setSeconds(5).build())
        .build();

Assignment ketQua = moHinh.solveWithParameters(thamSo);
```

### 5.3 Bốn quyết định phải giải thích được khi vấn đáp

**a) Vì sao phải nhân lên số nguyên.** OR-Tools làm việc với `long`, không nhận `double`.
Nên phải quy đổi:

```
khoi luong: kg  x 1 000       -> gam
the tich:   m3  x 1 000 000   -> cm3
khoang cach: da la met, chi can lam tron
```

Quan trọng: **làm tròn nhu cầu LÊN, làm tròn sức chứa XUỐNG**. Làm ngược lại thì sai số
làm tròn có thể tạo ra tuyến trông hợp lệ nhưng thực tế quá tải vài trăm gam.

**b) Vì sao chặng về kho tự có (CBR-01).** `RoutingModel` định nghĩa mỗi xe bằng một cặp
`start(v)` / `end(v)`. Khi khởi tạo với `depot = 0` thì cả hai đầu đều trỏ về kho, nên cung
cuối cùng `điểm cuối → kho` là một phần của mô hình chứ không phải thứ ta cộng thêm.
`objectiveValue()` đã bao gồm nó. `OrToolsSmokeTest` kiểm điều này bằng cách cộng tay từng
chặng rồi đối chiếu.

**c) Vì sao cần `addDisjunction`.** Mặc định OR-Tools bắt **mọi** điểm phải được thăm. Nếu
tổng sức chứa không đủ, `solveWithParameters` trả về `null` — không phân biệt được "thiếu
sức chứa" với "thư viện lỗi". Khai `addDisjunction` với tiền phạt biến việc bỏ điểm thành
một lựa chọn đắt: bộ giải chỉ bỏ điểm khi thật sự không còn cách nào. Các điểm bị bỏ đọc
ra bằng cách kiểm `ketQua.value(moHinh.nextVar(i)) == i`, rồi đổ vào `unassignedPoints` để
hiển thị MSG-08.

Tiền phạt phải **lớn hơn quãng đường xa nhất có thể** của một điểm, nếu không bộ giải sẽ
thấy bỏ điểm rẻ hơn là đi giao. Đặt `TIEN_PHAT_BO_DIEM = 10 × (giá trị lớn nhất trong ma trận)`.

**Tiền phạt riêng từng đơn (BR03-14, `09` mục A10).** Tiền phạt bằng nhau thì khi hết chỗ,
bộ giải bỏ đơn **tốn đường nhất**, thường là khách ở xa, kể cả khách đặt từ sáng sớm hoặc đã
bị hoãn hôm qua. Vì vậy mỗi đơn có một `dropPriority` và tiền phạt riêng:

```
tien phat = phan co so + bac uu tien x dropPriority
phan co so = 10 x o lon nhat                       (nhu cu, dropPriority = 0 la y het)
bac uu tien > moi chenh lech so km giua hai phuong an bat ky
           = (so diem + so xe + 1) x (o lon nhat + 1) + so diem x phan co so
```

Bậc ưu tiên lớn hơn mọi chênh lệch số km, nên giữ đơn ưu tiên cao **luôn** thắng, dù phải
chạy vòng xa bao nhiêu. `RoutingService.dropPriorities` gán `dropPriority`:

| Bước | Cách tính |
|---|---|
| Xếp hạng | Hoãn nhiều lần hơn đứng trước, cùng số lần hoãn thì đặt sớm hơn đứng trước. Đơn đầu nhận hạng n, đơn cuối nhận hạng 1 |
| Nhóm đã hoãn | Cộng thêm `postponeCount × (n(n+1)/2 + 1)`, lớn hơn tổng hạng của mọi đơn |

Bảo đảm thu được, phải nói đúng khi vấn đáp:

- **Đơn đã hoãn không bao giờ bị bỏ để nhường chỗ cho đơn chưa hoãn**, dù bỏ nó thì xếp vừa
  được bao nhiêu đơn chưa hoãn đi nữa.
- **Trong cùng nhóm, đơn đặt sớm không bao giờ bị bỏ để nhường chỗ cho *một* đơn đặt muộn.**
  Nhưng nếu bỏ một đơn đặt sớm mà xếp vừa *hai* đơn đặt muộn có tổng hạng lớn hơn thì bộ giải
  chọn cách đó. Muốn thứ tự tuyệt đối với mọi tập đơn thì tiền phạt phải tăng theo lũy thừa 2
  của hạng, tràn `long` khi một ngày có quá khoảng 40 đơn.

Test: `OrToolsSolverTest.orToolsDropsLowerPriorityPointsFirstWhenCapacityRunsOut`, ba lần
chạy trên cùng một hình, tính tay được.

**d) Vì sao cần đặt giới hạn thời gian.** `GUIDED_LOCAL_SEARCH` là siêu tìm kiếm — nó
**không tự dừng**, cứ cải thiện mãi cho tới khi hết thời gian cho phép. Không đặt
`setTimeLimit` là màn hình phân tuyến treo vĩnh viễn. 5 giây là dư cho quy mô Minh Phú.

### 5.4 Điều vẫn phải tự làm

OR-Tools chỉ giải bài toán thuần. Những thứ sau nằm ngoài nó và vẫn là code của đồ án:

- Nạp và **đối xứng hóa** ma trận khoảng cách (mục 4)
- Tính **ETA** cho từng điểm (mục 9) — OR-Tools trả thứ tự, không trả giờ đến
- **Cheapest Insertion** cho xử lý sự cố (mục 10)
- Ghi `status_history`, sinh mã tuyến, vẽ lên bản đồ, toàn bộ nghiệp vụ

---

## 6. ~~Nearest Neighbor tự cài đặt~~ — đã bỏ

## 7. ~~2-opt tự cài đặt~~ — đã bỏ

Hai bộ giải tự viết đã bỏ cùng các test của chúng (`09` mục A7). Giữ lại số mục 6, 7 để các
chỗ khác trỏ tới mục 8–13 không bị lệch. Mã giả và phân tích cũ còn trong lịch sử Git
(trước commit `bcd645a`) nếu cần tra.

Khi vấn đáp vẫn nên nói được ý tưởng, vì OR-Tools dùng đúng hai ý đó ở hai giai đoạn:

| Giai đoạn trong OR-Tools | Ý tưởng |
|---|---|
| `PATH_CHEAPEST_ARC` — lời giải ban đầu | Giống Nearest Neighbor: từ điểm đang đứng nối tiếp cung rẻ nhất |
| `GUIDED_LOCAL_SEARCH` — cải thiện | Gồm cả phép đảo đoạn kiểu 2-opt **và** chuyển điểm giữa các xe. Phép thứ hai là thứ 2-opt không làm được, và là lý do chính OR-Tools cho tuyến ngắn hơn |

---

## 8. Tính tổng quãng đường

```
HAM calculateTotalDistance(tuyen, matrix):

B1.  NEU tuyen khong co diem nao THI TRA VE 0

B2.  tong = matrix[KHO][tuyen.diem[0]]          // chang dau: kho -> diem 1

B3.  VOI i tu 0 den soDiem - 2:
B4.      tong = tong + matrix[tuyen.diem[i]][tuyen.diem[i+1]]

B5.  tong = tong + matrix[tuyen.diem[cuoi]][KHO]  // CHANG CUOI VE KHO

B6.  TRA VE tong
```

**Bước B5 là lỗi kinh điển của các đồ án VRP.** Quên chặng về kho khiến kết quả sai và mọi chỉ số so sánh sai theo. Viết unit test riêng cho hàm này.

---

## 9. Tính ETA

```
HAM calculateEta(route, matrix, departureTime, speedKmh, loadingMinutesPerStop):

B1.  cumulativeDistance = matrix[DEPOT][route.points[0]]
     completedLocations = 0

B2.  VOI i tu 0 den pointCount - 1:
B3.      NEU i > 0 THI
B4.          leg = matrix[route.points[i-1]][route.points[i]]
B5.          cumulativeDistance = cumulativeDistance + leg
B6.          NEU leg > 0 THI completedLocations = completedLocations + 1
B7.      travelMinutes       = (cumulativeDistance / 1000.0) / speedKmh * 60
B8.      totalLoadingMinutes = completedLocations * loadingMinutesPerStop   // so CHO DUNG da do xong
B9.
B10.     eta = departureTime + travelMinutes + totalLoadingMinutes
B11.     route.points[i].estimatedArrival = eta
```

Bốc dỡ tính theo **chỗ dừng**, không theo số đơn. Hai đơn của cùng một khách đứng cạnh nhau
trong tuyến thì chặng giữa chúng bằng 0 m: xe không di chuyển, dỡ cả hai đơn trong một lần
dừng, nên hai đơn **cùng ETA** và chỉ tính một lần bốc dỡ. Khi mỗi chỗ chỉ có một đơn thì
`completedLocations` đúng bằng `i`, ra lại công thức gốc. Xem `docs/09` mục B5.

Ví dụ kiểm chứng bằng tay:
- Điểm 2, quãng đường tích lũy 13300 m, tốc độ 30 km/h, bốc dỡ 15 phút
- Di chuyển: `13.3 / 30 × 60 = 26.6` phút
- Bốc dỡ: `1 × 15 = 15` phút
- ETA `= 08:00 + 26.6 + 15 ≈ 08:42`

---

## 10. Cheapest Insertion — dùng cho xử lý sự cố

Bài toán: chèn một điểm giao vào tuyến đang chạy sao cho quãng đường tăng thêm là ít nhất.

```
HAM insertionCost(tuyen, diemMoi, viTriHienTaiCuaXe, matrix):

B1.  bestCost = VO_CUC
B2.  bestPosition  = -1

B3.  // Chi xet tu vi tri hien tai tro di, khong the chen vao qua khu
B4.  VOI i tu viTriHienTaiCuaXe den soDiem:
B5.      a = diem tai vi tri i       (hoac vi tri hien tai cua xe neu i la dau)
B6.      b = diem tai vi tri i + 1   (hoac KHO neu i la cuoi)
B7.
B8.      chiPhi = matrix[a][diemMoi] + matrix[diemMoi][b] - matrix[a][b]
B9.
B10.     NEU chiPhi < bestCost THI
B11.         bestCost = chiPhi
B12.         bestPosition  = i + 1

B13. TRA VE (bestCost, bestPosition)
```

**Bước B4 rất quan trọng:** xe đã đi qua các điểm trước rồi, không thể chèn vào quá khứ.

**Sau mỗi lần chèn phải kiểm tra lại sức chứa.** Nếu tuyến ứng viên không đủ chỗ cho toàn bộ điểm còn lại thì loại tuyến đó khỏi danh sách phương án.

**Chi tiết thực tế cần xử lý:** hàng của tuyến gặp sự cố đang nằm trên xe hỏng. Xe cứu phải ghé qua điểm sự cố để sang hàng. Vậy tổng km phát sinh của Phương án B gồm:

```
km phat sinh = (vi tri hien tai xe cuu -> diem su co)
             + (diem su co -> quay lai lo trinh)
             + tong chi phi chen cac diem con lai
```

Ghi rõ điều này trong báo cáo, đừng bỏ qua — nó cho thấy bạn hiểu bài toán thực tế chứ không chỉ chép công thức.

---

## 11. Các mốc so sánh

Chi tiết ở `07-thuc-nghiem.md`, tóm tắt ở đây:

### Mốc A — mô phỏng quy trình hiện tại

Mỗi đơn đi một chuyến khứ hồi riêng từ kho:

```
baselineADistance = TONG (2 × matrix[KHO][diem]) voi moi diem giao
```

Cho con số giảm đẹp (40–50%) nhưng dễ bị hội đồng bẻ là "tự dựng mốc kém rồi so".

### Mốc B — cô lập đóng góp của việc sắp thứ tự

Dùng **đúng cách chia xe mà thuật toán đã tạo ra**, nhưng thứ tự giao theo thứ tự nhận đơn (tức là theo `id` đơn hàng tăng dần):

```
HAM tinhMocB(routes, matrix):
    tong = 0
    VOI moi tuyen:
        tuyenTheoThuTuNhanDon = sap xep diem cua tuyen theo orderId tang dan
        tong = tong + calculateTotalDistance(tuyenTheoThuTuNhanDon, matrix)
    TRA VE tong
```

Cho con số khiêm tốn hơn nhiều (8–15%) **nhưng đáng tin hơn hẳn**.

**Báo cáo cả hai mốc.** Trình bày hai con số kèm giải thích ý nghĩa từng cái sẽ được đánh giá cao hơn nhiều so với một con số 45% không có bối cảnh.

### Đặt cạnh kết quả OR-Tools

Ba con số đặt cạnh nhau thành bảng của chương 4:

| Cách phân tuyến | Ý nghĩa |
|---|---|
| Mốc A — mỗi đơn một chuyến | Cách Minh Phú đang làm |
| Mốc B — đúng cách chia xe, sai thứ tự | Cô lập đóng góp của việc sắp thứ tự |
| **OR-Tools** | Tuyến hệ thống thực sự chạy |

Còn câu hỏi "OR-Tools cách lời giải tốt nhất bao nhiêu" thì trả lời bằng benchmark Augerat:
các bộ dữ liệu đó có lời giải tối ưu đã công bố, nên đo được `gap` (`07-thuc-nghiem.md`).

---

## 12. Chương trình chạy thử độc lập

`AlgorithmRunner` chạy bằng `main()`, **không cần web, không cần MySQL, không cần OSRM**:

- Tách bạch lỗi thuật toán với lỗi Spring/JPA/giao diện
- Chạy thử nhanh, không cần khởi động server
- `BenchmarkRunner` dùng cùng cách đó để chạy bộ Augerat

```java
// File: src/main/java/com/minhphu/mproute/service/optimization/AlgorithmRunner.java
public class AlgorithmRunner {
    public static void main(String[] args) {
        // 1. Tao du lieu cung: 1 kho, cac diem giao, doi xe du rong
        // 2. Tinh ma tran bang Haversine (khong can OSRM)
        // 3. Chay OrToolsSolver, in tung tuyen
        // 4. In bang: moc A, moc B, OR-Tools
    }
}
```

---

## 13. Danh sách unit test bắt buộc

Bốn test cho phần tính toán dùng chung:

| Test | Kiểm tra gì |
|---|---|
| `totalDistanceIncludesReturnLegToDepot` | Tuyến 3 điểm, kiểm tổng có gồm chặng cuối |
| `haversineMatchesHandCalculatedDistances` | So với giá trị tính tay của hai tọa độ đã biết |
| `etaOfFirstStopMatchesHandCalculation` | Khớp với tính tay |
| `cheapestInsertionPicksCorrectPosition` | Ví dụ nhỏ tính tay được |

Ba test cho bộ giải:

| Test | Kiểm tra gì |
|---|---|
| `orToolsSolvesSmallCvrpToHandCalculatedOptimum` | Bài 4 điểm 2 xe, ra đúng 8000 m tính tay |
| `orToolsRespectsBothCapacityDimensions` | Dữ liệu vừa nặng vừa cồng kềnh, kiểm cả hai Dimension |
| `orToolsDropsPointsWhenCapacityIsInsufficient` | `addDisjunction` hoạt động, trả `unassignedPoints` chứ không `null` |

Các test của Nearest Neighbor và 2-opt đã bỏ cùng hai bộ giải đó (`09` mục A7).

Các test này vừa đảm bảo đúng, vừa là thứ để trưng ra khi hội đồng hỏi "em kiểm thử thế nào".
