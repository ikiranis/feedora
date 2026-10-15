package eu.apps4net.feedora.utilities;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {
    private static final String SECRET = "test-secret-key-that-is-long-enough-for-hs256-signing-0123456789";

    private JwtUtil jwtUtil;

    private static JwtUtil jwtUtil(String secret, long expiration) {
        JwtUtil util = new JwtUtil();
        ReflectionTestUtils.setField(util, "secretKey", secret);
        ReflectionTestUtils.setField(util, "jwtExpiration", expiration);
        return util;
    }

    @BeforeEach
    void setUp() {
        jwtUtil = jwtUtil(SECRET, 60_000);
    }

    @Test
    void tokenContainsTheEmailAndUserId() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateToken("user@example.com", userId);

        assertThat(jwtUtil.extractEmail(token)).isEqualTo("user@example.com");
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(userId);
        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.isTokenValid(token, "user@example.com")).isTrue();
    }

    @Test
    void tokenIsInvalidForAnotherEmail() {
        String token = jwtUtil.generateToken("user@example.com", UUID.randomUUID());

        assertThat(jwtUtil.isTokenValid(token, "other@example.com")).isFalse();
    }

    @Test
    void malformedTokensAreInvalid() {
        assertThat(jwtUtil.validateToken("not.a.token")).isFalse();
        assertThat(jwtUtil.validateToken("")).isFalse();
        assertThat(jwtUtil.validateToken(null)).isFalse();
    }

    @Test
    void tokensSignedWithAnotherKeyAreInvalid() {
        String token = jwtUtil("another-secret-key-that-is-long-enough-for-hs256-signing", 60_000)
                .generateToken("user@example.com", UUID.randomUUID());

        assertThat(jwtUtil.validateToken(token)).isFalse();
    }

    @Test
    void tamperedTokensAreInvalid() {
        String token = jwtUtil.generateToken("user@example.com", UUID.randomUUID());
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + parts[1] + "x." + parts[2];

        assertThat(jwtUtil.validateToken(tampered)).isFalse();
    }

    @Test
    void expiredTokensAreInvalid() {
        String token = jwtUtil(SECRET, -1_000).generateToken("user@example.com", UUID.randomUUID());

        assertThat(jwtUtil.validateToken(token)).isFalse();
        assertThatThrownBy(() -> jwtUtil.extractEmail(token)).isInstanceOf(ExpiredJwtException.class);
    }
}
