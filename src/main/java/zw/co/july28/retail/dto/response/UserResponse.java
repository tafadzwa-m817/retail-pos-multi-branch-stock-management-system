package zw.co.july28.retail.dto.response;

import lombok.*;
import zw.co.july28.retail.entity.AppUser;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private Long branchId;
    private String branchName;
    private boolean active;
    private LocalDateTime createdAt;

    public static UserResponse from(AppUser user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .branchId(user.getBranch() != null ? user.getBranch().getId() : null)
                .branchName(user.getBranch() != null ? user.getBranch().getName() : null)
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
