package mw.nwra.ewaterpermit.service;

import mw.nwra.ewaterpermit.dto.WRMISAuthResponseDTO;
import mw.nwra.ewaterpermit.model.SysConfig;
import mw.nwra.ewaterpermit.util.WRMISJwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Implementation of WRMIS Authentication Service
 * Handles authentication for WRMIS system-to-system integration
 * Credentials are fetched from sys_config table (admin-configurable)
 */
@Service
public class WRMISAuthServiceImpl implements WRMISAuthService {

    private static final Logger log = LoggerFactory.getLogger(WRMISAuthServiceImpl.class);

    @Autowired
    private WRMISJwtUtil wrmisJwtUtil;

    @Autowired
    private SysConfigService sysConfigService;

    @Override
    public WRMISAuthResponseDTO authenticate(String clientId, String clientSecret) {
        log.info("🔐 WRMIS Authentication attempt for client: {}", clientId);

        // Validate client credentials
        if (clientId == null || clientSecret == null) {
            log.error("❌ WRMIS Authentication failed: Missing credentials");
            throw new RuntimeException("Client ID and Secret are required");
        }

        // Fetch WRMIS credentials from database
        String validClientId = null;
        String validClientSecret = null;
        try {
            SysConfig config = sysConfigService.getSystemConfig();
            if (config != null) {
                validClientId = config.getWrmisClientId();
                validClientSecret = config.getWrmisClientSecret();
            }
        } catch (Exception e) {
            log.error("❌ Error fetching WRMIS configuration: {}", e.getMessage());
            throw new RuntimeException("WRMIS configuration error");
        }

        // Check if WRMIS is configured
        if (validClientId == null || validClientId.trim().isEmpty() ||
            validClientSecret == null || validClientSecret.trim().isEmpty()) {
            log.error("❌ WRMIS Authentication failed: WRMIS not configured in system settings");
            throw new RuntimeException("WRMIS integration not configured. Please contact system administrator.");
        }

        // Validate client ID
        if (!validClientId.equals(clientId)) {
            log.error("❌ WRMIS Authentication failed: Invalid client ID: {}", clientId);
            throw new RuntimeException("Invalid client credentials");
        }

        // Validate client secret
        if (!validClientSecret.equals(clientSecret)) {
            log.error("❌ WRMIS Authentication failed: Invalid client secret for client: {}", clientId);
            throw new RuntimeException("Invalid client credentials");
        }

        // Generate JWT token
        String token = wrmisJwtUtil.generateToken(clientId);
        long expiresIn = wrmisJwtUtil.getExpirationTimeInSeconds();

        log.info("✅ WRMIS Authentication successful for client: {} - Token expires in {} seconds", clientId, expiresIn);

        return new WRMISAuthResponseDTO(token, expiresIn);
    }

    @Override
    public boolean validateToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return false;
            }

            String clientId = wrmisJwtUtil.extractClientId(token);
            return wrmisJwtUtil.validateToken(token, clientId);
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String extractClientId(String token) {
        try {
            return wrmisJwtUtil.extractClientId(token);
        } catch (Exception e) {
            log.error("Failed to extract client ID from token: {}", e.getMessage());
            return null;
        }
    }
}
