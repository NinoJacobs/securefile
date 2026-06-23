package com.capitec.securefile.database.repository;

import com.capitec.securefile.database.entity.AccountTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, Long> {

    List<AccountTransaction> findByAccountIdAndTransactionDateBetweenOrderByTransactionDateAscIdAsc(
            Long accountId,
            LocalDate periodStart,
            LocalDate periodEnd
    );
}
