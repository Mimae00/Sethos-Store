package com.possystem.pos.repository;

import com.possystem.pos.domain.DocumentCounter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DocumentCounterRepository extends JpaRepository<DocumentCounter, Long> {

    /**
     * Locks the counter row for the duration of the transaction so two concurrent
     * checkouts cannot be handed the same receipt number.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from DocumentCounter c where c.scope = :scope")
    Optional<DocumentCounter> findByScopeForUpdate(@Param("scope") String scope);
}
