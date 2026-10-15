package eu.apps4net.feedora.services;

import eu.apps4net.feedora.exceptions.RegistrationDisabledException;
import eu.apps4net.feedora.models.Role;
import eu.apps4net.feedora.models.User;
import eu.apps4net.feedora.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
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
    private static final String LEGACY_ADMIN_EMAIL = "rocean@error.gr";

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
    void migrateUserRolesPromotesTheLegacyAdmin() {
        User first = new User("aaa", "aaa@example.com", "hash", null);
        User legacy = new User("admin", LEGACY_ADMIN_EMAIL, "hash", null);
        when(userRepository.findByRoleIsNull()).thenReturn(List.of(first, legacy));
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);

        userService.migrateUserRoles();

        assertThat(legacy.getRole()).isEqualTo(Role.ADMIN);
        assertThat(first.getRole()).isEqualTo(Role.USER);
        verify(userRepository).saveAll(List.of(first, legacy));
    }

    @Test
    void migrateUserRolesPromotesTheFirstUserByUsernameWithoutLegacyAdmin() {
        User bob = new User("bob", "bob@example.com", "hash", null);
        User alice = new User("Alice", "alice@example.com", "hash", null);
        when(userRepository.findByRoleIsNull()).thenReturn(List.of(bob, alice));
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);

        userService.migrateUserRoles();

        assertThat(alice.getRole()).isEqualTo(Role.ADMIN);
        assertThat(bob.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void migrateUserRolesKeepsTheExistingAdmin() {
        User legacy = new User("admin", LEGACY_ADMIN_EMAIL, "hash", null);
        when(userRepository.findByRoleIsNull()).thenReturn(List.of(legacy));
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(true);

        userService.migrateUserRoles();

        assertThat(legacy.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void registerUserMakesTheFirstUserAdmin() {
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // The first user can register even with registration disabled
        User user = userService.registerUser("new", "new@example.com", "password", false);

        assertThat(user.getUsername()).isEqualTo("new");
        assertThat(user.getPassword()).isEqualTo("encoded");
        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void registerUserMakesTheNextUsersSimpleUsers() {
        when(userRepository.count()).thenReturn(1L);
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        User user = userService.registerUser("new", "new@example.com", "password", true);

        assertThat(user.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void registerUserRejectsNewUsersWhenRegistrationIsDisabled() {
        when(userRepository.count()).thenReturn(1L);

        assertThatThrownBy(() -> userService.registerUser("new", "new@example.com", "password", false))
                .isInstanceOf(RegistrationDisabledException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUserRejectsAnExistingEmail() {
        when(userRepository.count()).thenReturn(1L);
        when(userRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> userService.registerUser("x", "taken@example.com", "password", true))
                .hasMessage("User with email taken@example.com already exists");
        verify(userRepository, never()).save(any());
    }

    @Test
    void changeRoleIsOnlyForTheAdminAndNotForThemselves() {
        User admin = new User("admin", "admin@example.com", "hash", Role.ADMIN);
        admin.setId(UUID.randomUUID());
        User simple = new User("simple", "simple@example.com", "hash");
        simple.setId(UUID.randomUUID());
        when(userRepository.findById(simple.getId())).thenReturn(Optional.of(simple));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> userService.changeRole(admin.getId(), Role.USER, simple))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> userService.changeRole(admin.getId(), Role.USER, admin))
                .hasMessage("You can't change your own role");

        userService.changeRole(simple.getId(), Role.ADMIN, admin);
        assertThat(simple.getRole()).isEqualTo(Role.ADMIN);
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
    void getCurrentUserFailsWithoutAnAuthenticatedUser() {
        assertThatThrownBy(() -> userService.getCurrentUser())
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("someone", null, List.of()));
        assertThatThrownBy(() -> userService.getCurrentUser())
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
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
