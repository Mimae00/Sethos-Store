package com.possystem.pos.service;

import com.possystem.pos.domain.Product;
import com.possystem.pos.domain.StockMovement;
import com.possystem.pos.domain.StockMovementType;
import com.possystem.pos.dto.PageResponse;
import com.possystem.pos.dto.StockMovementResponse;
import com.possystem.pos.repository.ProductRepository;
import com.possystem.pos.repository.StockMovementRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The only place stock levels are allowed to change.
 *
 * <p>Keeping this in one collaborator means the balance on {@code Product} and the
 * {@code stock_movements} journal can never drift apart: both are written together, in the
 * caller's transaction.</p>
 */
@Service
@Transactional(readOnly = true)
public class StockService {

    private final ProductRepository products;
    private final StockMovementRepository movements;

    public StockService(ProductRepository products, StockMovementRepository movements) {
        this.products = products;
        this.movements = movements;
    }

    /**
     * Applies a signed delta to the product's balance and journals it.
     *
     * @return the movement that was recorded
     */
    @Transactional
    public StockMovement applyChange(Product product,
                                     int quantityChange,
                                     StockMovementType type,
                                     String reference,
                                     String reason) {
        int before = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
        product.setStockQuantity(before + quantityChange);
        products.save(product);
        return movements.save(StockMovement.of(product, type, quantityChange, before, reference, reason));
    }

    public PageResponse<StockMovementResponse> history(Long productId, Pageable pageable) {
        if (productId == null) {
            return PageResponse.of(movements.findAllByOrderByIdDesc(pageable), StockMovementResponse::from);
        }
        return PageResponse.of(
                movements.findByProductIdOrderByIdDesc(productId, pageable),
                StockMovementResponse::from);
    }
}
