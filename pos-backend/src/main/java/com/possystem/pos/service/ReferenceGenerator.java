package com.possystem.pos.service;

import com.possystem.pos.domain.DocumentCounter;
import com.possystem.pos.repository.DocumentCounterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Hands out receipt numbers of the form {@code S-20260927-0001}.
 *
 * <p>Runs in its own transaction (REQUIRES_NEW) so the counter row lock is released as
 * soon as the number is issued, instead of being held for the whole checkout.</p>
 */
@Service
public class ReferenceGenerator {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final DocumentCounterRepository counters;
    private final Clock clock;

    public ReferenceGenerator(DocumentCounterRepository counters, Clock clock) {
        this.counters = counters;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String nextSaleReference() {
        LocalDate today = LocalDate.now(clock);
        String day = today.format(DAY);
        String scope = "SALE-" + day;

        DocumentCounter counter = counters.findByScopeForUpdate(scope)
                .orElseGet(() -> counters.saveAndFlush(new DocumentCounter(scope)));
        long sequence = counter.take();
        counters.saveAndFlush(counter);

        return "S-%s-%04d".formatted(day, sequence);
    }
}
