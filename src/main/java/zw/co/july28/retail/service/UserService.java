package zw.co.july28.retail.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import zw.co.july28.retail.dto.request.UserRequest;
import zw.co.july28.retail.dto.response.UserResponse;
import zw.co.july28.retail.entity.AppUser;
import zw.co.july28.retail.entity.Branch;
import zw.co.july28.retail.enums.AuditAction;
import zw.co.july28.retail.exception.BadRequestException;
import zw.co.july28.retail.exception.ResourceNotFoundException;
import zw.co.july28.retail.repository.AppUserRepository;
import zw.co.july28.retail.repository.BranchRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AppUserRepository userRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    public UserResponse getUser(Long id) {
        return UserResponse.from(findById(id));
    }

    public List<UserResponse> getUsersByBranch(Long branchId) {
        return userRepository.findByBranchId(branchId).stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    public UserResponse createUser(UserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already in use: " + request.getEmail());
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BadRequestException("Password is required when creating a user");
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
                .role(request.getRole())
                .branch(branch)
                .active(request.isActive())
                .build();

        UserResponse response = UserResponse.from(userRepository.save(user));
        auditLogService.log("AppUser", response.getId(), AuditAction.CREATE, "User created: " + request.getEmail());
        return response;
    }

    public UserResponse updateUser(Long id, UserRequest request) {
        AppUser user = findById(id);

        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already in use: " + request.getEmail());
        }

        Branch branch = null;
        if (request.getBranchId() != null) {
            branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Branch", request.getBranchId()));
        }

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());
        user.setBranch(branch);
        user.setActive(request.isActive());

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        UserResponse response = UserResponse.from(userRepository.save(user));
        auditLogService.log("AppUser", id, AuditAction.UPDATE, "User updated: " + user.getEmail());
        return response;
    }

    public void deleteUser(Long id) {
        AppUser user = findById(id);
        user.setActive(false);
        userRepository.save(user);
        auditLogService.log("AppUser", id, AuditAction.DELETE, "User deactivated: " + user.getEmail());
    }

    public UserResponse getProfile(String email) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        return UserResponse.from(user);
    }

    public void changePassword(String email, String currentPassword, String newPassword) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (newPassword.length() < 6) {
            throw new BadRequestException("New password must be at least 6 characters");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        auditLogService.log("AppUser", user.getId(), AuditAction.UPDATE, "Password changed for: " + email);
    }

    private AppUser findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
