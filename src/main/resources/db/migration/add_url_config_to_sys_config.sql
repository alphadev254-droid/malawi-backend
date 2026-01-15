-- =====================================================
-- Migration: Add Frontend and Backend URL Configuration
-- Description: Adds frontend_url and backend_url to sys_config for dynamic URL configuration
-- =====================================================

-- Add frontend_url column
ALTER TABLE sys_config
ADD COLUMN frontend_url VARCHAR(255) DEFAULT NULL
COMMENT 'Frontend application URL (e.g., http://localhost:4200 or https://nwra.lomtechnology.com)';

-- Add backend_url column
ALTER TABLE sys_config
ADD COLUMN backend_url VARCHAR(255) DEFAULT NULL
COMMENT 'Backend API URL (e.g., http://localhost:8080/api/nwra-apis/ewaterpermit-ws or https://api.malawi.lomtechnology.com/api/nwra-apis/ewaterpermit-ws)';

-- Update existing row with default values (adjust as needed)
-- For development:
-- UPDATE sys_config SET frontend_url = 'http://localhost:4200', backend_url = 'http://localhost:8080/api/nwra-apis/ewaterpermit-ws';

-- For production:
-- UPDATE sys_config SET frontend_url = 'https://nwra.lomtechnology.com', backend_url = 'https://api.malawi.lomtechnology.com/api/nwra-apis/ewaterpermit-ws';

-- =====================================================
-- Verification
-- =====================================================
-- SELECT frontend_url, backend_url FROM sys_config;
