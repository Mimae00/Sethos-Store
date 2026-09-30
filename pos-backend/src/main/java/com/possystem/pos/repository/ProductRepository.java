package com.possystem.pos.repository;

import com.possystem.pos.domain.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    /**
     * Catalogue search. Filters are supplied as a {@code Specification} so absent ones are
     * left out of the SQL entirely; see
     * {@link com.possystem.pos.repository.spec.ProductSpecifications}.
     */
    @Override
    @EntityGraph(attributePaths = "category")
    Page<Product> findAll(Specification<Product> specification, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Optional<Product> findWithCategoryById(Long id);

    Optional<Product> findBySkuIgnoreCase(String sku);

    Optional<Product> findByBarcode(String barcode);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsByBarcode(String barcode);

    boolean existsByCategoryId(Long categoryId);

    /**
     * Locks the product rows taking part in a checkout so two terminals cannot both
     * sell the last unit. Ordered by id to keep a stable lock order and avoid deadlocks.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id in :ids order by p.id")
    List<Product> findAllByIdForUpdate(@Param("ids") List<Long> ids);

    @EntityGraph(attributePaths = "category")
    @Query("""
            select p from Product p
            where p.active = true and p.trackStock = true and p.stockQuantity <= p.reorderLevel
            order by (p.stockQuantity - p.reorderLevel) asc, p.name asc
            """)
    List<Product> findLowStock(Pageable pageable);

    @Query("select count(p) from Product p where p.active = true")
    long countActive();

    @Query("""
            select count(p) from Product p
            where p.active = true and p.trackStock = true and p.stockQuantity <= p.reorderLevel
            """)
    long countLowStock();

    @Query("""
            select coalesce(sum(p.cost * p.stockQuantity), 0)
            from Product p where p.active = true and p.trackStock = true
            """)
    BigDecimal inventoryCostValue();
}
