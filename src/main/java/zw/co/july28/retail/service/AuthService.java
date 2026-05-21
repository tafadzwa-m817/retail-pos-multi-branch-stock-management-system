package zw.co.july28.retail.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zw.co.july28.retail.dto.request.LoginRequest;
import zw.co.july28.retail.dto.request.RefreshTokenRequest;
import zw.co.july28.retail.dto.request.RegisterRequest;
import zw.co.july28.retail.dto.response.AuthResponse;
import zw.co.july28.retail.entity.AppUser;
import zw.co.july28.retail.entity.Branch;
import zw.co.july28.retail.entity.RefreshToken;
import zw.co.july28.retail.enums.AuditAction;
import zw.co.july28.retail.enums.Role;
import zw.co.july28.retail.exception.BadRequestException;
import zw.co.july28.retail.exception.ResourceNotFoundException;
import zw.co.july28.retail.repository.AppUserRepository;
import zw.co.july28.retail.repository.BranchRepository;
import zw.co.july28.retail.security.JwtTokenProvider;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository userRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final AuditLogService auditLogService;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        AppUser user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        String accessToken = jwtTokenProvider.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        auditLogService.log("AppUser", user.getId(), AuditAction.LOGIN, "User logged in");
        return buildAuthResponse(accessToken, refreshToken.getToken(), user);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already in use: " + request.getEmail());
        }

        Branch branch = null;
        if (request.getBranchId() != null) {
            branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Branch", request.getBranchId()));
        }

        AppUser user = AppUser.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole() != null ? request.getRole() : Role.CASHIER)
                .branch(branch)
                .build();

        userRepository.save(user);
        String accessToken = jwtTokenProvider.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        auditLogService.log("AppUser", user.getId(), AuditAction.CREATE, "New user registered: " + user.getEmail());
        return buildAuthResponse(accessToken, refreshToken.getToken(), user);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.verifyToken(request.getRefreshToken());
        AppUser user = refreshToken.getUser();
        String newAccessToken = jwtTokenProvider.generateToken(user);
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);
        return buildAuthResponse(newAccessToken, newRefreshToken.getToken(), user);
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.verifyToken(request.getRefreshToken());
        auditLogService.log("AppUser", refreshToken.getUser().getId(), AuditAction.LOGOUT, "User logged out");
        refreshTokenService.revokeToken(request.getRefreshToken());
    }

    private AuthResponse buildAuthResponse(String token, String refreshToken, AppUser user) {
        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .email(user.getEmail())
                .role(user.getRole().name())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .userId(user.getId())
                .branchId(user.getBranch() != null ? user.getBranch().getId() : null)
                .build();
    }
}
