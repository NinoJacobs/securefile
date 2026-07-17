package com.capitec.securefile.api;

import com.capitec.securefile.common.exception.ErrorResponse;
import com.capitec.securefile.model.request.StatementGenerationRequest;
import com.capitec.securefile.model.response.StatementDetailResponse;
import com.capitec.securefile.model.response.StatementSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
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
                                            array = @ArraySchema(schema = @Schema(implementation = StatementSummaryResponse.class)))
                            }),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Authentication is required.",
                            content = {
                                    @Content(
                                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = ErrorResponse.class))
                            }),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Current user is not allowed to access customer statements.",
                            content = {
                                    @Content(
                                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = ErrorResponse.class))
                            })
            })
    ResponseEntity<List<StatementSummaryResponse>> listStatements();

    @Operation(summary = "Request a statement for the current customer")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Statement generated successfully.",
                            content = {
                                    @Content(
                                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = StatementDetailResponse.class))
                            }),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Request body failed validation.",
                            content = {
                                    @Content(
                                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = ErrorResponse.class))
                            }),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Authentication is required.",
                            content = {
                                    @Content(
                                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = ErrorResponse.class))
                            }),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Current user is not allowed to generate customer statements.",
                            content = {
                                    @Content(
                                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = ErrorResponse.class))
                            })
            })
    ResponseEntity<StatementDetailResponse> requestStatement(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Statement generation options for the current customer.",
                    content = {
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = StatementGenerationRequest.class))
                    })
            @Valid @RequestBody StatementGenerationRequest request);

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
                            content = {
                                    @Content(
                                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = ErrorResponse.class))
                            }),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Statement identifier is invalid.",
                            content = {
                                    @Content(
                                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = ErrorResponse.class))
                            }),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Authentication is required.",
                            content = {
                                    @Content(
                                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = ErrorResponse.class))
                            }),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Current user is not allowed to access this statement.",
                            content = {
                                    @Content(
                                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = ErrorResponse.class))
                            })
            })
    ResponseEntity<StatementDetailResponse> getStatement(
            @Parameter(description = "Numeric statement identifier.", example = "1")
            Long statementId);

    @Operation(summary = "Download a statement using a temporary link token")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Statement downloaded successfully.",
                            content = {
                                    @Content(mediaType = MediaType.APPLICATION_PDF_VALUE)
                            }),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Statement identifier or token is invalid.",
                            content = {
                                    @Content(
                                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = ErrorResponse.class))
                            }),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Authentication is required.",
                            content = {
                                    @Content(
                                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = ErrorResponse.class))
                            }),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Download token is invalid or expired, or the current user is not allowed to download the statement.",
                            content = {
                                    @Content(
                                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = ErrorResponse.class))
                            }),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Statement not found.",
                            content = {
                                    @Content(
                                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = ErrorResponse.class))
                            })
            })
    ResponseEntity<Resource> downloadStatement(
            @Parameter(description = "Numeric statement identifier.", example = "1")
            Long statementId,
            @Parameter(description = "Short-lived signed token returned in the statement download URL.")
            String token);
}
