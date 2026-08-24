package com.minhphu.mproute.service.optimization.heuristic;

import com.minhphu.mproute.service.optimization.OptimizationPoint;
import com.minhphu.mproute.service.optimization.OptimizationRoute;
import com.minhphu.mproute.service.optimization.RouteDistanceCalculator;

import java.util.List;

/**
 * Cheapest Insertion — chen mot diem giao vao mot tuyen sao cho quang duong tang them
 * it nhat (docs/03 muc 10).
 *
 * DUNG O DAU: xu ly su co (UC06). Mot xe hong giua duong, cac diem chua giao cua no
 * phai chuyen sang xe khac dang chay. Lop nay tra loi cau "chen vao cho nao trong tuyen
 * cua xe cuu thi it phat sinh km nhat".
 *
 * VI SAO KHONG DUNG OR-TOOLS CHO VIEC NAY, du OR-Tools la bo giai chinh: bai toan o day
 * la chen vao mot tuyen DANG CHAY DO — xe dang o giua duong, cac diem da giao roi thi
 * khong duoc dong toi. Dung lai rang buoc "khong chen vao qua khu" trong OR-Tools ton
 * cong hon la viet thang vong lap muoi may dong nay.
 *
 * PACKAGE NAY CAM IMPORT THU VIEN VRP NAO.
 */
public class CheapestInsertion {

    /**
     * Ket qua mot lan tim cho chen.
     *
     * @param extraDistance so met tuyen dai them neu chen, KHONG phai tong quang duong moi
     * @param position      vi tri chen trong danh sach diem cua tuyen; -1 nghia la khong chen duoc
     */
    public record InsertionCandidate(double extraDistance, int position) {

        public boolean isFeasible() {
            return position >= 0;
        }
    }

    /** Khong tim duoc cho nao chen — vi qua suc chua, hoac tuyen da chay het. */
    private static final InsertionCandidate NOT_FEASIBLE =
            new InsertionCandidate(Double.MAX_VALUE, -1);

    /**
     * Tim cho chen re nhat.
     *
     * @param route            tuyen cua xe se nhan them diem
     * @param newPoint         diem can chen
     * @param lastVisitedIndex vi tri diem CUOI CUNG xe da giao xong trong danh sach
     *                         route.getPoints(); truyen -1 khi xe con dang o kho
     * @param matrix           ma tran khoang cach da nap san, met
     */
    public InsertionCandidate findCheapestPosition(OptimizationRoute route,
                                                   OptimizationPoint newPoint,
                                                   int lastVisitedIndex,
                                                   double[][] matrix) {

        // Kiem suc chua TRUOC khi tinh km: xe khong con cho thi khong co vi tri nao hop le
        if (!fits(route, newPoint)) {
            return NOT_FEASIBLE;
        }

        List<OptimizationPoint> points = route.getPoints();
        int newIndex = newPoint.getMatrixIndex();

        double bestCost = Double.MAX_VALUE;
        int bestPosition = -1;

        /*
         * B4. CHI XET TU VI TRI HIEN TAI TRO DI — buoc quan trong nhat cua ca ham.
         *
         * Xe da di qua cac diem truoc do roi, chen vao truoc chung la chen vao QUA KHU:
         * ket qua tinh ra se dep hon nhung khong thuc hien duoc, tai xe khong quay nguoc
         * lai duoc nhung diem da giao xong.
         *
         * lastVisitedIndex = -1 (xe con o kho) thi vong lap bat dau tu 0, tuc duoc phep
         * chen len dau tuyen.
         */
        int firstAllowedPosition = Math.max(lastVisitedIndex + 1, 0);

        // position = points.size() nghia la chen vao CUOI, ngay truoc chang ve kho
        for (int position = firstAllowedPosition; position <= points.size(); position++) {

            /*
             * Hai diem ke hai ben cho dinh chen:
             *
             *      ... a -> [ diem moi ] -> b ...
             *
             *   a = diem ngay TRUOC cho chen; chen len dau tuyen thi a la KHO
             *   b = diem ngay SAU cho chen;   chen vao cuoi thi b la KHO
             */
            int before = (position == 0)
                    ? RouteDistanceCalculator.DEPOT_INDEX
                    : points.get(position - 1).getMatrixIndex();
            int after = (position == points.size())
                    ? RouteDistanceCalculator.DEPOT_INDEX
                    : points.get(position).getMatrixIndex();

            /*
             * B8. CHI PHI CHEN = hai canh moi tru canh cu bi cat.
             *
             *     cost = D[a][moi] + D[moi][b] - D[a][b]
             *
             * Truong hop tuyen dang rong: a va b deu la kho nen D[a][b] = 0, cost thanh
             * ca mot chuyen khu hoi 2 x D[kho][moi] — dung nhu thuc te.
             *
             * Chi phi luon >= 0 voi khoang cach thoa bat dang thuc tam giac: di vong qua
             * mot diem khong bao gio ngan hon di thang.
             */
            double cost = matrix[before][newIndex] + matrix[newIndex][after] - matrix[before][after];

            // B10-B12. Chi thay khi RE HON that su, hoa nhau thi giu vi tri som hon
            if (cost < bestCost) {
                bestCost = cost;
                bestPosition = position;
            }
        }

        return bestPosition < 0 ? NOT_FEASIBLE : new InsertionCandidate(bestCost, bestPosition);
    }

    /**
     * Tim cho re nhat roi chen luon, va tinh lai tong quang duong cua tuyen.
     *
     * @return true neu chen duoc, false neu xe khong con suc chua
     */
    public boolean insertCheapest(OptimizationRoute route, OptimizationPoint newPoint,
                                  int lastVisitedIndex, double[][] matrix) {

        InsertionCandidate candidate = findCheapestPosition(route, newPoint, lastVisitedIndex, matrix);
        if (!candidate.isFeasible()) {
            return false;
        }

        /*
         * Khong dung route.addPoint() vi ham do chi them vao CUOI. O day phai chen vao
         * giua, nen tu them vao danh sach roi tu cong don nhu cau.
         */
        route.getPoints().add(candidate.position(), newPoint);
        route.setTotalWeight(route.getTotalWeight() + newPoint.getWeight());
        route.setTotalVolume(route.getTotalVolume() + newPoint.getVolume());

        RouteDistanceCalculator.updateTotalDistance(route, matrix);
        return true;
    }

    /**
     * Xe con cho duoc diem nay khong — kiem CA HAI rang buoc (CBR-03, CBR-04).
     *
     * Buoc nay de quen nhat khi xu ly su co: dang gap chuyen xe hong, chen diem vao
     * tuyen khac ma khong kiem lai suc chua thi sinh ra phuong an trong hop ly tren man
     * hinh nhung xe khong cho noi hang.
     */
    private boolean fits(OptimizationRoute route, OptimizationPoint newPoint) {
        if (route.getVehicle() == null) {
            return false;
        }
        if (route.getTotalWeight() + newPoint.getWeight() > route.getVehicle().getMaxWeight()) {
            return false;   // qua tai trong
        }
        if (route.getTotalVolume() + newPoint.getVolume() > route.getVehicle().getMaxVolume()) {
            return false;   // qua the tich thung xe
        }
        return true;
    }
}
