package mw.nwra.ewaterpermit.service;

import mw.nwra.ewaterpermit.dto.WRMISAuthResponseDTO;

/**
 * Service interface for WRMIS Authentication
 */
public interface WRMISAuthService {

    /**
     * Authenticate WRMIS system and generate JWT token
     * @param clientId WRMIS client ID
     * @param clientSecret WRMIS client secret
     * @return Authentication response with JWT token
     * @throws RuntimeException if authentication fails
     */
    WRMISAuthResponseDTO authenticate(String clientId, String clientSecret);

    /**
     * Validate WRMIS JWT token
     * @param token JWT token to validate
     * @return true if token is valid
     */
    boolean validateToken(String token);

    /**
     * Extract client ID from token
     * @param token JWT token
     * @return Client ID
     */
    String extractClientId(String token);
}
