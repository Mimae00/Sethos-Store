package com.possystem.pos.repository.spec;

import com.possystem.pos.domain.Customer;
import com.possystem.pos.domain.Sale;
import com.possystem.pos.domain.SaleStatus;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic filters for the sales history.
 *
 * <p>See {@link ProductSpecifications} for why these are Criteria predicates rather than a
 * JPQL query with optional parameters.</p>
 */
public final class SaleSpecifications {

    private SaleSpecifications() {
    }

    /** Matches receipt number, cashier or customer name, case-insensitively. */
    public static Specification<Sale> matchesTerm(String term) {
        if (term == null || term.isBlank()) {
            return null;
        }
        String pattern = "%" + term.trim().toLowerCase() + "%";
        return (root, query, builder) -> {
            // LEFT JOIN: walk-in sales have no customer and must still be searchable.
            var customer = root.<Sale, Customer>join("customer", JoinType.LEFT);
            return builder.or(
                    builder.like(builder.lower(root.get("reference")), pattern),
                    builder.like(builder.lower(root.get("cashierName")), pattern),
                    builder.like(builder.lower(builder.coalesce(customer.get("name"), "")), pattern));
        };
    }

    public static Specification<Sale> hasStatus(SaleStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("status"), status);
    }

    /** Inclusive lower bound on the business timestamp. */
    public static Specification<Sale> soldOnOrAfter(Instant from) {
        if (from == null) {
            return null;
        }
        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get("soldAt"), from);
    }

    /** Exclusive upper bound, so the whole final day is included. */
    public static Specification<Sale> soldBefore(Instant to) {
        if (to == null) {
            return null;
        }
        return (root, query, builder) -> builder.lessThan(root.get("soldAt"), to);
    }

    public static Specification<Sale> search(String term, SaleStatus status, Instant from, Instant to) {
        List<Specification<Sale>> parts = new ArrayList<>();
        addIfPresent(parts, matchesTerm(term));
        addIfPresent(parts, hasStatus(status));
        addIfPresent(parts, soldOnOrAfter(from));
        addIfPresent(parts, soldBefore(to));

        if (parts.isEmpty()) {
            return (root, query, builder) -> builder.conjunction();
        }
        return Specification.allOf(parts);
    }

    private static void addIfPresent(List<Specification<Sale>> parts, Specification<Sale> spec) {
        if (spec != null) {
            parts.add(spec);
        }
    }
}
