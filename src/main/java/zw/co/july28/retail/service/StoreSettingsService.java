package zw.co.july28.retail.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import zw.co.july28.retail.dto.request.StoreSettingsRequest;
import zw.co.july28.retail.entity.StoreSettings;
import zw.co.july28.retail.repository.StoreSettingsRepository;

@Service
@RequiredArgsConstructor
public class StoreSettingsService {

    private final StoreSettingsRepository repository;

    public StoreSettings get() {
        return repository.findById(1L).orElseGet(() ->
                repository.save(StoreSettings.builder().id(1L).build()));
    }

    public StoreSettings update(StoreSettingsRequest req) {
        StoreSettings settings = get();
        if (req.getStoreName() != null) settings.setStoreName(req.getStoreName());
        if (req.getStorePhone() != null) settings.setStorePhone(req.getStorePhone());
        if (req.getStoreAddress() != null) settings.setStoreAddress(req.getStoreAddress());
        if (req.getLogoUrl() != null) settings.setLogoUrl(req.getLogoUrl());
        if (req.getReceiptFooterText() != null) settings.setReceiptFooterText(req.getReceiptFooterText());
        if (req.getCurrency() != null) settings.setCurrency(req.getCurrency());
        return repository.save(settings);
    }
}
