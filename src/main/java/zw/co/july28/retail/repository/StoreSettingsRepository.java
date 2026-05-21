package zw.co.july28.retail.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import zw.co.july28.retail.entity.StoreSettings;

@Repository
public interface StoreSettingsRepository extends JpaRepository<StoreSettings, Long> {
}
