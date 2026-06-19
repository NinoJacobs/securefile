package com.capitec.securefile.api;

import com.capitec.securefile.model.response.AdminCustomerResponse;
import com.capitec.securefile.model.response.StatementDetailResponse;
import com.capitec.securefile.model.response.StatementSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@GlobalApi
public interface AdminStatementsApi {

    @Operation(summary = "List all customers for administrator selection")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved customers.",
                            content = {
                                    @Content(
                                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            array = @ArraySchema(schema = @Schema(implementation = AdminCustomerResponse.class)))
                            })
            })
    ResponseEntity<List<AdminCustomerResponse>> listCustomers();

    @Operation(summary = "List statements for a customer as an administrator")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved statements for the customer.",
                            content = {
                                    @Content(
                                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = StatementSummaryResponse.class))
                            })
            })
    ResponseEntity<List<StatementSummaryResponse>> listCustomerStatements(@Valid @NotBlank String customerId);

    @Operation(summary = "Request statement generation for a customer as an administrator")
    ResponseEntity<StatementDetailResponse> generateCustomerStatement(
            @Valid @NotBlank String customerId);

    @Operation(summary = "Upload a PDF statement for a customer as an administrator")
    ResponseEntity<StatementDetailResponse> uploadCustomerStatement(
            @Valid @NotBlank String customerId,
            MultipartFile file);
}
