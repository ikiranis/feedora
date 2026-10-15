package eu.apps4net.feedora.configurations;

import eu.apps4net.feedora.models.User;
import eu.apps4net.feedora.services.UserService;
import eu.apps4net.feedora.utilities.JwtUtil;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private UserService userService;
    @Mock
    private FilterChain filterChain;
    @InjectMocks
    private JwtAuthenticationFilter filter;

    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final User user = new User("tester", "tester@example.com", "hash");

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validTokenAuthenticatesTheUser() throws Exception {
        request.addHeader("Authorization", "Bearer good");
        when(jwtUtil.extractEmail("good")).thenReturn("tester@example.com");
        when(userService.findByEmail("tester@example.com")).thenReturn(Optional.of(user));
        when(jwtUtil.validateToken("good")).thenReturn(true);
        when(jwtUtil.isTokenValid("good", "tester@example.com")).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.getPrincipal()).isSameAs(user);
        assertThat(authentication.isAuthenticated()).isTrue();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void requestsWithoutBearerTokenAreNotAuthenticated() throws Exception {
        request.addHeader("Authorization", "Basic abc");

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtUtil, userService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void unreadableTokenIsIgnored() throws Exception {
        request.addHeader("Authorization", "Bearer bad");
        when(jwtUtil.extractEmail("bad")).thenThrow(new RuntimeException("bad token"));

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(userService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void tokenOfUnknownUserIsNotAuthenticated() throws Exception {
        request.addHeader("Authorization", "Bearer good");
        when(jwtUtil.extractEmail("good")).thenReturn("gone@example.com");
        when(userService.findByEmail("gone@example.com")).thenReturn(Optional.empty());

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void tokenThatFailsValidationIsNotAuthenticated() throws Exception {
        request.addHeader("Authorization", "Bearer good");
        when(jwtUtil.extractEmail("good")).thenReturn("tester@example.com");
        when(userService.findByEmail("tester@example.com")).thenReturn(Optional.of(user));
        when(jwtUtil.validateToken("good")).thenReturn(true);
        when(jwtUtil.isTokenValid("good", "tester@example.com")).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void existingAuthenticationIsKept() throws Exception {
        Authentication existing = new UsernamePasswordAuthenticationToken("someone", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(existing);
        request.addHeader("Authorization", "Bearer good");
        when(jwtUtil.extractEmail("good")).thenReturn("tester@example.com");

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing);
        verify(userService, never()).findByEmail(anyString());
    }
}
