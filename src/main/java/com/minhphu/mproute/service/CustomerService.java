package com.minhphu.mproute.service;

import com.minhphu.mproute.dto.CustomerForm;
import com.minhphu.mproute.entity.Customer;
import com.minhphu.mproute.entity.enums.RecordStatus;
import com.minhphu.mproute.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nghiep vu danh muc khach hang (CN-01-04).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public Page<Customer> search(String keyword, RecordStatus status, Pageable pageable) {
        // O tim kiem de trong gui len chuoi rong chu khong phai null.
        // Doi ve null de cau JPQL bo qua dieu kien do
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return customerRepository.search(normalizedKeyword, status, pageable);
    }

    @Transactional(readOnly = true)
    public Customer getById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng"));
    }

    /**
     * Chan trung so dien thoai.
     *
     * So dien thoai KHONG dat unique trong CSDL (docs/02 chi danh index de tra cuu),
     * vi thuc te co truong hop hai cua hang cua cung mot chu dung chung mot so.
     * Nhung trung so lam trang tra cuu cong khai /track tra ve nhieu khach khac nhau,
     * nen van canh bao — chi la canh bao mem, khong chan cung.
     *
     * excludeId dung khi dang SUA: khach tu giu so cua chinh minh la binh thuong.
     */
    @Transactional(readOnly = true)
    public boolean isPhoneTaken(String phone, Long excludeId) {
        if (phone == null || phone.isBlank()) {
            return false;
        }
        return customerRepository.findByPhone(phone.trim()).stream()
                .anyMatch(other -> !other.getId().equals(excludeId));
    }

    @Transactional
    public Customer save(CustomerForm form) {
        // id null la them moi, co id la sua ban ghi dang co
        Customer customer = (form.getId() == null) ? new Customer() : getById(form.getId());

        customer.setName(form.getName().trim());
        customer.setContactPerson(blankToNull(form.getContactPerson()));
        customer.setPhone(form.getPhone().trim());
        customer.setEmail(blankToNull(form.getEmail()));
        customer.setAddress(form.getAddress().trim());
        customer.setLatitude(form.getLatitude());
        customer.setLongitude(form.getLongitude());
        customer.setPaymentMethod(form.getPaymentMethod());
        customer.setDeliveryNote(blankToNull(form.getDeliveryNote()));
        customer.setStatus(form.getStatus());

        Customer saved = customerRepository.save(customer);
        log.info("Luu khach hang {} - {} ({}, {})",
                saved.getName(), saved.getPhone(), saved.getLatitude(), saved.getLongitude());
        return saved;
    }

    /**
     * Doi giua ACTIVE va INACTIVE.
     *
     * CBR-17 / BR01-09: khong co ham xoa cung. Khach da tung dat don ma bi xoa thi
     * don cu mat thong tin giao cho ai, bao cao doanh thu sai theo. Khach INACTIVE
     * khong hien trong o chon luc tao don nua nhung lich su van con.
     */
    @Transactional
    public RecordStatus toggleStatus(Long id) {
        Customer customer = getById(id);
        RecordStatus newStatus = (customer.getStatus() == RecordStatus.ACTIVE)
                ? RecordStatus.INACTIVE
                : RecordStatus.ACTIVE;
        customer.setStatus(newStatus);
        customerRepository.save(customer);
        log.info("Khach hang {} chuyen sang trang thai {}", customer.getName(), newStatus);
        return newStatus;
    }

    // O nhap khong bat buoc de trong gui len chuoi rong, luu vao DB nen la NULL cho gon
    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
