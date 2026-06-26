package com.capitec.securefile.service;

import com.capitec.securefile.common.mapper.StatementApiMapper;
import com.capitec.securefile.common.util.CurrentUser;
import com.capitec.securefile.database.entity.Account;
import com.capitec.securefile.database.entity.Customer;
import com.capitec.securefile.database.entity.Statement;
import com.capitec.securefile.database.repository.StatementRepository;
import com.capitec.securefile.model.response.DownloadLinkResponse;
import com.capitec.securefile.model.response.StatementSummaryResponse;
import com.capitec.securefile.storage.service.StatementObjectStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerStatementsServiceTest {

    @Mock
    private StatementRepository statementRepository;
    @Mock
    private StatementApiMapper statementApiMapper;
    @Mock
    private StatementDomainSupportService statementDomainSupportService;
    @Mock
    private StatementDownloadLinkService statementDownloadLinkService;
    @Mock
    private StatementGenerationService statementGenerationService;
    @Mock
    private StatementObjectStorageService statementObjectStorageService;

    @Test
    void listMyStatementsExcludesCustomStatements() {
        CustomerStatementsService service = new CustomerStatementsService(
                statementRepository,
                statementApiMapper,
                statementDomainSupportService,
                statementDownloadLinkService,
                statementGenerationService,
                statementObjectStorageService
        );

        Customer customer = Customer.builder().id(1L).build();
        Account account = Account.builder().id(10L).accountNumber("100000000001").customer(customer).build();

        Statement customStatement = Statement.builder()
                .id(100L)
                .customer(customer)
                .account(account)
                .statementName("Custom Statement")
                .periodStart(LocalDate.of(2026, 1, 10))
                .periodEnd(LocalDate.of(2026, 2, 10))
                .build();

        Statement standardStatement = Statement.builder()
                .id(101L)
                .customer(customer)
                .account(account)
                .statementName("1 Month Statement")
                .periodStart(LocalDate.of(2026, 5, 1))
                .periodEnd(LocalDate.of(2026, 5, 31))
                .build();

        DownloadLinkResponse downloadLink = DownloadLinkResponse.builder()
                .statementId("101")
                .url("/api/v1/customers/me/statements/101/download?token=test")
                .expiresAt(OffsetDateTime.parse("2026-06-26T12:00:00Z"))
                .build();

        StatementSummaryResponse expectedResponse = StatementSummaryResponse.builder()
                .statementId("101")
                .statementName("1 Month Statement")
                .customerId("1")
                .accountNumberMasked("****0001")
                .periodStart(LocalDate.of(2026, 5, 1))
                .periodEnd(LocalDate.of(2026, 5, 31))
                .generatedAt(OffsetDateTime.parse("2026-06-01T08:00:00Z"))
                .downloadUrl(downloadLink.getUrl())
                .downloadUrlExpiresAt(downloadLink.getExpiresAt())
                .build();

        when(statementRepository.findByCustomerIdOrderByPeriodEndDesc(1L))
                .thenReturn(List.of(customStatement, standardStatement));
        when(statementDownloadLinkService.refreshDownloadLink(standardStatement)).thenReturn(downloadLink);
        when(statementApiMapper.toStatementSummaryResponse(standardStatement, downloadLink)).thenReturn(expectedResponse);

        try (MockedStatic<CurrentUser> currentUser = mockStatic(CurrentUser.class)) {
            currentUser.when(CurrentUser::requiredCustomerId).thenReturn(1L);

            List<StatementSummaryResponse> result = service.listMyStatements();

            assertThat(result).containsExactly(expectedResponse);
        }

        verify(statementRepository).findByCustomerIdOrderByPeriodEndDesc(1L);
        verify(statementDownloadLinkService).refreshDownloadLink(standardStatement);
        verify(statementApiMapper).toStatementSummaryResponse(standardStatement, downloadLink);
        verify(statementRepository).save(standardStatement);
        verifyNoMoreInteractions(statementDownloadLinkService, statementApiMapper);
    }
}
