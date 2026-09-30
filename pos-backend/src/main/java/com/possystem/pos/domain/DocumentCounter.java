package com.possystem.pos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Row-per-day counter used to hand out gap-free receipt numbers.
 *
 * <p>Deliberately not a {@code BaseEntity}: it carries no auditing value and is read
 * under a pessimistic write lock, so the optimistic {@code @Version} column would only
 * add contention.</p>
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(
        name = "document_counters",
        uniqueConstraints = @UniqueConstraint(name = "uk_document_counters_scope", columnNames = "scope")
)
public class DocumentCounter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Namespace for the counter, e.g. {@code SALE-20260927}. */
    @Column(name = "scope", nullable = false, length = 64)
    private String scope;

    @Column(name = "next_value", nullable = false)
    private long nextValue = 1L;

    public DocumentCounter(String scope) {
        this.scope = scope;
        this.nextValue = 1L;
    }

    public long take() {
        long current = nextValue;
        nextValue = current + 1;
        return current;
    }
}
