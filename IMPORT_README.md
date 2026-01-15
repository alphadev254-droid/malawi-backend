# Legacy Water Permit Data Import

This script imports legacy water permit data from CSV into the MySQL database.

## Prerequisites

1. Python 3.6 or higher
2. Access to the MySQL database
3. CSV file: `data upload_29.12.2025.csv` in the same directory

## Installation

Install required Python package:

```bash
pip install -r requirements_import.txt
```

Or install directly:

```bash
pip install pymysql
```

## Configuration

The script is pre-configured with these database credentials:
- Host: 91.108.121.232
- Port: 3306
- User: root
- Password: (empty)
- Database: ewaterdb

If you need to change these, edit the `DB_CONFIG` dictionary in `import_legacy_data.py`.

## Usage

Run the import script:

```bash
python import_legacy_data.py
```

## What the Script Does

The script will:

1. **Connect to the database** using the provided credentials
2. **Read the CSV file** and skip the header rows
3. **Process each row** and create:
   - Customer records (license holders)
   - Water source records
   - License applications
   - Licenses
   - License assessments (with calculated annual rental)
   - Water use records
   - Invoices (for unpaid years)
   - Payments (for paid years)
   - Link invoices to payments

4. **Handle data cleaning** including:
   - Parsing multiple date formats
   - Handling "N/A", "NA", empty values
   - Parsing volume values with various formats
   - Splitting multiple water uses (Irrigation, Domestic, etc.)
   - Years in arrear logic:
     - "NO" or "0" = Paid (creates invoice + payment)
     - "YES", "N/A", empty = Owed (creates unpaid invoice)

5. **Use default values** where needed:
   - Expiry date: "18/3/2026" if not provided
   - Renewal fee: 200,000 if not provided or invalid

## Output

The script will display:
- Progress updates every 50 records
- Error messages for any failed imports
- Final summary with success/failure counts

## Important Notes

- Each row is processed in a transaction - if any step fails, the entire row is rolled back
- Duplicate customers are detected by name and reused
- Duplicate water sources are detected by name and reused
- Invalid rows (like "MISSING FILES") are skipped automatically
- The script can be run multiple times - it will create duplicate records for the same data

## Troubleshooting

**Connection Error:**
```
Can't connect to MySQL server on '91.108.121.232'
```
- Check that the database server is accessible
- Verify the IP address, port, and credentials

**Module Not Found:**
```
ModuleNotFoundError: No module named 'pymysql'
```
- Install the required package: `pip install pymysql`

**File Not Found:**
```
FileNotFoundError: data upload_29.12.2025.csv
```
- Ensure the CSV file is in the same directory as the script

## CSV Column Mapping

| CSV Column | Database Table | Field |
|------------|---------------|-------|
| Licence holder | core_customer | name |
| Physical Address | core_customer | physical_address |
| PHONE CONTACTS | core_customer | mobile_number |
| EMAIL ADDRESS | core_customer | email_address |
| GRANT NUMBER | core_license | license_number |
| Expiry Date | core_license | expiration_date |
| Expected Annual rental | core_license_assessment | calculated_annual_rental |
| Volume (M3/Day) | core_license_water_use | amount_per_day_m3 |
| Source | core_water_source | name |
| Use | core_license_water_use | (creates multiple records) |
| Expected renewal fee | core_invoice | amount |
| Years in arrear (2023-2014) | core_invoice + core_application_payment | (creates records per year) |
