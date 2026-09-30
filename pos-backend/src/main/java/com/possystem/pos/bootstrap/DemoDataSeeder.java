package com.possystem.pos.bootstrap;

import com.possystem.pos.config.PosProperties;
import com.possystem.pos.domain.PaymentMethod;
import com.possystem.pos.dto.CategoryRequest;
import com.possystem.pos.dto.CategoryResponse;
import com.possystem.pos.dto.CheckoutItemRequest;
import com.possystem.pos.dto.CheckoutRequest;
import com.possystem.pos.dto.CustomerRequest;
import com.possystem.pos.dto.ProductRequest;
import com.possystem.pos.dto.ProductResponse;
import com.possystem.pos.dto.SaleResponse;
import com.possystem.pos.repository.CategoryRepository;
import com.possystem.pos.repository.SaleRepository;
import com.possystem.pos.service.CategoryService;
import com.possystem.pos.service.CustomerService;
import com.possystem.pos.service.ProductService;
import com.possystem.pos.service.SaleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Loads a small demo catalogue and two weeks of sales on first start, so the dashboard and
 * reports have something to show.
 *
 * <p>Deliberately goes through the same services the API uses rather than writing rows
 * directly: the seeded data is then guaranteed to satisfy the same rules as real data,
 * and the seeder doubles as a smoke test of the checkout path at boot.</p>
 *
 * <p>Disable with {@code pos.seed-demo-data: false}.</p>
 */
@Component
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final PosProperties properties;
    private final CategoryRepository categoryRepository;
    private final CategoryService categories;
    private final ProductService products;
    private final CustomerService customers;
    private final SaleService sales;
    private final SaleRepository saleRepository;
    private final Clock clock;

    public DemoDataSeeder(PosProperties properties,
                          CategoryRepository categoryRepository,
                          CategoryService categories,
                          ProductService products,
                          CustomerService customers,
                          SaleService sales,
                          SaleRepository saleRepository,
                          Clock clock) {
        this.properties = properties;
        this.categoryRepository = categoryRepository;
        this.categories = categories;
        this.products = products;
        this.customers = customers;
        this.sales = sales;
        this.saleRepository = saleRepository;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.seedDemoData()) {
            return;
        }
        if (categoryRepository.count() > 0) {
            log.debug("Demo data already present, skipping seed");
            return;
        }

        log.info("Seeding demo catalogue and sales history");
        Map<String, Long> categoryIds = seedCategories();
        List<ProductResponse> catalogue = seedProducts(categoryIds);
        seedCustomers();
        seedSales(catalogue);
        log.info("Seeded {} products across {} categories", catalogue.size(), categoryIds.size());
    }

    private Map<String, Long> seedCategories() {
        List<CategoryRequest> requests = List.of(
                new CategoryRequest("Beverages", "Coffee, tea and cold drinks", "#2563eb", 1),
                new CategoryRequest("Bakery", "Breads and pastries", "#d97706", 2),
                new CategoryRequest("Snacks", "Chips, nuts and sweets", "#dc2626", 3),
                new CategoryRequest("Produce", "Fruit and vegetables", "#16a34a", 4),
                new CategoryRequest("Household", "Cleaning and paper goods", "#7c3aed", 5)
        );

        Map<String, Long> ids = new java.util.LinkedHashMap<>();
        for (CategoryRequest request : requests) {
            CategoryResponse created = categories.create(request);
            ids.put(created.name(), created.id());
        }
        return ids;
    }

    private List<ProductResponse> seedProducts(Map<String, Long> categoryIds) {
        record Seed(String sku, String barcode, String name, String category,
                    String price, String cost, String tax, int stock, int reorder, String unit) {
        }

        List<Seed> seeds = List.of(
                new Seed("BEV-001", "4800001000017", "House Blend Coffee 250g", "Beverages", "8.50", "5.10", "10.00", 60, 12, "pack"),
                new Seed("BEV-002", "4800001000024", "Green Tea 20 bags", "Beverages", "4.25", "2.40", "10.00", 80, 15, "box"),
                new Seed("BEV-003", "4800001000031", "Sparkling Water 500ml", "Beverages", "1.40", "0.70", "10.00", 150, 40, "bottle"),
                new Seed("BEV-004", "4800001000048", "Orange Juice 1L", "Beverages", "3.60", "2.10", "10.00", 45, 10, "bottle"),
                new Seed("BAK-001", "4800002000016", "Sourdough Loaf", "Bakery", "5.75", "2.80", "0.00", 24, 6, "loaf"),
                new Seed("BAK-002", "4800002000023", "Butter Croissant", "Bakery", "2.20", "0.95", "0.00", 40, 12, "pc"),
                new Seed("BAK-003", "4800002000030", "Blueberry Muffin", "Bakery", "2.60", "1.10", "0.00", 36, 10, "pc"),
                new Seed("SNK-001", "4800003000015", "Salted Potato Chips 150g", "Snacks", "2.95", "1.50", "10.00", 90, 20, "pack"),
                new Seed("SNK-002", "4800003000022", "Roasted Almonds 200g", "Snacks", "7.40", "4.60", "10.00", 35, 8, "pack"),
                new Seed("SNK-003", "4800003000039", "Dark Chocolate Bar 80g", "Snacks", "3.30", "1.70", "10.00", 70, 15, "bar"),
                new Seed("SNK-004", "4800003000046", "Granola Bar 6-pack", "Snacks", "5.10", "3.00", "10.00", 8, 12, "pack"),
                new Seed("PRD-001", "4800004000014", "Bananas", "Produce", "1.10", "0.55", "0.00", 120, 25, "kg"),
                new Seed("PRD-002", "4800004000021", "Gala Apples", "Produce", "2.40", "1.30", "0.00", 85, 20, "kg"),
                new Seed("PRD-003", "4800004000038", "Roma Tomatoes", "Produce", "2.10", "1.05", "0.00", 60, 18, "kg"),
                new Seed("PRD-004", "4800004000045", "Baby Spinach 200g", "Produce", "3.15", "1.80", "0.00", 5, 10, "pack"),
                new Seed("HHD-001", "4800005000013", "Dish Soap 500ml", "Household", "3.85", "2.05", "10.00", 55, 12, "bottle"),
                new Seed("HHD-002", "4800005000020", "Paper Towels 2-roll", "Household", "4.50", "2.60", "10.00", 48, 12, "pack"),
                new Seed("HHD-003", "4800005000037", "Laundry Detergent 1L", "Household", "9.20", "5.80", "10.00", 30, 8, "bottle")
        );

        List<ProductResponse> created = new ArrayList<>();
        for (Seed seed : seeds) {
            created.add(products.create(new ProductRequest(
                    seed.sku(),
                    seed.barcode(),
                    seed.name(),
                    null,
                    new BigDecimal(seed.price()),
                    new BigDecimal(seed.cost()),
                    new BigDecimal(seed.tax()),
                    seed.stock(),
                    seed.reorder(),
                    seed.unit(),
                    null,
                    true,
                    true,
                    categoryIds.get(seed.category())
            )));
        }
        return created;
    }

    private void seedCustomers() {
        List<CustomerRequest> requests = List.of(
                new CustomerRequest("Alina Reyes", "+1 555 0111", "alina.reyes@example.com", "45 Oak Avenue", null),
                new CustomerRequest("Marcus Cole", "+1 555 0122", "marcus.cole@example.com", "12 Pine Road", "Prefers e-receipts"),
                new CustomerRequest("Priya Nair", "+1 555 0133", "priya.nair@example.com", "8 Cedar Lane", null)
        );
        requests.forEach(customers::create);
    }

    /**
     * Rings up a spread of sales across the last two weeks.
     *
     * <p>Checkout always stamps the current time, so each seeded sale is backdated
     * afterwards. That is acceptable for demo data and keeps the pricing rules in
     * {@code SaleService} as the single source of truth.</p>
     */
    private void seedSales(List<ProductResponse> catalogue) {
        // Fixed seed: the demo dashboard looks the same on every machine.
        Random random = new Random(20260927L);
        LocalDate today = LocalDate.now(clock);
        PaymentMethod[] methods = {PaymentMethod.CASH, PaymentMethod.CARD, PaymentMethod.EWALLET, PaymentMethod.CASH};
        String[] cashiers = {"Jamie", "Ravi", "Noor"};

        for (int dayOffset = 13; dayOffset >= 0; dayOffset--) {
            LocalDate day = today.minusDays(dayOffset);
            int saleCount = 2 + random.nextInt(4);

            for (int n = 0; n < saleCount; n++) {
                int lineCount = 1 + random.nextInt(4);
                List<CheckoutItemRequest> items = new ArrayList<>();
                List<Integer> used = new ArrayList<>();

                for (int l = 0; l < lineCount; l++) {
                    int index = random.nextInt(catalogue.size());
                    if (used.contains(index)) {
                        continue;
                    }
                    used.add(index);
                    items.add(new CheckoutItemRequest(
                            catalogue.get(index).id(),
                            1 + random.nextInt(3),
                            null,
                            null));
                }
                if (items.isEmpty()) {
                    continue;
                }

                PaymentMethod method = methods[random.nextInt(methods.length)];
                // Over-tender generously; the service works out the change.
                BigDecimal tendered = method.requiresTender() ? new BigDecimal("500.00") : BigDecimal.ZERO;

                try {
                    SaleResponse sale = sales.checkout(new CheckoutRequest(
                            items,
                            null,
                            method,
                            tendered,
                            BigDecimal.ZERO,
                            cashiers[random.nextInt(cashiers.length)],
                            null));

                    backdate(sale.id(), day, 9 + random.nextInt(11), random.nextInt(60));
                } catch (RuntimeException ex) {
                    // Demo data must never stop the application from booting.
                    log.debug("Skipped a seeded sale: {}", ex.getMessage());
                }
            }
        }
    }

    private void backdate(Long saleId, LocalDate day, int hour, int minute) {
        saleRepository.findById(saleId).ifPresent(sale -> {
            sale.setSoldAt(day.atTime(LocalTime.of(hour, minute)).atZone(clock.getZone()).toInstant());
            saleRepository.save(sale);
        });
    }
}
