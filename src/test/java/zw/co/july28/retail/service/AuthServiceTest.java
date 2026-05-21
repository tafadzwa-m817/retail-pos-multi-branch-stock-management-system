package zw.co.july28.retail.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import zw.co.july28.retail.dto.request.LoginRequest;
import zw.co.july28.retail.dto.request.RegisterRequest;
import zw.co.july28.retail.dto.response.AuthResponse;
import zw.co.july28.retail.entity.AppUser;
import zw.co.july28.retail.entity.RefreshToken;
import zw.co.july28.retail.enums.Role;
import zw.co.july28.retail.exception.BadRequestException;
import zw.co.july28.retail.repository.AppUserRepository;
import zw.co.july28.retail.repository.BranchRepository;
import zw.co.july28.retail.security.JwtTokenProvider;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService unit tests")
class AuthServiceTest {

    @Mock AppUserRepository userRepository;
    @Mock BranchRepository branchRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock AuthenticationManager authManager;
    @Mock RefreshTokenService refreshTokenService;
    @Mock AuditLogService auditLogService;

    @InjectMocks AuthService authService;

    private AppUser user;
    private RefreshToken refreshToken;

    @BeforeEach
    void setUp() {
        user = AppUser.builder().id(1L).firstName("Test").lastName("User")
                .email("test@test.com").password("encoded")
                .role(Role.CASHIER).active(true).build();
        refreshToken = RefreshToken.builder().id(1L).token("refresh-token-abc")
                .user(user).expiryDate(LocalDateTime.now().plusDays(7)).build();
    }

    @Test
    @DisplayName("login: valid credentials return AuthResponse with token")
    void login_validCredentials_returnsAuthResponse() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateToken(user)).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshToken);

        AuthResponse response = authService.login(new LoginRequest("test@test.com", "password123"));

        assertThat(response.getToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token-abc");
        assertThat(response.getEmail()).isEqualTo("test@test.com");
        assertThat(response.getRole()).isEqualTo("CASHIER");
        verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("login: invalid credentials propagate BadCredentialsException")
    void login_invalidCredentials_throwsBadCredentials() {
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(new LoginRequest("test@test.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("register: new email creates user and returns AuthResponse")
    void register_newEmail_createsUserAndReturnsToken() {
        RegisterRequest req = new RegisterRequest("John", "Doe", "new@test.com", "pass123", Role.CASHIER, null);

        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("encoded_pass");
        when(userRepository.save(any())).thenAnswer(inv -> {
            AppUser u = inv.getArgument(0);
            u.setId(99L);
            return u;
        });
        when(jwtTokenProvider.generateToken(any())).thenReturn("new-token");
        when(refreshTokenService.createRefreshToken(any())).thenReturn(refreshToken);

        AuthResponse response = authService.register(req);

        assertThat(response.getToken()).isEqualTo("new-token");
        assertThat(response.getEmail()).isEqualTo("new@test.com");
        verify(userRepository).save(any(AppUser.class));
    }

    @Test
    @DisplayName("register: duplicate email throws BadRequestException")
    void register_duplicateEmail_throwsBadRequest() {
        when(userRepository.existsByEmail("test@test.com")).thenReturn(true);

        assertThatThrownBy(() ->
                authService.register(new RegisterRequest("A", "B", "test@test.com", "pass", null, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already in use");
    }
}
