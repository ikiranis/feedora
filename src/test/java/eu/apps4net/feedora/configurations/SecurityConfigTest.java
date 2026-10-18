package eu.apps4net.feedora.configurations;

import eu.apps4net.feedora.support.ApiTestBase;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityConfigTest extends ApiTestBase {

    @Test
    void publicEndpointsDontNeedAToken() throws Exception {
        mockMvc.perform(get("/api/general/appAlive")).andExpect(status().isOk());
        mockMvc.perform(get("/api/language/all/en")).andExpect(status().isOk());
        mockMvc.perform(get("/api/auth/status")).andExpect(status().isOk());
    }

    @Test
    void frontendFilesInSubfoldersDontNeedAToken() throws Exception {
        // <img> requests don't send the JWT, so e.g. the language flags must be public
        mockMvc.perform(get("/flags/en.svg"))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()));
    }

    @Test
    void protectedEndpointsRejectRequestsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/folders")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/posts/unreadCounts")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/feed/getAllFeeds")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/logs")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/setting/all")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointsAcceptAValidToken() throws Exception {
        mockMvc.perform(auth(get("/api/folders"))).andExpect(status().isOk());
    }

    @Test
    void malformedTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/folders").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/folders").header("Authorization", token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() throws Exception {
        String forged = Jwts.builder()
                .setSubject(user.getEmail())
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor("another-secret-key-that-is-long-enough-for-hs256".getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256)
                .compact();

        mockMvc.perform(get("/api/folders").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenOfDeletedUserIsRejected() throws Exception {
        userRepository.delete(user);

        mockMvc.perform(auth(get("/api/folders"))).andExpect(status().isUnauthorized());
    }

    @Test
    void corsPreflightIsAllowed() throws Exception {
        mockMvc.perform(options("/api/folders")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
