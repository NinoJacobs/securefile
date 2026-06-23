package com.capitec.securefile.service;

import com.capitec.securefile.database.entity.AccountTransaction;
import com.capitec.securefile.database.entity.Statement;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class StatementDocumentService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final PDType1Font REGULAR_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font BOLD_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    public byte[] createStatementDocument(Statement statement, List<AccountTransaction> transactions) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                TextWriter writer = new TextWriter(content, 50, 790);
                writer.bold("Securefile Customer Statement", 18);
                writer.space(18);
                writer.line("Customer Number: %s".formatted(statement.getCustomer().getCustomerNumber()));
                writer.line("Account Number: %s".formatted(maskAccountNumber(statement.getAccount().getAccountNumber())));
                writer.line("Account Type: %s".formatted(statement.getAccount().getAccountType()));
                writer.line("Statement Period: %s to %s".formatted(
                        DATE_FORMAT.format(statement.getPeriodStart()),
                        DATE_FORMAT.format(statement.getPeriodEnd())));
                writer.line("Generated At: %s".formatted(DATE_TIME_FORMAT.format(statement.getGeneratedAt())));
                writer.space(18);

                writer.bold("Transactions", 13);
                writer.line("Date        Description                         Reference        Amount       Balance");
                writer.line("----------  ----------------------------------  ---------------  -----------  -----------");

                if (transactions.isEmpty()) {
                    writer.line("No transactions for this period.");
                } else {
                    for (AccountTransaction transaction : transactions) {
                        writer.line(transactionLine(transaction));
                    }
                }
            }

            document.save(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to generate statement PDF",
                    ex);
        }
    }

    private String transactionLine(AccountTransaction transaction) {
        return "%-10s  %-34s  %-15s  %11s  %11s".formatted(
                DATE_FORMAT.format(transaction.getTransactionDate()),
                trim(transaction.getDescription(), 34),
                trim(transaction.getReference(), 15),
                money(transaction.getAmount()),
                money(transaction.getBalanceAfter()));
    }

    private String trim(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 1);
    }

    private String money(BigDecimal value) {
        return "R %.2f".formatted(value == null ? BigDecimal.ZERO : value);
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return "****";
        }
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }

    private static class TextWriter {
        private final PDPageContentStream content;
        private final float x;
        private float y;

        private TextWriter(PDPageContentStream content, float x, float y) {
            this.content = content;
            this.x = x;
            this.y = y;
        }

        private void bold(String text, int size) throws IOException {
            write(text, BOLD_FONT, size);
        }

        private void line(String text) throws IOException {
            write(text, REGULAR_FONT, 10);
        }

        private void space(float height) {
            y -= height;
        }

        private void write(String text, PDType1Font font, int size) throws IOException {
            content.beginText();
            content.setFont(font, size);
            content.newLineAtOffset(x, y);
            content.showText(text);
            content.endText();
            y -= size + 5;
        }
    }
}
