package mw.nwra.ewaterpermit.dto;

/**
 * DTO for WRMIS Authentication Response
 * Returned to WRMIS after successful authentication
 */
public class WRMISAuthResponseDTO {

    private String accessToken;
    private String tokenType; // Usually "Bearer"
    private long expiresIn; // Expiration time in seconds
    private String scope;
    private long issuedAt; // Timestamp when token was issued

    public WRMISAuthResponseDTO() {
        this.tokenType = "Bearer";
    }

    public WRMISAuthResponseDTO(String accessToken, long expiresIn) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.expiresIn = expiresIn;
        this.scope = "wrmis_data_access";
        this.issuedAt = System.currentTimeMillis() / 1000;
    }

    // Getters and Setters
    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public long getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(long issuedAt) {
        this.issuedAt = issuedAt;
    }
}
