-- Database Performance Indexes for Water Permit System
-- This script adds indexes to optimize query performance from 19 seconds to subsecond response times
-- Created to eliminate slow performance identified in production logs

-- Select your database (replace 'your_database_name' with actual database name)
-- USE your_database_name;

-- Primary indexes for core_license_application table
CREATE INDEX IF NOT EXISTS idx_core_license_app_date_created ON core_license_application(date_created DESC);
CREATE INDEX IF NOT EXISTS idx_core_license_app_user_id ON core_license_application(user_account_id);
CREATE INDEX IF NOT EXISTS idx_core_license_app_license_type ON core_license_application(license_type_id);
CREATE INDEX IF NOT EXISTS idx_core_license_app_status ON core_license_application(application_status_id);
CREATE INDEX IF NOT EXISTS idx_core_license_app_step ON core_license_application(application_step_id);

-- Composite indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_core_license_app_user_date ON core_license_application(user_account_id, date_created DESC);
CREATE INDEX IF NOT EXISTS idx_core_license_app_status_date ON core_license_application(application_status_id, date_created DESC);
CREATE INDEX IF NOT EXISTS idx_core_license_app_type_date ON core_license_application(license_type_id, date_created DESC);

-- Composite index for admin dashboard filtering
CREATE INDEX IF NOT EXISTS idx_core_license_app_admin_filter ON core_license_application(application_status_id, license_type_id, date_created DESC);

-- Indexes for sys_user_account table (for JOINs)
CREATE INDEX IF NOT EXISTS idx_sys_user_account_names ON sys_user_account(first_name, last_name);
CREATE INDEX IF NOT EXISTS idx_sys_user_account_email ON sys_user_account(email_address);

-- Indexes for core_application_payment table (for payment queries)
CREATE INDEX IF NOT EXISTS idx_core_app_payment_license_app ON core_application_payment(license_application_id);
CREATE INDEX IF NOT EXISTS idx_core_app_payment_fees_type ON core_application_payment(fees_type_id);
CREATE INDEX IF NOT EXISTS idx_core_app_payment_status ON core_application_payment(payment_status);

-- Composite index for payment optimization
CREATE INDEX IF NOT EXISTS idx_core_app_payment_composite ON core_application_payment(license_application_id, fees_type_id, payment_status);

-- Indexes for lookup tables
CREATE INDEX IF NOT EXISTS idx_core_license_type_name ON core_license_type(name);
CREATE INDEX IF NOT EXISTS idx_core_app_status_name ON core_application_status(name);
CREATE INDEX IF NOT EXISTS idx_core_app_step_name ON core_application_step(name);
CREATE INDEX IF NOT EXISTS idx_core_fees_type_name ON core_fees_type(name);

-- Additional performance indexes for dashboard queries
CREATE INDEX IF NOT EXISTS idx_core_license_app_date_submitted ON core_license_application(date_submitted);

-- Critical indexes for the specific admin query performance
CREATE INDEX IF NOT EXISTS idx_core_license_app_main_query ON core_license_application(date_created DESC, user_account_id, license_type_id, application_status_id, application_step_id);

-- Indexes for the foreign key relationships to speed up JOINs
CREATE INDEX IF NOT EXISTS idx_sys_user_pk ON sys_user_account(id);
CREATE INDEX IF NOT EXISTS idx_core_license_type_pk ON core_license_type(id);
CREATE INDEX IF NOT EXISTS idx_core_app_status_pk ON core_application_status(id);
CREATE INDEX IF NOT EXISTS idx_core_app_step_pk ON core_application_step(id);

-- Covering index for the full query (if table allows large indexes)
CREATE INDEX IF NOT EXISTS idx_core_license_app_covering ON core_license_application(
    date_created DESC, 
    user_account_id, 
    license_type_id, 
    application_status_id, 
    application_step_id,
    id,
    application_type,
    date_submitted,
    client_info,
    location_info,
    application_metadata,
    form_specific_data
);