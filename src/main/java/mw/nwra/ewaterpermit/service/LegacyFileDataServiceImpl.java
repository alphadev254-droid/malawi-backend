package mw.nwra.ewaterpermit.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import mw.nwra.ewaterpermit.dto.WRMISApprovedPermitDTO;
import mw.nwra.ewaterpermit.dto.WRMISPermitApplicationDTO;

@Service
public class LegacyFileDataServiceImpl implements LegacyFileDataService {

    private static final Logger log = LoggerFactory.getLogger(LegacyFileDataServiceImpl.class);

    // batch2_27_01_2026.csv — surface water, 2-row merged headers, data from row 3
    @Value("${wrmis.legacy.csv-path}")
    private String surfaceWaterCsvPath;

    // additional data (2).csv — drillers, single header row, data from row 2
    @Value("${wrmis.legacy.xlsx-path}")
    private String drillersCsvPath;

    private final List<WRMISApprovedPermitDTO> approvedPermits = new ArrayList<>();
    private final List<WRMISPermitApplicationDTO> permitApplications = new ArrayList<>();

    @PostConstruct
    public void loadFiles() {
        log.info("=== LEGACY FILE LOADER STARTING ===");
        log.info("Surface water CSV: [{}]", surfaceWaterCsvPath);
        log.info("Drillers CSV: [{}]", drillersCsvPath);

        File f1 = new File(surfaceWaterCsvPath);
        File f2 = new File(drillersCsvPath);
        log.info("Surface water CSV exists: {}, size: {} bytes", f1.exists(), f1.length());
        log.info("Drillers CSV exists: {}, size: {} bytes", f2.exists(), f2.length());

        try {
            loadSurfaceWaterCsv();
        } catch (Exception e) {
            log.error("Failed to load surface water CSV: {}", e.getMessage(), e);
        }
        try {
            loadDrillersCsv();
        } catch (Exception e) {
            log.error("Failed to load drillers CSV: {}", e.getMessage(), e);
        }

        log.info("=== LEGACY FILE LOADER DONE - approved permits: {}, permit applications: {} ===",
                approvedPermits.size(), permitApplications.size());
    }

    // ===================== SURFACE WATER CSV =====================
    // batch2_27_01_2026.csv
    // Row 1 + Row 2 merged as headers, data starts row 3
    // Columns: FILE, Licence holder, DISTRCIT, GRANT NUMBER, Duration (YRS),
    //          Expiry Date, Registration Date/Renewal, Volume (M3/Day, Source,
    //          Use, X, Y, LOCATION, TA, EMAIL ADDRESS, PHONE CONTACTS, MAILING ADDRESS

    private void loadSurfaceWaterCsv() throws Exception {
        File file = new File(surfaceWaterCsvPath);
        if (!file.exists()) {
            log.warn("Surface water CSV not found: {}", surfaceWaterCsvPath);
            return;
        }

        List<List<String>> rows = readCsvRows(file);
        log.info("Surface water CSV raw rows: {}", rows.size());
        if (rows.size() < 3) {
            log.warn("Surface water CSV has fewer than 3 rows, skipping");
            return;
        }

        // Merge row 1 and row 2 as headers
        List<String> row1 = rows.get(0);
        List<String> row2 = rows.get(1);
        List<String> headers = new ArrayList<>();
        int maxCols = Math.max(row1.size(), row2.size());
        for (int i = 0; i < maxCols; i++) {
            String h1 = i < row1.size() ? row1.get(i).trim() : "";
            String h2 = i < row2.size() ? row2.get(i).trim() : "";
            headers.add(!h2.isEmpty() ? h2 : h1);
        }
        log.info("Surface water CSV merged headers ({}): {}", headers.size(), headers);

        int loaded = 0;
        for (int i = 2; i < rows.size(); i++) {
            List<String> values = rows.get(i);
            if (values.isEmpty()) continue;

            Map<String, String> row = new LinkedHashMap<>();
            for (int j = 0; j < headers.size() && j < values.size(); j++) {
                row.put(headers.get(j), values.get(j).trim());
            }

            String fileRef = row.getOrDefault("FILE", "").trim();
            if (fileRef.isEmpty()) continue;

            try {
                approvedPermits.add(mapSurfaceWaterToApprovedPermit(row));
                permitApplications.add(mapSurfaceWaterToPermitApplication(row));
                loaded++;
            } catch (Exception e) {
                log.warn("Skipping surface water CSV row {}: {}", i + 1, e.getMessage());
            }
        }
        log.info("Surface water CSV loaded: {} records", loaded);
    }

    private WRMISApprovedPermitDTO mapSurfaceWaterToApprovedPermit(Map<String, String> row) {
        WRMISApprovedPermitDTO dto = new WRMISApprovedPermitDTO();

        String grantNumber = row.getOrDefault("GRANT NUMBER", "").trim();
        String fileRef = row.getOrDefault("FILE", "").trim();
        String category = row.getOrDefault("COUNT", "").trim();

        dto.setPermitNumber(!grantNumber.isEmpty() ? grantNumber : fileRef);
        dto.setApplicationId(fileRef);
        dto.setCategory(category);
        dto.setLicenseType(resolveCategoryToLicenseType(category, row.getOrDefault("Use ", "").trim()));
        dto.setLicenseStatus(resolveStatusFromExpiry(row.getOrDefault("Expiry Date", "")));
        dto.setLicenseVersion(1);

        dto.setHolderName(row.getOrDefault("Licence holder", "").trim());
        dto.setHolderEmail(row.getOrDefault("EMAIL ADDRESS", "").trim());
        dto.setHolderPhone(row.getOrDefault("PHONE CONTACTS", "").trim());
        dto.setHolderAddress(row.getOrDefault("MAILING ADDRESS", "").trim());
        dto.setHolderPhysicalAddress(row.getOrDefault("Physical Adress / Plot Number", "").trim());
        dto.setHolderContactPerson(row.getOrDefault("CONTACT PERSON", "").trim());
        dto.setHolderDistrict(row.getOrDefault("DISTRCIT", "").trim());

        String vol = row.getOrDefault("Volume (M3/Day", row.getOrDefault("Volume (M3/Day)", "")).trim();
        dto.setApprovedVolume(parseBigDecimal(vol));
        dto.setVolumeUnit("m3/day");
        dto.setWaterSource(row.getOrDefault("Source", "").trim());
        dto.setWaterUse(row.getOrDefault("Use ", "").trim());
        dto.setGranterName(row.getOrDefault("Name of Granter", "").trim());

        dto.setDateIssued(parseDate(row.getOrDefault("Registration Date/Renewal", "")));
        dto.setExpirationDate(parseDate(row.getOrDefault("Expiry Date", "")));

        String dur = row.getOrDefault("Duration (YRS)", "").trim();
        if (!dur.isEmpty() && !dur.equalsIgnoreCase("N/A")) dto.setValidityPeriod(dur + " years");

        dto.setSourceLatitude(row.getOrDefault("X", "").trim());
        dto.setSourceLongitude(row.getOrDefault("Y", "").trim());
        dto.setSourceVillage(row.getOrDefault("LOCATION", "").trim());
        dto.setSourceTA(row.getOrDefault("TA", "").trim());
        dto.setSourceDistrict(row.getOrDefault("DISTRCIT", "").trim());

        return dto;
    }

    private WRMISPermitApplicationDTO mapSurfaceWaterToPermitApplication(Map<String, String> row) {
        WRMISPermitApplicationDTO dto = new WRMISPermitApplicationDTO();

        String fileRef = row.getOrDefault("FILE", "").trim();
        String grantNumber = row.getOrDefault("GRANT NUMBER", "").trim();
        String category = row.getOrDefault("COUNT", "").trim();

        dto.setApplicationId(fileRef);
        dto.setApplicationNumber(!grantNumber.isEmpty() ? grantNumber : fileRef);
        dto.setApplicationType("NEW");
        dto.setCategory(category);
        dto.setLicenseType(resolveCategoryToLicenseType(category, row.getOrDefault("Use ", "").trim()));
        dto.setApplicationStatus("APPROVED");

        dto.setApplicantName(row.getOrDefault("Licence holder", "").trim());
        dto.setApplicantEmail(row.getOrDefault("EMAIL ADDRESS", "").trim());
        dto.setApplicantPhone(row.getOrDefault("PHONE CONTACTS", "").trim());
        dto.setApplicantAddress(row.getOrDefault("MAILING ADDRESS", "").trim());
        dto.setApplicantPhysicalAddress(row.getOrDefault("Physical Adress / Plot Number", "").trim());
        dto.setApplicantContactPerson(row.getOrDefault("CONTACT PERSON", "").trim());
        dto.setApplicantDistrict(row.getOrDefault("DISTRCIT", "").trim());
        dto.setApplicantTA(row.getOrDefault("TA", "").trim());

        String vol = row.getOrDefault("Volume (M3/Day", row.getOrDefault("Volume (M3/Day)", "")).trim();
        dto.setRequestedVolume(parseBigDecimal(vol));
        dto.setVolumeUnit("m3/day");
        dto.setWaterSource(row.getOrDefault("Source", "").trim());
        dto.setWaterUse(row.getOrDefault("Use ", "").trim());
        dto.setGranterName(row.getOrDefault("Name of Granter", "").trim());

        String dur = row.getOrDefault("Duration (YRS)", "").trim();
        if (!dur.isEmpty() && !dur.equalsIgnoreCase("N/A")) {
            try { dto.setPermitDuration(Double.parseDouble(dur)); } catch (Exception ignored) {}
        }

        dto.setDateSubmitted(parseDate(row.getOrDefault("Registration Date/Renewal", "")));
        dto.setExpiryDate(parseDate(row.getOrDefault("Expiry Date", "")));

        dto.setSourceLatitude(row.getOrDefault("X", "").trim());
        dto.setSourceLongitude(row.getOrDefault("Y", "").trim());
        dto.setSourceVillage(row.getOrDefault("LOCATION", "").trim());
        dto.setSourceTA(row.getOrDefault("TA", "").trim());
        dto.setSourceDistrict(row.getOrDefault("DISTRCIT", "").trim());

        return dto;
    }

    // ===================== DRILLERS CSV =====================
    // additional data (2).csv
    // Single header row, data starts row 2
    // Columns: NO., Client Name, Address, Contact, Email address, Location,
    //          Grant number, Registration Date, Expire date, Renewal fee,
    //          Application fees, Renewal Fees 2021-2022, Renewal Fees 2022-2023,
    //          Renewal Fees 2023-2024, Status

    private void loadDrillersCsv() throws Exception {
        File file = new File(drillersCsvPath);
        if (!file.exists()) {
            log.warn("Drillers CSV not found: {}", drillersCsvPath);
            return;
        }

        List<List<String>> rows = readCsvRows(file);
        log.info("Drillers CSV raw rows: {}", rows.size());
        if (rows.size() < 2) {
            log.warn("Drillers CSV has fewer than 2 rows, skipping");
            return;
        }

        // Single header row
        List<String> headers = new ArrayList<>();
        for (String h : rows.get(0)) headers.add(h.trim());
        log.info("Drillers CSV headers ({}): {}", headers.size(), headers);

        int loaded = 0;
        for (int i = 1; i < rows.size(); i++) {
            List<String> values = rows.get(i);
            if (values.isEmpty()) continue;

            Map<String, String> row = new LinkedHashMap<>();
            for (int j = 0; j < headers.size() && j < values.size(); j++) {
                row.put(headers.get(j), values.get(j).trim());
            }

            // headers are trimmed so key is "Client Name" not "Client Name "
            String name = row.getOrDefault("Client Name", "").trim();
            if (name.isEmpty()) continue;

            try {
                approvedPermits.add(mapDrillersToApprovedPermit(row));
                permitApplications.add(mapDrillersToPermitApplication(row));
                loaded++;
            } catch (Exception e) {
                log.warn("Skipping drillers CSV row {}: {}", i + 1, e.getMessage());
            }
        }
        log.info("Drillers CSV loaded: {} records", loaded);
    }

    private WRMISApprovedPermitDTO mapDrillersToApprovedPermit(Map<String, String> row) {
        WRMISApprovedPermitDTO dto = new WRMISApprovedPermitDTO();

        String grantNumber = row.getOrDefault("Grant number", "").trim();
        String no = row.getOrDefault("NO.", "").trim();
        dto.setPermitNumber(!grantNumber.isEmpty() ? grantNumber : no);
        dto.setApplicationId(!grantNumber.isEmpty() ? grantNumber : no);
        dto.setCategory("Drilling");
        dto.setLicenseType("DRILLING");
        dto.setLicenseStatus(resolveXlsxStatus(row.getOrDefault("Status", "Active")));
        dto.setLicenseVersion(1);

        dto.setHolderName(row.getOrDefault("Client Name", "").trim());
        dto.setHolderEmail(row.getOrDefault("Email address", "").trim());
        dto.setHolderPhone(row.getOrDefault("Contact", "").trim());
        dto.setHolderAddress(row.getOrDefault("Address", "").trim());
        dto.setHolderDistrict(row.getOrDefault("Location", "").trim());

        dto.setVolumeUnit("m3/day");
        dto.setDateIssued(parseDate(row.getOrDefault("Registration Date", "")));
        dto.setExpirationDate(parseDate(row.getOrDefault("Expire date", "")));

        dto.setSourceDistrict(row.getOrDefault("Location", "").trim());
        dto.setSourceVillage(row.getOrDefault("Location", "").trim());

        return dto;
    }

    private WRMISPermitApplicationDTO mapDrillersToPermitApplication(Map<String, String> row) {
        WRMISPermitApplicationDTO dto = new WRMISPermitApplicationDTO();

        String no = row.getOrDefault("NO.", "").trim();
        String grantNumber = row.getOrDefault("Grant number", "").trim();
        String appId = !grantNumber.isEmpty() ? grantNumber : no;
        dto.setApplicationId(appId);
        dto.setApplicationNumber(appId);
        dto.setApplicationType("NEW");
        dto.setCategory("Drilling");
        dto.setLicenseType("DRILLING");
        dto.setApplicationStatus(resolveXlsxStatus(row.getOrDefault("Status", "Active")));

        dto.setApplicantName(row.getOrDefault("Client Name", "").trim());
        dto.setApplicantEmail(row.getOrDefault("Email address", "").trim());
        dto.setApplicantPhone(row.getOrDefault("Contact", "").trim());
        dto.setApplicantAddress(row.getOrDefault("Address", "").trim());
        dto.setApplicantDistrict(row.getOrDefault("Location", "").trim());

        dto.setVolumeUnit("m3/day");
        dto.setDateSubmitted(parseDate(row.getOrDefault("Registration Date", "")));
        dto.setExpiryDate(parseDate(row.getOrDefault("Expire date", "")));
        dto.setSourceDistrict(row.getOrDefault("Location", "").trim());
        dto.setSourceVillage(row.getOrDefault("Location", "").trim());

        return dto;
    }

    // ===================== CSV READER =====================

    private List<List<String>> readCsvRows(File file) {
        for (String charset : new String[]{"UTF-8", "windows-1252", "latin-1"}) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), Charset.forName(charset)))) {
                List<List<String>> rows = new ArrayList<>();
                String line;
                StringBuilder pending = null;
                while ((line = br.readLine()) != null) {
                    if (pending != null) {
                        pending.append("\n").append(line);
                        if (countQuotes(pending.toString()) % 2 == 0) {
                            rows.add(parseCsvLine(pending.toString()));
                            pending = null;
                        }
                    } else {
                        if (countQuotes(line) % 2 != 0) {
                            pending = new StringBuilder(line);
                        } else {
                            rows.add(parseCsvLine(line));
                        }
                    }
                }
                log.info("Read {} rows from {} using {}", rows.size(), file.getName(), charset);
                return rows;
            } catch (Exception e) {
                log.warn("Failed reading {} with {}: {}", file.getName(), charset, e.getMessage());
            }
        }
        return new ArrayList<>();
    }

    private int countQuotes(String s) {
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

    // ===================== HELPERS =====================

    private String resolveCategoryToLicenseType(String category, String use) {
        if (category != null && !category.isEmpty()) {
            String cat = category.toLowerCase().trim();
            if (cat.contains("ground")) return "GROUND_WATER";
            if (cat.contains("non") && cat.contains("consumptive")) return "SURFACE_WATER_NON_CONSUMPTIVE";
            if (cat.contains("association")) return "WATER_USER_ASSOCIATION";
            // "Surface Water" and "Surface_NonConsumptive_Users" fall through to use-based resolution
        }
        return resolveUseToLicenseType(use);
    }

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

    private String resolveStatusFromExpiry(String expiryDateStr) {
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
        if (lower.contains("renewed")) return "ACTIVE";
        return "ACTIVE";
    }

    private Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty() || dateStr.trim().equalsIgnoreCase("N/A")) return null;
        String[] formats = {"dd/MM/yyyy", "d/M/yyyy", "MM/dd/yyyy", "yyyy-MM-dd", "dd-MM-yyyy", "d/MM/yyyy", "dd/M/yyyy"};
        for (String fmt : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(fmt);
                sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                return sdf.parse(dateStr.trim());
            } catch (Exception ignored) {}
        }
        log.debug("Could not parse date: [{}]", dateStr);
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
        if (date == null) return dateFrom == null && dateTo == null;
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
