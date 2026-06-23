package com.capitec.securefile.util.mapper;

import com.capitec.securefile.database.entity.Customer;
import com.capitec.securefile.database.entity.Statement;
import com.capitec.securefile.model.response.AdminCustomerResponse;
import com.capitec.securefile.model.response.DownloadLinkResponse;
import com.capitec.securefile.model.response.StatementDetailResponse;
import com.capitec.securefile.model.response.StatementSummaryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel = "spring")
public interface StatementApiMapper {

    @Mapping(target = "customerId", source = "id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "email", source = "user.email")
    AdminCustomerResponse toAdminCustomerResponse(Customer customer);

    @Mapping(target = "statementId", source = "statement.id")
    @Mapping(target = "customerId", source = "statement.customer.id")
    @Mapping(target = "accountNumberMasked", source = "statement.account.accountNumber", qualifiedByName = "maskAccountNumber")
    @Mapping(target = "generatedAt", source = "statement.generatedAt")
    @Mapping(target = "downloadUrl", source = "downloadLink.url")
    @Mapping(target = "downloadUrlExpiresAt", source = "downloadLink.expiresAt")
    StatementSummaryResponse toStatementSummaryResponse(Statement statement, DownloadLinkResponse downloadLink);

    @Mapping(target = "statementId", source = "statement.id")
    @Mapping(target = "customerId", source = "statement.customer.id")
    @Mapping(target = "accountNumberMasked", source = "statement.account.accountNumber", qualifiedByName = "maskAccountNumber")
    @Mapping(target = "generatedAt", source = "statement.generatedAt")
    @Mapping(target = "fileSizeBytes", source = "statement.fileSizeBytes")
    @Mapping(target = "downloadUrl", source = "downloadLink.url")
    @Mapping(target = "downloadUrlExpiresAt", source = "downloadLink.expiresAt")
    StatementDetailResponse toStatementDetailResponse(Statement statement, DownloadLinkResponse downloadLink);

    default String map(Long value) {
        return value == null ? null : value.toString();
    }

    @Named("maskAccountNumber")
    static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return "****";
        }
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }

    default OffsetDateTime map(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }
}
