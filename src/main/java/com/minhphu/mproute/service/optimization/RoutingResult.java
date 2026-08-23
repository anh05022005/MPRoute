package com.minhphu.mproute.service.optimization;

import com.minhphu.mproute.entity.enums.AlgorithmType;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Toan bo ket qua mot lan chay thuat toan.
 *
 * Hai bo giai (OR-Tools va NN+2-opt tu cai dat) deu tra ve dung kieu nay, nho vay
 * man hinh phan tuyen va bang so sanh chuong 4 khong phai biet ben trong chay gi.
 *
 * unassignedPoints la truong QUAN TRONG chu khong phai truong phu: thuat toan khong
 * xep het don thi PHAI noi ra (CN-03-14), khong duoc im lang bo qua. Danh sach rong
 * nghia la gan het — khac han voi null.
 */
@Getter
@Setter
public class RoutingResult {

    private List<OptimizationRoute> routes = new ArrayList<>();

    /** Diem khong xep duoc vao xe nao. RONG chu khong bao gio null. */
    private List<OptimizationPoint> unassignedPoints = new ArrayList<>();

    /** Tong quang duong cua ket qua, met. Da gom chang ve kho cua tung tuyen. */
    private double optimizedDistance;

    /*
     * Hai moc so sanh, tinh o MetricsService (tuan 7):
     *   moc A — moi don mot chuyen khu hoi rieng tu kho, mo phong cach lam hien tai
     *   moc B — cung cach chia xe nhung giao theo thu tu nhan don, do RIENG dong gop
     *           cua viec sap thu tu
     * Luon bao cao ca hai: chi dua moc A de bi be "tu dung ra cach lam kem roi so voi no".
     */
    private double baselineADistance;
    private double baselineBDistance;

    private long executionTimeMs;
    private AlgorithmType algorithm;

    /** True khi ma tran phai dung Haversine vi OSRM khong chay (MSG-09, BR08-03). */
    private boolean approximateDistance;

    public void addRoute(OptimizationRoute route) {
        routes.add(route);
    }

    public int assignedPointCount() {
        int total = 0;
        for (OptimizationRoute route : routes) {
            total += route.getPoints().size();
        }
        return total;
    }

    public boolean hasUnassignedPoints() {
        return !unassignedPoints.isEmpty();
    }
}
