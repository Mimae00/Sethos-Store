package com.possystem.pos.repository.spec;

import com.possystem.pos.domain.Category;
import com.possystem.pos.domain.Product;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic catalogue filters.
 *
 * <p>Built with the Criteria API rather than a JPQL query full of
 * {@code (:param is null or ...)} branches. That pattern leaves the bind parameter
 * untyped when the value is null: H2 tolerates it, but PostgreSQL resolves
 * {@code lower(?)} to {@code lower(bytea)} and fails outright. Omitting an absent filter
 * from the predicate tree avoids the question entirely.</p>
 */
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    /** Matches name, SKU or barcode, case-insensitively. Blank terms match everything. */
    public static Specification<Product> matchesTerm(String term) {
        if (term == null || term.isBlank()) {
            return null;
        }
        String pattern = "%" + term.trim().toLowerCase() + "%";
        return (root, query, builder) -> builder.or(
                builder.like(builder.lower(root.get("name")), pattern),
                builder.like(builder.lower(root.get("sku")), pattern),
                builder.like(builder.lower(builder.coalesce(root.get("barcode"), "")), pattern));
    }

    public static Specification<Product> inCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return (root, query, builder) -> {
            // LEFT JOIN so the join itself never filters; the id comparison does.
            var category = root.<Product, Category>join("category", JoinType.LEFT);
            return builder.equal(category.get("id"), categoryId);
        };
    }

    public static Specification<Product> hasActive(Boolean active) {
        if (active == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("active"), active);
    }

    /** Stock at or below the reorder level, for tracked products only. */
    public static Specification<Product> lowStockOnly(Boolean lowStock) {
        if (lowStock == null || !lowStock) {
            return null;
        }
        return (root, query, builder) -> builder.and(
                builder.isTrue(root.get("trackStock")),
                builder.lessThanOrEqualTo(root.get("stockQuantity"), root.get("reorderLevel")));
    }

    /** Combines the filters that are actually present. */
    public static Specification<Product> search(String term,
                                                Long categoryId,
                                                Boolean active,
                                                Boolean lowStock) {
        List<Specification<Product>> parts = new ArrayList<>();
        addIfPresent(parts, matchesTerm(term));
        addIfPresent(parts, inCategory(categoryId));
        addIfPresent(parts, hasActive(active));
        addIfPresent(parts, lowStockOnly(lowStock));

        if (parts.isEmpty()) {
            // An always-true predicate keeps the caller's code uniform.
            return (root, query, builder) -> builder.conjunction();
        }
        return Specification.allOf(parts);
    }

    private static void addIfPresent(List<Specification<Product>> parts, Specification<Product> spec) {
        if (spec != null) {
            parts.add(spec);
        }
    }
}
