package com.capitec.securefile.service;

import com.capitec.securefile.database.entity.Account;
import com.capitec.securefile.database.entity.AccountTransaction;
import com.capitec.securefile.database.entity.Customer;
import com.capitec.securefile.database.entity.Statement;
import com.capitec.securefile.database.repository.AccountRepository;
import com.capitec.securefile.database.repository.AccountTransactionRepository;
import com.capitec.securefile.database.repository.StatementRepository;
import com.capitec.securefile.model.request.StatementPeriod;
import com.capitec.securefile.storage.service.StatementObjectStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class StatementGenerationServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AccountTransactionRepository accountTransactionRepository;
    @Mock
    private StatementRepository statementRepository;
    @Mock
    private StatementDocumentService statementDocumentService;
    @Mock
    private StatementObjectStorageService statementObjectStorageService;
    @Mock
    private StatementFileFactory statementFileFactory;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private TransactionStatus transactionStatus;

    private StatementGenerationService service;

    @BeforeEach
    void setUp() {
        service = new StatementGenerationService(
                accountRepository,
                accountTransactionRepository,
                statementRepository,
                statementDocumentService,
                statementObjectStorageService,
                statementFileFactory,
                transactionManager
        );
    }

    @Test
    void returnsExistingStatementWhenOneAlreadyMatchesRequestedRange() {
        Account account = activeAccount();
        LocalDate start = LocalDate.of(2026, 5, 1);
        LocalDate end = LocalDate.of(2026, 5, 31);
        Statement existing = existingStatement(account.getCustomer(), account, start, end);

        when(accountRepository.findFirstByCustomerIdAndStatusOrderByIdAsc(1L, "ACTIVE"))
                .thenReturn(Optional.of(account));
        when(statementRepository.findByCustomerIdAndAccountIdAndPeriodStartAndPeriodEnd(1L, 10L, start, end))
                .thenReturn(Optional.of(existing));

        Statement result = service.generateStatement(1L, StatementPeriod.CUSTOM, start, end);

        assertThat(result).isSameAs(existing);
        verifyNoInteractions(accountTransactionRepository, statementDocumentService, statementObjectStorageService, statementFileFactory);
    }

    @Test
    void createsAndStoresStatementWhenNoExistingStatementMatchesRange() {
        Account account = activeAccount();
        Customer customer = account.getCustomer();
        LocalDate start = LocalDate.of(2026, 5, 1);
        LocalDate end = LocalDate.of(2026, 5, 31);
        List<AccountTransaction> transactions = List.of(transaction(account, "Salary", "REF001"));
        StatementFileFactory.StatementFile statementFile = new StatementFileFactory.StatementFile(
                "Statement 2026-05-01-to-2026-05-31",
                "CUST-0001-2026-05.pdf",
                "statements/CUST-0001/2026-05.pdf",
                "application/pdf"
        );
        byte[] documentBytes = "pdf-content".getBytes();

        when(accountRepository.findFirstByCustomerIdAndStatusOrderByIdAsc(1L, "ACTIVE"))
                .thenReturn(Optional.of(account));
        when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(transactionStatus);
        when(statementRepository.findByCustomerIdAndAccountIdAndPeriodStartAndPeriodEnd(1L, 10L, start, end))
                .thenReturn(Optional.empty());
        when(accountTransactionRepository.findByAccountIdAndTransactionDateBetweenOrderByTransactionDateAscIdAsc(10L, start, end))
                .thenReturn(transactions);
        when(statementFileFactory.create(customer, StatementPeriod.CUSTOM, new StatementGenerationService.DateRange(start, end)))
                .thenReturn(statementFile);
        when(statementRepository.saveAndFlush(any(Statement.class))).thenAnswer(invocation -> {
            Statement statement = invocation.getArgument(0);
            statement.setId(100L);
            return statement;
        });
        when(statementDocumentService.createStatementDocument(any(Statement.class), any())).thenReturn(documentBytes);
        when(statementObjectStorageService.storeStatement(statementFile.fileKey(), statementFile.contentType(), documentBytes))
                .thenReturn(new StatementObjectStorageService.StoredStatementObject(statementFile.fileKey(), documentBytes.length, "checksum-123"));
        when(statementRepository.save(any(Statement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Statement result = service.generateStatement(1L, StatementPeriod.CUSTOM, start, end);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getStatementName()).isEqualTo(statementFile.statementName());
        assertThat(result.getFileName()).isEqualTo(statementFile.fileName());
        assertThat(result.getFileKey()).isEqualTo(statementFile.fileKey());
        assertThat(result.getContentType()).isEqualTo(statementFile.contentType());
        assertThat(result.getFileSizeBytes()).isEqualTo((long) documentBytes.length);
        assertThat(result.getChecksum()).isEqualTo("checksum-123");

        ArgumentCaptor<Statement> savedStatementCaptor = ArgumentCaptor.forClass(Statement.class);
        verify(statementRepository).saveAndFlush(savedStatementCaptor.capture());
        Statement savedStatement = savedStatementCaptor.getValue();
        assertThat(savedStatement.getCustomer()).isSameAs(customer);
        assertThat(savedStatement.getAccount()).isSameAs(account);
        assertThat(savedStatement.getPeriodStart()).isEqualTo(start);
        assertThat(savedStatement.getPeriodEnd()).isEqualTo(end);
        assertThat(savedStatement.getGeneratedAt()).isNotNull();
        verify(statementObjectStorageService).storeStatement(statementFile.fileKey(), statementFile.contentType(), documentBytes);
        verify(transactionManager).commit(transactionStatus);
    }

    @Test
    void returnsExistingStatementWhenConcurrentInsertCausesDuplicateSave() {
        Account account = activeAccount();
        LocalDate start = LocalDate.of(2026, 5, 1);
        LocalDate end = LocalDate.of(2026, 5, 31);
        List<AccountTransaction> transactions = List.of(transaction(account, "Salary", "REF001"));
        Statement existing = existingStatement(account.getCustomer(), account, start, end);

        when(accountRepository.findFirstByCustomerIdAndStatusOrderByIdAsc(1L, "ACTIVE"))
                .thenReturn(Optional.of(account));
        when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(transactionStatus);
        when(statementRepository.findByCustomerIdAndAccountIdAndPeriodStartAndPeriodEnd(1L, 10L, start, end))
                .thenReturn(Optional.empty(), Optional.of(existing));
        when(accountTransactionRepository.findByAccountIdAndTransactionDateBetweenOrderByTransactionDateAscIdAsc(10L, start, end))
                .thenReturn(transactions);
        when(statementFileFactory.create(any(), any(), any()))
                .thenReturn(new StatementFileFactory.StatementFile("Statement", "file.pdf", "key.pdf", "application/pdf"));
        when(statementRepository.saveAndFlush(any(Statement.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        Statement result = service.generateStatement(1L, StatementPeriod.CUSTOM, start, end);

        assertThat(result).isSameAs(existing);
        verifyNoInteractions(statementDocumentService, statementObjectStorageService);
        verify(transactionManager).rollback(transactionStatus);
    }

    @Test
    void throwsWhenCustomerHasNoActiveAccount() {
        when(accountRepository.findFirstByCustomerIdAndStatusOrderByIdAsc(1L, "ACTIVE"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateStatement(1L, StatementPeriod.ONE_MONTH, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(NOT_FOUND));

        verifyNoInteractions(accountTransactionRepository, statementRepository, statementDocumentService, statementObjectStorageService, statementFileFactory);
    }

    @Test
    void throwsWhenNoTransactionsExistForRequestedPeriod() {
        Account account = activeAccount();
        LocalDate start = LocalDate.of(2026, 5, 1);
        LocalDate end = LocalDate.of(2026, 5, 31);

        when(accountRepository.findFirstByCustomerIdAndStatusOrderByIdAsc(1L, "ACTIVE"))
                .thenReturn(Optional.of(account));
        when(statementRepository.findByCustomerIdAndAccountIdAndPeriodStartAndPeriodEnd(1L, 10L, start, end))
                .thenReturn(Optional.empty());
        when(accountTransactionRepository.findByAccountIdAndTransactionDateBetweenOrderByTransactionDateAscIdAsc(10L, start, end))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.generateStatement(1L, StatementPeriod.CUSTOM, start, end))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(NOT_FOUND));

        verify(statementRepository, never()).saveAndFlush(any(Statement.class));
        verifyNoInteractions(statementDocumentService, statementObjectStorageService);
    }

    @Test
    void createStatementDocumentDelegatesToDocumentServiceWithTransactionsForStatementRange() {
        Account account = activeAccount();
        Statement statement = existingStatement(account.getCustomer(), account, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));
        List<AccountTransaction> transactions = List.of(transaction(account, "POS Purchase", "REF002"));
        byte[] documentBytes = "statement-pdf".getBytes();

        when(accountTransactionRepository.findByAccountIdAndTransactionDateBetweenOrderByTransactionDateAscIdAsc(
                10L,
                statement.getPeriodStart(),
                statement.getPeriodEnd()))
                .thenReturn(transactions);
        when(statementDocumentService.createStatementDocument(statement, transactions)).thenReturn(documentBytes);

        byte[] result = service.createStatementDocument(statement);

        assertThat(result).isEqualTo(documentBytes);
        verify(statementDocumentService).createStatementDocument(statement, transactions);
    }

    @Test
    void resolveDateRangeDefaultsToOneMonthWhenPeriodIsNull() {
        StatementGenerationService.DateRange dateRange = service.resolveDateRange(null, null, null);

        LocalDate expectedEnd = YearMonth.now().minusMonths(1).atEndOfMonth();
        LocalDate expectedStart = expectedEnd.withDayOfMonth(1);
        assertThat(dateRange.start()).isEqualTo(expectedStart);
        assertThat(dateRange.end()).isEqualTo(expectedEnd);
    }

    @Test
    void resolveDateRangeSupportsPresetPeriods() {
        StatementGenerationService.DateRange dateRange = service.resolveDateRange(StatementPeriod.THREE_MONTHS, null, null);

        LocalDate expectedEnd = YearMonth.now().minusMonths(1).atEndOfMonth();
        LocalDate expectedStart = expectedEnd.minusMonths(2).withDayOfMonth(1);
        assertThat(dateRange.start()).isEqualTo(expectedStart);
        assertThat(dateRange.end()).isEqualTo(expectedEnd);
    }

    @Test
    void resolveDateRangeRejectsMissingCustomDates() {
        assertThatThrownBy(() -> service.resolveDateRange(StatementPeriod.CUSTOM, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(BAD_REQUEST));
    }

    @Test
    void resolveDateRangeRejectsCustomRangeWhenStartIsAfterEnd() {
        assertThatThrownBy(() -> service.resolveDateRange(
                StatementPeriod.CUSTOM,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 5, 31)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(BAD_REQUEST));
    }

    private Account activeAccount() {
        Customer customer = Customer.builder()
                .id(1L)
                .customerNumber("CUST-0001")
                .status("ACTIVE")
                .build();
        return Account.builder()
                .id(10L)
                .customer(customer)
                .accountNumber("100000000001")
                .accountType("TRANSACTIONAL")
                .currentBalance(new BigDecimal("1200.00"))
                .status("ACTIVE")
                .build();
    }

    private Statement existingStatement(Customer customer, Account account, LocalDate start, LocalDate end) {
        return Statement.builder()
                .id(77L)
                .customer(customer)
                .account(account)
                .statementName("Statement")
                .periodStart(start)
                .periodEnd(end)
                .fileKey("existing.pdf")
                .fileName("existing.pdf")
                .contentType("application/pdf")
                .generatedAt(LocalDateTime.of(2026, 6, 1, 10, 0))
                .build();
    }

    private AccountTransaction transaction(Account account, String description, String reference) {
        return AccountTransaction.builder()
                .id(200L)
                .account(account)
                .transactionDate(LocalDate.of(2026, 5, 10))
                .description(description)
                .reference(reference)
                .amount(new BigDecimal("100.00"))
                .balanceAfter(new BigDecimal("1200.00"))
                .build();
    }
}
