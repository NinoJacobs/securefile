package com.capitec.securefile.service;

import com.capitec.securefile.database.entity.Customer;
import com.capitec.securefile.model.request.StatementPeriod;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class StatementFileFactory {

    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final String CUSTOM_STATEMENT_NAME = "Custom Statement";

    public StatementFile create(Customer customer, StatementPeriod period, StatementGenerationService.DateRange dateRange) {
        String periodSuffix = "%s-to-%s".formatted(dateRange.start(), dateRange.end());
        String customerNumber = customer.getCustomerNumber();
        return new StatementFile(
                statementName(period, periodSuffix),
                "%s-%s.pdf".formatted(customerNumber, periodSuffix),
                "statements/%s/%s-%s.pdf".formatted(customerNumber, periodSuffix, UUID.randomUUID()),
                PDF_CONTENT_TYPE);
    }

    private String statementName(StatementPeriod period, String periodSuffix) {
        if (period == StatementPeriod.CUSTOM) {
            return CUSTOM_STATEMENT_NAME;
        }
        return "Statement %s".formatted(periodSuffix);
    }

    public record StatementFile(
            String statementName,
            String fileName,
            String fileKey,
            String contentType) {
    }
}
