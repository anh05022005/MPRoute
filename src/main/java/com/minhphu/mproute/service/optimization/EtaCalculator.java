package com.minhphu.mproute.service.optimization;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tinh gio du kien den tung diem giao — ETA (CBR-13, docs/03 muc 9).
 *
 * CONG THUC:
 *     ETA(diem thu N) = gio xuat phat
 *                     + (quang duong tich luy toi diem N / toc do trung binh)
 *                     + (so cho dung da do xong truoc N) x thoi gian boc do moi diem
 *
 * Gio xuat phat do ben goi quyet dinh: gio ke hoach khi chua chay, gio roi kho THAT khi tai xe
 * da nhan hang (RoutingService.recalculate). Lop nay chi biet cong thuc, khong biet tuyen o
 * trang thai nao.
 *
 * Hai tham so toc do va thoi gian boc do KHONG hard-code trong lop nay ma nhan tu ben
 * ngoai, vi chung la tham so he thong (AVERAGE_SPEED = 30 km/h, LOADING_TIME = 15 phut)
 * cho quan tri sua duoc o man hinh cai dat. Sua tham so roi chay lai phan tuyen thi ETA
 * phai doi theo — do la cach kiem nhanh xem man hinh cai dat co that su tac dung khong.
 *
 * Lop nay khong doc CSDL va khong goi mang, nen test JUnit goi thang duoc.
 *
 * HAN CHE PHAI NOI THANG KHI VAN DAP: mo hinh nay dung MOT toc do trung binh cho ca
 * ngay, khong biet gio cao diem, khong biet tac duong. Voi Ha Noi thi ETA buoi chieu
 * chac chan lac quan hon thuc te. Muon dung hon phai lay duration cua OSRM theo tung
 * chang thay vi chia deu quang duong — day la huong phat trien ghi o chuong 5.
 */
public final class EtaCalculator {

    private EtaCalculator() {
        // lop tien ich, khong tao doi tuong
    }

    /**
     * Dat estimatedArrival cho tung diem cua tuyen.
     *
     * @param route                 tuyen da co thu tu diem cuoi cung (sau 2-opt hoac OR-Tools)
     * @param matrix                ma tran khoang cach da nap san, met
     * @param departureTime         gio xe roi kho
     * @param speedKmh              toc do trung binh, km/h (tham so AVERAGE_SPEED)
     * @param loadingMinutesPerStop so phut boc do tai MOI diem (tham so LOADING_TIME)
     */
    public static void assignEta(OptimizationRoute route, double[][] matrix,
                                 LocalDateTime departureTime,
                                 double speedKmh, int loadingMinutesPerStop) {
        assignEta(route, matrix, RouteDistanceCalculator.DEPOT_INDEX,
                departureTime, speedKmh, loadingMinutesPerStop);
    }

    /**
     * Nhu tren nhung xe XUAT PHAT TU MOT DIEM KHAC KHO.
     *
     * Dung khi xu ly su co (UC06): xe cuu ho di tu diem xay ra su co, con xe duoc chen them
     * diem thi dang o giua duong tai diem no vua giao xong. Ca hai truong hop deu khong bat
     * dau tu kho, nhung CONG THUC TINH GIO THI KHONG DOI — chi khac moc xuat phat.
     *
     * Tach tham so originIndex thay vi viet mot ham tinh gio thu hai: hai cong thuc song song
     * la som muon lech nhau, ma luc do gio bao cho khach va gio hien cho tai xe se khac nhau.
     *
     * @param originIndex chi so trong ma tran cua diem xe dang dung truoc khi chay chang dau
     */
    public static void assignEta(OptimizationRoute route, double[][] matrix,
                                 int originIndex,
                                 LocalDateTime departureTime,
                                 double speedKmh, int loadingMinutesPerStop) {

        List<OptimizationPoint> points = route.getPoints();
        if (points.isEmpty()) {
            return;
        }

        // Toc do 0 thi phep chia duoi kia ra vo cuc; chan tu day cho ro loi
        if (speedKmh <= 0) {
            throw new IllegalArgumentException("Toc do trung binh phai lon hon 0, dang la " + speedKmh);
        }

        // B1. Chang dau tien: diem xuat phat -> diem giao thu nhat
        double cumulativeMeters = matrix[originIndex][points.get(0).getMatrixIndex()];

        // So CHO DUNG da do hang xong truoc khi toi diem dang xet (xem B8)
        int completedLocations = 0;

        for (int i = 0; i < points.size(); i++) {

            // B3-B5. Tu diem thu hai tro di, cong them chang tu diem truoc sang diem nay
            if (i > 0) {
                int previousIndex = points.get(i - 1).getMatrixIndex();
                int currentIndex = points.get(i).getMatrixIndex();
                double legMeters = matrix[previousIndex][currentIndex];
                cumulativeMeters += legMeters;

                /*
                 * Chang 0 m nghia la xe KHONG di chuyen: hai don cua cung mot khach (cung toa
                 * do) dung canh nhau trong tuyen. Ngoai thuc te xe do hang ca hai don trong
                 * MOT lan dung, nen khong cong them mot lan boc do va hai don cung mot ETA.
                 * Chi khi xe that su roi cho cu (chang > 0) thi cho cu moi tinh la da do xong.
                 *
                 * Xet theo ma tran chu khong so toa do de routing-arrange.js (chi co ma tran)
                 * tinh ra dung cung con so khi xem truoc keo tha.
                 */
                if (legMeters > 0) {
                    completedLocations++;
                }
            }

            // B7. Thoi gian chay xe, tinh tu dau chuyen den luc TOI diem nay
            double travelMinutes = (cumulativeMeters / 1000.0) / speedKmh * 60.0;

            /*
             * B8. Thoi gian boc do CONG DON, theo so CHO DUNG chu khong theo so don.
             *
             * Den cho dung thu nhat thi xe chua do hang o dau ca (completedLocations = 0),
             * den cho dung thu ba thi da do xong hai cho truoc. Khi moi cho chi co mot don
             * thi completedLocations dung bang i, giong cong thuc goc cua docs/03.
             *
             * Hai bien nay docs/03 ban dau dat trung mot ten (docs/09 muc B4), chep thang
             * vao code la sai toan bo gio du kien nen o day tach han lam hai ten.
             */
            double totalLoadingMinutes = (double) completedLocations * loadingMinutesPerStop;

            // B10. Cong bang GIAY de khong mat phan le cua phut (26.6 phut = 26 phut 36 giay)
            long offsetSeconds = Math.round((travelMinutes + totalLoadingMinutes) * 60.0);
            points.get(i).setEstimatedArrival(departureTime.plusSeconds(offsetSeconds));
        }
    }

    /** Dat ETA cho moi tuyen cua mot ket qua phan tuyen — cac xe cung roi kho mot gio. */
    public static void assignEta(List<OptimizationRoute> routes, double[][] matrix,
                                 LocalDateTime departureTime,
                                 double speedKmh, int loadingMinutesPerStop) {
        for (OptimizationRoute route : routes) {
            assignEta(route, matrix, departureTime, speedKmh, loadingMinutesPerStop);
        }
    }

    /**
     * Gio xe du kien ve toi kho: ETA cua diem cuoi + boc do o diem cuoi + chang ve kho.
     *
     * Dung de canh bao dieu phoi vien khi mot tuyen keo dai qua gio lam viec, va de
     * hien "du kien xong luc ..." tren man hinh ket qua phan tuyen.
     */
    public static LocalDateTime estimatedReturnToDepot(OptimizationRoute route, double[][] matrix,
                                                       double speedKmh, int loadingMinutesPerStop) {

        List<OptimizationPoint> points = route.getPoints();
        if (points.isEmpty() || points.get(points.size() - 1).getEstimatedArrival() == null) {
            return null;
        }

        OptimizationPoint lastPoint = points.get(points.size() - 1);
        double returnMeters = matrix[lastPoint.getMatrixIndex()][RouteDistanceCalculator.DEPOT_INDEX];
        double returnMinutes = (returnMeters / 1000.0) / speedKmh * 60.0;

        // Xe phai do xong hang o diem cuoi roi moi chay ve, nen cong ca lan boc do do
        long offsetSeconds = Math.round((returnMinutes + loadingMinutesPerStop) * 60.0);
        return lastPoint.getEstimatedArrival().plusSeconds(offsetSeconds);
    }
}
