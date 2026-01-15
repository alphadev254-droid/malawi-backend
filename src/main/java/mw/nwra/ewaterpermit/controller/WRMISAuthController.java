package mw.nwra.ewaterpermit.controller;

import mw.nwra.ewaterpermit.dto.WRMISAuthRequestDTO;
import mw.nwra.ewaterpermit.dto.WRMISAuthResponseDTO;
import mw.nwra.ewaterpermit.service.WRMISAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Authentication Controller for WRMIS Integration
 * Provides JWT token generation endpoint for WRMIS system
 */
@RestController
@RequestMapping("/v1/wrmis/auth")
public class WRMISAuthController {

    private static final Logger log = LoggerFactory.getLogger(WRMISAuthController.class);

    @Autowired
    private WRMISAuthService wrmisAuthService;

    /**
     * WRMIS Token Endpoint
     * WRMIS calls this endpoint to obtain a JWT token for API access
     *
     * @param authRequest Contains clientId, clientSecret, and grantType
     * @return JWT token response with access token and expiration
     */
    @PostMapping("/token")
    public ResponseEntity<WRMISAuthResponseDTO> getToken(@RequestBody WRMISAuthRequestDTO authRequest) {
        try {
            log.info("🔑 WRMIS Token request received for client: {}", authRequest.getClientId());

            // Validate grant type
            if (!"client_credentials".equals(authRequest.getGrantType())) {
                log.error("❌ Invalid grant type: {}", authRequest.getGrantType());
                return ResponseEntity.badRequest().build();
            }

            // Authenticate and generate token
            WRMISAuthResponseDTO response = wrmisAuthService.authenticate(
                    authRequest.getClientId(),
                    authRequest.getClientSecret()
            );

            log.info("✅ WRMIS Token issued successfully to client: {}", authRequest.getClientId());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("❌ WRMIS Authentication failed: {}", e.getMessage());

            // Return 401 Unauthorized
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            log.error("❌ WRMIS Authentication error: {}", e.getMessage(), e);

            // Return 500 Internal Server Error
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Verify Token Endpoint
     * Allows WRMIS to verify if a token is still valid
     *
     * @param token JWT token to verify (from Authorization header)
     * @return Validation result
     */
    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        Map<String, Object> response = new HashMap<>();

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                response.put("valid", false);
                response.put("message", "Missing or invalid Authorization header");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String token = authHeader.substring(7);
            boolean isValid = wrmisAuthService.validateToken(token);

            if (isValid) {
                String clientId = wrmisAuthService.extractClientId(token);
                response.put("valid", true);
                response.put("client_id", clientId);
                log.info("✅ Token verified successfully for client: {}", clientId);
                return ResponseEntity.ok(response);
            } else {
                response.put("valid", false);
                response.put("message", "Token is invalid or expired");
                log.warn("⚠️ Invalid token verification attempt");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

        } catch (Exception e) {
            log.error("❌ Token verification error: {}", e.getMessage());
            response.put("valid", false);
            response.put("message", "Token verification failed");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Health check endpoint for WRMIS authentication service
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("service", "WRMIS Authentication");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
}
