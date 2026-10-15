package eu.apps4net.feedora.models;

import java.util.UUID;

public class AuthResponse {
    private String token;
    private String email;
    private String username;
    private UUID userId;
    private Role role;
    
    public AuthResponse() {}
    
    public AuthResponse(String token, String email, String username, UUID userId) {
        this.token = token;
        this.email = email;
        this.username = username;
        this.userId = userId;
    }

    public AuthResponse(String token, User user) {
        this(token, user.getEmail(), user.getUsername(), user.getId());
        this.role = user.getRole();
    }
    
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
