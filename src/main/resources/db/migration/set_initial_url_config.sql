-- =====================================================
-- Set Initial URL Configuration
-- Description: Sets the frontend and backend URLs in sys_config
-- =====================================================

-- Option 1: For Development Environment
-- Uncomment these lines for local development:
/*
UPDATE sys_config
SET
    frontend_url = 'http://localhost:4200',
    backend_url = 'http://localhost:8080/api/nwra-apis/ewaterpermit-ws'
WHERE id = (SELECT id FROM sys_config LIMIT 1);
*/

-- Option 2: For Production Environment
-- Uncomment these lines for production:
/*
UPDATE sys_config
SET
    frontend_url = 'https://nwra.lomtechnology.com',
    backend_url = 'https://api.malawi.lomtechnology.com/api/nwra-apis/ewaterpermit-ws'
WHERE id = (SELECT id FROM sys_config LIMIT 1);
*/

-- =====================================================
-- Verify Configuration
-- =====================================================
-- Run this to check your settings:
SELECT
    id,
    system_name,
    frontend_url,
    backend_url,
    contact_email_address
FROM sys_config;
