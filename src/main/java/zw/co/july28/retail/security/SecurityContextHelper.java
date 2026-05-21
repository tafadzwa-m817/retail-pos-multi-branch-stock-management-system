package zw.co.july28.retail.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import zw.co.july28.retail.entity.AppUser;
import zw.co.july28.retail.enums.Role;

import java.util.Optional;

@Component
public class SecurityContextHelper {

    public Optional<AppUser> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public boolean isGlobalAdmin() {
        return getCurrentUser()
                .map(u -> u.getRole() == Role.SUPER_ADMIN || u.getRole() == Role.ADMIN)
                .orElse(false);
    }

    public Optional<Long> getCurrentUserBranchId() {
        return getCurrentUser()
                .filter(u -> u.getRole() == Role.BRANCH_MANAGER || u.getRole() == Role.CASHIER)
                .map(AppUser::getBranch)
                .map(b -> b != null ? b.getId() : null);
    }
}
