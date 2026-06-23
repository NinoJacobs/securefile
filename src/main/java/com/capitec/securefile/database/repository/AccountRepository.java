package com.capitec.securefile.database.repository;

import com.capitec.securefile.database.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findFirstByCustomerIdAndStatusOrderByIdAsc(Long customerId, String status);
}
