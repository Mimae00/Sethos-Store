package com.possystem.pos.repository;

import com.possystem.pos.domain.Sale;
import com.possystem.pos.repository.projection.PaymentMethodTotalRow;
import com.possystem.pos.repository.projection.SaleTotalsRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long>, JpaSpecificationExecutor<Sale> {

    /**
     * Sales history. Filters come in as a {@code Specification} so absent ones are left
     * out of the SQL; see {@link com.possystem.pos.repository.spec.SaleSpecifications}.
     */
    @Override
    @EntityGraph(attributePaths = "customer")
    Page<Sale> findAll(Specification<Sale> specification, Pageable pageable);

    @EntityGraph(attributePaths = {"items", "customer"})
    Optional<Sale> findWithItemsById(Long id);

    @EntityGraph(attributePaths = {"items", "customer"})
    Optional<Sale> findWithItemsByReference(String reference);

    @Query("""
            select new com.possystem.pos.repository.projection.SaleTotalsRow(
                count(s), sum(s.subtotal), sum(s.discountTotal), sum(s.taxTotal), sum(s.total))
            from Sale s
            where s.status = com.possystem.pos.domain.SaleStatus.COMPLETED
              and s.soldAt >= :from and s.soldAt < :to
            """)
    SaleTotalsRow totalsBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select new com.possystem.pos.repository.projection.PaymentMethodTotalRow(
                s.paymentMethod, count(s), sum(s.total))
            from Sale s
            where s.status = com.possystem.pos.domain.SaleStatus.COMPLETED
              and s.soldAt >= :from and s.soldAt < :to
            group by s.paymentMethod
            order by sum(s.total) desc
            """)
    List<PaymentMethodTotalRow> totalsByPaymentMethod(@Param("from") Instant from, @Param("to") Instant to);

    /**
     * Lightweight two-column pull used to build the daily series in memory. Grouping by
     * calendar day is done in Java so the report keeps the store's time zone and stays
     * free of database specific date functions.
     */
    @Query("""
            select s.soldAt, s.total from Sale s
            where s.status = com.possystem.pos.domain.SaleStatus.COMPLETED
              and s.soldAt >= :from and s.soldAt < :to
            order by s.soldAt asc
            """)
    List<Object[]> soldAtAndTotalBetween(@Param("from") Instant from, @Param("to") Instant to);

    boolean existsByReference(String reference);

    /**
     * Detaches a customer from their sales so the customer record can be removed without
     * destroying receipt history; those sales simply become walk-in sales.
     */
    @Modifying
    @Query("update Sale s set s.customer = null where s.customer.id = :customerId")
    int detachCustomer(@Param("customerId") Long customerId);
}
