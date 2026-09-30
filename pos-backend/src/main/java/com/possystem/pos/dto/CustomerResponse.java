package com.possystem.pos.dto;

import com.possystem.pos.domain.Customer;

import java.time.Instant;

public record CustomerResponse(
        Long id,
        String name,
        String phone,
        String email,
        String address,
        String notes,
        Instant createdAt
) {

    public static CustomerResponse from(Customer customer) {
        if (customer == null) {
            return null;
        }
        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getPhone(),
                customer.getEmail(),
                customer.getAddress(),
                customer.getNotes(),
                customer.getCreatedAt()
        );
    }
}
