package com.capitec.securefile.service;

import com.capitec.securefile.database.entity.Account;
import com.capitec.securefile.database.entity.AccountTransaction;
import com.capitec.securefile.database.entity.Customer;
import com.capitec.securefile.database.entity.Statement;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StatementDocumentServiceTest {

    private final StatementDocumentService service = new StatementDocumentService();

    @Test
    void generatesPdfForNormalStatementWithTransactions() throws IOException {
        Statement statement = statement("100000000001");
        AccountTransaction transaction = AccountTransaction.builder()
                .transactionDate(LocalDate.of(2026, 5, 10))
                .description("Salary payment")
                .reference("REF123")
                .amount(new BigDecimal("1000.00"))
                .balanceAfter(new BigDecimal("2000.00"))
                .build();

        byte[] pdfBytes = service.createStatementDocument(statement, List.of(transaction));
        String text = extractText(pdfBytes);

        assertThat(pdfBytes).isNotEmpty();
        assertThat(text).contains("Securefile Customer Statement");
        assertThat(text).contains("Customer Number: CUST-0001");
        assertThat(text).contains("Account Number: ****0001");
        assertThat(text).contains("Account Type: TRANSACTIONAL");
        assertThat(text).contains("Salary payment");
        assertThat(text).contains("REF123");
        assertThat(text).contains("R 1000.00");
        assertThat(text).contains("R 2000.00");
    }

    @Test
    void handlesEmptyTransactionListWithNoTransactionsMessage() throws IOException {
        Statement statement = statement("1234");

        byte[] pdfBytes = service.createStatementDocument(statement, List.of());
        String text = extractText(pdfBytes);

        assertThat(text).contains("No transactions for this period.");
        assertThat(text).contains("Account Number: ****");
    }

    @Test
    void trimsLongDescriptionAndReferenceValuesCorrectly() throws IOException {
        Statement statement = statement("100000000001");
        String longDescription = "This description is much longer than thirty four chars";
        String longReference = "REFERENCE-TOO-LONG";
        AccountTransaction transaction = AccountTransaction.builder()
                .transactionDate(LocalDate.of(2026, 5, 11))
                .description(longDescription)
                .reference(longReference)
                .amount(new BigDecimal("15.50"))
                .balanceAfter(new BigDecimal("1984.50"))
                .build();

        byte[] pdfBytes = service.createStatementDocument(statement, List.of(transaction));
        String text = extractText(pdfBytes);

        assertThat(text).contains(longDescription.substring(0, 33));
        assertThat(text).doesNotContain(longDescription);
        assertThat(text).contains(longReference.substring(0, 14));
        assertThat(text).doesNotContain(longReference);
    }

    @Test
    void masksNullAccountNumbersAsGenericMaskedValue() throws IOException {
        Statement statement = statement(null);

        byte[] pdfBytes = service.createStatementDocument(statement, List.of());
        String text = extractText(pdfBytes);

        assertThat(text).contains("Account Number: ****");
    }

    private Statement statement(String accountNumber) {
        Customer customer = Customer.builder()
                .id(1L)
                .customerNumber("CUST-0001")
                .build();
        Account account = Account.builder()
                .id(10L)
                .customer(customer)
                .accountNumber(accountNumber)
                .accountType("TRANSACTIONAL")
                .build();
        return Statement.builder()
                .id(55L)
                .customer(customer)
                .account(account)
                .statementName("Statement")
                .periodStart(LocalDate.of(2026, 5, 1))
                .periodEnd(LocalDate.of(2026, 5, 31))
                .generatedAt(LocalDateTime.of(2026, 6, 1, 12, 0))
                .build();
    }

    private String extractText(byte[] pdfBytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            return new PDFTextStripper().getText(document);
        }
    }
}
