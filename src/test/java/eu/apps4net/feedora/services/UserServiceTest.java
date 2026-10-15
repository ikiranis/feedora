package eu.apps4net.feedora.services;

import eu.apps4net.feedora.models.User;
import eu.apps4net.feedora.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    private static final String ADMIN_EMAIL = "rocean@error.gr";

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserService userService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getOrCreateAdminUserReturnsTheExistingAdmin() {
        User admin = new User("admin", ADMIN_EMAIL, "hash");
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(admin));

        assertThat(userService.getOrCreateAdminUser()).isSameAs(admin);
        verify(userRepository, never()).save(any());
    }

    @Test
    void getOrCreateAdminUserCreatesTheAdminWithEncodedPassword() {
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode("111111")).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        User admin = userService.getOrCreateAdminUser();

        assertThat(admin.getUsername()).isEqualTo("admin");
        assertThat(admin.getEmail()).isEqualTo(ADMIN_EMAIL);
        assertThat(admin.getPassword()).isEqualTo("encoded");
    }

    @Test
    void registerUserEncodesThePassword() {
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        User user = userService.registerUser("new", "new@example.com", "password");

        assertThat(user.getUsername()).isEqualTo("new");
        assertThat(user.getPassword()).isEqualTo("encoded");
    }

    @Test
    void registerUserRejectsAnExistingEmail() {
        when(userRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> userService.registerUser("x", "taken@example.com", "password"))
                .hasMessage("User with email taken@example.com already exists");
        verify(userRepository, never()).save(any());
    }

    @Test
    void checkPasswordUsesTheEncoder() {
        User user = new User("u", "u@example.com", "hash");
        when(passwordEncoder.matches("right", "hash")).thenReturn(true);
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThat(userService.checkPassword(user, "right")).isTrue();
        assertThat(userService.checkPassword(user, "wrong")).isFalse();
    }

    @Test
    void getCurrentUserReturnsTheAuthenticatedUser() {
        User user = new User("u", "u@example.com", "hash");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));

        assertThat(userService.getCurrentUser()).isSameAs(user);
    }

    @Test
    void getCurrentUserFallsBackToTheAdmin() {
        User admin = new User("admin", ADMIN_EMAIL, "hash");
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(admin));

        assertThat(userService.getCurrentUser()).isSameAs(admin);

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("someone", null, List.of()));
        assertThat(userService.getCurrentUser()).isSameAs(admin);
    }

    @Test
    void findersDelegateToTheRepository() {
        User user = new User();
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("u@example.com")).thenReturn(Optional.of(user));
        when(userRepository.findAll()).thenReturn(List.of(user));

        assertThat(userService.findById(id)).containsSame(user);
        assertThat(userService.findByEmail("u@example.com")).containsSame(user);
        assertThat(userService.getAllUsers()).containsExactly(user);
    }
}
