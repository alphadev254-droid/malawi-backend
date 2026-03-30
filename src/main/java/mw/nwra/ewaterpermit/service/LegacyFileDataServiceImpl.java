package mw.nwra.ewaterpermit.service;

import mw.nwra.ewaterpermit.dto.WRMISApprovedPermitDTO;
import mw.nwra.ewaterpermit.dto.WRMISPermitApplicationDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LegacyFileDataServiceImpl implements LegacyFileDataService {

    private static final Logger log = LoggerFactory.getLogger(LegacyFileDataServiceImpl.class);

    @Value("${wrmis.legacy.xlsx-path}")
    private String xlsxPath;

    @Value("${wrmis.legacy.csv-path}")
    private String csvPath;

    private final List<WRMISApprovedPermitDTO> approvedPermits = new ArrayList<>();
    private final List<WRMISPermitApplicationDTO> permitApplications = new ArrayList<>();

    @PostConstruct
    public void loadFiles() {
        log.info("=== LEGACY FILE LOADER STARTING ===");
        log.info("CSV path configured: [{}]", csvPath);
        log.info("XLSX path configured: [{}]", xlsxPath);

        File csvFile = new File(csvPath);
        File xlsxFile = new File(xlsxPath);
        log.info("CSV file exists: {}, size: {} bytes", csvFile.exists(), csvFile.length());
        log.info("XLSX file exists: {}, size: {} bytes", xlsxFile.exists(), xlsxFile.length());

        try {
            loadCsv();
        } catch (Exception e) {
            log.error("Failed to load CSV file: {}", e.getMessage(), e);
        }
        try {
            loadXlsx();
        } catch (Exception e) {
            log.error("Failed to load XLSX file: {}", e.getMessage(), e);
        }
        log.info("=== LEGACY FILE LOADER DONE — approved permits: {}, permit applications: {} ===",
                approvedPermits.size(), permitApplications.size());
    }

    // ===================== CSV LOADING =====================

    private void loadCsv() throws Exception {
        File file = new File(csvPath);
        if (!file.exists()) {
            log.warn("CSV file not found: {}", csvPath);
            return;
        }

        // Try UTF-8 first, fall back to Windows-1252 for smart quotes
        List<List<String>> rows = readCsvRows(file);
        if (rows.size() < 3) return;

        // Merge row 1 and row 2 as headers (same pattern as import script)
        List<String> row1 = rows.get(0);
        List<String> row2 = rows.get(1);
        List<String> headers = new ArrayList<>();
        int maxCols = Math.max(row1.size(), row2.size());
        for (int i = 0; i < maxCols; i++) {
            String h1 = i < row1.size() ? row1.get(i).trim() : "";
            String h2 = i < row2.size() ? row2.get(i).trim() : "";
            headers.add(!h2.isEmpty() ? h2 : h1);
        }

        log.info("CSV headers ({}): {}", headers.size(), headers);

        int loaded = 0;
        // Data starts at row index 2 (row 3 in file)
        for (int i = 2; i < rows.size(); i++) {
            List<String> values = rows.get(i);
            if (values.isEmpty()) continue;

            Map<String, String> row = new LinkedHashMap<>();
            for (int j = 0; j < headers.size() && j < values.size(); j++) {
                row.put(headers.get(j), values.get(j).trim());
            }

            String file_ = row.getOrDefault("FILE", "").trim();
            if (file_.isEmpty()) continue;

            try {
                WRMISApprovedPermitDTO approved = mapCsvToApprovedPermit(row);
                approvedPermits.add(approved);

                WRMISPermitApplicationDTO application = mapCsvToPermitApplication(row);
                permitApplications.add(application);
                loaded++;
            } catch (Exception e) {
                log.warn("Skipping CSV row {}: {}", i + 1, e.getMessage());
            }
        }
        log.info("CSV loaded: {} records from {}", loaded, csvPath);
    }

    private List<List<String>> readCsvRows(File file) {
        // Try UTF-8 first, then Windows-1252
        for (String charset : new String[]{"UTF-8", "windows-1252"}) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), Charset.forName(charset)))) {
                List<List<String>> rows = new ArrayList<>();
                String line;
                StringBuilder pending = null;
                while ((line = br.readLine()) != null) {
                    if (pending != null) {
                        pending.append("\n").append(line);
                        if (countUnescapedQuotes(pending.toString()) % 2 == 0) {
                            rows.add(parseCsvLine(pending.toString()));
                            pending = null;
                        }
                    } else {
                        if (countUnescapedQuotes(line) % 2 != 0) {
                            pending = new StringBuilder(line);
                        } else {
                            rows.add(parseCsvLine(line));
                        }
                    }
                }
                return rows;
            } catch (Exception e) {
                log.warn("Failed reading CSV with {}: {}", charset, e.getMessage());
            }
        }
        return new ArrayList<>();
    }

    private int countUnescapedQuotes(String s) {
        int count = 0;
        for (char c : s.toCharArray()) if (c == '"') count++;
        return count;
    }

    private List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        fields.add(sb.toString());
        return fields;
    }

    // ===================== XLSX LOADING =====================

    private void loadXlsx() throws Exception {
        File file = new File(xlsxPath);
        if (!file.exists()) {
            log.warn("XLSX file not found: {}", xlsxPath);
            return;
        }

        try (Workbook wb = new XSSFWorkbook(new FileInputStream(file))) {
            for (int s = 0; s < wb.getNumberOfSheets(); s++) {
                Sheet sheet = wb.getSheetAt(s);
                String sheetName = sheet.getSheetName();
                log.info("Processing sheet: {}", sheetName);

                String licenseType = resolveSheetLicenseType(sheetName);

                // Row 0 = headers
                Row headerRow = sheet.getRow(0);
                if (headerRow == null) continue;
                List<String> headers = new ArrayList<>();
                for (Cell cell : headerRow) {
                    headers.add(getCellString(cell).trim());
                }

                for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;

                    Map<String, String> rowMap = new LinkedHashMap<>();
                    for (int c = 0; c < headers.size(); c++) {
                        Cell cell = row.getCell(c);
                        rowMap.put(headers.get(c), cell != null ? getCellString(cell).trim() : "");
                    }

                    // Skip empty rows
                    String clientName = rowMap.getOrDefault("Client Name ", "").trim();
                    String licenseHolder = rowMap.getOrDefault("Licence holder", "").trim();
                    String name = !clientName.isEmpty() ? clientName : licenseHolder;
                    if (name.isEmpty()) continue;

                    try {
                        WRMISApprovedPermitDTO approved = mapXlsxToApprovedPermit(rowMap, licenseType);
                        approvedPermits.add(approved);

                        WRMISPermitApplicationDTO application = mapXlsxToPermitApplication(rowMap, licenseType);
                        permitApplications.add(application);
                    } catch (Exception e) {
                        log.warn("Skipping XLSX sheet [{}] row {}: {}", sheetName, r + 1, e.getMessage());
                    }
                }
                log.info("Sheet [{}] done", sheetName);
            }
        }
        log.info("XLSX loaded from {}", xlsxPath);
    }

    private String resolveSheetLicenseType(String sheetName) {
        String lower = sheetName.toLowerCase();
        if (lower.contains("drill")) return "DRILLING";
        if (lower.contains("effluent")) return "EFFLUENT_DISCHARGE";
        if (lower.contains("unlicensed")) return "SURFACE_WATER_UNLICENSED";
        return "SURFACE_WATER";
    }

    private String getCellString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    Date d = cell.getDateCellValue();
                    return new SimpleDateFormat("dd/MM/yyyy").format(d);
                }
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val)) return String.valueOf((long) val);
                return String.valueOf(val);
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try { return String.valueOf(cell.getNumericCellValue()); }
                catch (Exception e) { return cell.getStringCellValue(); }
            default: return "";
        }
    }

    // ===================== CSV MAPPERS =====================

    private WRMISApprovedPermitDTO mapCsvToApprovedPermit(Map<String, String> row) {
        WRMISApprovedPermitDTO dto = new WRMISApprovedPermitDTO();

        // Grant number is the official permit number
        String grantNumber = row.getOrDefault("GRANT NUMBER", "").trim();
        String fileRef = row.getOrDefault("FILE", "").trim();
        dto.setPermitNumber(!grantNumber.isEmpty() ? grantNumber : fileRef);

        dto.setLicenseType(resolveUseToLicenseType(row.getOrDefault("Use ", "").trim()));
        dto.setLicenseStatus(resolveStatus(row.getOrDefault("Expiry Date", "")));
        dto.setLicenseVersion(1);
        dto.setApplicationId(fileRef);

        // Holder info
        dto.setHolderEmail(row.getOrDefault("EMAIL ADDRESS", "").trim());
        dto.setHolderPhone(row.getOrDefault("PHONE CONTACTS", "").trim());
        dto.setHolderAddress(row.getOrDefault("MAILING ADDRESS", "").trim());
        dto.setHolderDistrict(row.getOrDefault("DISTRCIT", "").trim());

        // Volume
        String vol = row.getOrDefault("Volume (M3/Day", row.getOrDefault("Volume (M3/Day)", "")).trim();
        dto.setApprovedVolume(parseBigDecimal(vol));
        dto.setVolumeUnit("m³");

        // Dates
        dto.setDateIssued(parseDate(row.getOrDefault("Registration Date/Renewal", "")));
        dto.setExpirationDate(parseDate(row.getOrDefault("Expiry Date", "")));

        // Duration → validity period
        String dur = row.getOrDefault("Duration (YRS)", "").trim();
        if (!dur.isEmpty() && !dur.equals("N/A")) dto.setValidityPeriod(dur + " years");

        // Location
        dto.setSourceLatitude(row.getOrDefault("X", "").trim());
        dto.setSourceLongitude(row.getOrDefault("Y", "").trim());
        dto.setSourceVillage(row.getOrDefault("LOCATION", "").trim());
        dto.setSourceDistrict(row.getOrDefault("DISTRCIT", "").trim());

        return dto;
    }

    private WRMISPermitApplicationDTO mapCsvToPermitApplication(Map<String, String> row) {
        WRMISPermitApplicationDTO dto = new WRMISPermitApplicationDTO();

        String fileRef = row.getOrDefault("FILE", "").trim();
        dto.setApplicationId(fileRef);
        dto.setApplicationNumber(fileRef);
        dto.setApplicationType("NEW");
        dto.setLicenseType(resolveUseToLicenseType(row.getOrDefault("Use ", "").trim()));
        dto.setApplicationStatus("APPROVED");

        // Applicant
        dto.setApplicantName(row.getOrDefault("Licence holder", "").trim());
        dto.setApplicantEmail(row.getOrDefault("EMAIL ADDRESS", "").trim());
        dto.setApplicantPhone(row.getOrDefault("PHONE CONTACTS", "").trim());
        dto.setApplicantAddress(row.getOrDefault("MAILING ADDRESS", "").trim());
        dto.setApplicantDistrict(row.getOrDefault("DISTRCIT", "").trim());

        // Volume
        String vol = row.getOrDefault("Volume (M3/Day", row.getOrDefault("Volume (M3/Day)", "")).trim();
        dto.setRequestedVolume(parseBigDecimal(vol));
        dto.setVolumeUnit("m³");

        // Duration
        String dur = row.getOrDefault("Duration (YRS)", "").trim();
        if (!dur.isEmpty() && !dur.equals("N/A")) {
            try { dto.setPermitDuration(Double.parseDouble(dur)); } catch (Exception ignored) {}
        }

        // Dates
        dto.setDateSubmitted(parseDate(row.getOrDefault("Registration Date/Renewal", "")));

        // Location
        dto.setSourceLatitude(row.getOrDefault("X", "").trim());
        dto.setSourceLongitude(row.getOrDefault("Y", "").trim());
        dto.setSourceVillage(row.getOrDefault("LOCATION", "").trim());
        dto.setSourceDistrict(row.getOrDefault("DISTRCIT", "").trim());

        return dto;
    }

    // ===================== XLSX MAPPERS =====================

    private WRMISApprovedPermitDTO mapXlsxToApprovedPermit(Map<String, String> row, String licenseType) {
        WRMISApprovedPermitDTO dto = new WRMISApprovedPermitDTO();

        String grantNumber = row.getOrDefault("Grant number", row.getOrDefault("GRANT NUMBER", "")).trim();
        String fileRef = row.getOrDefault("FILE", row.getOrDefault("COUNT", "")).trim();
        dto.setPermitNumber(!grantNumber.isEmpty() ? grantNumber : fileRef);

        dto.setLicenseType(licenseType);
        dto.setLicenseStatus(resolveXlsxStatus(row.getOrDefault("Status", "Active")));
        dto.setLicenseVersion(1);
        dto.setApplicationId(fileRef);

        // Holder
        String name = row.getOrDefault("Client Name ", row.getOrDefault("Licence holder", "")).trim();
        dto.setHolderEmail(row.getOrDefault("Email address", row.getOrDefault("EMAIL ADDRESS", "")).trim());
        dto.setHolderPhone(row.getOrDefault("Contact", row.getOrDefault("PHONE CONTACTS", "")).trim());
        dto.setHolderAddress(row.getOrDefault("Address", row.getOrDefault("MAILING ADDRESS", "")).trim());
        dto.setHolderDistrict(row.getOrDefault("Location", row.getOrDefault("DISTRCIT", "")).trim());

        // Volume
        String vol = row.getOrDefault("Volume (M3/Day", "").trim();
        dto.setApprovedVolume(parseBigDecimal(vol));
        dto.setVolumeUnit("m³");

        // Dates
        dto.setDateIssued(parseDate(row.getOrDefault("Registration Date", row.getOrDefault("Registration Date/Renewal", ""))));
        dto.setExpirationDate(parseDate(row.getOrDefault("Expire date", row.getOrDefault("Expired date", row.getOrDefault("Expiry Date", "")))));

        // Location
        dto.setSourceLatitude(row.getOrDefault("X", "").trim());
        dto.setSourceLongitude(row.getOrDefault("Y", "").trim());
        dto.setSourceVillage(row.getOrDefault("Location", row.getOrDefault("LOCATION", "")).trim());
        dto.setSourceDistrict(row.getOrDefault("Location", row.getOrDefault("DISTRCIT", "")).trim());

        return dto;
    }

    private WRMISPermitApplicationDTO mapXlsxToPermitApplication(Map<String, String> row, String licenseType) {
        WRMISPermitApplicationDTO dto = new WRMISPermitApplicationDTO();

        String fileRef = row.getOrDefault("FILE", row.getOrDefault("COUNT", "")).trim();
        dto.setApplicationId(fileRef);
        dto.setApplicationNumber(row.getOrDefault("Grant number", row.getOrDefault("GRANT NUMBER", fileRef)).trim());
        dto.setApplicationType("NEW");
        dto.setLicenseType(licenseType);
        dto.setApplicationStatus(resolveXlsxStatus(row.getOrDefault("Status", "Active")));

        String name = row.getOrDefault("Client Name ", row.getOrDefault("Licence holder", "")).trim();
        dto.setApplicantName(name);
        dto.setApplicantEmail(row.getOrDefault("Email address", row.getOrDefault("EMAIL ADDRESS", "")).trim());
        dto.setApplicantPhone(row.getOrDefault("Contact", row.getOrDefault("PHONE CONTACTS", "")).trim());
        dto.setApplicantAddress(row.getOrDefault("Address", row.getOrDefault("MAILING ADDRESS", "")).trim());
        dto.setApplicantDistrict(row.getOrDefault("Location", row.getOrDefault("DISTRCIT", "")).trim());

        String vol = row.getOrDefault("Volume (M3/Day", "").trim();
        dto.setRequestedVolume(parseBigDecimal(vol));
        dto.setVolumeUnit("m³");

        dto.setDateSubmitted(parseDate(row.getOrDefault("Registration Date", row.getOrDefault("Registration Date/Renewal", ""))));

        dto.setSourceLatitude(row.getOrDefault("X", "").trim());
        dto.setSourceLongitude(row.getOrDefault("Y", "").trim());
        dto.setSourceVillage(row.getOrDefault("Location", row.getOrDefault("LOCATION", "")).trim());
        dto.setSourceDistrict(row.getOrDefault("Location", row.getOrDefault("DISTRCIT", "")).trim());

        return dto;
    }

    // ===================== HELPERS =====================

    private String resolveUseToLicenseType(String use) {
        if (use == null || use.isEmpty()) return "SURFACE_WATER";
        String lower = use.toLowerCase().trim();
        if (lower.contains("irrigation")) return "IRRIGATION";
        if (lower.contains("domestic")) return "DOMESTIC";
        if (lower.contains("industrial")) return "INDUSTRIAL";
        if (lower.contains("public")) return "PUBLIC_WATER_SUPPLY";
        if (lower.contains("livestock")) return "LIVESTOCK";
        if (lower.contains("mining")) return "MINING";
        return "SURFACE_WATER";
    }

    private String resolveStatus(String expiryDateStr) {
        Date expiry = parseDate(expiryDateStr);
        if (expiry == null) return "ACTIVE";
        return expiry.before(new Date()) ? "EXPIRED" : "ACTIVE";
    }

    private String resolveXlsxStatus(String status) {
        if (status == null || status.isEmpty()) return "ACTIVE";
        String lower = status.toLowerCase().trim();
        if (lower.contains("active")) return "ACTIVE";
        if (lower.contains("expired") || lower.contains("expire")) return "EXPIRED";
        if (lower.contains("suspend")) return "SUSPENDED";
        return status.toUpperCase();
    }

    private Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty() || dateStr.trim().equalsIgnoreCase("N/A")) return null;
        String[] formats = {"dd/MM/yyyy", "MM/dd/yyyy", "yyyy-MM-dd", "d/M/yyyy", "dd-MM-yyyy"};
        for (String fmt : formats) {
            try {
                return new SimpleDateFormat(fmt).parse(dateStr.trim());
            } catch (Exception ignored) {}
        }
        return null;
    }

    private BigDecimal parseBigDecimal(String val) {
        if (val == null || val.isEmpty() || val.equalsIgnoreCase("N/A") || val.equalsIgnoreCase("NA")) return null;
        try {
            return new BigDecimal(val.replace(",", "").trim());
        } catch (Exception e) {
            return null;
        }
    }

    private boolean matchesDateRange(Date date, Date dateFrom, Date dateTo) {
        if (date == null) return false;
        if (dateFrom != null && date.before(dateFrom)) return false;
        if (dateTo != null && date.after(dateTo)) return false;
        return true;
    }

    // ===================== SERVICE METHODS =====================

    @Override
    public List<WRMISApprovedPermitDTO> getApprovedPermits(Date dateFrom, Date dateTo) {
        if (dateFrom == null && dateTo == null) return new ArrayList<>(approvedPermits);
        return approvedPermits.stream()
                .filter(p -> matchesDateRange(p.getDateIssued(), dateFrom, dateTo))
                .collect(Collectors.toList());
    }

    @Override
    public List<WRMISApprovedPermitDTO> getApprovedPermitsByDate(Date specificDate) {
        if (specificDate == null) return Collections.emptyList();
        LocalDate target = specificDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return approvedPermits.stream()
                .filter(p -> p.getDateIssued() != null)
                .filter(p -> {
                    LocalDate d = p.getDateIssued().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    return d.equals(target);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<WRMISApprovedPermitDTO> getApprovedPermitsByEmail(String email, Date dateFrom, Date dateTo) {
        if (email == null) return getApprovedPermits(dateFrom, dateTo);
        return approvedPermits.stream()
                .filter(p -> email.equalsIgnoreCase(p.getHolderEmail()))
                .filter(p -> matchesDateRange(p.getDateIssued(), dateFrom, dateTo))
                .collect(Collectors.toList());
    }

    @Override
    public WRMISApprovedPermitDTO getApprovedPermitByLicenseNumber(String licenseNumber) {
        if (licenseNumber == null) return null;
        return approvedPermits.stream()
                .filter(p -> licenseNumber.equalsIgnoreCase(p.getPermitNumber()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<WRMISPermitApplicationDTO> getPermitApplications(Date dateFrom, Date dateTo) {
        if (dateFrom == null && dateTo == null) return new ArrayList<>(permitApplications);
        return permitApplications.stream()
                .filter(p -> matchesDateRange(p.getDateSubmitted(), dateFrom, dateTo))
                .collect(Collectors.toList());
    }

    @Override
    public List<WRMISPermitApplicationDTO> getPermitApplicationsByDate(Date specificDate) {
        if (specificDate == null) return Collections.emptyList();
        LocalDate target = specificDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return permitApplications.stream()
                .filter(p -> p.getDateSubmitted() != null)
                .filter(p -> {
                    LocalDate d = p.getDateSubmitted().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    return d.equals(target);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<WRMISPermitApplicationDTO> getPermitApplicationsByEmail(String email, Date dateFrom, Date dateTo) {
        if (email == null) return getPermitApplications(dateFrom, dateTo);
        return permitApplications.stream()
                .filter(p -> email.equalsIgnoreCase(p.getApplicantEmail()))
                .filter(p -> matchesDateRange(p.getDateSubmitted(), dateFrom, dateTo))
                .collect(Collectors.toList());
    }

    @Override
    public int getTotalApprovedPermitsCount() {
        return approvedPermits.size();
    }

    @Override
    public int getTotalPermitApplicationsCount() {
        return permitApplications.size();
    }
}
