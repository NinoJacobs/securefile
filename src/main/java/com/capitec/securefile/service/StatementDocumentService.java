package com.capitec.securefile.service;

import com.capitec.securefile.database.entity.Statement;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class StatementDocumentService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final String PDF_HEADER = "%PDF-1.4\n";

    public byte[] createStatementDocument(Statement statement) {
        List<String> lines = List.of(
                "Securefile Statement",
                "Statement ID: %s".formatted(statement.getId() == null ? "pending" : statement.getId()),
                "Customer Number: %s".formatted(statement.getCustomer().getCustomerNumber()),
                "Statement Name: %s".formatted(statement.getStatementName()),
                "Period Start: %s".formatted(formatDate(statement.getPeriodStart())),
                "Period End: %s".formatted(formatDate(statement.getPeriodEnd())),
                "Generated At: %s".formatted(formatDateTime(statement.getGeneratedAt()))
        );

        return buildPdf(lines);
    }

    private byte[] buildPdf(List<String> lines) {
        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append("BT\n");
        contentBuilder.append("/F1 12 Tf\n");
        contentBuilder.append("50 760 Td\n");
        contentBuilder.append("16 TL\n");

        for (int index = 0; index < lines.size(); index++) {
            if (index > 0) {
                contentBuilder.append("T*\n");
            }
            contentBuilder.append("(").append(escapePdf(lines.get(index))).append(") Tj\n");
        }

        contentBuilder.append("ET\n");
        byte[] contentBytes = contentBuilder.toString().getBytes(StandardCharsets.US_ASCII);

        List<Integer> offsets = new ArrayList<>();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        write(output, PDF_HEADER);

        writeObject(output, offsets, 1, """
                << /Type /Catalog /Pages 2 0 R >>
                """);
        writeObject(output, offsets, 2, """
                << /Type /Pages /Kids [3 0 R] /Count 1 >>
                """);
        writeObject(output, offsets, 3, """
                << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>
                """);
        writeObject(output, offsets, 4, """
                << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>
                """);

        offsets.add(output.size());
        write(output, "5 0 obj\n");
        write(output, "<< /Length %d >>\n".formatted(contentBytes.length));
        write(output, "stream\n");
        output.writeBytes(contentBytes);
        write(output, "endstream\n");
        write(output, "endobj\n");

        int xrefStart = output.size();
        write(output, "xref\n");
        write(output, "0 6\n");
        write(output, "0000000000 65535 f \n");
        for (Integer offset : offsets) {
            write(output, "%010d 00000 n \n".formatted(offset));
        }
        write(output, "trailer\n");
        write(output, "<< /Size 6 /Root 1 0 R >>\n");
        write(output, "startxref\n");
        write(output, String.valueOf(xrefStart));
        write(output, "\n%%EOF\n");

        return output.toByteArray();
    }

    private void writeObject(ByteArrayOutputStream output, List<Integer> offsets, int objectNumber, String body) {
        offsets.add(output.size());
        write(output, "%d 0 obj\n".formatted(objectNumber));
        write(output, body.strip());
        write(output, "\nendobj\n");
    }

    private void write(ByteArrayOutputStream output, String value) {
        output.writeBytes(value.getBytes(StandardCharsets.US_ASCII));
    }

    private String escapePdf(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    private String formatDate(java.time.LocalDate value) {
        return value == null ? "n/a" : DATE_FORMAT.format(value);
    }

    private String formatDateTime(java.time.LocalDateTime value) {
        return value == null ? "n/a" : DATE_TIME_FORMAT.format(value);
    }
}
