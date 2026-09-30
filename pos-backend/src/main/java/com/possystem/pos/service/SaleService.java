package com.possystem.pos.service;

import com.possystem.pos.domain.Customer;
import com.possystem.pos.domain.PaymentMethod;
import com.possystem.pos.domain.Product;
import com.possystem.pos.domain.Sale;
import com.possystem.pos.domain.SaleItem;
import com.possystem.pos.domain.SaleStatus;
import com.possystem.pos.domain.StockMovementType;
import com.possystem.pos.dto.CheckoutItemRequest;
import com.possystem.pos.dto.CheckoutRequest;
import com.possystem.pos.dto.PageResponse;
import com.possystem.pos.dto.SaleResponse;
import com.possystem.pos.dto.SaleSummaryResponse;
import com.possystem.pos.exception.BusinessRuleException;
import com.possystem.pos.exception.InsufficientStockException;
import com.possystem.pos.exception.ResourceNotFoundException;
import com.possystem.pos.repository.CustomerRepository;
import com.possystem.pos.repository.ProductRepository;
import com.possystem.pos.repository.SaleRepository;
import com.possystem.pos.repository.spec.SaleSpecifications;
import com.possystem.pos.support.Money;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Checkout, receipt lookup and voiding.
 *
 * <p>Pricing rules, in one place so they are easy to audit:</p>
 * <ul>
 *   <li>Prices are tax exclusive. Per line: {@code gross = price x qty},
 *       {@code net = gross - lineDiscount}, {@code tax = net x taxRate},
 *       {@code lineTotal = net + tax}.</li>
 *   <li>The order level discount is taken off the taxed total, which keeps each line's
 *       recorded tax equal to the tax that was actually charged on it.</li>
 *   <li>The total is never negative.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class SaleService {

    private final SaleRepository sales;
    private final ProductRepository products;
    private final CustomerRepository customers;
    private final StockService stock;
    private final ReferenceGenerator references;
    private final Clock clock;

    public SaleService(SaleRepository sales,
                       ProductRepository products,
                       CustomerRepository customers,
                       StockService stock,
                       ReferenceGenerator references,
                       Clock clock) {
        this.sales = sales;
        this.products = products;
        this.customers = customers;
        this.stock = stock;
        this.references = references;
        this.clock = clock;
    }

    public PageResponse<SaleSummaryResponse> search(String term,
                                                    SaleStatus status,
                                                    Instant from,
                                                    Instant to,
                                                    Pageable pageable) {
        return PageResponse.of(
                sales.findAll(SaleSpecifications.search(blankToNull(term), status, from, to), pageable),
                SaleSummaryResponse::from);
    }

    public SaleResponse findById(Long id) {
        return SaleResponse.from(sales.findWithItemsById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Sale", id)));
    }

    public SaleResponse findByReference(String reference) {
        return SaleResponse.from(sales.findWithItemsByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("No sale with reference " + reference)));
    }

    /**
     * Rings up a basket: locks the products, verifies stock, prices the lines, records the
     * sale and moves the stock. All of it in one transaction, so a failure anywhere leaves
     * neither a half-written receipt nor a wrong stock level.
     */
    @Transactional
    public SaleResponse checkout(CheckoutRequest request) {
        Map<Long, MergedLine> lines = mergeLines(request.items());

        // Stable lock order (by id) keeps concurrent checkouts from deadlocking each other.
        List<Long> ids = lines.keySet().stream().sorted().toList();
        Map<Long, Product> locked = products.findAllByIdForUpdate(ids).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<Long> missing = ids.stream().filter(id -> !locked.containsKey(id)).toList();
        if (!missing.isEmpty()) {
            throw new ResourceNotFoundException("Unknown product ids: " + missing);
        }

        List<String> stockProblems = new ArrayList<>();
        List<String> inactive = new ArrayList<>();
        for (Long id : ids) {
            Product product = locked.get(id);
            MergedLine line = lines.get(id);

            if (!product.isActive()) {
                inactive.add(product.getName() + " (" + product.getSku() + ")");
                continue;
            }
            if (product.isTrackStock() && product.getStockQuantity() < line.quantity()) {
                stockProblems.add("%s (%s): asked for %d, only %d on hand"
                        .formatted(product.getName(), product.getSku(),
                                line.quantity(), product.getStockQuantity()));
            }
        }
        if (!inactive.isEmpty()) {
            throw new BusinessRuleException("These products are no longer for sale", inactive);
        }
        if (!stockProblems.isEmpty()) {
            throw new InsufficientStockException(stockProblems);
        }

        Sale sale = new Sale();
        sale.setSoldAt(Instant.now(clock));
        sale.setStatus(SaleStatus.COMPLETED);
        sale.setPaymentMethod(request.paymentMethod());
        sale.setCashierName(defaultCashier(request.cashierName()));
        sale.setNote(blankToNull(request.note()));
        sale.setCustomer(resolveCustomer(request.customerId()));

        BigDecimal subtotal = Money.ZERO;
        BigDecimal lineDiscounts = Money.ZERO;
        BigDecimal taxTotal = Money.ZERO;
        BigDecimal taxedTotal = Money.ZERO;

        for (Long id : ids) {
            Product product = locked.get(id);
            MergedLine line = lines.get(id);

            BigDecimal unitPrice = line.unitPrice() == null
                    ? Money.of(product.getPrice())
                    : Money.of(line.unitPrice());

            BigDecimal gross = Money.multiply(unitPrice, line.quantity());
            // A discount can never exceed the line it is applied to.
            BigDecimal discount = Money.min(Money.of(line.discountAmount()), gross);
            BigDecimal net = Money.subtract(gross, discount);
            BigDecimal taxRate = product.getTaxRate() == null ? Money.ZERO : product.getTaxRate();
            BigDecimal tax = Money.percentageOf(net, taxRate);
            BigDecimal lineTotal = Money.add(net, tax);

            SaleItem item = new SaleItem();
            item.setProduct(product);
            item.setProductName(product.getName());
            item.setProductSku(product.getSku());
            item.setUnit(product.getUnit());
            item.setUnitPrice(unitPrice);
            item.setUnitCost(Money.of(product.getCost()));
            item.setQuantity(line.quantity());
            item.setGrossAmount(gross);
            item.setDiscountAmount(discount);
            item.setNetAmount(net);
            item.setTaxRate(taxRate);
            item.setTaxAmount(tax);
            item.setLineTotal(lineTotal);
            sale.addItem(item);

            subtotal = Money.add(subtotal, gross);
            lineDiscounts = Money.add(lineDiscounts, discount);
            taxTotal = Money.add(taxTotal, tax);
            taxedTotal = Money.add(taxedTotal, lineTotal);
        }

        BigDecimal orderDiscount = Money.of(request.orderDiscount());
        if (orderDiscount.compareTo(taxedTotal) > 0) {
            throw new BusinessRuleException("Order discount of " + orderDiscount
                    + " is larger than the amount due of " + taxedTotal);
        }

        BigDecimal total = Money.atLeastZero(Money.subtract(taxedTotal, orderDiscount));
        BigDecimal tendered = Money.of(request.amountTendered());

        if (request.paymentMethod().requiresTender()) {
            if (tendered.compareTo(total) < 0) {
                throw new BusinessRuleException(
                        "Amount tendered (" + tendered + ") is less than the total due (" + total + ")");
            }
        } else {
            // Card, e-wallet and transfers settle for the exact amount and give no change.
            tendered = total;
        }

        sale.setSubtotal(subtotal);
        sale.setOrderDiscount(orderDiscount);
        sale.setDiscountTotal(Money.add(lineDiscounts, orderDiscount));
        sale.setTaxTotal(taxTotal);
        sale.setTotal(total);
        sale.setAmountTendered(tendered);
        sale.setChangeDue(Money.atLeastZero(Money.subtract(tendered, total)));
        sale.setReference(references.nextSaleReference());

        Sale saved = sales.save(sale);

        for (Long id : ids) {
            Product product = locked.get(id);
            if (!product.isTrackStock()) {
                continue;
            }
            stock.applyChange(product, -lines.get(id).quantity(), StockMovementType.SALE,
                    saved.getReference(), "Sold on " + saved.getReference());
        }

        return SaleResponse.from(saved);
    }

    /**
     * Cancels a completed sale and puts the stock back. The row is kept so the receipt
     * number is never reused and the audit trail stays intact.
     */
    @Transactional
    public SaleResponse voidSale(Long id, String reason) {
        Sale sale = sales.findWithItemsById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Sale", id));

        if (sale.getStatus() != SaleStatus.COMPLETED) {
            throw new BusinessRuleException(
                    "Sale " + sale.getReference() + " is already " + sale.getStatus().name().toLowerCase());
        }

        List<Long> productIds = sale.getItems().stream()
                .map(SaleItem::getProduct)
                .filter(product -> product != null)
                .map(Product::getId)
                .distinct()
                .sorted()
                .toList();

        Map<Long, Product> locked = productIds.isEmpty()
                ? Map.of()
                : products.findAllByIdForUpdate(productIds).stream()
                        .collect(Collectors.toMap(Product::getId, Function.identity()));

        for (SaleItem item : sale.getItems()) {
            if (item.getProduct() == null) {
                continue;
            }
            Product product = locked.get(item.getProduct().getId());
            if (product == null || !product.isTrackStock()) {
                continue;
            }
            stock.applyChange(product, item.getQuantity(), StockMovementType.RETURN,
                    sale.getReference(), "Void of " + sale.getReference());
        }

        sale.setStatus(SaleStatus.VOIDED);
        sale.setVoidReason(reason.trim());
        sale.setVoidedAt(Instant.now(clock));

        return SaleResponse.from(sales.save(sale));
    }

    /**
     * Folds repeated scans of the same product into one line. Without this the same
     * product could appear twice and each copy would be stock-checked in isolation,
     * letting the basket oversell.
     */
    private Map<Long, MergedLine> mergeLines(List<CheckoutItemRequest> items) {
        Map<Long, MergedLine> merged = new LinkedHashMap<>();
        for (CheckoutItemRequest item : items) {
            merged.merge(
                    item.productId(),
                    new MergedLine(item.quantity(), item.unitPrice(), Money.of(item.discountAmount())),
                    (existing, incoming) -> new MergedLine(
                            existing.quantity() + incoming.quantity(),
                            // Later override wins; sending two different prices for one
                            // product is a client bug, not a business case.
                            incoming.unitPrice() != null ? incoming.unitPrice() : existing.unitPrice(),
                            Money.add(existing.discountAmount(), incoming.discountAmount())));
        }
        return merged;
    }

    private Customer resolveCustomer(Long customerId) {
        if (customerId == null) {
            return null;
        }
        return customers.findById(customerId)
                .orElseThrow(() -> ResourceNotFoundException.of("Customer", customerId));
    }

    private String defaultCashier(String cashierName) {
        String trimmed = blankToNull(cashierName);
        return trimmed == null ? "Staff" : trimmed;
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** One basket line after duplicates have been folded together. */
    private record MergedLine(int quantity, BigDecimal unitPrice, BigDecimal discountAmount) {
    }
}
