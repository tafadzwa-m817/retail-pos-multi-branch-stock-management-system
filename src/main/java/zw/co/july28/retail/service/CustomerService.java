package zw.co.july28.retail.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import zw.co.july28.retail.dto.request.CustomerRequest;
import zw.co.july28.retail.dto.response.CustomerResponse;
import zw.co.july28.retail.dto.response.SaleResponse;
import zw.co.july28.retail.entity.Customer;
import zw.co.july28.retail.exception.BadRequestException;
import zw.co.july28.retail.exception.ResourceNotFoundException;
import zw.co.july28.retail.repository.CustomerRepository;
import zw.co.july28.retail.repository.SaleRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final SaleRepository saleRepository;

    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(CustomerResponse::from)
                .collect(Collectors.toList());
    }

    public CustomerResponse getCustomer(Long id) {
        return CustomerResponse.from(findById(id));
    }

    public List<CustomerResponse> searchCustomers(String query) {
        return customerRepository.searchCustomers(query).stream()
                .map(CustomerResponse::from)
                .collect(Collectors.toList());
    }

    public CustomerResponse createCustomer(CustomerRequest request) {
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && customerRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Customer email already exists: " + request.getEmail());
        }
        Customer customer = Customer.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .build();
        return CustomerResponse.from(customerRepository.save(customer));
    }

    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        Customer customer = findById(id);
        if (request.getEmail() != null && !request.getEmail().equals(customer.getEmail())
                && customerRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Customer email already exists: " + request.getEmail());
        }
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setAddress(request.getAddress());
        return CustomerResponse.from(customerRepository.save(customer));
    }

    public void deleteCustomer(Long id) {
        if (!customerRepository.existsById(id)) {
            throw new ResourceNotFoundException("Customer", id);
        }
        customerRepository.deleteById(id);
    }

    public List<SaleResponse> getCustomerPurchaseHistory(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer", customerId);
        }
        return saleRepository.findByCustomerId(customerId).stream()
                .map(SaleResponse::from)
                .collect(Collectors.toList());
    }

    public Customer findById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    }

    public void addLoyaltyPoints(Customer customer, int points) {
        customer.setLoyaltyPoints(customer.getLoyaltyPoints() + points);
        customerRepository.save(customer);
    }

    public void deductLoyaltyPoints(Customer customer, int points) {
        int newBalance = Math.max(0, customer.getLoyaltyPoints() - points);
        customer.setLoyaltyPoints(newBalance);
        customerRepository.save(customer);
    }
}
