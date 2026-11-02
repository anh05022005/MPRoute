# 07 — Thực nghiệm và đánh giá

Đây là nội dung **chương 4 của báo cáo tốt nghiệp** — phần biến đồ án từ "làm phần mềm" thành "có nghiên cứu".

---

## Cảnh báo quan trọng

**Mọi con số minh họa trong tài liệu thiết kế (44.9%, 412ms, 108 km...) là số giả để giải thích cơ chế. Tuyệt đối không đưa vào báo cáo như số liệu thật.**

Nếu hội đồng hỏi "số này đo ở đâu" mà không trả lời được thì mất điểm nặng hơn là không có số nào.

Mọi số liệu trong báo cáo phải do chính bạn chạy và ghi lại.

---

## Hai loại số liệu

| Loại | Độ thuyết phục | Cần Minh Phú hợp tác? | Chi phí |
|---|---|---|---|
| **1. Benchmark chuẩn** | Cao nhất | Không | ~1 ngày |
| **3. So sánh với thực tế** | Trung bình | Có | Phụ thuộc |

Loại 2 *"so sánh giữa các thuật toán"* **đã bỏ** cùng hai bộ giải tự viết NN và NN + 2-opt
(`09` mục A7). Giữ số "Loại 3" để các chỗ khác trỏ tới không bị lệch.

**Làm loại 1 trước.** Nó độc lập với việc Minh Phú có cho dữ liệu hay không. Loại 3 là phần thêm nếu xin được dữ liệu.

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

| Bài toán | Số điểm | Số xe | Tối ưu đã biết | OR-Tools | Gap |
|---|---|---|---|---|---|
| A-n32-k5 | 31 | 5 | 784 | **784** | **+0,0%** |
| A-n39-k5 | 38 | 5 | 822 | 825 | +0,4% |
| A-n45-k6 | 44 | 6 | 944 | 1 002 | +6,1% |
| A-n60-k9 | 59 | 9 | 1 354 | 1 359 | +0,4% |
| A-n80-k10 | 79 | 10 | 1 763 | 1 817 | +3,1% |
| B-n35-k5 | 34 | 5 | 955 | **955** | **+0,0%** |
| B-n50-k7 | 49 | 7 | 741 | **741** | **+0,0%** |
| P-n40-k5 | 39 | 5 | 458 | **458** | **+0,0%** |
| P-n55-k7 | 54 | 7 | 568 | 575 | +1,2% |
| P-n76-k5 | 75 | 5 | 627 | 668 | +6,5% |
| **Trung bình** | | | | | **+1,8%** |

Lần chạy này còn có hai cột NN và NN + 2-opt (gap trung bình 39,6% và 33,4%, NN còn sót một
điểm ở A-n45-k6). Hai bộ giải đó đã bỏ nên bảng chỉ giữ cột OR-Tools; số cũ còn trong
lịch sử Git của file này nếu cần dẫn lại.

**A-n45-k6 gap 6,1%** là bài khó nhất về sức chứa: nhu cầu 593 trên tổng sức chứa 600, tức
phải lấp đầy **98,8%** đội xe. OR-Tools vẫn xếp hết điểm nhờ tìm kiếm cục bộ chuyển được
điểm giữa các xe, nhưng phải trả bằng tuyến dài hơn tối ưu một chút.

**Bằng chứng mạnh nhất rằng cách đo là đúng:** OR-Tools ra **đúng bằng lời giải tối ưu đã
công bố** ở 4 trong 10 bài (784, 955, 741, 458 — không lệch một đơn vị nào). Con số chỉ
khớp chính xác khi cả ba thứ cùng đúng: quy ước làm tròn EUC_2D, kho nằm ở điểm số 1, và
**tổng quãng đường có tính chặng cuối về kho** (CBR-01). Thiếu chặng về kho thì mọi dòng
đều nhỏ hơn tối ưu — không bao giờ khớp được.

**Gap 1,8% và trùng khít tối ưu ở 4 bài là mức bình thường của OR-Tools** với giới hạn 5 giây
ở quy mô 30–80 điểm. Hai bài lệch nhiều nhất (A-n45-k6, P-n76-k5) là bài sát sức chứa và bài
nhiều điểm nhất trên mỗi tuyến — tăng thời gian giới hạn sẽ kéo gap xuống thêm.

**Dấu hiệu code SAI là gap âm:** ra thấp hơn lời giải tối ưu đã công bố thì chắc chắn đang tính thiếu, hay gặp nhất là **quên chặng về kho**. Gap trên 50% thì thuật toán có lỗi logic, hoặc đọc file `.vrp` sai (kiểm lại quy ước làm tròn EUC_2D).

---

## ~~Loại 2 — So sánh giữa các thuật toán~~ — đã bỏ

Bỏ cùng hai bộ giải NN và NN + 2-opt (`09` mục A7). Câu hỏi "riêng việc sắp thứ tự đóng góp
bao nhiêu" giờ do **mốc B** ở loại 3 trả lời.

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
4.4  Thuc nghiem 2: Ap dung vao du lieu Minh Phu
     4.4.1  Mo ta du lieu
     4.4.2  Hai moc so sanh
     4.4.3  Ket qua (bang + bieu do)
     4.4.4  Nhan xet
4.5  Danh gia hieu nang
     - Thoi gian chay theo so diem (10, 30, 50, 100)
4.6  Han che
```

**Mục 4.6 rất quan trọng.** Nêu thẳng hạn chế cho thấy bạn hiểu công việc mình làm:

- OR-Tools với `GUIDED_LOCAL_SEARCH` là **heuristic có giới hạn thời gian**, không chứng minh được tối ưu. Trên Augerat nó trùng tối ưu ở 4/10 bài nhưng lệch tới 6,5% ở bài khác
- **Tổng sức chứa đủ chưa chắc đã xếp hết đơn** — phân đơn cho xe là bài toán xếp thùng. Hệ thống không im lặng bỏ sót mà báo rõ đơn nào chưa xếp được (CN-03-14)
- **Ưu tiên khi hết chỗ chỉ đúng từng cặp trong cùng nhóm.** Đơn đã hoãn luôn được giữ trước đơn chưa hoãn, nhưng giữa các đơn chưa hoãn, bộ giải có thể bỏ một đơn đặt sớm để xếp vừa hai đơn đặt muộn (`03` mục 5.3c). Ưu tiên tuyệt đối cho mọi tập đơn cần tiền phạt tăng theo lũy thừa, tràn số `long`
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

*(Câu trả lời mẫu dưới đây đã cập nhật sau khi bỏ NN và 2-opt — `09` mục A7.)*

> Em dùng OR-Tools của Google, nằm ở package `optimization/ortools`. Lúc đầu em có tự cài Nearest Neighbor và 2-opt để so sánh, nhưng đo trên bộ chuẩn Augerat thì bản tự viết cách lời giải tối ưu khoảng 33%, còn OR-Tools chỉ 1,8%. Sản phẩm giao cho doanh nghiệp thì phải chạy bằng cái tốt hơn, nên em bỏ bản tự viết để khỏi bảo trì hai bộ giải.

**Nếu bị hỏi vặn "vậy phần của em là gì?":**

> Là mô hình hóa và đo lường. OR-Tools chỉ giải bài toán thuần. Em phải tự dựng ma trận khoảng cách từ OSRM và có phương án dự phòng Haversine, tự khai hai ràng buộc khối lượng và thể tích thành hai Dimension, tự quy đổi số thực sang số nguyên và làm tròn đúng chiều, tự cho phép bỏ điểm bằng `addDisjunction` để báo được đơn chưa xếp, tự tính ETA, tự viết Cheapest Insertion cho màn xử lý sự cố, và tự dựng hai mốc so sánh cùng benchmark để đo kết quả.

### 2. "Tại sao không để OR-Tools tự chạy mãi cho ra tối ưu?"

> Vì `GUIDED_LOCAL_SEARCH` là siêu tìm kiếm, nó không tự dừng — không đặt giới hạn thời gian là màn hình phân tuyến treo. Em đặt 5 giây: trên Augerat 30–80 điểm thì gap trung bình đã còn 1,8%, mà quy mô một ngày của Minh Phú chỉ khoảng 15 đơn.

### 3. "Con số giảm phần trăm của em lấy ở đâu ra? Mốc so sánh có công bằng không?"

> Em dùng hai mốc. Mốc A mô phỏng quy trình hiện tại của doanh nghiệp là mỗi đơn một chuyến khứ hồi, cho mức giảm X%. Nhưng em nhận thấy phần lớn mức giảm đó đến từ việc gom đơn chứ không phải từ thuật toán, nên em dùng thêm mốc B: giữ nguyên cách chia xe, chỉ đổi thứ tự giao. Mốc B cho mức giảm Y%, đây mới là đóng góp riêng của phần sắp thứ tự. Ngoài ra em đánh giá trên bộ dữ liệu chuẩn Augerat có lời giải tối ưu đã công bố, gap trung bình Z%.
>
> Benchmark là mốc khó bẻ nhất, vì nó **bất lợi** cho em: nó cho thấy kết quả còn kém lời giải tốt nhất bao nhiêu, chứ không phải hơn bao nhiêu.

Học thuộc ba câu này. Chúng chiếm phần lớn điểm vấn đáp.
