package mw.nwra.ewaterpermit.dto;

/**
 * DTO for WRMIS Authentication Request
 * Used when WRMIS requests a JWT token for API access
 */
public class WRMISAuthRequestDTO {

    private String clientId;
    private String clientSecret;
    private String grantType; // Should be "client_credentials"

    public WRMISAuthRequestDTO() {}

    public WRMISAuthRequestDTO(String clientId, String clientSecret, String grantType) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.grantType = grantType;
    }

    // Getters and Setters
    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }
}
