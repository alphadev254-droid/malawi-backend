-- Database indexes for JOIN FETCH optimization
-- These indexes will improve performance of the optimized query

-- Index for username lookup (primary filter)
CREATE INDEX IF NOT EXISTS idx_sys_user_account_username 
ON sys_user_account(username);

-- Composite index for license application user lookup
CREATE INDEX IF NOT EXISTS idx_core_license_application_user_created 
ON core_license_application(user_account_id, date_created DESC);

-- Index for application status lookup
CREATE INDEX IF NOT EXISTS idx_core_license_application_status 
ON core_license_application(application_status_id);

-- Index for application step lookup
CREATE INDEX IF NOT EXISTS idx_core_license_application_step 
ON core_license_application(application_step_id);

-- Index for water source lookup
CREATE INDEX IF NOT EXISTS idx_core_license_application_water_source 
ON core_license_application(water_source_id);

-- Index for application payments lookup
CREATE INDEX IF NOT EXISTS idx_core_application_payment_license_app 
ON core_application_payment(license_application_id);

-- Index for license type lookup in application step
CREATE INDEX IF NOT EXISTS idx_core_application_step_license_type 
ON core_application_step(license_type_id);

-- Index for water resource area lookup
CREATE INDEX IF NOT EXISTS idx_core_water_source_area 
ON core_water_source(water_resource_area_id);

-- Indexes for user account associations
CREATE INDEX IF NOT EXISTS idx_sys_user_account_district 
ON sys_user_account(district_id);

CREATE INDEX IF NOT EXISTS idx_sys_user_account_status 
ON sys_user_account(account_status_id);

CREATE INDEX IF NOT EXISTS idx_sys_user_account_salutation 
ON sys_user_account(salutation_id);

CREATE INDEX IF NOT EXISTS idx_sys_user_account_group 
ON sys_user_account(user_group_id);

-- Performance monitoring query to check index usage
-- Run this after implementing the optimization to verify index effectiveness
/*
EXPLAIN (ANALYZE, BUFFERS) 
SELECT DISTINCT app.* 
FROM core_license_application app 
LEFT JOIN sys_user_account userAccount ON app.user_account_id = userAccount.id
LEFT JOIN core_application_status ON app.application_status_id = core_application_status.id
LEFT JOIN core_application_step step ON app.application_step_id = step.id
LEFT JOIN core_license_type ON step.license_type_id = core_license_type.id
LEFT JOIN core_water_source waterSource ON app.water_source_id = waterSource.id
LEFT JOIN core_water_resource_area ON waterSource.water_resource_area_id = core_water_resource_area.id
LEFT JOIN core_district ON userAccount.district_id = core_district.id
LEFT JOIN sys_account_status ON userAccount.account_status_id = sys_account_status.id
LEFT JOIN sys_salutation ON userAccount.salutation_id = sys_salutation.id
LEFT JOIN sys_user_group ON userAccount.user_group_id = sys_user_group.id
LEFT JOIN core_application_payment ON app.id = core_application_payment.license_application_id
WHERE userAccount.username = 'test_username'
ORDER BY app.date_created DESC;
*/