package com.possystem.pos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Optional buyer attached to a sale. Walk-in sales leave this null.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(
        name = "customers",
        indexes = {
                @Index(name = "idx_customers_name", columnList = "name"),
                @Index(name = "idx_customers_phone", columnList = "phone")
        }
)
public class Customer extends BaseEntity {

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "phone", length = 32)
    private String phone;

    @Column(name = "email", length = 160)
    private String email;

    @Column(name = "address", length = 320)
    private String address;

    @Column(name = "notes", length = 500)
    private String notes;
}
