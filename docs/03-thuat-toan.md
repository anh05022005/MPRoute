# 03 — Đặc tả thuật toán

**Đây là phần lõi của đồ án.** Toàn bộ điểm số phụ thuộc vào phần này. Mọi thứ khác chỉ là hỗ trợ.

Nguyên tắc bắt buộc khi viết code phần này:

- **Không dùng thư viện VRP nào.** Không OR-Tools, không jsprit, không OptaPlanner. Tự cài đặt là phần đóng góp học thuật của đồ án.
- **Không gọi OSRM hay truy vấn DB bên trong vòng lặp thuật toán.** Ma trận nạp sẵn vào bộ nhớ trước.
- Cách viết code thì bình thường — Stream API, lambda, Lombok đều dùng được.
- **Nhưng phải giảng lại được từng bước bằng tiếng Việt.** Đây là chỗ hội đồng hỏi sâu nhất: họ chỉ vào một dòng và hỏi "dòng này làm gì". Viết một chuỗi Stream lồng nhau mà lúc bị hỏi không tách ra giải thích được thì nên viết lại đoạn đó rõ hơn.

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

Dùng **Strategy pattern** cho cả tính khoảng cách lẫn thuật toán phân tuyến. Đây là chi tiết ghi điểm khi vấn đáp, và cũng là điều kiện để chạy so sánh thực nghiệm.

```
service/optimization/
├── OptimizationPoint.java              Lop du lieu mot diem trong bai toan
├── OptimizationVehicle.java                Lop du lieu mot xe
├── OptimizationRoute.java             Lop du lieu ket qua mot tuyen
├── RoutingResult.java        Toan bo ket qua mot lan chay
│
├── DistanceCalculator.java         INTERFACE
├── OsrmDistanceCalculator.java         Cai dat goi OSRM
├── HaversineDistanceCalculator.java    Cai dat cong thuc Haversine
├── DistanceMatrixService.java Nap ma tran, co cache DB
│
├── RoutingAlgorithm.java     INTERFACE
├── NearestNeighborSolver.java  Giai doan 1
├── TwoOptImprover.java         Giai doan 2
├── NN2OptSolver.java           Ket hop ca hai
├── CheapestInsertion.java      Dung cho xu ly su co
│
└── MetricsService.java       Tinh moc so sanh va cac chi so
```

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

### Bước B3 — vì sao bắt buộc phải đối xứng hóa

OSRM trả về ma trận **bất đối xứng**: `D[i][j] ≠ D[j][i]` vì đường một chiều, cấm rẽ trái, dải phân cách.

Nhưng công thức độ lệch của 2-opt ở mục 6 **chỉ đúng khi ma trận đối xứng**. Nếu để nguyên ma trận OSRM thô, 2-opt sẽ tính sai — và sai theo kiểu tệ nhất: nó *tưởng* là cải thiện rồi nhận phép đảo, trong khi tuyến thật dài ra.

Ví dụ kiểm chứng bằng số (ma trận bất đối xứng, đảo đoạn hai điểm đầu):

```
Cong thuc delta = -9  ->  thuat toan ket luan "cai thien, nhan dao doan"
Tuyen truoc: 30  ->  tuyen sau: 49   =>  thuc te DAI THEM 19
```

Sai số đúng bằng tổng độ bất đối xứng của các cạnh bên trong đoạn bị đảo.

Hậu quả nếu bỏ qua bước B3: test `test2OptKhongLamTangQuangDuong` **xanh khi chạy với Haversine** (vốn đối xứng sẵn) nhưng **đỏ khi chạy với OSRM thật**. Đây là loại lỗi lúc được lúc không, rất khó truy.

Đối xứng hóa bằng trung bình cộng biến giả định "khoảng cách đối xứng" thành **đúng theo cấu tạo**, và 2-opt chứng minh được là không bao giờ làm tăng quãng đường. **Ghi rõ một câu về bước này trong báo cáo** — nó cho thấy bạn hiểu điều kiện áp dụng của công thức chứ không chép công thức về dùng.

**Quy tắc tuyệt đối: không được gọi OSRM hay truy vấn DB bên trong vòng lặp của thuật toán.** 2-opt gọi hàm khoảng cách hàng nghìn lần — mỗi lần một HTTP request thì chương trình sẽ treo. Thuật toán chỉ được tra mảng `matrix[i][j]` trong bộ nhớ.

---

## 5. Giai đoạn 1 — Nearest Neighbor

### Ý tưởng

Xe đứng ở kho, chọn điểm giao gần nhất mà mình còn đủ sức chứa. Đến nơi lại chọn điểm gần nhất tiếp theo. Khi không thêm được điểm nào nữa thì đóng tuyến, quay về kho, chuyển sang xe kế tiếp.

### Mã giả

```
HAM solveNearestNeighbor(points, vehicles, matrix):

B1.  unassignedPoints = ban sao cua points
B2.  routes   = danh sach rong

B3.  VOI moi xe trong vehicles:
B4.      NEU unassignedPoints rong THI thoat vong lap

B5.      tuyen        = tuyen moi voi xe nay
B6.      klHienTai    = 0
B7.      ttHienTai    = 0
B8.      currentPoint  = kho

B9.      LAP:
B10.         nearestPoint     = null
B11.         minDistance = VO_CUC

B12.         VOI moi diem trong unassignedPoints:
B13.             // Kiem tra DONG THOI ca hai rang buoc
B14.             NEU klHienTai + diem.weight > xe.maxWeight THI bo qua diem nay
B15.             NEU ttHienTai + diem.volume   > xe.maxVolume  THI bo qua diem nay
B16.
B17.             kc = matrix[currentPoint][diem]
B18.             NEU kc < minDistance THI
B19.                 minDistance = kc
B20.                 nearestPoint = diem

B21.         NEU nearestPoint = null THI thoat LAP   // xe day roi

B22.         them nearestPoint vao cuoi tuyen
B23.         klHienTai = klHienTai + nearestPoint.weight
B24.         ttHienTai = ttHienTai + nearestPoint.volume
B25.         xoa nearestPoint khoi unassignedPoints
B26.         currentPoint = nearestPoint

B27.     NEU tuyen co it nhat 1 diem THI them tuyen vao routes

B28. TRA VE routes va unassignedPoints (cac diem con sot lai)
```

### Điểm cần lưu ý khi cài đặt

1. **B14 và B15 phải kiểm cả hai**, không được chỉ kiểm khối lượng.
2. **B21** — nếu không tìm được điểm nào thỏa mãn thì đóng tuyến, đừng lặp vô hạn.
3. **B28** — nếu vẫn còn điểm chưa gán sau khi hết xe, đó là trường hợp thiếu sức chứa. Phải trả về danh sách này để hiển thị lỗi MSG-08, không được im lặng bỏ qua.
4. Duyệt xe theo thứ tự **tải trọng giảm dần** sẽ cho kết quả tốt hơn — xe to nhận nhiều điểm trước.

### Độ phức tạp

`O(m × n²)` với n là số điểm giao, m là số xe. Với n = 100, m = 5 thì khoảng 50.000 phép so sánh — chạy trong vài mili giây.

### Nhược điểm (phải nói được khi vấn đáp)

Nearest Neighbor là thuật toán **tham lam**: chọn tốt nhất ở mỗi bước mà không nhìn xa. Hậu quả là các điểm ở xa nhau bị dồn về cuối tuyến, tạo ra chặng dài bất hợp lý. Đó chính là lý do cần Giai đoạn 2.

---

## 6. Giai đoạn 2 — 2-opt

### Ý tưởng

Cho một tuyến đã có, thử **đảo ngược một đoạn con** xem tổng quãng đường có giảm không. Nếu giảm thì giữ lại. Lặp đến khi không cải thiện được nữa.

Ví dụ tuyến `KHO → A → B → C → D → KHO`. Đảo đoạn từ B đến C được `KHO → A → C → B → D → KHO`.

### Mã giả

```
HAM improveWith2Opt(tuyen, matrix):

B1.  improved = dung

B2.  LAP KHI improved = dung:
B3.      improved = sai

B4.      VOI i tu 0 den soDiem - 2:
B5.          VOI j tu i + 1 den soDiem - 1:

B6.              a = diemTruoc(i)      // i = 0 thi a la KHO
B7.              b = tuyen.diem[i]
B8.              c = tuyen.diem[j]
B9.              d = diemSau(j)        // j = cuoi thi d la KHO

B10.             delta = matrix[a][c] + matrix[b][d]
B11.                    - matrix[a][b] - matrix[c][d]

B12.             NEU delta < -0.001 THI     // dung nguong nho de tranh sai so double
B13.                 dao nguoc doan tu i den j trong tuyen
B14.                 improved = dung

B15. TRA VE tuyen
```

### Hàm tính độ lệch — giải thích kỹ

Đây là chỗ hay bị hỏi nhất. Khi đảo đoạn từ `b` đến `c`:

- **Mất đi** hai cạnh: `a→b` và `c→d`
- **Thêm vào** hai cạnh: `a→c` và `b→d`
- Các cạnh **bên trong** đoạn bị đảo vẫn giữ nguyên độ dài (chỉ đổi chiều đi)

Nên chỉ cần so sánh bốn cạnh, không phải tính lại toàn bộ tuyến:

```
delta = (canh moi) - (canh cu)
       = D[a][c] + D[b][d] - D[a][b] - D[c][d]
```

`delta < 0` nghĩa là tuyến mới ngắn hơn.

**Vì sao các cạnh bên trong không đổi?** Vì khoảng cách là đối xứng: `D[x][y] = D[y][x]`. Đi từ B tới C hay từ C tới B đều cùng độ dài.

**Điều kiện này không tự nhiên mà có.** Ma trận OSRM thô là bất đối xứng (đường một chiều, cấm rẽ trái). Vì vậy `DistanceMatrixService` **bắt buộc đối xứng hóa ma trận ở bước B3** trước khi giao cho thuật toán — xem mục 4. Bỏ bước đó thì công thức bốn cạnh sai, và 2-opt có thể làm tuyến dài ra trong khi vẫn báo là cải thiện.

Ghi vào báo cáo: *luận văn giả định khoảng cách đối xứng, và bảo đảm giả định này bằng cách lấy trung bình cộng hai chiều của ma trận OSRM. Hạn chế: cách này làm mất một phần thông tin về đường một chiều.*

### Điểm ăn điểm quan trọng nhất

**2-opt không làm thay đổi tập điểm trong tuyến, nên tổng khối lượng và tổng thể tích không đổi. Ràng buộc sức chứa đã thỏa mãn ở Giai đoạn 1 thì vẫn tiếp tục thỏa mãn sau Giai đoạn 2.**

Đây là lý do hai giai đoạn tách rời được, và là câu trả lời cho câu hỏi "làm sao em chắc đảo đoạn không làm xe quá tải". Học thuộc câu này.

### Độ phức tạp

Mỗi vòng lặp `O(n²)`. Số vòng phụ thuộc dữ liệu, thực nghiệm thường hội tụ sau dưới 10 vòng ở quy mô đồ án.

---

## 7. Tính tổng quãng đường

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

## 8. Tính ETA

```
HAM calculateEta(route, matrix, departureTime, speedKmh, loadingMinutesPerStop):

B1.  cumulativeDistance = matrix[DEPOT][route.points[0]]

B2.  VOI i tu 0 den pointCount - 1:
B3.      NEU i > 0 THI
B4.          cumulativeDistance = cumulativeDistance
B5.                             + matrix[route.points[i-1]][route.points[i]]
B6.
B7.      travelMinutes       = (cumulativeDistance / 1000.0) / speedKmh * 60
B8.      totalLoadingMinutes = i * loadingMinutesPerStop    // i diem da giao truoc do
B9.
B10.     eta = departureTime + travelMinutes + totalLoadingMinutes
B11.     route.points[i].estimatedArrival = eta
```

Ví dụ kiểm chứng bằng tay:
- Điểm 2, quãng đường tích lũy 13300 m, tốc độ 30 km/h, bốc dỡ 15 phút
- Di chuyển: `13.3 / 30 × 60 = 26.6` phút
- Bốc dỡ: `1 × 15 = 15` phút
- ETA `= 08:00 + 26.6 + 15 ≈ 08:42`

---

## 9. Cheapest Insertion — dùng cho xử lý sự cố

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

## 10. Hai mốc so sánh

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

---

## 11. Chương trình chạy thử độc lập

**Tuần 6–7 phải viết thuật toán chạy console trước, chưa gắn web.** Điều này giúp:

- Tách bạch lỗi thuật toán với lỗi Spring/JPA/giao diện
- Chạy thử nhanh, không cần khởi động server
- Dùng chính lớp này để chạy benchmark ở tuần 10

```java
// File: src/test/java/com/minhphu/mproute/AlgorithmRunner.java
public class AlgorithmRunner {
    public static void main(String[] args) {
        // 1. Tao du lieu cung: 1 kho, 10 diem giao, 3 vehicle
        // 2. Tinh ma tran bang Haversine (khong can OSRM)
        // 3. Chay NearestNeighborSolver, in ket qua
        // 4. Chay TwoOptImprover, in ket qua
        // 5. In bang so sanh truoc/sau
    }
}
```

Chạy được file này rồi mới gắn vào web ở tuần 8.

---

## 12. Danh sách unit test bắt buộc

| Test | Kiểm tra gì |
|---|---|
| `testTinhTongQuangDuongCoChangVeKho` | Tuyến 3 điểm, kiểm tổng có gồm chặng cuối |
| `testHaversineHaiDiemBiet` | So với giá trị tính tay của hai tọa độ đã biết |
| `testNNKhongVuotTaiTrong` | Sinh dữ liệu nặng, kiểm mọi tuyến ≤ tải trọng |
| `testNNKhongVuotTheTich` | Sinh dữ liệu cồng kềnh, kiểm mọi tuyến ≤ thể tích |
| `testNNGanHetDiemKhiDuSucChua` | Không sót điểm nào |
| `testNNTraVeDiemSotKhiThieuSucChua` | Trả về danh sách chưa gán, không im lặng |
| `test2OptKhongLamTangQuangDuong` | Sau 2-opt luôn ≤ trước 2-opt |
| `test2OptGiuNguyenTapDiem` | Số điểm và tổng kg/m³ không đổi |
| `testEtaDiemDauTien` | Khớp với tính tay |
| `testCheapestInsertionChonViTriDung` | Ví dụ nhỏ tính tay được |

Các test này vừa đảm bảo đúng, vừa là thứ để trưng ra khi hội đồng hỏi "em kiểm thử thế nào".
