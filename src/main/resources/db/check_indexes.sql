-- Check existing indexes on the core tables
SHOW INDEX FROM core_license_application;
SHOW INDEX FROM sys_user_account;  
SHOW INDEX FROM core_license_type;
SHOW INDEX FROM core_application_status;
SHOW INDEX FROM core_application_step;

-- Check table sizes to understand the performance issue
SELECT 'core_license_application' as table_name, COUNT(*) as row_count FROM core_license_application
UNION ALL
SELECT 'sys_user_account', COUNT(*) FROM sys_user_account
UNION ALL  
SELECT 'core_license_type', COUNT(*) FROM core_license_type
UNION ALL
SELECT 'core_application_status', COUNT(*) FROM core_application_status
UNION ALL
SELECT 'core_application_step', COUNT(*) FROM core_application_step;

-- Analyze the query performance with EXPLAIN
EXPLAIN SELECT app.id,
    CONCAT(COALESCE(u.first_name, ''), ' ', COALESCE(u.last_name, '')),
    u.email_address,
    lt.name,
    lt.id,
    st.name,
    CASE WHEN app.application_type IS NOT NULL THEN app.application_type ELSE 'NEW' END,
    app.date_created,
    app.date_submitted,
    lt.application_fees,
    lt.license_fees,
    app.client_info,
    app.location_info,
    app.application_metadata,
    app.form_specific_data,
    0.0 as app_payment_amount,
    'PENDING' as app_payment_status,
    0.0 as license_payment_amount,
    'PENDING' as license_payment_status,
    app.application_step_id,
    step.name as step_name,
    step.sequence_number as step_sequence
FROM core_license_application app
LEFT JOIN sys_user_account u ON app.user_account_id = u.id
LEFT JOIN core_license_type lt ON app.license_type_id = lt.id
LEFT JOIN core_application_status st ON app.application_status_id = st.id
LEFT JOIN core_application_step step ON app.application_step_id = step.id
WHERE (NULL IS NULL OR st.name = NULL)
AND (NULL IS NULL OR lt.name = NULL)
AND (NULL IS NULL OR NULL = '' OR
     LOWER(CONCAT(u.first_name, ' ', u.last_name)) LIKE LOWER(CONCAT('%', NULL, '%')) OR
     LOWER(u.email_address) LIKE LOWER(CONCAT('%', NULL, '%')))
ORDER BY app.date_created DESC 
LIMIT 25;