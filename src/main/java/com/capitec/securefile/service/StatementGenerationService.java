package com.capitec.securefile.service;

import com.capitec.securefile.database.entity.Account;
import com.capitec.securefile.database.entity.AccountTransaction;
import com.capitec.securefile.database.entity.Customer;
import com.capitec.securefile.database.entity.Statement;
import com.capitec.securefile.database.repository.AccountRepository;
import com.capitec.securefile.database.repository.AccountTransactionRepository;
import com.capitec.securefile.database.repository.StatementRepository;
import com.capitec.securefile.model.request.StatementPeriod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class StatementGenerationService {

    private final AccountRepository accountRepository;
    private final AccountTransactionRepository accountTransactionRepository;
    private final StatementRepository statementRepository;
    private final StatementDocumentService statementDocumentService;
    private final StatementObjectStorageService statementObjectStorageService;

    @Transactional
    public Statement generateStatement(Long customerId, StatementPeriod period, LocalDate startDate, LocalDate endDate) {
        Account account = accountRepository.findFirstByCustomerIdAndStatusOrderByIdAsc(customerId, "ACTIVE")
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No active account found for customer"));

        return generateStatement(account, period, startDate, endDate);
    }

    private Statement generateStatement(Account account, StatementPeriod period, LocalDate startDate, LocalDate endDate) {
        Customer customer = account.getCustomer();
        StatementPeriod selectedPeriod = period == null ? StatementPeriod.ONE_MONTH : period;
        DateRange dateRange = resolveDateRange(selectedPeriod, startDate, endDate);

        return statementRepository.findByCustomerIdAndAccountIdAndPeriodStartAndPeriodEnd(
                        customer.getId(),
                        account.getId(),
                        dateRange.start(),
                        dateRange.end())
                .orElseGet(() -> createStatement(customer, account, selectedPeriod, dateRange));
    }

    public byte[] createStatementDocument(Statement statement) {
        List<AccountTransaction> transactions = transactionsFor(statement);
        return statementDocumentService.createStatementDocument(statement, transactions);
    }

    private Statement createStatement(Customer customer, Account account, StatementPeriod period, DateRange dateRange) {
        List<AccountTransaction> transactions = transactionsFor(account, dateRange);
        if (transactions.isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "No account transactions found for requested period");
        }

        LocalDateTime generatedAt = LocalDateTime.now();
        String periodSuffix = "%s-to-%s".formatted(dateRange.start(), dateRange.end());
        String fileName = "%s-%s.pdf".formatted(customer.getCustomerNumber(), periodSuffix);
        String fileKey = "statements/%s/%s-%s.pdf".formatted(customer.getCustomerNumber(), periodSuffix, UUID.randomUUID());

        Statement statement = Statement.builder()
                .customer(customer)
                .account(account)
                .statementName(statementName(period, periodSuffix))
                .periodStart(dateRange.start())
                .periodEnd(dateRange.end())
                .fileKey(fileKey)
                .fileName(fileName)
                .contentType("application/pdf")
                .generatedAt(generatedAt)
                .build();

        byte[] content = statementDocumentService.createStatementDocument(statement, transactions);
        StatementObjectStorageService.StoredStatementObject storedObject =
                statementObjectStorageService.storeStatement(fileKey, statement.getContentType(), content);

        statement.setFileSizeBytes(storedObject.fileSizeBytes());
        statement.setChecksum(storedObject.checksum());
        return statementRepository.save(statement);
    }

    private String statementName(StatementPeriod period, String periodSuffix) {
        if (period == StatementPeriod.CUSTOM) {
            return "Custom Statement";
        }
        return "Statement %s".formatted(periodSuffix);
    }

    private List<AccountTransaction> transactionsFor(Statement statement) {
        return transactionsFor(statement.getAccount(), new DateRange(statement.getPeriodStart(), statement.getPeriodEnd()));
    }

    private List<AccountTransaction> transactionsFor(Account account, DateRange dateRange) {
        return accountTransactionRepository.findByAccountIdAndTransactionDateBetweenOrderByTransactionDateAscIdAsc(
                account.getId(),
                dateRange.start(),
                dateRange.end());
    }

    public DateRange resolveDateRange(StatementPeriod period, LocalDate startDate, LocalDate endDate) {
        StatementPeriod selectedPeriod = period == null ? StatementPeriod.ONE_MONTH : period;
        return switch (selectedPeriod) {
            case ONE_MONTH -> presetRange(1);
            case THREE_MONTHS -> presetRange(3);
            case SIX_MONTHS -> presetRange(6);
            case NINE_MONTHS -> presetRange(9);
            case CUSTOM -> customRange(startDate, endDate);
        };
    }

    private DateRange presetRange(int months) {
        LocalDate end = YearMonth.now().minusMonths(1).atEndOfMonth();
        LocalDate start = end.minusMonths(months - 1).withDayOfMonth(1);
        return new DateRange(start, end);
    }

    private DateRange customRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Custom statements require startDate and endDate");
        }
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(BAD_REQUEST, "startDate must be on or before endDate");
        }
        return new DateRange(startDate, endDate);
    }

    public record DateRange(LocalDate start, LocalDate end) {
    }
}
