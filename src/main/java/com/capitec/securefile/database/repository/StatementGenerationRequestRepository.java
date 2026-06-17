package com.capitec.securefile.database.repository;

import com.capitec.securefile.database.entity.StatementGenerationRequest;
import com.capitec.securefile.database.enums.GenerationRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StatementGenerationRequestRepository extends JpaRepository<StatementGenerationRequest, Long> {

    List<StatementGenerationRequest> findByCustomerIdOrderByRequestedAtDesc(Long customerId);

    List<StatementGenerationRequest> findByStatusOrderByRequestedAtAsc(GenerationRequestStatus status);
}
