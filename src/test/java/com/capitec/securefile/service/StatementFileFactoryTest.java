package com.capitec.securefile.service;

import com.capitec.securefile.database.entity.Customer;
import com.capitec.securefile.model.request.StatementPeriod;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class StatementFileFactoryTest {

    private final StatementFileFactory statementFileFactory = new StatementFileFactory();

    @Test
    void shouldCreateStandardStatementFileDetails() {
        Customer customer = Customer.builder().customerNumber("CUST-0001").build();
        StatementGenerationService.DateRange dateRange = new StatementGenerationService.DateRange(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31));

        StatementFileFactory.StatementFile statementFile =
                statementFileFactory.create(customer, StatementPeriod.ONE_MONTH, dateRange);

        assertThat(statementFile.statementName()).isEqualTo("Statement 2026-01-01-to-2026-01-31");
        assertThat(statementFile.fileName()).isEqualTo("CUST-0001-2026-01-01-to-2026-01-31.pdf");
        assertThat(statementFile.fileKey())
                .startsWith("statements/CUST-0001/2026-01-01-to-2026-01-31-")
                .endsWith(".pdf");
        assertThat(statementFile.contentType()).isEqualTo("application/pdf");
    }

    @Test
    void shouldCreateCustomStatementFileDetails() {
        Customer customer = Customer.builder().customerNumber("CUST-0002").build();
        StatementGenerationService.DateRange dateRange = new StatementGenerationService.DateRange(
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 28));

        StatementFileFactory.StatementFile statementFile =
                statementFileFactory.create(customer, StatementPeriod.CUSTOM, dateRange);

        assertThat(statementFile.statementName()).isEqualTo("Custom Statement");
        assertThat(statementFile.fileName()).isEqualTo("CUST-0002-2026-02-01-to-2026-02-28.pdf");
        assertThat(statementFile.fileKey())
                .startsWith("statements/CUST-0002/2026-02-01-to-2026-02-28-")
                .endsWith(".pdf");
        assertThat(statementFile.contentType()).isEqualTo("application/pdf");
    }
}
