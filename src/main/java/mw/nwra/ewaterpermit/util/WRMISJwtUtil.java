package mw.nwra.ewaterpermit.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT Utility for WRMIS System-to-System Authentication
 * Generates and validates JWT tokens for WRMIS backend integration
 */
@Component
public class WRMISJwtUtil {

    private final SecretKey SECRET_KEY;
    private final long EXPIRATION_TIME;

    public WRMISJwtUtil(
            @Value("${wrmis.jwt.secret}") String secretKey,
            @Value("${wrmis.jwt.expiration:3600000}") long expiration) { // Default 1 hour
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        this.SECRET_KEY = Keys.hmacShaKeyFor(keyBytes);
        this.EXPIRATION_TIME = expiration;
    }

    /**
     * Generate JWT token for WRMIS system
     * @param clientId The WRMIS client identifier
     * @return JWT token string
     */
    public String generateToken(String clientId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("client_type", "WRMIS");
        claims.put("scope", "wrmis_data_access");
        return createToken(claims, clientId);
    }

    /**
     * Create JWT token with claims
     */
    private String createToken(Map<String, Object> claims, String subject) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + EXPIRATION_TIME))
                .signWith(SECRET_KEY)
                .compact();
    }

    /**
     * Extract client ID from token
     */
    public String extractClientId(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract expiration date from token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract specific claim from token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract all claims from token
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(SECRET_KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            throw new RuntimeException("Invalid JWT token: " + e.getMessage());
        }
    }

    /**
     * Check if token is expired
     */
    public Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Validate WRMIS JWT token
     * @param token JWT token to validate
     * @param clientId Expected client ID
     * @return true if token is valid
     */
    public Boolean validateToken(String token, String clientId) {
        try {
            final String extractedClientId = extractClientId(token);
            return (extractedClientId.equals(clientId) && !isTokenExpired(token));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get token expiration time in seconds
     */
    public long getExpirationTimeInSeconds() {
        return EXPIRATION_TIME / 1000;
    }
}
