package com.possystem.pos.repository;

import com.possystem.pos.domain.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    @EntityGraph(attributePaths = "product")
    Page<StockMovement> findByProductIdOrderByIdDesc(Long productId, Pageable pageable);

    @EntityGraph(attributePaths = "product")
    Page<StockMovement> findAllByOrderByIdDesc(Pageable pageable);
}
