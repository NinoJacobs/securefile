package com.capitec.securefile.mapper;

import com.capitec.securefile.database.entity.Customer;
import com.capitec.securefile.database.entity.Statement;
import com.capitec.securefile.model.response.AdminCustomerResponse;
import com.capitec.securefile.model.response.StatementDetailResponse;
import com.capitec.securefile.model.response.StatementSummaryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel = "spring")
public interface StatementApiMapper {

    @Mapping(target = "customerId", source = "id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "email", source = "user.email")
    AdminCustomerResponse toAdminCustomerResponse(Customer customer);

    @Mapping(target = "statementId", source = "id")
    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "accountNumberMasked", source = "customer.customerNumber")
    @Mapping(target = "generatedAt", source = "generatedAt")
    StatementSummaryResponse toStatementSummaryResponse(Statement statement);

    @Mapping(target = "statementId", source = "statement.id")
    @Mapping(target = "customerId", source = "statement.customer.id")
    @Mapping(target = "accountNumberMasked", source = "statement.customer.customerNumber")
    @Mapping(target = "generatedAt", source = "statement.generatedAt")
    @Mapping(target = "fileSizeBytes", source = "statement.fileSizeBytes")
    StatementDetailResponse toStatementDetailResponse(Statement statement);

    default String map(Long value) {
        return value == null ? null : value.toString();
    }

    default String mapCustomerNumber(String customerNumber) {
        if (customerNumber == null || customerNumber.length() <= 4) {
            return "****";
        }
        return "****" + customerNumber.substring(customerNumber.length() - 4);
    }

    default OffsetDateTime map(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }
}
