package com.capitec.securefile.database.repository;

import com.capitec.securefile.database.entity.Statement;
import com.capitec.securefile.database.enums.StatementStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StatementRepository extends JpaRepository<Statement, Long> {

    Optional<Statement> findByIdAndCustomerId(Long id, Long customerId);

    List<Statement> findByCustomerIdOrderByPeriodEndDesc(Long customerId);

    List<Statement> findByCustomerIdAndStatusOrderByPeriodEndDesc(Long customerId, StatementStatus status);

    List<Statement> findByCustomerIdAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqualOrderByPeriodEndDesc(
            Long customerId,
            LocalDate periodStart,
            LocalDate periodEnd
    );
}
