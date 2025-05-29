package eu.apps4net.feedora.models;

import java.util.UUID;

public class AuthResponse {
    private String token;
    private String email;
    private String username;
    private UUID userId;
    
    public AuthResponse() {}
    
    public AuthResponse(String token, String email, String username, UUID userId) {
        this.token = token;
        this.email = email;
        this.username = username;
        this.userId = userId;
    }
    
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
}
