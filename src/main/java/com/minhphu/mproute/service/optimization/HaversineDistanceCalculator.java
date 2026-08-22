package com.minhphu.mproute.service.optimization;

/**
 * Khoang cach duong chim bay tren mat cau, nhan mot he so de xap xi duong di that.
 *
 * Day la PHUONG AN DU PHONG BAT BUOC (CBR-11): OSRM chet thi he thong van phai ra
 * duoc tuyen, chi la tuyen do bi danh dau xap xi (MSG-09) va bi loai khoi so lieu
 * trung binh trong bao cao de khong lam nhieu ket qua (BR08-03).
 *
 * Lop nay khong biet gi ve Spring: khong @Service, khong tiem phu thuoc. Nho vay
 * test JUnit dung thang duoc, khong can dung Spring context, khong can MySQL.
 *
 * VI SAO PHAI NHAN HE SO: duong chim bay luon ngan hon duong xe chay that, vi xe
 * phai di theo pho, vong qua song, ho, khu dan cu. He so 1.3 la gia tri thuc nghiem
 * thuong dung cho khu vuc do thi, doc tu tham so HAVERSINE_FACTOR nen doi duoc
 * ma khong phai bien dich lai.
 */
public class HaversineDistanceCalculator implements DistanceCalculator {

    // Ban kinh trung binh cua Trai Dat, don vi met
    private static final double BAN_KINH_TRAI_DAT = 6371000.0;

    public static final double HE_SO_QUY_DOI_MAC_DINH = 1.3;

    private final double heSoQuyDoi;

    public HaversineDistanceCalculator() {
        this(HE_SO_QUY_DOI_MAC_DINH);
    }

    public HaversineDistanceCalculator(double heSoQuyDoi) {
        this.heSoQuyDoi = heSoQuyDoi;
    }

    @Override
    public double khoangCach(OptimizationPoint diemA, OptimizationPoint diemB) {
        return duongChimBay(diemA.getLatitude(), diemA.getLongitude(),
                            diemB.getLatitude(), diemB.getLongitude()) * heSoQuyDoi;
    }

    /**
     * Cong thuc Haversine — khoang cach tren mat cau, chua nhan he so quy doi.
     *
     * Tach ra thanh ham tinh (static) de test kiem duoc rieng phan cong thuc,
     * khong lan voi he so.
     *
     * Giai thich tung dong de con giang lai truoc hoi dong:
     *   phi1, phi2        vi do cua hai diem, doi sang radian
     *   deltaPhi          chenh lech vi do, radian
     *   deltaLambda       chenh lech kinh do, radian
     *   a                 binh phuong nua day cung giua hai diem
     *   c                 goc o tam Trai Dat giua hai diem, radian
     *   R * c             do dai cung tron ung voi goc do — chinh la khoang cach
     *
     * Vi sao khong dung dinh ly cosin cho gon hon: voi hai diem gan nhau (vai km),
     * cos cua mot goc rat nho gan bang 1, phep tru hai so gan bang nhau lam mat
     * chu so co nghia. Haversine dung sin cua nua goc nen khong bi loi do.
     */
    public static double duongChimBay(double lat1, double lon1, double lat2, double lon2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = Math.toRadians(lat2 - lat1);
        double deltaLambda = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2)
                 + Math.cos(phi1) * Math.cos(phi2)
                   * Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return BAN_KINH_TRAI_DAT * c;
    }

    public double getHeSoQuyDoi() {
        return heSoQuyDoi;
    }

    @Override
    public String sourceName() {
        return "HAVERSINE";
    }
}
