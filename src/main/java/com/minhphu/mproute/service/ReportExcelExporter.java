package com.minhphu.mproute.service;

import com.minhphu.mproute.dto.DeliveryReport;
import com.minhphu.mproute.dto.OptimizationReport;
import com.minhphu.mproute.dto.OrderReport;
import com.minhphu.mproute.dto.ProductBoxCount;
import com.minhphu.mproute.entity.StockMovement;
import com.minhphu.mproute.entity.enums.OrderStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Xuat ba bao cao ra file .xlsx (CN-08-05, BR08-05).
 *
 * MOI FILE TU GIAI THICH DUOC BO LOC CUA NO. Dong dau moi trang tinh ghi ro ten bao cao
 * va khoang ngay — file Excel roi khoi phan mem thi khong con man hinh nao noi ho no la
 * so lieu cua ky nao, ma "bao cao thang 8" nam trong thu muc Tai ve canh "bao cao thang 9"
 * thi nhin ten file khong phan biet noi.
 *
 * Xuat DUNG DU LIEU DANG HIEN TREN MAN HINH: controller goi lai chinh ham cua ReportService
 * voi dung tham so cua man hinh, roi day ket qua vao day. Khong viet lai phep tinh nao o
 * lop nay — viet lai la co ngay file Excel va man hinh khong khop nhau, va do la kieu sai
 * khong ai phat hien ra cho toi luc dem hai cai ra so.
 *
 * Dung XSSFWorkbook (giu ca file trong bo nho) chu khong dung SXSSFWorkbook (ghi dan ra
 * dia): bao cao lon nhat cua do an la 365 dong, chua toi mot phan tram nguong ma SXSSF
 * sinh ra de giai quyet.
 */
@Service
@Slf4j
public class ReportExcelExporter {

    private static final DateTimeFormatter DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // So kho ghi ca gio phut: trong mot ngay co nhieu phat sinh, thieu gio thi khong
    // doc ra duoc thu tu cac dong
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Kieu MIME cua .xlsx. Sai chuoi nay thi trinh duyet tai ve mot file khong mo duoc. */
    public static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    // ==================== RPT-01 ====================

    public byte[] exportOrderReport(OrderReport report, LocalDate fromDate, LocalDate toDate) {
        try (Workbook workbook = new XSSFWorkbook()) {

            Styles styles = new Styles(workbook);

            Sheet periods = workbook.createSheet("Đơn hàng theo kỳ");
            int rowIndex = writeTitle(periods, styles,
                    "Thống kê đơn hàng (" + report.grouping().getDisplayName().toLowerCase() + ")",
                    fromDate, toDate, 5);

            rowIndex = writeHeader(periods, styles, rowIndex,
                    "Kỳ", "Số đơn", "Khối lượng (kg)", "Thể tích (m³)", "Giá trị (đ)");

            for (OrderReport.PeriodRow row : report.rows()) {
                Row sheetRow = periods.createRow(rowIndex++);
                text(sheetRow, 0, row.label(), styles.text);
                number(sheetRow, 1, row.orderCount(), styles.integer);
                number(sheetRow, 2, row.totalWeight(), styles.decimal);
                number(sheetRow, 3, row.totalVolume(), styles.decimal3);
                number(sheetRow, 4, row.totalAmount(), styles.money);
            }

            OrderReport.Totals totals = report.totals();
            Row totalRow = periods.createRow(rowIndex);
            text(totalRow, 0, "Toàn kỳ", styles.totalText);
            number(totalRow, 1, totals.orderCount(), styles.totalInteger);
            number(totalRow, 2, totals.totalWeight(), styles.totalDecimal);
            number(totalRow, 3, totals.totalVolume(), styles.totalDecimal);
            number(totalRow, 4, totals.totalAmount(), styles.totalMoney);
            autoSize(periods, 5);

            Sheet products = workbook.createSheet("Số thùng theo sản phẩm");
            int productRow = writeTitle(products, styles, "Số thùng theo sản phẩm",
                    fromDate, toDate, 6);
            productRow = writeHeader(products, styles, productRow,
                    "Mã", "Sản phẩm", "Số thùng", "Khối lượng (kg)", "Thể tích (m³)", "Số đơn có mặt");

            for (ProductBoxCount product : report.products()) {
                Row sheetRow = products.createRow(productRow++);
                text(sheetRow, 0, product.productCode(), styles.text);
                text(sheetRow, 1, product.productName(), styles.text);
                number(sheetRow, 2, orZero(product.boxCount()), styles.integer);
                number(sheetRow, 3, orZero(product.totalWeight()), styles.decimal);
                number(sheetRow, 4, orZero(product.totalVolume()), styles.decimal3);
                number(sheetRow, 5, orZero(product.orderCount()), styles.integer);
            }
            autoSize(products, 6);

            return toBytes(workbook);
        } catch (IOException failed) {
            throw new UncheckedIOException("Không tạo được file Excel báo cáo đơn hàng", failed);
        }
    }

    // ==================== RPT-02 ====================

    public byte[] exportDeliveryReport(DeliveryReport report, LocalDate fromDate, LocalDate toDate) {
        try (Workbook workbook = new XSSFWorkbook()) {

            Styles styles = new Styles(workbook);

            Sheet sheet = workbook.createSheet("Tỷ lệ giao thành công");
            int rowIndex = writeTitle(sheet, styles, "Tỷ lệ giao thành công",
                    fromDate, toDate, 3);

            /*
             * Ghi thang cong thuc BR08-06 vao file. Nguoi mo file Excel khong co man hinh
             * ben canh de doc ghi chu, ma "92,1%" khong noi ro mau so la gi.
             */
            Row formulaRow = sheet.createRow(rowIndex++);
            text(formulaRow, 0,
                    "Tỷ lệ thành công = số đơn Đã giao ÷ tổng đơn đã phân tuyến. "
                            + "Đơn chờ duyệt và đơn đã hủy không nằm trong mẫu số.",
                    styles.note);
            sheet.addMergedRegion(new CellRangeAddress(formulaRow.getRowNum(), formulaRow.getRowNum(), 0, 2));
            rowIndex++;

            rowIndex = writeHeader(sheet, styles, rowIndex, "Trạng thái", "Số đơn", "Tỷ lệ (%)");

            for (DeliveryReport.StatusRow row : report.statusRows()) {
                Row sheetRow = sheet.createRow(rowIndex++);
                text(sheetRow, 0, statusLabel(row.status()), styles.text);
                number(sheetRow, 1, row.orderCount(), styles.integer);
                number(sheetRow, 2, row.share(), styles.decimal);
            }

            Row totalRow = sheet.createRow(rowIndex++);
            text(totalRow, 0, "Tổng đơn đã phân tuyến", styles.totalText);
            number(totalRow, 1, report.routedOrderCount(), styles.totalInteger);
            number(totalRow, 2, 100.0, styles.totalDecimal);

            if (report.cancelledCount() > 0) {
                rowIndex++;
                Row cancelledRow = sheet.createRow(rowIndex);
                text(cancelledRow, 0, "Đơn đã hủy (không tính vào mẫu số)", styles.text);
                number(cancelledRow, 1, report.cancelledCount(), styles.integer);
            }
            autoSize(sheet, 3);

            Sheet failures = workbook.createSheet("Lý do giao thất bại");
            int failureRow = writeTitle(failures, styles, "Phân tích lý do giao thất bại",
                    fromDate, toDate, 3);
            failureRow = writeHeader(failures, styles, failureRow, "Lý do", "Số lần", "Tỷ lệ (%)");

            for (DeliveryReport.FailureRow row : report.failureRows()) {
                Row sheetRow = failures.createRow(failureRow++);
                text(sheetRow, 0, row.reason().getDisplayName(), styles.text);
                number(sheetRow, 1, row.stopCount(), styles.integer);
                number(sheetRow, 2, row.share(), styles.decimal);
            }
            autoSize(failures, 3);

            return toBytes(workbook);
        } catch (IOException failed) {
            throw new UncheckedIOException("Không tạo được file Excel báo cáo tỷ lệ giao", failed);
        }
    }

    // ==================== RPT-03 ====================

    public byte[] exportOptimizationReport(OptimizationReport report) {
        try (Workbook workbook = new XSSFWorkbook()) {

            Styles styles = new Styles(workbook);

            Sheet sheet = workbook.createSheet("Hiệu quả tối ưu");
            int rowIndex = writeTitle(sheet, styles, "Báo cáo hiệu quả tối ưu",
                    report.fromDate(), report.toDate(), 10);

            rowIndex = writeHeader(sheet, styles, rowIndex,
                    "Ngày giao", "Số đơn", "Số tuyến", "Mốc A (km)", "Mốc B (km)", "Sau tối ưu (km)",
                    "Giảm so A (%)", "Giảm so B (%)", "Chuyến tiết kiệm", "Thuật toán");

            for (OptimizationReport.DayRow row : report.rows()) {
                rowIndex = writeOptimizationRow(sheet, styles, rowIndex, row);
            }

            OptimizationReport.PeriodTotals totals = report.totals();
            Row totalRow = sheet.createRow(rowIndex++);
            text(totalRow, 0, "Toàn kỳ", styles.totalText);
            number(totalRow, 1, totals.orderCount(), styles.totalInteger);
            number(totalRow, 2, totals.routeCount(), styles.totalInteger);
            number(totalRow, 3, toKilometres(totals.baselineADistance()), styles.totalDecimal);
            number(totalRow, 4, toKilometres(totals.baselineBDistance()), styles.totalDecimal);
            number(totalRow, 5, toKilometres(totals.optimizedDistance()), styles.totalDecimal);
            number(totalRow, 6, totals.reductionAPercent(), styles.totalDecimal);
            number(totalRow, 7, totals.reductionBPercent(), styles.totalDecimal);
            number(totalRow, 8, totals.tripsSaved(), styles.totalInteger);
            text(totalRow, 9, totals.dayCount() + " ngày tính vào trung bình", styles.totalText);

            rowIndex++;
            Row noteRow = sheet.createRow(rowIndex);
            text(noteRow, 0,
                    "Quãng đường đã gồm chặng cuối quay về kho. Mỗi ngày lấy lần chạy "
                            + "gần nhất. Phần trăm toàn kỳ tính từ tổng quãng đường, không phải "
                            + "trung bình cộng phần trăm từng ngày.",
                    styles.note);
            sheet.addMergedRegion(new CellRangeAddress(noteRow.getRowNum(), noteRow.getRowNum(), 0, 9));
            autoSize(sheet, 10);

            /*
             * Ngay dung khoang cach xap xi ra TRANG TINH RIENG, dung nhu tren man hinh
             * (BR08-03). Tron chung vao trang chinh la nguoi nhan file cong ca cot lai roi
             * bao cao mot con so da bi pha bang so lieu Haversine.
             */
            if (!report.approximateRows().isEmpty()) {
                Sheet approximate = workbook.createSheet("Ngày dùng khoảng cách xấp xỉ");
                int approximateRow = writeTitle(approximate, styles,
                        "Ngày dùng khoảng cách xấp xỉ, KHÔNG tính vào trung bình",
                        report.fromDate(), report.toDate(), 10);
                approximateRow = writeHeader(approximate, styles, approximateRow,
                        "Ngày giao", "Số đơn", "Số tuyến", "Mốc A (km)", "Mốc B (km)",
                        "Sau tối ưu (km)", "Giảm so A (%)", "Giảm so B (%)", "Chuyến tiết kiệm",
                        "Thuật toán");
                for (OptimizationReport.DayRow row : report.approximateRows()) {
                    approximateRow = writeOptimizationRow(approximate, styles, approximateRow, row);
                }
                autoSize(approximate, 10);
            }

            return toBytes(workbook);
        } catch (IOException failed) {
            throw new UncheckedIOException("Không tạo được file Excel báo cáo hiệu quả tối ưu", failed);
        }
    }

    private int writeOptimizationRow(Sheet sheet, Styles styles, int rowIndex,
                                     OptimizationReport.DayRow row) {
        Row sheetRow = sheet.createRow(rowIndex);
        text(sheetRow, 0, row.deliveryDate().format(DAY_MONTH_YEAR), styles.text);
        number(sheetRow, 1, row.orderCount(), styles.integer);
        number(sheetRow, 2, row.routeCount(), styles.integer);
        number(sheetRow, 3, toKilometres(row.baselineADistance()), styles.decimal);
        number(sheetRow, 4, toKilometres(row.baselineBDistance()), styles.decimal);
        number(sheetRow, 5, toKilometres(row.optimizedDistance()), styles.decimal);
        number(sheetRow, 6, row.reductionAPercent(), styles.decimal);
        number(sheetRow, 7, row.reductionBPercent(), styles.decimal);
        number(sheetRow, 8, row.tripsSaved(), styles.integer);
        text(sheetRow, 9, row.algorithm().getDisplayName(), styles.text);
        return rowIndex + 1;
    }

    // ==================== So kho ====================

    /**
     * Xuat so kho ra Excel.
     *
     * Dat o day chu khong viet mot lop xuat file rieng de dung lai nguyen bo dinh dang
     * va cac ham writeTitle / writeHeader / autoSize — hai bo dinh dang song song la hai
     * cho phai sua moi khi doi mot thu, va chung se lech nhau.
     *
     * Cot "Phát sinh" mang dau: +500 la hang vao kho, -220 la hang di. Nguoi mo file
     * khong co man hinh ben canh de doan dau, nen phai ghi ro trong chinh file.
     */
    public byte[] exportStockLedger(List<StockMovement> movements,
                                    LocalDate fromDate, LocalDate toDate) {
        try (Workbook workbook = new XSSFWorkbook()) {

            Styles styles = new Styles(workbook);
            Sheet sheet = workbook.createSheet("Sổ kho");

            int rowIndex = writeTitle(sheet, styles, "Sổ kho thành phẩm", fromDate, toDate, 8);
            rowIndex = writeHeader(sheet, styles, rowIndex,
                    "Thời điểm", "Mã SP", "Sản phẩm", "Loại phát sinh",
                    "Phát sinh (thùng)", "Tồn sau (thùng)", "Đơn hàng", "Người thực hiện");

            for (StockMovement movement : movements) {
                Row row = sheet.createRow(rowIndex++);
                text(row, 0, movement.getCreatedAt().format(DATE_TIME), styles.text);
                text(row, 1, movement.getProduct().getCode(), styles.text);
                text(row, 2, movement.getProduct().getName(), styles.text);
                text(row, 3, movement.getMovementType().getDisplayName(), styles.text);
                number(row, 4, movement.getSignedBoxCount(), styles.integer);
                number(row, 5, movement.getStockAfter(), styles.integer);
                text(row, 6, movement.getOrder() == null ? "" : movement.getOrder().getCode(), styles.text);
                text(row, 7, movement.getCreatedBy() == null
                        ? "Hệ thống" : movement.getCreatedBy().getFullName(), styles.text);
            }

            Row noteRow = sheet.createRow(rowIndex + 1);
            text(noteRow, 0, "Sổ kho chỉ ghi thêm, không sửa và không xóa dòng nào. "
                    + "Ghi sai được sửa bằng một dòng kiểm kê đảo lại, nên mọi dấu vết đều còn.",
                    styles.note);

            autoSize(sheet, 8);
            return toBytes(workbook);

        } catch (IOException e) {
            throw new IllegalStateException("Không xuất được sổ kho ra Excel", e);
        }
    }

    // ==================== Ham dung chung ====================

    /**
     * Hai dong dau moi trang tinh: ten bao cao va khoang ngay.
     *
     * @return chi so dong tiep theo con trong
     */
    private int writeTitle(Sheet sheet, Styles styles, String title,
                           LocalDate fromDate, LocalDate toDate, int columnCount) {

        Row titleRow = sheet.createRow(0);
        text(titleRow, 0, title, styles.title);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, columnCount - 1));

        Row rangeRow = sheet.createRow(1);
        text(rangeRow, 0, "Kỳ báo cáo: " + fromDate.format(DAY_MONTH_YEAR)
                + " – " + toDate.format(DAY_MONTH_YEAR), styles.note);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, columnCount - 1));

        return 3;   // chua mot dong trong giua tieu de va bang
    }

    private int writeHeader(Sheet sheet, Styles styles, int rowIndex, String... headings) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < headings.length; i++) {
            text(row, i, headings[i], styles.header);
        }
        return rowIndex + 1;
    }

    private void text(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private void number(Row row, int column, double value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void number(Row row, int column, BigDecimal value, CellStyle style) {
        number(row, column, value == null ? 0 : value.doubleValue(), style);
    }

    /**
     * Gian cot cho vua noi dung.
     *
     * autoSizeColumn phai goi SAU khi da ghi het du lieu — no do be rong theo cac o dang co,
     * goi truoc thi cot van hep. Cong them mot chut vi POI do hep hon Excel mot ty.
     */
    private void autoSize(Sheet sheet, int columnCount) {
        for (int column = 0; column < columnCount; column++) {
            sheet.autoSizeColumn(column);
            sheet.setColumnWidth(column, Math.min(sheet.getColumnWidth(column) + 700, 12000));
        }
    }

    private byte[] toBytes(Workbook workbook) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        workbook.write(buffer);
        return buffer.toByteArray();
    }

    /** Khoang cach luu bang met trong CSDL, bao cao hien bang km (quy uoc docs/10). */
    private double toKilometres(double metres) {
        return metres / 1000.0;
    }

    /** ROUTED trong bang trang thai cua RPT-02 gom ca don dang giao — dat ten cho ro. */
    private String statusLabel(OrderStatus status) {
        return status == OrderStatus.ROUTED ? "Đang giao / chờ giao" : status.getDisplayName();
    }

    private double orZero(Number value) {
        return value == null ? 0 : value.doubleValue();
    }

    /**
     * Cac dinh dang o dung chung trong mot workbook.
     *
     * POI GIOI HAN 64 000 CellStyle moi workbook, va moi lan goi createCellStyle() la mot
     * kieu moi — tao kieu ben trong vong lap ghi dong la bao cao vai nghin dong se dung
     * chuong trinh. Nen tao mot lan o day roi dung lai cho moi o.
     */
    private static final class Styles {

        private final CellStyle title;
        private final CellStyle note;
        private final CellStyle header;
        private final CellStyle text;
        private final CellStyle integer;
        private final CellStyle decimal;
        private final CellStyle decimal3;
        private final CellStyle money;
        private final CellStyle totalText;
        private final CellStyle totalInteger;
        private final CellStyle totalDecimal;
        private final CellStyle totalMoney;

        private Styles(Workbook workbook) {

            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 13);

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);

            Font noteFont = workbook.createFont();
            noteFont.setItalic(true);
            noteFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());

            title = workbook.createCellStyle();
            title.setFont(titleFont);

            note = workbook.createCellStyle();
            note.setFont(noteFont);
            note.setWrapText(true);

            header = workbook.createCellStyle();
            header.setFont(headerFont);
            header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setBorderBottom(BorderStyle.THIN);
            header.setAlignment(HorizontalAlignment.CENTER);

            text = workbook.createCellStyle();

            integer = numberStyle(workbook, "#,##0", false);
            decimal = numberStyle(workbook, "#,##0.0", false);
            decimal3 = numberStyle(workbook, "#,##0.000", false);
            money = numberStyle(workbook, "#,##0", false);

            totalText = workbook.createCellStyle();
            totalText.setFont(headerFont);
            totalText.setBorderTop(BorderStyle.THIN);

            totalInteger = numberStyle(workbook, "#,##0", true);
            totalInteger.setFont(headerFont);
            totalDecimal = numberStyle(workbook, "#,##0.0", true);
            totalDecimal.setFont(headerFont);
            totalMoney = numberStyle(workbook, "#,##0", true);
            totalMoney.setFont(headerFont);
        }

        private static CellStyle numberStyle(Workbook workbook, String format, boolean topBorder) {
            CellStyle style = workbook.createCellStyle();
            style.setDataFormat(workbook.createDataFormat().getFormat(format));
            if (topBorder) {
                style.setBorderTop(BorderStyle.THIN);
            }
            return style;
        }
    }

    /**
     * Ten file tai ve.
     *
     * Chi dung chu khong dau va gach ngang: ten file co dau di qua header HTTP la mot cho
     * de hong encoding, va file nam trong thu muc Tai ve cua may khach thi ten khong dau
     * van doc duoc. Ngay nam trong ten de hai ky bao cao khac nhau khong de len nhau.
     */
    public static String fileName(String reportCode, LocalDate fromDate, LocalDate toDate) {
        DateTimeFormatter compact = DateTimeFormatter.ofPattern("yyyyMMdd");
        return reportCode + "-" + fromDate.format(compact) + "-" + toDate.format(compact) + ".xlsx";
    }
}
