package eu.apps4net.feedora.utilities;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {
    
    @Value("${jwt.secret}")
    private String secretKey;
    
    @Value("${jwt.expiration}")
    private long jwtExpiration;
    
    // For users who chose to stay connected at login (default: 30 days)
    @Value("${jwt.remember-me-expiration:2592000000}")
    private long rememberMeExpiration;
    
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }
    
    public String generateToken(String email, UUID userId) {
        return generateToken(email, userId, false);
    }
    
    /**
     * Generates a token that lasts for jwt.expiration, or for jwt.remember-me-expiration when the user stays connected
     */
    public String generateToken(String email, UUID userId, boolean rememberMe) {
        long expiration = rememberMe ? rememberMeExpiration : jwtExpiration;
        return Jwts.builder()
                .setSubject(email)
                .claim("userId", userId.toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }
    
    public UUID extractUserId(String token) {
        Claims claims = extractClaims(token);
        String userIdStr = claims.get("userId", String.class);
        return UUID.fromString(userIdStr);
    }
    
    public boolean isTokenValid(String token, String email) {
        final String extractedEmail = extractEmail(token);
        return (extractedEmail.equals(email) && !isTokenExpired(token));
    }
    
    public Date extractExpiration(String token) {
        return extractClaims(token).getExpiration();
    }
    
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    
    private Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
