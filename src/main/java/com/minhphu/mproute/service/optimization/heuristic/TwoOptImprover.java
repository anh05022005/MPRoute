package com.minhphu.mproute.service.optimization.heuristic;

import com.minhphu.mproute.service.optimization.OptimizationPoint;
import com.minhphu.mproute.service.optimization.OptimizationRoute;
import com.minhphu.mproute.service.optimization.RouteDistanceCalculator;

import java.util.List;

/**
 * 2-opt — giai doan 2 cua ban TU CAI DAT (docs/03 muc 7).
 *
 * Y TUONG, noi bang mot cau: cho mot tuyen da co san, thu DAO NGUOC mot doan con xem
 * tong quang duong co giam khong; giam thi giu lai, lap den khi khong cai thien duoc nua.
 *
 * Vi du tuyen KHO -> A -> B -> C -> D -> KHO, dao doan tu B den C duoc
 * KHO -> A -> C -> B -> D -> KHO.
 *
 * VI SAO CAN GIAI DOAN NAY: Nearest Neighbor la thuat toan tham lam, no don cac diem
 * con lai o xa ve cuoi tuyen, tao ra mot chang dai bat hop ly o gan cuoi. 2-opt chinh
 * la de go dung chang do — hinh anh de nho: tuyen bi "that nut", 2-opt thao nut ra.
 *
 * PACKAGE NAY CAM IMPORT THU VIEN VRP NAO. Day la ban tu cai dat lam moc so sanh.
 *
 * BA DIEU PHAI NOI DUOC KHI VAN DAP — chi tiet o comment cua tung phan ben duoi:
 *   1. Cong thuc do lech chi so BON canh, khong tinh lai ca tuyen (ham improveOnePass)
 *   2. Cong thuc do CHI DUNG khi ma tran doi xung (comment ngay tren cong thuc)
 *   3. 2-opt KHONG lam thay doi TAP diem cua tuyen, nen rang buoc suc chua da thoa man
 *      o giai doan 1 thi van con thoa man sau giai doan 2 (comment o ham reverseSegment)
 */
public class TwoOptImprover {

    /**
     * Nguong nhan mot phep dao. Phai la so am du xa 0, khong duoc dung "delta < 0".
     *
     * Ly do: matrix la double, cong tru bon so thuc luon con sai so cuoi cung khoang
     * 1e-12. Neu de "delta < 0" thi mot phep dao that ra khong doi gi van co the ra
     * delta = -0.0000000001, thuat toan nhan no, vong sau lai dao nguoc lai, va vong
     * lap ngoai khong bao gio dung. Nguong 1 mm chan dung truong hop do.
     */
    private static final double IMPROVEMENT_THRESHOLD = -0.001;

    /**
     * Chan tren so vong quet, de phong. Thuc nghiem o quy mo do an hoi tu sau duoi 10
     * vong; dat 100 la du xa. Co con so nay thi du sau nay ma tran bi lech doi xung
     * (loi o cho khac) chuong trinh cung chi cham chu khong treo man hinh phan tuyen.
     */
    private static final int MAX_PASSES = 100;

    /**
     * Cai thien mot tuyen tai cho. Ham SUA THANG vao danh sach diem cua tuyen va
     * tinh lai totalDistance.
     *
     * @return so vong quet co nhan phep dao (0 nghia la tuyen khong cai thien duoc gi)
     */
    public int improve(OptimizationRoute route, double[][] matrix) {

        /*
         * Duoi 3 diem thi khong co gi de dao: tuyen 2 diem KHO -> A -> B -> KHO dao lai
         * thanh KHO -> B -> A -> KHO co dung tong quang duong (ma tran doi xung).
         */
        if (route.getPoints().size() < 3) {
            return 0;
        }

        int improvingPasses = 0;
        int passes = 0;
        boolean improved = true;

        // B2. Quet di quet lai den khi mot vong tron ven khong tim ra phep dao nao nua
        while (improved && passes < MAX_PASSES) {
            improved = improveOnePass(route.getPoints(), matrix);
            passes++;
            if (improved) {
                improvingPasses++;
            }
        }

        // Thu tu diem da doi thi quang duong cu khong con dung nua
        RouteDistanceCalculator.updateTotalDistance(route, matrix);

        return improvingPasses;
    }

    /** Cai thien lan luot moi tuyen cua mot ket qua phan tuyen. */
    public void improveAll(List<OptimizationRoute> routes, double[][] matrix) {
        for (OptimizationRoute route : routes) {
            improve(route, matrix);
        }
    }

    /**
     * Mot vong quet: thu MOI cap (i, j) hop le, dao ngay khi thay co loi.
     *
     * @return true neu vong nay co it nhat mot phep dao duoc nhan
     */
    private boolean improveOnePass(List<OptimizationPoint> points, double[][] matrix) {

        boolean improved = false;
        int pointCount = points.size();

        // B4, B5. i la dau doan bi dao, j la cuoi doan bi dao
        for (int i = 0; i < pointCount - 1; i++) {
            for (int j = i + 1; j < pointCount; j++) {

                /*
                 * Bon diem quanh doan bi dao:
                 *
                 *      ... a -> [ b ... c ] -> d ...
                 *               ^^^^^^^^^^^
                 *               doan se bi dao nguoc
                 *
                 *   a = diem NGAY TRUOC doan. i = 0 thi doan bat dau tu diem giao dau
                 *       tien, nen a chinh la KHO.
                 *   d = diem NGAY SAU doan. j la diem cuoi cung thi d cung la KHO.
                 *
                 * Hai truong hop KHO nay chinh la cho de quen nhat cua 2-opt: bo qua
                 * chung thi khong bao gio dao duoc doan dau va doan cuoi tuyen, ma chang
                 * dai bat hop ly do Nearest Neighbor de lai thuong nam dung o cuoi.
                 */
                int a = (i == 0)
                        ? RouteDistanceCalculator.DEPOT_INDEX
                        : points.get(i - 1).getMatrixIndex();
                int b = points.get(i).getMatrixIndex();
                int c = points.get(j).getMatrixIndex();
                int d = (j == pointCount - 1)
                        ? RouteDistanceCalculator.DEPOT_INDEX
                        : points.get(j + 1).getMatrixIndex();

                /*
                 * CONG THUC DO LECH — cho hoi dong hoi nhieu nhat.
                 *
                 * Dao doan [b..c] thi tuyen:
                 *   MAT hai canh : a->b va c->d
                 *   THEM hai canh: a->c va b->d
                 *   cac canh BEN TRONG doan giu nguyen do dai, chi doi chieu di
                 *
                 * Nen khong can tinh lai ca tuyen, chi can so bon canh:
                 *   delta = (canh moi) - (canh cu) = D[a][c] + D[b][d] - D[a][b] - D[c][d]
                 *
                 * VI SAO cac canh ben trong khong doi: vi khoang cach DOI XUNG,
                 * D[x][y] = D[y][x], di tu B toi C hay tu C toi B deu cung do dai.
                 *
                 * DIEU KIEN NAY KHONG TU NHIEN MA CO. Ma tran OSRM tho la bat doi xung
                 * (duong mot chieu, cam re trai), nen DistanceMatrixService bat buoc
                 * phai doi xung hoa bang trung binh cong o buoc B3 truoc khi giao ma
                 * tran cho ham nay. Bo buoc do thi cong thuc bon canh sai, va sai theo
                 * kieu te nhat: 2-opt TUONG la cai thien roi nhan phep dao, trong khi
                 * tuyen that dai them. Xem docs/03 muc 4 co vi du bang so.
                 */
                double delta = matrix[a][c] + matrix[b][d] - matrix[a][b] - matrix[c][d];

                // B12-B14
                if (delta < IMPROVEMENT_THRESHOLD) {
                    reverseSegment(points, i, j);
                    improved = true;
                }
            }
        }

        return improved;
    }

    /**
     * Dao nguoc doan tu vi tri i den vi tri j (ke ca hai dau), doi cho tu ngoai vao trong.
     *
     * DAY LA CAU TRA LOI CHO CAU HOI "lam sao em chac dao doan khong lam xe qua tai":
     * ham nay chi DOI CHO cac phan tu trong chinh danh sach do — khong them, khong bot,
     * khong chuyen diem sang tuyen khac. Tap diem cua tuyen y nguyen, nen tong kg va
     * tong m3 y nguyen. Rang buoc suc chua da thoa man o giai doan 1 thi tiep tuc thoa
     * man sau giai doan 2, khong can kiem lai lan nao.
     *
     * Chinh vi the ma hai giai doan tach roi nhau duoc.
     */
    private void reverseSegment(List<OptimizationPoint> points, int from, int to) {
        int left = from;
        int right = to;
        while (left < right) {
            OptimizationPoint temporary = points.get(left);
            points.set(left, points.get(right));
            points.set(right, temporary);
            left++;
            right--;
        }
    }
}
