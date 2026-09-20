package com.minhphu.mproute.config;

import com.minhphu.mproute.dto.OrderForm;
import com.minhphu.mproute.dto.OrderItemForm;
import com.minhphu.mproute.dto.OrderTotals;
import com.minhphu.mproute.entity.Customer;
import com.minhphu.mproute.entity.Driver;
import com.minhphu.mproute.entity.Order;
import com.minhphu.mproute.entity.Product;
import com.minhphu.mproute.entity.Route;
import com.minhphu.mproute.entity.RouteStop;
import com.minhphu.mproute.entity.User;
import com.minhphu.mproute.entity.Vehicle;
import com.minhphu.mproute.entity.enums.AlgorithmType;
import com.minhphu.mproute.entity.enums.EntityType;
import com.minhphu.mproute.entity.enums.FailureReason;
import com.minhphu.mproute.entity.enums.OrderStatus;
import com.minhphu.mproute.entity.enums.PaymentStatus;
import com.minhphu.mproute.entity.enums.RecordStatus;
import com.minhphu.mproute.entity.enums.Role;
import com.minhphu.mproute.entity.enums.RouteStatus;
import com.minhphu.mproute.entity.enums.StopStatus;
import com.minhphu.mproute.entity.enums.VehicleStatus;
import com.minhphu.mproute.repository.CustomerRepository;
import com.minhphu.mproute.repository.DriverRepository;
import com.minhphu.mproute.repository.OrderRepository;
import com.minhphu.mproute.repository.ProductRepository;
import com.minhphu.mproute.repository.RouteRepository;
import com.minhphu.mproute.repository.RouteStopRepository;
import com.minhphu.mproute.repository.UserRepository;
import com.minhphu.mproute.repository.VehicleRepository;
import com.minhphu.mproute.service.OrderService;
import com.minhphu.mproute.service.RoutePlanService;
import com.minhphu.mproute.service.RoutingService;
import com.minhphu.mproute.service.StatusHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/**
 * Sinh lich su giao hang cua muoi ngay da qua, de bao cao RPT-03 co du lieu that
 * (docs/08 muc "Hom qua va cac ngay truoc").
 *
 * ==========================================================================
 * DIEU QUAN TRONG NHAT VE LOP NAY: no KHONG che so.
 *
 * Cach lam nhanh la INSERT thang vao optimization_results vai dong quang duong
 * nhin cho hop ly. Tuyet doi khong duoc lam vay: docs/07 mo dau bang dung canh
 * bao do, va hoi dong chi can hoi "so nay do o dau" la khong tra loi duoc.
 *
 * Nen lop nay chay DUNG ca duong that ma dieu phoi vien di moi ngay:
 *
 *     tao don -> duyet don -> chay OR-Tools -> gan tai xe -> chot ke hoach -> giao
 *
 * Moi con so trong optimization_results deu do chinh thuat toan tinh ra, tren
 * ma tran OSRM that, dung nhu mot ngay lam viec binh thuong. Chay lai lan nua
 * cung ra dung ket qua do (bo sinh so ngau nhien dung hat giong co dinh).
 * ==========================================================================
 *
 * CACH CHAY — chi chay khi duoc goi ten, khong tu chay moi lan khoi dong:
 *
 *     mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--mproute.seed-history=true
 *
 * Can bat ca hai container truoc: mysql-8.0.44 va osrm-vietnam. Thieu OSRM thi van
 * chay nhung khoang cach la Haversine xap xi, va ca muoi ngay se bi RPT-03 xep vao
 * bang "khong tinh vao trung binh" (BR08-03) — tuc la seed xong mà bao cao van trong.
 *
 * CHAY LAI NHIEU LAN KHONG SAO: ngay nao da co don thi bo qua ngay do.
 *
 * MOT CHO KHONG THAT DUOC, da can nhac: cot status_history.changed_at ghi thoi diem
 * CHAY SEED chu khong phai thoi diem lich su. StatusHistoryService luon dat
 * LocalDateTime.now(), va sua chu ky cua no chi de phuc vu seed thi khong dang —
 * do la lop dung chung cua ca he thong. Cac moc thoi gian THUOC VE NGHIEP VU
 * (departed_at, completed_at, actual_arrival) thi deu dat dung ngay lich su.
 */
@Component
@ConditionalOnProperty(name = "mproute.seed-history", havingValue = "true")
@org.springframework.core.annotation.Order(3)   // sau CatalogSeeder: can co san tai khoan, khach, san pham, xe
@RequiredArgsConstructor
@Slf4j
public class HistorySeeder implements CommandLineRunner {

    /** So ngay lich su can sinh. docs/08 doi it nhat 10 ngay. */
    private static final int DAYS_TO_SEED = 10;

    /** Di lui toi da bay nhieu ngay de tim du DAYS_TO_SEED ngay chua co don nao. */
    private static final int MAX_LOOKBACK_DAYS = 30;

    /**
     * So don moi ngay. docs/08 giai thich rat ky vi sao phai 12-15 chu khong phai 5-8:
     * voi 3 xe thi 12-15 don cho ra tuyen 4-5 diem, luc do THU TU GIAO moi co cho tao ra
     * chenh lech. Tuyen 2 diem thi moc B luon ra dung 0% vi ma tran doi xung — cot "giam
     * so voi moc B" cua RPT-03 se toan so 0 ma khong phai do code sai.
     */
    private static final int MIN_ORDERS_PER_DAY = 12;
    private static final int MAX_ORDERS_PER_DAY = 15;

    /**
     * Chi dung toi bay nhieu phan tram suc chua doi xe moi ngay.
     *
     * Vi sao khong lap day 100%: gan day thi bai toan tro thanh xep thung gan hoan hao,
     * ma OR-Tools se phai bo lai vai don — luc do confirmPlan tu choi chot vi con don
     * chua xep (dung theo CN-03-14). Chua 75% van du cao de ty le lap day trong bao cao
     * nhin that, ma khong ngay nao bi ket.
     */
    private static final double CAPACITY_BUDGET = 0.75;

    /**
     * Hat giong co dinh cho bo sinh so ngau nhien.
     *
     * Nho no ma seed lai lan hai ra dung bo don cua lan mot, va con so trong bao cao doi
     * chieu lai duoc. Sinh ngau nhien that thi moi lan seed ra mot bang khac, chep vao
     * luan van la khong ai kiem chung duoc.
     */
    private static final long RANDOM_SEED = 20260827L;

    /** Gio xe roi kho va gio ve kho, chi de cac moc thoi gian lich su nhin hop ly. */
    private static final LocalTime DEPARTURE_TIME = LocalTime.of(7, 45);
    private static final int MINUTES_BACK_TO_DEPOT = 35;

    /**
     * Cu ba ngay thi co mot don giao that bai, de bao cao RPT-02 co du lieu (docs/08).
     * Doi ly do theo vong de bang "phan tich that bai theo ly do" khong chi co mot dong.
     */
    private static final int FAILURE_EVERY_N_DAYS = 3;
    private static final FailureReason[] FAILURE_REASONS = {
            FailureReason.CUSTOMER_ABSENT,
            FailureReason.CUSTOMER_REFUSED,
            FailureReason.WRONG_ADDRESS
    };

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;

    private final OrderService orderService;
    private final RoutingService routingService;
    private final RoutePlanService routePlanService;
    private final StatusHistoryService statusHistoryService;

    @Override
    public void run(String... args) {

        List<Customer> customers = customerRepository.findByStatus(RecordStatus.ACTIVE);
        List<Product> products = productRepository.findByStatus(RecordStatus.ACTIVE);
        List<Driver> drivers = driverRepository.findByStatus(RecordStatus.ACTIVE);

        if (customers.size() < MIN_ORDERS_PER_DAY || products.isEmpty() || drivers.isEmpty()) {
            log.warn("Bo qua seed lich su: can it nhat {} khach ACTIVE, mot san pham va mot tai xe. "
                            + "Dang co {} khach, {} san pham, {} tai xe",
                    MIN_ORDERS_PER_DAY, customers.size(), products.size(), drivers.size());
            return;
        }

        Long dispatcherId = dispatcherUserId();
        List<Vehicle> fleet = chooseFleet(drivers.size());
        if (fleet.isEmpty()) {
            log.warn("Bo qua seed lich su: khong co xe nao trong danh muc");
            return;
        }

        /*
         * Nho trang thai xe hien tai roi tra lai o cuoi.
         *
         * Ba trong bon xe dang MAINTENANCE / IN_USE — do la trang thai da dung san cho
         * KICH BAN DEMO (buoc 2 va 5b cua docs/08), khong duoc lam hong. Nhung phan tuyen
         * chi nhan xe AVAILABLE (CBR-05, BR03-02) nen phai muon tam trong luc seed.
         */
        Map<Long, VehicleStatus> statusBeforeSeeding = new LinkedHashMap<>();
        for (Vehicle vehicle : fleet) {
            statusBeforeSeeding.put(vehicle.getId(), vehicle.getStatus());
        }

        int seededDays = 0;
        try {
            releaseFleet(fleet);

            Random random = new Random(RANDOM_SEED);
            List<LocalDate> days = daysToSeed();
            for (int i = 0; i < days.size(); i++) {
                if (seedOneDay(days.get(i), customers, products, fleet, drivers, dispatcherId,
                        random, i % FAILURE_EVERY_N_DAYS == 1)) {
                    seededDays++;
                }
            }
        } finally {
            // Tra trang thai xe ve nhu cu DU CO LOI GIUA CHUNG, khong thi kich ban demo hong
            restoreFleet(statusBeforeSeeding);
        }

        log.info("Seed lich su xong: {} ngay moi duoc sinh. Mo /reports/optimization de xem RPT-03",
                seededDays);
    }

    /**
     * Muoi ngay lam viec de sinh, tinh lui tu hom qua tro ve truoc.
     *
     * BO QUA NGAY DA CO DON. Trong CSDL dang co san vai ngay do cac lan kiem thu tao ra
     * (17/08, 21/08, 26/08...), moi ngay chi vai don. Ghi de len chung thi hong mat cac
     * tinh huong dang phuc vu phep thu khac — tuyen dang chay, su co da xu ly. Nen di lui
     * xa them cho du muoi ngay SACH thay vi chen vao ngay da co.
     *
     * Tra ve theo thu tu tu cu den moi, de dong log doc theo dung chieu thoi gian.
     */
    private List<LocalDate> daysToSeed() {
        List<LocalDate> days = new ArrayList<>();
        for (int daysAgo = 1; daysAgo <= MAX_LOOKBACK_DAYS && days.size() < DAYS_TO_SEED; daysAgo++) {
            LocalDate day = LocalDate.now().minusDays(daysAgo);
            if (orderRepository.countByDeliveryDate(day) == 0) {
                days.add(day);
            }
        }
        java.util.Collections.reverse(days);
        return days;
    }

    // ==================== Mot ngay ====================

    /**
     * Sinh tron mot ngay lam viec, tu luc nhap don den luc xe ve kho.
     *
     * @return true neu ngay nay vua duoc sinh, false neu da co san tu truoc
     */
    private boolean seedOneDay(LocalDate day, List<Customer> customers, List<Product> products,
                               List<Vehicle> fleet, List<Driver> drivers, Long dispatcherId,
                               Random random, boolean includeFailedDelivery) {

        // Chay lai khong ghi de: ngay nao da co don thi de nguyen
        if (orderRepository.countByDeliveryDate(day) > 0) {
            log.info("Ngay {} da co don, bo qua", day);
            return false;
        }

        List<Order> orders = createApprovedOrders(day, customers, products, fleet, dispatcherId, random);
        if (orders.isEmpty()) {
            log.warn("Ngay {} khong tao duoc don nao", day);
            return false;
        }

        List<Long> vehicleIds = fleet.stream().map(Vehicle::getId).toList();
        RoutingService.RoutingOutcome outcome =
                routingService.optimize(day, vehicleIds, AlgorithmType.OR_TOOLS, dispatcherId);

        /*
         * Con don chua xep thi confirmPlan se tu choi chot (dung theo CN-03-14), va ngay do
         * se treo o giua chung. Dung lai va bao ro chu khong chay tiep: du lieu nua voi con
         * kho go hon la khong co gi.
         */
        if (outcome.hasUnassignedOrders()) {
            throw new IllegalStateException(String.format(
                    "Ngay %s con %d don chua xep duoc vao tuyen. Giam CAPACITY_BUDGET roi seed lai.",
                    day, outcome.unassignedOrders().size()));
        }

        assignDrivers(outcome.routes(), drivers);
        routePlanService.confirmPlan(day, dispatcherId);

        deliverEverything(day, dispatcherId, includeFailedDelivery);

        log.info("Ngay {}: {} don, {} tuyen, {} km sau toi uu, giam {}% so voi moc A",
                day, orders.size(), outcome.routes().size(),
                Math.round(outcome.metrics().getOptimizedDistance() / 100.0) / 10.0,
                Math.round(outcome.metrics().getReductionAPercent() * 10) / 10.0);
        return true;
    }

    // ==================== Don hang ====================

    /**
     * Tao va duyet don cua mot ngay.
     *
     * Moi khach toi da mot don mot ngay — dung nhu thuc te cua Minh Phu, va cung de tuyen
     * co dung mot diem dung cho moi khach.
     */
    private List<Order> createApprovedOrders(LocalDate day, List<Customer> customers,
                                             List<Product> products, List<Vehicle> fleet,
                                             Long dispatcherId, Random random) {

        int orderCount = Math.min(customers.size(),
                MIN_ORDERS_PER_DAY + random.nextInt(MAX_ORDERS_PER_DAY - MIN_ORDERS_PER_DAY + 1));

        List<Customer> shuffled = new ArrayList<>(customers);
        java.util.Collections.shuffle(shuffled, random);

        double weightBudget = fleet.stream().mapToDouble(Vehicle::getMaxWeight).sum() * CAPACITY_BUDGET;
        double volumeBudget = fleet.stream().mapToDouble(Vehicle::getMaxVolume).sum() * CAPACITY_BUDGET;

        double usedWeight = 0;
        double usedVolume = 0;
        List<Order> created = new ArrayList<>();

        for (int i = 0; i < orderCount; i++) {
            Customer customer = shuffled.get(i);

            OrderForm form = buildOrderForm(day, customer, products, random);
            OrderTotals totals = fitWithinBudget(form,
                    weightBudget - usedWeight, volumeBudget - usedVolume);
            if (totals == null) {
                continue;   // het cho cho don nay, ngay nay du don roi
            }

            /*
             * Kiem BR02-05 y het man hinh nhap don: phai ton tai MOT xe cho duoc ca kg lan m3.
             * Seed ma sinh ra don khong xe nao cho duoc thi do la du lieu khong the ton tai
             * that, dua vao bao cao la sai tu goc.
             */
            Optional<String> tooBig = orderService.checkCapacity(totals);
            if (tooBig.isPresent()) {
                log.warn("Ngay {} bo qua mot don vi {}", day, tooBig.get());
                continue;
            }

            Order order = orderService.createOrder(form, dispatcherId);
            orderService.approveOrder(order.getId(), dispatcherId);

            usedWeight += totals.weight();
            usedVolume += totals.volume();
            created.add(order);
        }
        return created;
    }

    /** Mot don: 1-3 dong hang, moi dong 3-12 thung, chon tu danh muc san pham that. */
    private OrderForm buildOrderForm(LocalDate day, Customer customer,
                                     List<Product> products, Random random) {

        OrderForm form = new OrderForm();
        form.setCustomerId(customer.getId());
        form.setDeliveryDate(day);
        form.setDeliveryAddress(customer.getAddress());
        form.setLatitude(customer.getLatitude());
        form.setLongitude(customer.getLongitude());
        form.setPaymentMethod(customer.getPaymentMethod());

        List<Product> pool = new ArrayList<>(products);
        java.util.Collections.shuffle(pool, random);

        int lineCount = 1 + random.nextInt(Math.min(3, pool.size()));
        List<OrderItemForm> items = new ArrayList<>();
        for (int line = 0; line < lineCount; line++) {
            items.add(new OrderItemForm(pool.get(line).getId(), 3 + random.nextInt(10)));
        }
        form.setItems(items);
        return form;
    }

    /**
     * Thu nho don cho vua phan suc chua con lai cua ngay.
     *
     * Chia doi so thung cho toi khi vua, chu khong bo han don: bo han thi ngay do it don
     * hon 12 va bang RPT-03 lai thua. Chia doi toi muc moi dong con mot thung ma van khong
     * vua thi moi tra ve null.
     *
     * @return tong kg/m3/tien cua don sau khi da chinh, hoac null neu khong con cho
     */
    private OrderTotals fitWithinBudget(OrderForm form, double weightLeft, double volumeLeft) {
        for (int attempt = 0; attempt < 6; attempt++) {
            OrderTotals totals = orderService.calculateTotals(form);
            if (totals.weight() <= weightLeft && totals.volume() <= volumeLeft) {
                return totals;
            }
            boolean shrank = false;
            for (OrderItemForm line : form.getItems()) {
                if (line.getBoxCount() != null && line.getBoxCount() > 1) {
                    line.setBoxCount(Math.max(1, line.getBoxCount() / 2));
                    shrank = true;
                }
            }
            if (!shrank) {
                return null;
            }
        }
        return null;
    }

    // ==================== Tai xe va giao hang ====================

    /** Moi tuyen mot tai xe khac nhau — mot tai xe chi nhan mot tuyen mot ngay (CBR-06). */
    private void assignDrivers(List<Route> routes, List<Driver> drivers) {
        if (routes.size() > drivers.size()) {
            throw new IllegalStateException(String.format(
                    "Sinh ra %d tuyen nhung chi co %d tai xe. Bot xe trong doi hinh seed.",
                    routes.size(), drivers.size()));
        }
        for (int i = 0; i < routes.size(); i++) {
            routePlanService.assignDriver(routes.get(i).getId(), drivers.get(i).getId());
        }
    }

    /**
     * Cho ca ngay chay xong: xe roi kho, giao het cac diem, roi ve kho.
     *
     * VI SAO KHONG GOI DELIVERYSERVICE: moi ham cua no deu bat dau bang "tuyen cua tai xe
     * dang dang nhap TRONG NGAY HOM NAY" (BR05-01) — dung cho man hinh tai xe that, nhung
     * khong dung duoc cho ngay da qua. Cac buoc chuyen trang thai duoi day di theo dung
     * DeliveryService.confirmPickup / markDelivered / markFailed / finishRouteIfDone;
     * sua luong trang thai o ben do thi phai sua ca ben nay.
     */
    private void deliverEverything(LocalDate day, Long dispatcherId, boolean includeFailedDelivery) {

        List<Route> routes = routeRepository.findWithStopsByDeliveryDateAndStatusIn(
                day, List.of(RouteStatus.CONFIRMED));
        routes.sort(Comparator.comparing(Route::getCode));

        boolean failureUsed = !includeFailedDelivery;

        for (Route route : routes) {

            // ---------- Xe roi kho: CONFIRMED -> IN_PROGRESS ----------
            route.setStatus(RouteStatus.IN_PROGRESS);
            route.setDepartedAt(LocalDateTime.of(day, DEPARTURE_TIME));
            routeRepository.save(route);
            statusHistoryService.recordChange(EntityType.ROUTE, route.getId(),
                    RouteStatus.CONFIRMED.name(), RouteStatus.IN_PROGRESS.name(),
                    dispatcherId, "Tai xe xac nhan nhan hang");

            // Hang len xe thi don sang DELIVERING, dung luong ROUTED -> DELIVERING -> DELIVERED
            for (RouteStop stop : route.getStops()) {
                Order order = stop.getOrder();
                if (order.getStatus() == OrderStatus.ROUTED) {
                    order.setStatus(OrderStatus.DELIVERING);
                    orderRepository.save(order);
                    statusHistoryService.recordChange(EntityType.ORDER, order.getId(),
                            OrderStatus.ROUTED.name(), OrderStatus.DELIVERING.name(),
                            dispatcherId, "Tai xe da nhan hang, dang tren duong giao");
                }
            }

            // ---------- Tung diem ----------
            LocalDateTime lastArrival = route.getDepartedAt();
            List<RouteStop> stops = new ArrayList<>(route.getStops());
            stops.sort(Comparator.comparing(RouteStop::getSequence));

            for (RouteStop stop : stops) {
                LocalDateTime arrival = (stop.getEstimatedArrival() != null)
                        ? stop.getEstimatedArrival()
                        : lastArrival.plusMinutes(20);
                lastArrival = arrival;

                boolean thisOneFails = !failureUsed && stop.getSequence() == stops.size();
                if (thisOneFails) {
                    failureUsed = true;
                    markStopFailed(stop, arrival, day, dispatcherId);
                } else {
                    markStopDelivered(stop, arrival, dispatcherId);
                }
            }

            // ---------- Xe ve kho: IN_PROGRESS -> COMPLETED ----------
            route.setStatus(RouteStatus.COMPLETED);
            route.setCompletedAt(lastArrival.plusMinutes(MINUTES_BACK_TO_DEPOT));
            routeRepository.save(route);
            statusHistoryService.recordChange(EntityType.ROUTE, route.getId(),
                    RouteStatus.IN_PROGRESS.name(), RouteStatus.COMPLETED.name(),
                    dispatcherId, "Diem cuoi da co ket qua");

            // Quen tra xe ve AVAILABLE thi ngay hom sau khong phan tuyen duoc nua
            Vehicle vehicle = route.getVehicle();
            if (vehicle.getStatus() == VehicleStatus.IN_USE) {
                vehicle.setStatus(VehicleStatus.AVAILABLE);
                vehicleRepository.save(vehicle);
            }
        }
    }

    /** Giao thanh cong: thu du so phai thu, don sang DELIVERED (BR05-14 cho don cong no). */
    private void markStopDelivered(RouteStop stop, LocalDateTime arrival, Long dispatcherId) {

        Order order = stop.getOrder();

        stop.setStatus(StopStatus.DELIVERED);
        stop.setActualArrival(arrival);
        if (order.getAmountToCollect().signum() > 0) {
            // Thu dung so phai thu: thu lech thi BR05-12 doi ly do va bao dieu phoi NTF-09
            stop.setCollectedAmount(order.getAmountToCollect());
        }
        routeStopRepository.save(stop);

        String previousStatus = order.getStatus().name();
        order.setStatus(OrderStatus.DELIVERED);
        order.setPaymentStatus(paymentStatusAfterDelivery(order));
        orderRepository.save(order);
        statusHistoryService.recordChange(EntityType.ORDER, order.getId(),
                previousStatus, OrderStatus.DELIVERED.name(), dispatcherId, "Tai xe giao thanh cong");
    }

    /** Giao that bai: don sang FAILED, tien giu nguyen UNPAID (BR05-13). */
    private void markStopFailed(RouteStop stop, LocalDateTime arrival, LocalDate day, Long dispatcherId) {

        FailureReason reason = FAILURE_REASONS[day.getDayOfMonth() % FAILURE_REASONS.length];

        Order order = stop.getOrder();

        stop.setStatus(StopStatus.FAILED);
        stop.setActualArrival(arrival);
        stop.setFailureReason(reason);
        routeStopRepository.save(stop);

        String previousStatus = order.getStatus().name();
        order.setStatus(OrderStatus.FAILED);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        orderRepository.save(order);
        statusHistoryService.recordChange(EntityType.ORDER, order.getId(),
                previousStatus, OrderStatus.FAILED.name(), dispatcherId,
                "Giao that bai: " + reason.getDisplayName());
    }

    /** Giong DeliveryService: don cong no thanh DEBT chu khong phai PAID (BR05-14). */
    private PaymentStatus paymentStatusAfterDelivery(Order order) {
        return switch (order.getPaymentMethod()) {
            case CREDIT -> PaymentStatus.DEBT;
            case CASH, PREPAID -> PaymentStatus.PAID;
        };
    }

    // ==================== Doi xe ====================

    /**
     * Chon doi xe cho ca dot seed: cac xe tai trong lon nhat, khong nhieu hon so tai xe.
     *
     * Khong duoc nhieu hon so tai xe vi moi tuyen phai co mot tai xe rieng (CBR-06) —
     * sinh ra bon tuyen ma chi co ba tai xe thi khong chot ke hoach duoc.
     */
    private List<Vehicle> chooseFleet(int driverCount) {
        List<Vehicle> vehicles = new ArrayList<>(vehicleRepository.findAll());
        vehicles.sort(Comparator.comparingDouble(Vehicle::getMaxWeight).reversed());
        return vehicles.stream().limit(driverCount).toList();
    }

    private void releaseFleet(List<Vehicle> fleet) {
        for (Vehicle vehicle : fleet) {
            vehicle.setStatus(VehicleStatus.AVAILABLE);
            vehicleRepository.save(vehicle);
        }
        log.info("Muon tam {} xe cho dot seed, se tra lai trang thai cu khi xong", fleet.size());
    }

    private void restoreFleet(Map<Long, VehicleStatus> statusBeforeSeeding) {
        for (Map.Entry<Long, VehicleStatus> entry : statusBeforeSeeding.entrySet()) {
            vehicleRepository.findById(entry.getKey()).ifPresent(vehicle -> {
                vehicle.setStatus(entry.getValue());
                vehicleRepository.save(vehicle);
            });
        }
        log.info("Da tra {} xe ve trang thai truoc khi seed", statusBeforeSeeding.size());
    }

    /** Nguoi "bam nut" trong toan bo dot seed, de created_by va status_history co chu. */
    private Long dispatcherUserId() {
        List<User> dispatchers = userRepository.findByRoleOrderByUsernameAsc(Role.DISPATCHER);
        return dispatchers.isEmpty() ? null : dispatchers.get(0).getId();
    }
}
