package com.possystem.pos.service;

import com.possystem.pos.domain.Customer;
import com.possystem.pos.dto.CustomerRequest;
import com.possystem.pos.dto.CustomerResponse;
import com.possystem.pos.dto.PageResponse;
import com.possystem.pos.exception.ResourceNotFoundException;
import com.possystem.pos.repository.CustomerRepository;
import com.possystem.pos.repository.SaleRepository;
import com.possystem.pos.repository.spec.CustomerSpecifications;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customers;
    private final SaleRepository sales;

    public CustomerService(CustomerRepository customers, SaleRepository sales) {
        this.customers = customers;
        this.sales = sales;
    }

    public PageResponse<CustomerResponse> search(String term, Pageable pageable) {
        return PageResponse.of(
                customers.findAll(CustomerSpecifications.search(term), pageable),
                CustomerResponse::from);
    }

    public CustomerResponse findById(Long id) {
        return CustomerResponse.from(require(id));
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        Customer customer = new Customer();
        apply(customer, request);
        return CustomerResponse.from(customers.save(customer));
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = require(id);
        apply(customer, request);
        return CustomerResponse.from(customers.save(customer));
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = require(id);
        // Past receipts must survive. Detach the customer from their sales first, then
        // remove the record; those sales become walk-in sales.
        sales.detachCustomer(id);
        customers.delete(customer);
    }

    private void apply(Customer customer, CustomerRequest request) {
        customer.setName(request.name().trim());
        customer.setPhone(blankToNull(request.phone()));
        customer.setEmail(blankToNull(request.email()));
        customer.setAddress(blankToNull(request.address()));
        customer.setNotes(blankToNull(request.notes()));
    }

    private Customer require(Long id) {
        return customers.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Customer", id));
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
