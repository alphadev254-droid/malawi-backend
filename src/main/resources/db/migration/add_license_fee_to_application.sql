-- Add license fee fields to core_license_application table
-- This allows managers to set application-specific license fees based on assessment data

ALTER TABLE core_license_application
ADD COLUMN license_fee DOUBLE DEFAULT NULL COMMENT 'Application-specific license fee set by manager',
ADD COLUMN license_fee_set_by_user_id VARCHAR(36) DEFAULT NULL COMMENT 'User ID of manager who set the fee',
ADD COLUMN license_fee_set_date TIMESTAMP DEFAULT NULL COMMENT 'Date when the license fee was set';

-- Add index for fee lookup optimization
CREATE INDEX idx_license_fee ON core_license_application(license_fee);
CREATE INDEX idx_license_fee_set_by ON core_license_application(license_fee_set_by_user_id);
