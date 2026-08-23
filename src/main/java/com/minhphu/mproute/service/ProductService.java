package com.minhphu.mproute.service;

import com.minhphu.mproute.dto.ProductForm;
import com.minhphu.mproute.entity.Product;
import com.minhphu.mproute.entity.enums.RecordStatus;
import com.minhphu.mproute.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nghiep vu danh muc san pham (CN-01-01).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public Page<Product> search(String keyword, RecordStatus status, Pageable pageable) {
        // O tim kiem de trong gui len chuoi rong chu khong phai null.
        // Doi ve null de cau JPQL bo qua dieu kien do
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return productRepository.search(normalizedKeyword, status, pageable);
    }

    @Transactional(readOnly = true)
    public Product getById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));
    }

    /**
     * BR01-02: ma san pham khong duoc trung.
     *
     * excludeId dung khi dang SUA: san pham tu no giu ma cua chinh no la binh thuong,
     * chi bao trung khi ma do thuoc ve mot san pham KHAC.
     */
    @Transactional(readOnly = true)
    public boolean isCodeTaken(String code, Long excludeId) {
        return productRepository.findByCode(code)
                .filter(product -> !product.getId().equals(excludeId))
                .isPresent();
    }

    @Transactional
    public Product save(ProductForm form) {
        // id null la them moi, co id la sua ban ghi dang co
        Product product = (form.getId() == null) ? new Product() : getById(form.getId());

        product.setCode(form.getCode().trim());
        product.setName(form.getName().trim());
        product.setUnit(form.getUnit().trim());
        product.setWeightPerBox(form.getWeightPerBox());
        product.setVolumePerBox(form.getVolumePerBox());
        product.setUnitPrice(form.getUnitPrice());
        product.setNote(form.getNote());
        product.setStatus(form.getStatus());

        Product saved = productRepository.save(product);
        log.info("Luu san pham {} - {}", saved.getCode(), saved.getName());
        return saved;
    }

    /**
     * Doi giua ACTIVE va INACTIVE.
     *
     * CBR-17 / BR01-09: khong co ham xoa cung. San pham da nam trong don hang cu
     * ma bi xoa thi don hang cu mat thong tin, bao cao cu sai theo.
     */
    @Transactional
    public RecordStatus toggleStatus(Long id) {
        Product product = getById(id);
        RecordStatus newStatus = (product.getStatus() == RecordStatus.ACTIVE)
                ? RecordStatus.INACTIVE
                : RecordStatus.ACTIVE;
        product.setStatus(newStatus);
        productRepository.save(product);
        log.info("San pham {} chuyen sang trang thai {}", product.getCode(), newStatus);
        return newStatus;
    }
}
