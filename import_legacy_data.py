#!/usr/bin/env python3
"""
Legacy Water Permit Data Import Script

This script imports water permit data from CSV into the MySQL database.
It handles all data cleaning, validation, and creates all necessary records.

Usage: python import_legacy_data.py
"""

import csv
import pymysql
import uuid
from datetime import datetime
from decimal import Decimal
import re
import sys
import hashlib
import os

# Database Configuration
DB_CONFIG = {
    'host': '91.108.121.232',
    'port': 3306,
    'user': 'root',
    'password': 'brianndesa001',
    'database': 'ewaterdb',
    'charset': 'utf8mb4',
    'cursorclass': pymysql.cursors.DictCursor
}

# CSV File Path
CSV_FILE = 'data upload_29.12.2025.csv'
PROGRESS_FILE = 'import_progress.txt'

# Default Values
DEFAULT_RENEWAL_FEE = 200000.00
DEFAULT_EXPIRY_DATE = '18/3/2026'
SYSTEM_USER_ID = 'SYSTEM_IMPORT'

# Tracking
success_count = 0
error_count = 0
errors = []


def get_last_processed_row():
    """Get the last successfully processed row number"""
    try:
        if os.path.exists(PROGRESS_FILE):
            with open(PROGRESS_FILE, 'r') as f:
                return int(f.read().strip())
    except:
        pass
    return 0


def save_progress(row_number):
    """Save the last successfully processed row number"""
    try:
        with open(PROGRESS_FILE, 'w') as f:
            f.write(str(row_number))
    except Exception as e:
        print(f"Warning: Could not save progress: {e}")


def safe_execute(cursor, query, params, default_values=None):
    """
    Execute SQL - wrapper for future enhancements.
    """
    cursor.execute(query, params)
    return True


def clean_value(value):
    """Clean and normalize CSV values"""
    if value is None or value == '':
        return None
    value = value.strip()
    if value.upper() in ['N/A', 'NA', '-', '']:
        return None
    return value


def parse_renewal_fee(fee_str):
    """Parse renewal fee with default fallback"""
    if not fee_str or 'N/A' in str(fee_str).upper() or '-' in fee_str:
        return DEFAULT_RENEWAL_FEE
    try:
        cleaned = re.sub(r'[^0-9.]', '', fee_str)
        return float(cleaned) if cleaned else DEFAULT_RENEWAL_FEE
    except:
        return DEFAULT_RENEWAL_FEE


def parse_annual_rental(rental_str):
    """Parse annual rental, returns None for invalid values"""
    if not rental_str:
        return None
    rental_str = str(rental_str).lower()
    if 'not in use' in rental_str or 'need for inspection' in rental_str or 'n/a' in rental_str:
        return None
    try:
        cleaned = re.sub(r'[^0-9.]', '', rental_str)
        return Decimal(cleaned) if cleaned else None
    except:
        return None


def parse_volume(volume_str):
    """Parse volume with various formats"""
    if not volume_str or 'N/A' in str(volume_str).upper():
        return 0.0

    volume_str = str(volume_str).strip()

    # Handle values in parentheses: "30.2/68.2/54.5(153.0)"
    if '(' in volume_str:
        try:
            start = volume_str.index('(') + 1
            end = volume_str.index(')')
            in_parens = volume_str[start:end]
            cleaned = re.sub(r'[^0-9.]', '', in_parens)
            return float(cleaned) if cleaned else 0.0
        except:
            pass

    # Handle multiple values: "230, 230" or "80, 60, 260"
    if ',' in volume_str or '/' in volume_str:
        parts = re.split(r'[,/]', volume_str)
        try:
            cleaned = re.sub(r'[^0-9.]', '', parts[0].strip())
            return float(cleaned) if cleaned else 0.0
        except:
            return 0.0

    # Standard parsing
    try:
        cleaned = re.sub(r'[^0-9.]', '', volume_str)
        return float(cleaned) if cleaned else 0.0
    except:
        return 0.0


def parse_water_uses(use_str):
    """Parse water uses - can have multiple separated by comma, &, or /"""
    uses = []
    if not use_str or 'N/A' in str(use_str).upper():
        return uses

    # Split by comma, ampersand, OR slash
    parts = re.split(r'[,&/]', use_str)
    for part in parts:
        cleaned = part.strip()
        if cleaned:
            uses.append(cleaned)

    return uses


def is_paid(year_value):
    """Check if year was paid (NO or 0) or owed (YES, N/A, empty)"""
    if not year_value or str(year_value).strip() == '':
        return False  # Empty = they owe
    val = str(year_value).strip().upper()
    return val in ['NO', '0']


def parse_duration(duration_str):
    """Parse permit duration"""
    if not duration_str or 'N/A' in str(duration_str).upper():
        return None
    try:
        cleaned = re.sub(r'[^0-9.]', '', str(duration_str))
        return float(cleaned) if cleaned else None
    except:
        return None


def parse_date(date_str):
    """Parse date with multiple format support"""
    if not date_str or 'N/A' in str(date_str).upper():
        return None

    date_str = str(date_str).strip()
    formats = ['%d/%m/%Y', '%d/%m/%y', '%-d/%-m/%Y', '%-d/%-m/%y']

    for fmt in formats:
        try:
            dt = datetime.strptime(date_str, fmt)
            # Handle 2-digit years
            if dt.year < 100:
                dt = dt.replace(year=dt.year + 2000)
            return dt.strftime('%Y-%m-%d')
        except:
            continue

    return None


def should_skip_row(license_holder):
    """Check if row should be skipped"""
    if not license_holder:
        return True
    upper = license_holder.upper()
    return 'MISSING FILES' in upper or 'SEE FILE' in upper or upper == 'N/A'


def find_or_create_customer(cursor, name, physical_address, phone, email, postal_address, district):
    """Find existing or create new user account for customer (applicant role)"""
    # Try to find existing user by email
    if email:
        cursor.execute("SELECT id FROM sys_user_account WHERE email_address = %s LIMIT 1", (email,))
        result = cursor.fetchone()
        if result:
            return result['id']

    # Try to find by company name
    cursor.execute("""
        SELECT id FROM sys_user_account
        WHERE company_registered_name = %s OR company_trading_name = %s
        LIMIT 1
    """, (name, name))
    result = cursor.fetchone()

    if result:
        return result['id']

    # Create new user account for this customer
    customer_id = str(uuid.uuid4())

    # Generate username from name (or use email)
    if email:
        username = email.split('@')[0][:50]
    else:
        username = re.sub(r'[^a-zA-Z0-9]', '', name.lower())[:50]

    if not username:
        username = f'legacy_{str(uuid.uuid4())[:8]}'

    # Check if username exists, make it unique
    cursor.execute("SELECT id FROM sys_user_account WHERE username = %s", (username,))
    if cursor.fetchone():
        username = f'{username}_{str(uuid.uuid4())[:8]}'

    # Create email if not provided
    if not email:
        email = f'{username}@legacy.import'

    # Create password hash (for legacy import - users should change this)
    password_hash = hashlib.sha256('ChangeMe123!'.encode()).hexdigest()

    # Get applicant role ID (fixed ID from your database)
    applicant_role_id = '308419ea-e8a5-11ef-b39b-c84bd6560e35'

    # Get default account status (Active)
    cursor.execute("SELECT id FROM sys_account_status WHERE name = 'Active' LIMIT 1")
    status_result = cursor.fetchone()
    account_status_id = status_result['id'] if status_result else None

    # Get district ID
    district_id = find_district(cursor, district)

    # Determine if this is a company or individual
    is_company = any(word in name.upper() for word in ['LTD', 'LIMITED', 'ESTATE', 'BOARD', 'COMPANY', 'CORPORATION', 'ASSOCIATION'])

    # Truncate phone number to fit database column (max 20 characters)
    if phone:
        phone = str(phone)[:20]

    # Insert user account
    safe_execute(cursor, """
        INSERT INTO sys_user_account (
            id, username, password, email_address,
            phone_number, postal_address,
            user_group_id, account_status_id,
            district_id,
            company_registered_name, company_trading_name,
            date_created
        ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, NOW())
    """, (
        customer_id, username, password_hash, email,
        phone, postal_address,
        applicant_role_id, account_status_id,
        district_id,
        name if is_company else None,  # company_registered_name
        name if is_company else None   # company_trading_name
    ))

    # Create activation record (already activated for legacy imports)
    activation_id = str(uuid.uuid4())
    token = str(uuid.uuid4()).replace('-', '')[:8]  # Simple 8-char token

    safe_execute(cursor, """
        INSERT INTO sys_user_account_activation (
            id, token, user_account_id, email_address,
            date_created, date_activated, registration_host
        ) VALUES (%s, %s, %s, %s, NOW(), NOW(), %s)
    """, (
        activation_id, token, customer_id, email, 'LEGACY_IMPORT'
    ))

    return customer_id


def find_or_create_water_source(cursor, source_name, category):
    """Find or create water source"""
    if not source_name:
        return None

    # Try to find existing
    cursor.execute("SELECT id FROM core_water_source WHERE name = %s LIMIT 1", (source_name,))
    result = cursor.fetchone()

    if result:
        return result['id']

    # Create new water source
    source_id = str(uuid.uuid4())
    type_id = find_or_create_water_source_type(cursor, category or 'Surface Water')

    safe_execute(cursor, """
        INSERT INTO core_water_source (id, name, description, water_source_type_id,
                                       date_created, date_updated)
        VALUES (%s, %s, %s, %s, NOW(), NOW())
    """, (source_id, source_name, f'Legacy import: {category}', type_id))

    return source_id


def find_or_create_water_source_type(cursor, category):
    """Find or create water source type"""
    cursor.execute("SELECT id FROM core_water_source_type WHERE name = %s LIMIT 1", (category,))
    result = cursor.fetchone()

    if result:
        return result['id']

    type_id = str(uuid.uuid4())
    safe_execute(cursor, """
        INSERT INTO core_water_source_type (id, name, date_created, date_updated)
        VALUES (%s, %s, NOW(), NOW())
    """, (type_id, category))

    return type_id


def find_district(cursor, district_name):
    """Find district by name"""
    if not district_name:
        return None

    cursor.execute("SELECT id FROM core_district WHERE name = %s LIMIT 1", (district_name,))
    result = cursor.fetchone()

    return result['id'] if result else None


def create_license_application(cursor, customer_id, water_source_id, district_id, location,
                               x_coord, y_coord, physical_address, duration, registration_date,
                               file_ref, gps_coordinates):
    """Create license application"""
    application_id = str(uuid.uuid4())

    metadata = {
        'fileReference': file_ref or '',
        'gpsCoordinates': gps_coordinates or '',
        'legacyImport': True
    }

    import json
    metadata_json = json.dumps(metadata)

    date_submitted = parse_date(registration_date)

    # Default license type ID (provided by user)
    license_type_id = '55bcc012-e93b-11ef-b39b-c84bd6560e35'

    safe_execute(cursor, """
        INSERT INTO core_license_application (
            id, owner_id, user_account_id, water_source_id, license_type_id,
            source_village, source_easting, source_northing, source_plot_number,
            permit_duration, date_submitted, application_metadata,
            application_type, application_priority, description,
            date_created, date_updated
        ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, NOW(), NOW())
    """, (
        application_id, customer_id, customer_id, water_source_id, license_type_id,
        location, x_coord, y_coord, physical_address, duration, date_submitted,
        metadata_json, 'LEGACY_IMPORT', 'NORMAL', 'Legacy data import'
    ))

    return application_id


def create_license(cursor, application_id, grant_number, expiry_date, registration_date):
    """Create license"""
    license_id = str(uuid.uuid4())

    # Use default expiry date if none provided
    if not expiry_date:
        expiry_date = DEFAULT_EXPIRY_DATE

    expiry = parse_date(expiry_date)
    issued = parse_date(registration_date)

    # Ensure expiry is never None - use fallback date
    if not expiry:
        expiry = parse_date(DEFAULT_EXPIRY_DATE)
        if not expiry:  # If still None, use a hardcoded fallback
            expiry = '2026-03-18'

    # Use current date as default if registration date is missing
    if not issued:
        issued = datetime.now().strftime('%Y-%m-%d')

    # Generate license number if none provided or truncate if too long
    if not grant_number:
        grant_number = f'LEGACY-{str(uuid.uuid4())[:8].upper()}'
    else:
        # Truncate to max 50 characters (adjust based on your column size)
        grant_number = str(grant_number)[:50]

    # Determine status
    status = 'EXPIRED' if expiry and expiry < datetime.now().strftime('%Y-%m-%d') else 'ACTIVE'

    safe_execute(cursor, """
        INSERT INTO core_license (
            id, license_application_id, license_number, expiration_date,
            date_issued, status, license_version, document_url,
            date_created, date_updated
        ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, NOW(), NOW())
    """, (license_id, application_id, grant_number, expiry, issued, status, 1, ''))

    return license_id


def create_assessment(cursor, application_id, annual_rental, volume):
    """Create license assessment"""
    rental = parse_annual_rental(annual_rental)
    if not rental or rental == 0:
        return  # Skip if no rental

    assessment_id = str(uuid.uuid4())
    volume_value = Decimal(str(parse_volume(volume)))

    safe_execute(cursor, """
        INSERT INTO core_license_assessment (
            id, license_application_id, calculated_annual_rental,
            rental_quantity, assessment_status, date_created, date_updated
        ) VALUES (%s, %s, %s, %s, %s, NOW(), NOW())
    """, (assessment_id, application_id, rental, volume_value, 'Completed'))


def create_water_uses(cursor, application_id, use, volume):
    """Create water use records"""
    uses = parse_water_uses(use)
    if not uses:
        return

    volume_value = parse_volume(volume)

    for use_name in uses:
        water_use_id = find_or_create_water_use(cursor, use_name)
        water_use_record_id = str(uuid.uuid4())

        safe_execute(cursor, """
            INSERT INTO core_license_water_use (
                id, license_application_id, water_use_id, amount_per_day_m3,
                description, date_created, date_updated
            ) VALUES (%s, %s, %s, %s, %s, NOW(), NOW())
        """, (water_use_record_id, application_id, water_use_id, volume_value, 'Legacy import'))


def find_or_create_water_use(cursor, use_name):
    """Find or create water use type"""
    cursor.execute("SELECT id FROM core_water_use WHERE name = %s LIMIT 1", (use_name,))
    result = cursor.fetchone()

    if result:
        return result['id']

    use_id = str(uuid.uuid4())
    safe_execute(cursor, """
        INSERT INTO core_water_use (id, name, date_created, date_updated)
        VALUES (%s, %s, NOW(), NOW())
    """, (use_id, use_name))

    return use_id


def create_invoices_and_payments(cursor, application_id, license_id, row, arrears_years, renewal_fee):
    """Create invoices and payments for years in arrear"""
    # Years start at column 24
    for i, year in enumerate(arrears_years):
        column_index = 24 + i

        if column_index >= len(row):
            break

        year_value = row[column_index]

        if is_paid(year_value):
            # Create PAID invoice + payment
            create_paid_invoice_and_payment(cursor, application_id, license_id, year, renewal_fee)
        else:
            # Create UNPAID invoice
            create_unpaid_invoice(cursor, application_id, year, renewal_fee)


def create_unpaid_invoice(cursor, application_id, year, renewal_fee):
    """Create unpaid/overdue invoice"""
    invoice_id = str(uuid.uuid4())
    invoice_number = f'INV-LEGACY-{year}-{application_id[:8].upper()}'

    safe_execute(cursor, """
        INSERT INTO core_invoice (
            id, invoice_number, invoice_type, invoice_status, amount, currency,
            issue_date, due_date, paid_date, description, license_application_id,
            payment_id, date_created, date_updated
        ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, NOW(), NOW())
    """, (
        invoice_id, invoice_number, 'LICENSE_RENEWAL_FEE', 'OVERDUE', renewal_fee, 'MWK',
        f'{year}-01-01', f'{year}-01-31', None,
        f'Annual rental for {year} (Legacy Import - Arrears)',
        application_id, None
    ))


def create_paid_invoice_and_payment(cursor, application_id, license_id, year, renewal_fee):
    """Create paid invoice with linked payment"""
    # Create payment first
    payment_id = str(uuid.uuid4())
    financial_year_id = find_or_create_financial_year(cursor, year)
    fees_type_id = find_or_create_fees_type(cursor, 'ANNUAL_RENTAL')

    safe_execute(cursor, """
        INSERT INTO core_application_payment (
            id, license_application_id, license_id, amount_paid, payment_status,
            payment_method, financial_year_id, fees_type_id, description, date_created, date_updated
        ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, NOW(), NOW())
    """, (payment_id, application_id, license_id, renewal_fee, 'COMPLETED',
          'LEGACY_IMPORT', financial_year_id, fees_type_id, 'Legacy import payment'))

    # Create invoice linked to payment
    invoice_id = str(uuid.uuid4())
    invoice_number = f'INV-LEGACY-{year}-{application_id[:8].upper()}'

    safe_execute(cursor, """
        INSERT INTO core_invoice (
            id, invoice_number, invoice_type, invoice_status, amount, currency,
            issue_date, due_date, paid_date, description, license_application_id,
            payment_id, date_created, date_updated
        ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, NOW(), NOW())
    """, (
        invoice_id, invoice_number, 'LICENSE_RENEWAL_FEE', 'PAID', renewal_fee, 'MWK',
        f'{year}-01-01', f'{year}-01-31', f'{year}-01-15',
        f'Annual rental for {year} (Legacy Import - Paid)',
        application_id, payment_id
    ))


def find_or_create_financial_year(cursor, year):
    """Find or create financial year"""
    # Try to find existing financial year by name pattern
    cursor.execute("SELECT id FROM core_financial_years WHERE name LIKE %s LIMIT 1", (f'%{year}%',))
    result = cursor.fetchone()

    if result:
        return result['id']

    # Create new financial year if not found
    year_id = str(uuid.uuid4())
    year_name = f'FY {year}'

    # Create date range for the year (July to June fiscal year)
    start_date = f'{year}-07-01'
    end_date = f'{int(year)+1}-06-30'

    safe_execute(cursor, """
        INSERT INTO core_financial_years (id, name, start_date, end_date, status, created_at)
        VALUES (%s, %s, %s, %s, %s, NOW())
    """, (year_id, year_name, start_date, end_date, 'CLOSED'))

    return year_id


def find_or_create_fees_type(cursor, type_name):
    """Find or create fees type"""
    cursor.execute("SELECT id FROM core_fees_type WHERE name = %s LIMIT 1", (type_name,))
    result = cursor.fetchone()

    if result:
        return result['id']

    type_id = str(uuid.uuid4())
    safe_execute(cursor, """
        INSERT INTO core_fees_type (id, name, date_created, date_updated)
        VALUES (%s, %s, NOW(), NOW())
    """, (type_id, type_name))

    return type_id


def process_row(cursor, row, row_number):
    """Process a single CSV row"""
    global success_count, error_count, errors

    # Extract CSV columns (0-indexed)
    category = clean_value(row[0]) if len(row) > 0 else None
    file_ref = clean_value(row[1]) if len(row) > 1 else None
    license_holder = clean_value(row[2]) if len(row) > 2 else None
    physical_address = clean_value(row[3]) if len(row) > 3 else None
    contact_person = clean_value(row[4]) if len(row) > 4 else None
    phone_contacts = clean_value(row[5]) if len(row) > 5 else None
    email_address = clean_value(row[6]) if len(row) > 6 else None
    mailing_address = clean_value(row[7]) if len(row) > 7 else None
    location = clean_value(row[8]) if len(row) > 8 else None
    x_coord = clean_value(row[9]) if len(row) > 9 else None
    y_coord = clean_value(row[10]) if len(row) > 10 else None
    district = clean_value(row[11]) if len(row) > 11 else None
    grant_number = clean_value(row[12]) if len(row) > 12 else None
    duration = clean_value(row[13]) if len(row) > 13 else None
    expiry_date = clean_value(row[14]) if len(row) > 14 else None
    registration_date = clean_value(row[15]) if len(row) > 15 else None
    gps_coordinates = clean_value(row[16]) if len(row) > 16 else None
    name_of_granter = clean_value(row[17]) if len(row) > 17 else None
    volume = clean_value(row[18]) if len(row) > 18 else None
    source = clean_value(row[19]) if len(row) > 19 else None
    use = clean_value(row[20]) if len(row) > 20 else None
    expected_annual_rental = clean_value(row[21]) if len(row) > 21 else None
    expected_renewal_fee = clean_value(row[22]) if len(row) > 22 else None

    # Skip invalid rows
    if should_skip_row(license_holder):
        return

    # Parse renewal fee with default
    renewal_fee = parse_renewal_fee(expected_renewal_fee)

    # Years in arrear
    arrears_years = ['2023', '2022', '2021', '2020', '2019',
                     '2018', '2017', '2016', '2015', '2014']

    try:
        # 1. Find or create customer
        customer_id = find_or_create_customer(
            cursor, license_holder, physical_address, phone_contacts,
            email_address, mailing_address, district
        )

        # 2. Find or create water source
        water_source_id = find_or_create_water_source(cursor, source, category)

        # 3. Find district
        district_id = find_district(cursor, district)

        # 4. Create license application
        application_id = create_license_application(
            cursor, customer_id, water_source_id, district_id, location,
            x_coord, y_coord, physical_address, parse_duration(duration),
            registration_date, file_ref, gps_coordinates
        )

        # 5. Create license
        license_id = create_license(
            cursor, application_id, grant_number, expiry_date, registration_date
        )

        # 6. Create assessment
        create_assessment(cursor, application_id, expected_annual_rental, volume)

        # 7. Create water uses
        create_water_uses(cursor, application_id, use, volume)

        # 8. Create invoices and payments
        create_invoices_and_payments(
            cursor, application_id, license_id, row, arrears_years, renewal_fee
        )

        success_count += 1

    except Exception as e:
        error_count += 1
        error_msg = f'Row {row_number}: {str(e)}'
        errors.append(error_msg)
        print(f'ERROR - {error_msg}')
        raise


def main():
    """Main execution function"""
    global success_count, error_count, errors

    print('=' * 50)
    print('Legacy Water Permit Data Import Script')
    print('=' * 50)
    print()

    connection = None

    try:
        # Connect to database
        print('Connecting to database...')
        connection = pymysql.connect(**DB_CONFIG)
        print('Connected successfully!\n')

        # Read CSV with proper encoding handling
        print(f'Reading CSV file: {CSV_FILE}')

        # Try different encodings
        encodings_to_try = ['utf-8', 'latin-1', 'cp1252', 'iso-8859-1', 'utf-8-sig']
        rows = None

        for encoding in encodings_to_try:
            try:
                with open(CSV_FILE, 'r', encoding=encoding) as file:
                    csv_reader = csv.reader(file)
                    rows = list(csv_reader)
                print(f'Successfully read CSV with encoding: {encoding}')
                break
            except UnicodeDecodeError:
                continue

        if rows is None:
            raise Exception('Could not read CSV file with any standard encoding')

        # Skip header rows (first 2)
        data_rows = rows[2:]
        print(f'Found {len(data_rows)} records in CSV file.\n')

        # Get resume point
        last_processed = get_last_processed_row()
        if last_processed > 0:
            print(f'Resuming from row {last_processed + 1}...\n')
        else:
            print('Starting fresh import...\n')

        # Process each row
        cursor = connection.cursor()

        for row_number, row in enumerate(data_rows, start=3):
            # Skip already processed rows
            if row_number <= last_processed:
                continue

            try:
                process_row(cursor, row, row_number)
                connection.commit()

                # Save progress after each successful row
                save_progress(row_number)

                if row_number % 50 == 0:
                    print(f'Processed {row_number - 2} records...')

            except Exception as e:
                connection.rollback()
                # Error already logged in process_row
                continue

        cursor.close()

        # Print summary
        print('\n' + '=' * 50)
        print('Import Summary')
        print('=' * 50)
        print(f'Total records processed: {success_count + error_count}')
        print(f'Successful imports: {success_count}')
        print(f'Failed imports: {error_count}')

        if errors:
            print('\nErrors:')
            for error in errors[:20]:  # Show first 20 errors
                print(f'  - {error}')
            if len(errors) > 20:
                print(f'  ... and {len(errors) - 20} more errors')

        print('=' * 50)
        print()

    except Exception as e:
        print(f'\nFATAL ERROR: {str(e)}')
        import traceback
        traceback.print_exc()
        sys.exit(1)

    finally:
        if connection:
            connection.close()
            print('Database connection closed.')


if __name__ == '__main__':
    main()
