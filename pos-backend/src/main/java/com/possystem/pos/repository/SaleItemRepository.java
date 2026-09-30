package com.possystem.pos.repository;

import com.possystem.pos.domain.SaleItem;
import com.possystem.pos.repository.projection.SaleItemTotalsRow;
import com.possystem.pos.repository.projection.TopProductRow;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    @Query("""
            select new com.possystem.pos.repository.projection.SaleItemTotalsRow(
                sum(i.quantity), sum(i.unitCost * i.quantity))
            from SaleItem i
            where i.sale.status = com.possystem.pos.domain.SaleStatus.COMPLETED
              and i.sale.soldAt >= :from and i.sale.soldAt < :to
            """)
    SaleItemTotalsRow totalsBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select new com.possystem.pos.repository.projection.TopProductRow(
                min(i.product.id), i.productSku, min(i.productName), sum(i.quantity), sum(i.lineTotal))
            from SaleItem i
            where i.sale.status = com.possystem.pos.domain.SaleStatus.COMPLETED
              and i.sale.soldAt >= :from and i.sale.soldAt < :to
            group by i.productSku
            order by sum(i.quantity) desc, sum(i.lineTotal) desc
            """)
    List<TopProductRow> topProductsBetween(@Param("from") Instant from,
                                           @Param("to") Instant to,
                                           Pageable pageable);

    boolean existsByProductId(Long productId);
}
