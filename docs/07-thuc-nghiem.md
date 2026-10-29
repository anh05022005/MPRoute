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

### Kết quả đã chạy thật — 27/08/2026

Chạy bằng `BenchmarkRunner`, mười bài đã tải về `src/main/resources/data/augerat/`.
Mỗi bài dùng **đúng số xe của đề bài**, khoảng cách EUC_2D làm tròn về số nguyên,
OR-Tools giới hạn 5 giây/bài. Máy: JDK 23, Windows 11.

| Bài toán | Số điểm | Số xe | Tối ưu đã biết | NN | Gap NN | NN+2-opt | Gap NN+2-opt | OR-Tools | Gap OR-Tools |
|---|---|---|---|---|---|---|---|---|---|
| A-n32-k5 | 31 | 5 | 784 | 1 145 | +46,0% | 1 116 | +42,3% | **784** | **+0,0%** |
| A-n39-k5 | 38 | 5 | 822 | 1 070 | +30,2% | 1 028 | +25,1% | 825 | +0,4% |
| A-n45-k6 | 44 | 6 | 944 | 1 345\* | sót 1 điểm | 1 275\* | sót 1 điểm | 1 002 | +6,1% |
| A-n60-k9 | 59 | 9 | 1 354 | 1 837 | +35,7% | 1 774 | +31,0% | 1 359 | +0,4% |
| A-n80-k10 | 79 | 10 | 1 763 | 2 348 | +33,2% | 2 290 | +29,9% | 1 817 | +3,1% |
| B-n35-k5 | 34 | 5 | 955 | 1 304 | +36,5% | 1 277 | +33,7% | **955** | **+0,0%** |
| B-n50-k7 | 49 | 7 | 741 | 1 036 | +39,8% | 1 018 | +37,4% | **741** | **+0,0%** |
| P-n40-k5 | 39 | 5 | 458 | 682 | +48,9% | 657 | +43,4% | **458** | **+0,0%** |
| P-n55-k7 | 54 | 7 | 568 | 754 | +32,7% | 723 | +27,3% | 575 | +1,2% |
| P-n76-k5 | 75 | 5 | 627 | 962 | +53,4% | 820 | +30,8% | 668 | +6,5% |
| **Trung bình** | | | | | **+39,6%** | | **+33,4%** | | **+1,8%** |

\* **A-n45-k6:** Nearest Neighbor không xếp hết 44 điểm vào 6 xe nên hai ô đó không đặt
cạnh lời giải tối ưu được, và **không tính vào trung bình**. Đây không phải lỗi: bài này
đòi lấp đầy **98,8%** đội xe (nhu cầu 593 trên sức chứa 600), tức là phải xếp thùng gần
như hoàn hảo — thuật toán tham lam về nguyên tắc không làm được. Chính là điểm yếu của NN
mà kịch bản demo ở `docs/08` dùng đến. OR-Tools xếp hết vì có `addDisjunction` và tìm kiếm
cục bộ chuyển được điểm giữa các xe.

**Bằng chứng mạnh nhất rằng cách đo là đúng:** OR-Tools ra **đúng bằng lời giải tối ưu đã
công bố** ở 4 trong 10 bài (784, 955, 741, 458 — không lệch một đơn vị nào). Con số chỉ
khớp chính xác khi cả ba thứ cùng đúng: quy ước làm tròn EUC_2D, kho nằm ở điểm số 1, và
**tổng quãng đường có tính chặng cuối về kho** (CBR-01). Thiếu chặng về kho thì mọi dòng
đều nhỏ hơn tối ưu — không bao giờ khớp được.

**Con số dự kiến ban đầu:** Nearest Neighbor thuần thường có gap 20–35%, thêm 2-opt thường kéo xuống khoảng 10–25%.

**Đo thật thì cao hơn dự kiến:** NN ra 39,6% và NN+2-opt ra 33,4%. Cần nói thẳng chỗ này
khi vấn đáp thay vì lờ đi. Lý do nằm ở chỗ 2-opt chỉ gỡ được **thứ tự bên trong một tuyến**,
mà ở các bài này mỗi tuyến chỉ có 6–9 điểm nên gần như không còn gì để gỡ — đo riêng thì
2-opt chỉ cải thiện **4,3%** so với NN thuần. Phần sai số lớn còn lại nằm ở **khâu chia
điểm cho xe**, đúng thứ mà 2-opt theo định nghĩa không đụng tới. Bằng chứng đó là bản chất
chứ không phải lỗi: cùng bộ đọc file, cùng ma trận, cùng hàm tính tổng quãng đường,
OR-Tools ra gap 1,8% và trùng khít lời giải tối ưu ở 4 bài.

Đừng hoảng nếu gap sau 2-opt vẫn nằm ở mức 20% — **đó không phải dấu hiệu code sai**. Lý do: 2-opt chỉ sắp lại thứ tự **bên trong** từng tuyến, nó không đụng được vào cách phân đơn cho xe. Mà với CVRP, phần sai số lớn nhất so với lời giải tối ưu lại nằm chính ở khâu phân đơn. Muốn kéo gap xuống nữa thì phải thêm bước hoán đổi đơn **giữa các tuyến** (ví dụ Or-opt hoặc relocate) — đó là hướng phát triển, không phải phạm vi đồ án.

**Dấu hiệu code SAI là gap quá thấp:** dưới 5% thì gần như chắc chắn đang tính thiếu, hay gặp nhất là **quên chặng về kho**. Gap trên 50% thì thuật toán có lỗi logic, hoặc đọc file `.vrp` sai (kiểm lại quy ước làm tròn EUC_2D).

---

## Loại 2 — So sánh giữa các thuật toán

Dùng chính dữ liệu benchmark ở trên. Bảng trên đo *cách lời giải tốt nhất bao nhiêu*,
bảng này đo **riêng đóng góp của bước 2-opt** — tức phần cải thiện do chính mình viết ra.
Hai bảng trả lời hai câu hỏi khác nhau nên phải có cả hai (CN-09-03).

Cấu hình *"thứ tự ngẫu nhiên"* trong bản thiết kế ban đầu **đã bỏ**: `CN-09-03` chỉ yêu cầu
chạy `NN` và `NN_2OPT`, và trong phần mềm thì **mốc B** (cùng cách chia xe, giao theo thứ tự
nhận đơn) đã giữ đúng vai trò "mốc dưới" đó bằng một cách đáng tin hơn — thứ tự nhận đơn là
cách người điều phối thật sự làm, còn thứ tự ngẫu nhiên thì không ai làm bao giờ.

### Kết quả đã chạy thật — 27/08/2026

| Bài toán | Số điểm | NN | NN + 2-opt | Cải thiện | t(NN) | t(2-opt) |
|---|---|---|---|---|---|---|
| A-n32-k5 | 31 | 1 145 | 1 116 | 2,5% | 13 ms | 1 ms |
| A-n39-k5 | 38 | 1 070 | 1 028 | 3,9% | 0 ms | 0 ms |
| A-n45-k6 | 44 | 1 345\* | 1 275\* | — | 0 ms | 0 ms |
| A-n60-k9 | 59 | 1 837 | 1 774 | 3,4% | 0 ms | 0 ms |
| A-n80-k10 | 79 | 2 348 | 2 290 | 2,5% | 1 ms | 0 ms |
| B-n35-k5 | 34 | 1 304 | 1 277 | 2,1% | 0 ms | 0 ms |
| B-n50-k7 | 49 | 1 036 | 1 018 | 1,7% | 0 ms | 0 ms |
| P-n40-k5 | 39 | 682 | 657 | 3,7% | 0 ms | 1 ms |
| P-n55-k7 | 54 | 754 | 723 | 4,1% | 0 ms | 0 ms |
| P-n76-k5 | 75 | 962 | 820 | **14,8%** | 0 ms | 0 ms |
| **Trung bình** | | | | **4,3%** | | |

`Cải thiện = (NN − NN2opt) / NN × 100`. Dòng A-n45-k6 để trống vì cả hai bên đều bỏ sót một
điểm, hai con số không đo cùng một thứ.

**Đọc bảng này thế nào:**

- **2-opt luôn dương, không bao giờ âm.** Đó là tính chất của chính thuật toán — nó chỉ nhận
  một phép đảo khi độ lệch bốn cạnh âm — và là thứ test `twoOptNeverIncreasesTotalDistance`
  kiểm. Chạy ra một dòng âm nghĩa là ma trận không đối xứng, không phải 2-opt sai.
- **P-n76-k5 cải thiện 14,8%, gấp ba lần trung bình.** Đây là bài có nhiều điểm nhất trên mỗi
  tuyến (75 điểm chia 5 xe, tức 15 điểm/tuyến), nên có nhiều thứ tự để gỡ nhất. Các bài còn
  lại chỉ 6–9 điểm mỗi tuyến, gần như không còn "nút" nào để tháo. **Đây chính là câu trả lời
  cho "2-opt có đáng làm không": càng nhiều điểm trên một tuyến thì càng đáng.**
- **Thời gian chạy gần như bằng 0** ở mọi bài. 2-opt không phải thứ phải cân nhắc về hiệu
  năng ở quy mô này — điều kiện là ma trận đã nạp sẵn trong bộ nhớ, không tra CSDL hay gọi
  OSRM trong vòng lặp.

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
| **Con số dự kiến ban đầu** | Giảm 40–50% | Giảm 8–15% |
| **Đo thật, 10 ngày seed** | **Giảm 65,9%** | **Giảm 33,1%** |
| **Độ tin cậy** | Thấp hơn | **Cao hơn** |

### Vì sao số đo thật cao hơn dự kiến

Cả hai mốc đều ra cao hơn con số dự kiến, và lý do là **quy mô mỗi ngày**, không phải cách đo
đổi đi. Bản thiết kế ước lượng theo ví dụ 8 đơn chia 3 xe; dữ liệu seed thật là **12–15 đơn
chia 2 xe**, tức mỗi tuyến 6–7 điểm thay vì 2–3 điểm.

- **Mốc A tăng theo số đơn.** `baselineA = Σ 2 × d(kho, điểm)` cộng thêm một số hạng đầy đủ
  cho mỗi đơn, trong khi tuyến tối ưu gom nhiều đơn vào cùng một vòng nên dài thêm rất ít.
  Càng nhiều đơn một ngày thì khoảng cách giữa hai bên càng giãn ra. Đây là điều đúng về mặt
  toán học chứ không phải một con số may mắn.
- **Mốc B chỉ có chỗ để chênh khi tuyến đủ dài.** Đúng như `docs/08` đã cảnh báo: tuyến 2 điểm
  thì mốc B ra **đúng 0%** vì ma trận đối xứng. Dữ liệu cũ (3 đơn/ngày) cho mốc B 0–18%; dữ liệu
  12–15 đơn/ngày cho **22,6% – 44,6%**. Sự chênh này là bằng chứng bằng số cho câu *"quy mô bài
  toán phải đủ lớn thì việc tối ưu thứ tự mới có ý nghĩa"*.

**Khi vấn đáp phải nói rõ con số 65,9% gắn với quy mô nào.** Nói trống "hệ thống giảm 66%" là
chỗ dễ bị bẻ nhất của cả báo cáo — nó đúng với 13 đơn/ngày, không đúng với mọi quy mô.

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
- **Mỗi xe một tuyến mỗi ngày, và xe hỏng không tự dời lịch các ngày sau.** Hệ thống lập kế hoạch trước cho nhiều ngày được (xe giữ lịch theo từng ngày, `09` mục B6), nhưng xe hỏng hôm nay thì tuyến đã chốt cho ngày mai vẫn nằm đó: hệ thống chỉ chặn nhận hàng và cảnh báo, điều phối viên phải tự hủy và phân lại
- **Chưa ràng buộc thời gian làm việc tối đa của tài xế** — tuyến dài bao nhiêu giờ cũng được chấp nhận
- Chưa xử lý khung giờ nhận hàng của khách (VRPTW). Dữ liệu có ghi chú kiểu *"chỉ nhận hàng buổi sáng"* nhưng thuật toán chỉ lưu, không ràng buộc
- Chưa **tối ưu** nhiều chuyến mỗi xe mỗi ngày (multi-trip). Xe và tài xế giao xong chuyến trước thì được phân chuyến tiếp (`09` mục B7), nhưng mỗi lần chạy thuật toán chỉ xếp một chuyến cho mỗi xe — không tự chia đơn trong ngày thành chuyến sáng và chuyến chiều
- Chưa tính đến tình trạng giao thông theo giờ, tốc độ trung bình lấy cố định từ tham số hệ thống

Mỗi hạn chế nêu ra chính là một ý cho mục "Hướng phát triển".

---

## Ba câu hỏi hội đồng chắc chắn hỏi

### 1. "Em tự viết thuật toán hay gọi thư viện?"

> Em làm cả hai, và đó là chủ ý. Em tự cài đặt Nearest Neighbor và 2-opt bằng vòng lặp thuần, khoảng 200 dòng, không dùng thư viện nào — nằm ở package `optimization/heuristic`. Sau đó em cắm thêm OR-Tools của Google vào cùng một interface, nằm ở package `optimization/ortools`.
>
> Bản tự cài đặt để em đo và hiểu: nó tốt hơn cách Minh Phú đang làm bao nhiêu, và nó còn cách lời giải tốt bao nhiêu. Bản OR-Tools là cận trên để so, và là cái hệ thống thực sự chạy khi giao cho doanh nghiệp — vì tuyến nó cho ngắn hơn khoảng [điền số] %.
>
> Em đánh giá cả hai trên bộ dữ liệu chuẩn Augerat có lời giải tối ưu đã công bố. Kết quả là bản tự viết gap trung bình [X] %, OR-Tools gap [Y] %.

**Nếu bị hỏi vặn "vậy phần của em là gì?":**

> Là mô hình hóa và đo lường. Em phải tự dựng ma trận khoảng cách và đối xứng hóa nó, tự đặt hai ràng buộc sức chứa cùng lúc, tự tính ETA, tự dựng ba mốc so sánh và giải thích được vì sao mốc A dễ gây hiểu nhầm. Riêng phần thuật toán, em viết được bản của mình rồi đo ra nó thua ở đâu và vì sao thua — 2-opt chỉ sắp lại thứ tự bên trong một tuyến nên không sửa được cách chia đơn cho xe, còn OR-Tools chuyển điểm qua lại giữa các tuyến được. Em coi việc biết giới hạn giải pháp của mình là một kết quả chứ không phải một thiếu sót.

### 2. "Tại sao đảo đoạn trong 2-opt không làm xe chở quá tải?"

> Vì phép đảo chỉ thay đổi thứ tự đi qua các điểm, không thêm hay bớt điểm nào khỏi tuyến. Tập điểm không đổi thì tổng khối lượng và tổng thể tích cũng không đổi. Ràng buộc đã thỏa mãn ở giai đoạn Nearest Neighbor thì vẫn tiếp tục thỏa mãn sau 2-opt. Đó là lý do em tách được hai giai đoạn.

### 3. "Con số giảm phần trăm của em lấy ở đâu ra? Mốc so sánh có công bằng không?"

> Em dùng ba mốc. Mốc A mô phỏng quy trình hiện tại của doanh nghiệp là mỗi đơn một chuyến khứ hồi, cho mức giảm X%. Nhưng em nhận thấy phần lớn mức giảm đó đến từ việc gom đơn chứ không phải từ thuật toán, nên em dùng thêm mốc B: giữ nguyên cách chia xe, chỉ đổi thứ tự giao. Mốc B cho mức giảm Y%, đây mới là đóng góp riêng của phần sắp thứ tự. Mốc C là OR-Tools chạy trên đúng ma trận đó, cho cận trên của chất lượng. Ngoài ra em còn đánh giá trên bộ dữ liệu chuẩn Augerat với gap trung bình Z%.
>
> Mốc C là mốc khó bẻ nhất, vì nó là mốc **bất lợi** cho em: nó cho thấy giải pháp của em còn kém bao nhiêu, chứ không phải hơn bao nhiêu.

Học thuộc ba câu này. Chúng chiếm phần lớn điểm vấn đáp.
