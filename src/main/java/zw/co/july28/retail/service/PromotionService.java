package zw.co.july28.retail.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import zw.co.july28.retail.dto.request.PromotionRequest;
import zw.co.july28.retail.dto.response.PromotionResponse;
import zw.co.july28.retail.entity.Product;
import zw.co.july28.retail.entity.Promotion;
import zw.co.july28.retail.exception.BadRequestException;
import zw.co.july28.retail.exception.ResourceNotFoundException;
import zw.co.july28.retail.repository.ProductRepository;
import zw.co.july28.retail.repository.PromotionRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final ProductRepository productRepository;

    public List<PromotionResponse> getAllPromotions() {
        return promotionRepository.findAll().stream()
                .map(PromotionResponse::from)
                .collect(Collectors.toList());
    }

    public List<PromotionResponse> getActivePromotions() {
        return promotionRepository.findActivePromotions(LocalDateTime.now()).stream()
                .map(PromotionResponse::from)
                .collect(Collectors.toList());
    }

    public PromotionResponse getPromotion(Long id) {
        return PromotionResponse.from(findById(id));
    }

    public PromotionResponse createPromotion(PromotionRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date must be after start date");
        }

        Promotion promotion = Promotion.builder()
                .name(request.getName())
                .description(request.getDescription())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .minimumPurchaseAmount(request.getMinimumPurchaseAmount())
                .applyToAll(request.isApplyToAll())
                .active(request.isActive())
                .applicableProducts(new ArrayList<>())
                .build();

        if (!request.isApplyToAll() && request.getProductIds() != null) {
            List<Product> products = productRepository.findAllById(request.getProductIds());
            promotion.setApplicableProducts(products);
        }

        return PromotionResponse.from(promotionRepository.save(promotion));
    }

    public PromotionResponse updatePromotion(Long id, PromotionRequest request) {
        Promotion promotion = findById(id);

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date must be after start date");
        }

        promotion.setName(request.getName());
        promotion.setDescription(request.getDescription());
        promotion.setDiscountType(request.getDiscountType());
        promotion.setDiscountValue(request.getDiscountValue());
        promotion.setStartDate(request.getStartDate());
        promotion.setEndDate(request.getEndDate());
        promotion.setMinimumPurchaseAmount(request.getMinimumPurchaseAmount());
        promotion.setApplyToAll(request.isApplyToAll());
        promotion.setActive(request.isActive());

        if (!request.isApplyToAll() && request.getProductIds() != null) {
            List<Product> products = productRepository.findAllById(request.getProductIds());
            promotion.setApplicableProducts(products);
        } else {
            promotion.getApplicableProducts().clear();
        }

        return PromotionResponse.from(promotionRepository.save(promotion));
    }

    public void deletePromotion(Long id) {
        if (!promotionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Promotion", id);
        }
        promotionRepository.deleteById(id);
    }

    public List<Promotion> findCurrentlyActivePromotions() {
        return promotionRepository.findActivePromotions(LocalDateTime.now());
    }

    private Promotion findById(Long id) {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", id));
    }
}
