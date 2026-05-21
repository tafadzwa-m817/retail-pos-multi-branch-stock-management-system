package zw.co.july28.retail.dto.response;

import lombok.*;
import zw.co.july28.retail.entity.Branch;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchResponse {
    private Long id;
    private String name;
    private String address;
    private String phone;
    private String email;
    private boolean active;
    private LocalDateTime createdAt;

    public static BranchResponse from(Branch branch) {
        return BranchResponse.builder()
                .id(branch.getId())
                .name(branch.getName())
                .address(branch.getAddress())
                .phone(branch.getPhone())
                .email(branch.getEmail())
                .active(branch.isActive())
                .createdAt(branch.getCreatedAt())
                .build();
    }
}
