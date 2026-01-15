-- Diagnose the extreme performance issue

-- 1. Check if there are any locks or long-running queries
SHOW PROCESSLIST;

-- 2. Check table engine and integrity
SHOW CREATE TABLE core_license_application;
SHOW CREATE TABLE sys_user_account;
SHOW CREATE TABLE core_license_type;
SHOW CREATE TABLE core_application_status;
SHOW CREATE TABLE core_application_step;

-- 3. Analyze table statistics
ANALYZE TABLE core_license_application;
ANALYZE TABLE sys_user_account;
ANALYZE TABLE core_license_type; 
ANALYZE TABLE core_application_status;
ANALYZE TABLE core_application_step;

-- 4. Check for fragmentation
SHOW TABLE STATUS LIKE 'core_license_application';
SHOW TABLE STATUS LIKE 'sys_user_account';

-- 5. Force index usage and see execution plan
EXPLAIN SELECT app.id,
    CONCAT(COALESCE(u.first_name, ''), ' ', COALESCE(u.last_name, '')),
    u.email_address,
    lt.name,
    lt.id,
    st.name,
    COALESCE(app.application_type, 'NEW'),
    app.date_created,
    app.date_submitted,
    lt.application_fees,
    lt.license_fees,
    app.client_info,
    app.location_info,
    app.application_metadata,
    app.form_specific_data,
    0.0,
    'PENDING',
    0.0,
    'PENDING',
    app.application_step_id,
    COALESCE(step.name, ''),
    COALESCE(step.sequence_number, 0)
FROM core_license_application app
INNER JOIN sys_user_account u ON app.user_account_id = u.id
INNER JOIN core_license_type lt ON app.license_type_id = lt.id
INNER JOIN core_application_status st ON app.application_status_id = st.id
LEFT JOIN core_application_step step ON app.application_step_id = step.id
ORDER BY app.date_created DESC
LIMIT 25;

-- 6. Try the simplest possible query first
SELECT COUNT(*) FROM core_license_application;
SELECT app.id, app.date_created FROM core_license_application app ORDER BY app.date_created DESC LIMIT 5;

-- 7. Check for corrupted data or extremely large TEXT/BLOB fields
SELECT 
    app.id,
    LENGTH(COALESCE(app.client_info, '')) as client_info_length,
    LENGTH(COALESCE(app.location_info, '')) as location_info_length, 
    LENGTH(COALESCE(app.application_metadata, '')) as metadata_length,
    LENGTH(COALESCE(app.form_specific_data, '')) as form_data_length
FROM core_license_application app
ORDER BY (LENGTH(COALESCE(app.client_info, '')) + 
          LENGTH(COALESCE(app.location_info, '')) + 
          LENGTH(COALESCE(app.application_metadata, '')) + 
          LENGTH(COALESCE(app.form_specific_data, ''))) DESC
LIMIT 5;