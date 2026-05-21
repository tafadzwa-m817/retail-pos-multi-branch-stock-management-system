package zw.co.july28.retail.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import zw.co.july28.retail.entity.Product;
import zw.co.july28.retail.entity.Supplier;
import zw.co.july28.retail.entity.SupplierProduct;
import zw.co.july28.retail.exception.ResourceNotFoundException;
import zw.co.july28.retail.repository.ProductRepository;
import zw.co.july28.retail.repository.SupplierProductRepository;
import zw.co.july28.retail.repository.SupplierRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierProductService {

    private final SupplierProductRepository supplierProductRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;

    public List<SupplierProduct> getCatalogForSupplier(Long supplierId) {
        return supplierProductRepository.findBySupplierId(supplierId);
    }

    public List<SupplierProduct> getSuppliersForProduct(Long productId) {
        return supplierProductRepository.findByProductId(productId);
    }

    public SupplierProduct upsert(Long supplierId, Long productId, BigDecimal unitCost, String supplierSku, boolean preferred) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", supplierId));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        SupplierProduct sp = supplierProductRepository
                .findBySupplierIdAndProductId(supplierId, productId)
                .orElse(SupplierProduct.builder().supplier(supplier).product(product).build());

        sp.setUnitCost(unitCost);
        sp.setSupplierSku(supplierSku);
        sp.setPreferred(preferred);
        return supplierProductRepository.save(sp);
    }

    public void delete(Long id) {
        supplierProductRepository.deleteById(id);
    }
}
