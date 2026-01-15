-- Fix email configuration in database
UPDATE sys_config SET 
    system_email_smtp = 'smtp.gmail.com',
    system_email_port = 587,
    system_email_address = 'brianndesa262@gmail.com',
    system_email_auth = 'tkgw jjmo pmbz imqx',
    system_email_smtp_security = 'STARTTLS'
WHERE id = 1;

-- Verify the update
SELECT system_email_smtp, system_email_port, system_email_address, system_email_auth 
FROM sys_config;