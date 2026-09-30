package com.possystem.pos.repository.spec;

import com.possystem.pos.domain.Customer;
import org.springframework.data.jpa.domain.Specification;

/**
 * Dynamic filter for the customer list.
 *
 * <p>See {@link ProductSpecifications} for why this is a Criteria predicate rather than a
 * JPQL query with an optional parameter.</p>
 */
public final class CustomerSpecifications {

    private CustomerSpecifications() {
    }

    /** Matches name, phone or email, case-insensitively. Blank terms match everything. */
    public static Specification<Customer> search(String term) {
        if (term == null || term.isBlank()) {
            return (root, query, builder) -> builder.conjunction();
        }
        String pattern = "%" + term.trim().toLowerCase() + "%";
        return (root, query, builder) -> builder.or(
                builder.like(builder.lower(root.get("name")), pattern),
                builder.like(builder.lower(builder.coalesce(root.get("phone"), "")), pattern),
                builder.like(builder.lower(builder.coalesce(root.get("email"), "")), pattern));
    }
}
