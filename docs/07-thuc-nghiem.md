# 07 — Thực nghiệm và đánh giá

Đây là nội dung **chương 4 của báo cáo tốt nghiệp** — phần biến đồ án từ "làm phần mềm" thành "có nghiên cứu".

---

## Cảnh báo quan trọng

**Mọi con số minh họa trong tài liệu thiết kế (44.9%, 412ms, 108 km...) là số giả để giải thích cơ chế. Tuyệt đối không đưa vào báo cáo như số liệu thật.**

Nếu hội đồng hỏi "số này đo ở đâu" mà không trả lời được thì mất điểm nặng hơn là không có số nào.

Mọi số liệu trong báo cáo phải do chính bạn chạy và ghi lại.

---

## Ba loại số liệu

| Loại | Độ thuyết phục | Cần Minh Phú hợp tác? | Chi phí |
|---|---|---|---|
| **1. Benchmark chuẩn** | Cao nhất | Không | ~1 ngày |
| **2. So sánh thuật toán** | Cao | Không | ~0.5 ngày |
| **3. So sánh với thực tế** | Trung bình | Có | Phụ thuộc |

**Làm loại 1 và 2 trước.** Chúng độc lập với việc Minh Phú có cho dữ liệu hay không. Loại 3 là phần thêm nếu xin được dữ liệu.

---

## Loại 1 — Benchmark trên bộ dữ liệu chuẩn

### Vì sao đây là loại mạnh nhất

Các bộ dữ liệu CVRP chuẩn đã được cộng đồng nghiên cứu công bố kèm **lời giải tối ưu đã biết**. So sánh với chúng cho ra con số khách quan, kiểm chứng được, không ai bẻ được.

### Bộ dữ liệu dùng

**Augerat 1995 — Set A, Set B, Set P.** Tải từ CVRPLIB (`http://vrp.galgos.inf.puc-rio.br/`).

Chọn 8–10 bài trong khoảng 30–80 điểm, ví dụ:

| Tên bài | Số điểm | Số xe | Lời giải tối ưu đã biết |
|---|---|---|---|
| A-n32-k5 | 31 | 5 | 784 |
| A-n39-k5 | 38 | 5 | 822 |
| A-n45-k6 | 44 | 6 | 944 |
| A-n60-k9 | 59 | 9 | 1354 |
| A-n80-k10 | 79 | 10 | 1763 |
| B-n35-k5 | 34 | 5 | 955 |
| B-n50-k7 | 49 | 7 | 741 |
| P-n40-k5 | 39 | 5 | 458 |
| P-n55-k7 | 54 | 7 | 568 |
| P-n76-k5 | 75 | 5 | 627 |

**Kiểm tra lại giá trị tối ưu từ CVRPLIB khi tải về**, đừng chép từ tài liệu này.

### Định dạng file `.vrp`

```
NAME : A-n32-k5
COMMENT : (Augerat et al, No of trucks: 5, Optimal value: 784)
TYPE : CVRP
DIMENSION : 32
EDGE_WEIGHT_TYPE : EUC_2D
CAPACITY : 100
NODE_COORD_SECTION
 1 82 76
 2 96 44
 ...
DEMAND_SECTION
 1 0
 2 19
 ...
DEPOT_SECTION
 1
 -1
EOF
```

Điểm số 1 là kho, `demand` của kho luôn bằng 0.

`EDGE_WEIGHT_TYPE : EUC_2D` nghĩa là khoảng cách tính bằng công thức Euclid trên mặt phẳng, **làm tròn về số nguyên gần nhất** — đây là quy ước của CVRPLIB, phải làm đúng thì kết quả mới so sánh được:

```
d(i,j) = lam_tron( can_bac_hai( (xi-xj)^2 + (yi-yj)^2 ) )
```

### Xử lý bộ chuẩn chỉ có một ràng buộc

Augerat chỉ có ràng buộc `demand` (một chiều), còn hệ thống của bạn có hai chiều (kg và m³).

**Cách xử lý:** khi chạy benchmark, gán `volume = 0` cho mọi điểm và `maxVolume = vô cực` cho mọi xe. Ràng buộc thể tích tự động luôn thỏa mãn, thuật toán trở về đúng CVRP một chiều.

**Ghi rõ cách xử lý này vào báo cáo.** Nó cho thấy bạn hiểu sự khác nhau giữa bài toán chuẩn và bài toán mở rộng của mình.

### Chỉ số báo cáo

```
gap (%) = (ket_qua_cua_em - loi_giai_toi_uu) / loi_giai_toi_uu × 100
```

### Bảng kết quả mẫu cho chương 4

| Bài toán | Số điểm | Tối ưu đã biết | NN | Gap NN | NN+2-opt | Gap NN+2-opt | Thời gian (ms) |
|---|---|---|---|---|---|---|---|
| A-n32-k5 | 31 | 784 | ... | ...% | ... | ...% | ... |
| A-n39-k5 | 38 | 822 | ... | ...% | ... | ...% | ... |
| ... | | | | | | | |
| **Trung bình** | | | | **...%** | | **...%** | |

**Con số cần đạt để yên tâm:** Nearest Neighbor thuần thường có gap 20–35%. Thêm 2-opt thường kéo xuống khoảng **10–25%**.

Đừng hoảng nếu gap sau 2-opt vẫn nằm ở mức 20% — **đó không phải dấu hiệu code sai**. Lý do: 2-opt chỉ sắp lại thứ tự **bên trong** từng tuyến, nó không đụng được vào cách phân đơn cho xe. Mà với CVRP, phần sai số lớn nhất so với lời giải tối ưu lại nằm chính ở khâu phân đơn. Muốn kéo gap xuống nữa thì phải thêm bước hoán đổi đơn **giữa các tuyến** (ví dụ Or-opt hoặc relocate) — đó là hướng phát triển, không phải phạm vi đồ án.

**Dấu hiệu code SAI là gap quá thấp:** dưới 5% thì gần như chắc chắn đang tính thiếu, hay gặp nhất là **quên chặng về kho**. Gap trên 50% thì thuật toán có lỗi logic, hoặc đọc file `.vrp` sai (kiểm lại quy ước làm tròn EUC_2D).

---

## Loại 2 — So sánh giữa các thuật toán

Dùng chính dữ liệu benchmark ở trên, chạy ba cấu hình:

| Cấu hình | Mô tả |
|---|---|
| Ngẫu nhiên | Thứ tự giao ngẫu nhiên (mốc dưới) |
| NN | Chỉ Nearest Neighbor |
| NN + 2-opt | Đầy đủ |

### Bảng kết quả mẫu

| Thuật toán | Quãng đường TB | Cải thiện so với ngẫu nhiên | Gap so với tối ưu | Thời gian TB |
|---|---|---|---|---|
| Ngẫu nhiên | ... | 0% | ...% | ~0 ms |
| Nearest Neighbor | ... | ...% | ...% | ... ms |
| NN + 2-opt | ... | ...% | ...% | ... ms |

Bảng này trả lời trực tiếp câu "2-opt có đáng làm không" bằng số liệu.

### Biểu đồ hội tụ của 2-opt

Ghi lại tổng quãng đường sau mỗi vòng lặp của 2-opt, vẽ biểu đồ đường. Hình này rất được đánh giá cao vì cho thấy bạn hiểu thuật toán đang làm gì bên trong, không phải hộp đen.

```
Vong lap:  0     1     2     3     4     5     6
Quang duong: 1024  902   861   843   838   836   836
                                              ^ hoi tu
```

---

## Loại 3 — So sánh với thực tế Minh Phú

### Hai mốc so sánh — điểm mấu chốt

Đây là chỗ hội đồng tấn công mạnh nhất. Câu hỏi hiểm: **"Mốc so sánh của em có công bằng không?"**

Nếu chỉ dùng mốc "mỗi đơn một chuyến", con số giảm sẽ rất đẹp (40–50%) nhưng dễ bị bẻ: *"Đó là em tự dựng ra một cách làm kém rồi so với nó."*

**Cách phòng: báo cáo hai mốc song song.**

| | Mốc A | Mốc B |
|---|---|---|
| **Định nghĩa** | Mỗi đơn một chuyến khứ hồi từ kho | Cùng cách chia xe như thuật toán, nhưng thứ tự giao theo thứ tự nhận đơn |
| **Mô phỏng** | Quy trình hiện tại của công ty | Trường hợp công ty đã gom đơn nhưng chưa tối ưu thứ tự |
| **Đo được gì** | Tổng lợi ích của hệ thống | Riêng đóng góp của việc sắp thứ tự |
| **Con số điển hình** | Giảm 40–50% | Giảm 8–15% |
| **Độ tin cậy** | Thấp hơn | **Cao hơn** |

### Cách tính

```
// Moc A
baselineADistance = TONG (2 × D[KHO][diem]) voi moi diem giao

// Moc B
baselineBDistance = 0
VOI moi tuyen do thuat toan sinh ra:
    sapXep cac diem cua tuyen theo orderId tang dan
    baselineBDistance += calculateTotalDistance(tuyen da sap xep lai)
```

### Trình bày trong báo cáo

Viết rõ ý nghĩa của từng mốc, đừng chỉ đưa hai con số. Ví dụ:

> So với quy trình hiện tại của doanh nghiệp (mỗi đơn một chuyến riêng), hệ thống giảm được X% tổng quãng đường. Tuy nhiên phần lớn mức giảm này đến từ việc gom đơn — vốn là quyết định về mô hình vận hành chứ không phải đóng góp của thuật toán. Để đánh giá riêng phần thuật toán, luận văn sử dụng thêm mốc B: giữ nguyên cách chia đơn cho xe, chỉ thay đổi thứ tự giao. Kết quả cho thấy việc sắp thứ tự đóng góp thêm Y% mức giảm.

**Trình bày như vậy sẽ được đánh giá cao hơn nhiều so với một con số 45% không có bối cảnh.** Nó cho thấy bạn hiểu giới hạn của kết quả mình đưa ra.

### Dữ liệu dùng

**Nếu xin được dữ liệu thật từ Minh Phú:** lấy 10–20 ngày, chạy qua hệ thống, so với quãng đường công ty thực sự đã đi. Ghi rõ nguồn dữ liệu và khoảng thời gian.

**Nếu không xin được:** sinh dữ liệu giả lập dựa trên quy mô tương đương — 15 khách hàng có tọa độ thật ở Hà Nội, số thùng đặt ngẫu nhiên trong khoảng hợp lý, chạy 20 ngày mô phỏng. **Ghi rõ trong báo cáo là dữ liệu giả lập**, đừng giấu. Hội đồng chấp nhận dữ liệu giả lập nếu bạn nói thật; nhưng nếu phát hiện bạn trình bày dữ liệu giả như dữ liệu thật thì rất nặng.

---

## Cấu trúc chương 4 báo cáo

```
4.1  Muc tieu thuc nghiem
4.2  Moi truong thuc nghiem
     - Cau hinh may (CPU, RAM, OS)
     - Phien ban Java, MySQL
4.3  Thuc nghiem 1: Danh gia chat luong loi giai tren bo du lieu chuan
     4.3.1  Bo du lieu Augerat
     4.3.2  Cach do (gap)
     4.3.3  Ket qua (bang)
     4.3.4  Nhan xet
4.4  Thuc nghiem 2: So sanh cac thuat toan
     4.4.1  Ba cau hinh so sanh
     4.4.2  Ket qua (bang + bieu do hoi tu)
     4.4.3  Nhan xet
4.5  Thuc nghiem 3: Ap dung vao du lieu Minh Phu
     4.5.1  Mo ta du lieu
     4.5.2  Hai moc so sanh
     4.5.3  Ket qua (bang + bieu do)
     4.5.4  Nhan xet
4.6  Danh gia hieu nang
     - Thoi gian chay theo so diem (10, 30, 50, 100)
4.7  Han che cua thuat toan
```

**Mục 4.7 rất quan trọng.** Nêu thẳng hạn chế cho thấy bạn hiểu công việc mình làm:

- Nearest Neighbor là thuật toán tham lam, không đảm bảo tối ưu toàn cục. **Có trường hợp tổng sức chứa đủ nhưng thuật toán vẫn không xếp hết đơn** — chính bộ dữ liệu demo cho thấy điều này (xem `08-du-lieu-mau.md`)
- 2-opt chỉ cải thiện **thứ tự trong từng tuyến**, không thay đổi được cách phân đơn cho xe. Sai số lớn nhất của lời giải nằm ở khâu phân đơn, mà 2-opt không chạm tới được
- 2-opt chỉ tìm được cực trị địa phương, không thoát ra được bằng chính nó
- Giả định khoảng cách đối xứng — bảo đảm bằng cách lấy trung bình cộng hai chiều của ma trận OSRM, đổi lại làm mất một phần thông tin về đường một chiều
- **Trạng thái xe là cờ toàn cục, chưa gắn với từng ngày.** Xe được đặt `IN_USE` khi chốt tuyến và chỉ trở lại `AVAILABLE` khi tuyến hoàn thành, nên hệ thống chưa lập kế hoạch trước cho nhiều ngày cùng lúc được. Đây là lựa chọn có ý thức để giữ mô hình đơn giản trong phạm vi đồ án
- **Chưa ràng buộc thời gian làm việc tối đa của tài xế** — tuyến dài bao nhiêu giờ cũng được chấp nhận
- Chưa xử lý khung giờ nhận hàng của khách (VRPTW). Dữ liệu có ghi chú kiểu *"chỉ nhận hàng buổi sáng"* nhưng thuật toán chỉ lưu, không ràng buộc
- Chưa xử lý nhiều chuyến mỗi xe mỗi ngày (multi-trip). Với bán kính giao hàng 5–15 km, một tuyến thường kết thúc trước trưa và xe không được dùng lại trong ngày
- Chưa tính đến tình trạng giao thông theo giờ, tốc độ trung bình lấy cố định từ tham số hệ thống

Mỗi hạn chế nêu ra chính là một ý cho mục "Hướng phát triển".

---

## Ba câu hỏi hội đồng chắc chắn hỏi

### 1. "Em tự viết thuật toán hay gọi thư viện?"

> Em tự cài đặt hoàn toàn. Nearest Neighbor và 2-opt đều chỉ dùng vòng lặp và phép so sánh khoảng cách, tổng khoảng 200 dòng code. Em không dùng OR-Tools hay jsprit. Em có so sánh kết quả với lời giải tối ưu đã công bố của bộ dữ liệu Augerat để đánh giá chất lượng lời giải của mình.

### 2. "Tại sao đảo đoạn trong 2-opt không làm xe chở quá tải?"

> Vì phép đảo chỉ thay đổi thứ tự đi qua các điểm, không thêm hay bớt điểm nào khỏi tuyến. Tập điểm không đổi thì tổng khối lượng và tổng thể tích cũng không đổi. Ràng buộc đã thỏa mãn ở giai đoạn Nearest Neighbor thì vẫn tiếp tục thỏa mãn sau 2-opt. Đó là lý do em tách được hai giai đoạn.

### 3. "Con số giảm phần trăm của em lấy ở đâu ra? Mốc so sánh có công bằng không?"

> Em dùng hai mốc. Mốc A mô phỏng quy trình hiện tại của doanh nghiệp là mỗi đơn một chuyến khứ hồi, cho mức giảm X%. Nhưng em nhận thấy phần lớn mức giảm đó đến từ việc gom đơn chứ không phải từ thuật toán, nên em dùng thêm mốc B: giữ nguyên cách chia xe, chỉ đổi thứ tự giao. Mốc B cho mức giảm Y%, đây mới là đóng góp riêng của phần sắp thứ tự. Ngoài ra em còn đánh giá trên bộ dữ liệu chuẩn Augerat với gap trung bình Z%.

Học thuộc ba câu này. Chúng chiếm phần lớn điểm vấn đáp.
