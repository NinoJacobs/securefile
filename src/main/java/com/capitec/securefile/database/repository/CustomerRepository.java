package com.capitec.securefile.database.repository;

import com.capitec.securefile.database.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findFirstByOrderByIdAsc();

    Optional<Customer> findByUserId(Long userId);

    Optional<Customer> findByCustomerNumber(String customerNumber);

    boolean existsByCustomerNumber(String customerNumber);
}
