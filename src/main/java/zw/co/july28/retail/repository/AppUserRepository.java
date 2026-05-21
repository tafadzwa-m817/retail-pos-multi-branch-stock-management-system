package zw.co.july28.retail.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import zw.co.july28.retail.entity.AppUser;
import zw.co.july28.retail.enums.Role;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);
    boolean existsByEmail(String email);
    List<AppUser> findByBranchId(Long branchId);
    List<AppUser> findByRole(Role role);
    List<AppUser> findByActiveTrue();
}
