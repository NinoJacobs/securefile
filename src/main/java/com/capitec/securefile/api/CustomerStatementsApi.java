package com.capitec.securefile.api;

import com.capitec.securefile.model.request.StatementGenerationRequest;
import com.capitec.securefile.model.response.StatementDetailResponse;
import com.capitec.securefile.model.response.StatementSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@GlobalApi
public interface CustomerStatementsApi {

    @Operation(summary = "List statements for the current customer")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved statements for the current customer.",
                            content = {
                                    @Content(
                                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = StatementSummaryResponse.class))
                            })
            })
    ResponseEntity<List<StatementSummaryResponse>> listStatements();

    @Operation(summary = "Request a statement for the current customer")
    ResponseEntity<StatementDetailResponse> requestStatement(@Valid @RequestBody StatementGenerationRequest request);

    @Operation(summary = "Get a single statement for the current customer")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved the statement for the current customer.",
                            content = {
                                    @Content(
                                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = StatementDetailResponse.class))
                            }),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Statement not found.",
                            content = @Content)
            })
    ResponseEntity<StatementDetailResponse> getStatement(@Valid @NotBlank String statementId);

    @Operation(summary = "Download a statement using a temporary link token")
    ResponseEntity<Resource> downloadStatement(@Valid @NotBlank String statementId, @Valid @NotBlank String token);
}
