package com.capitec.securefile.api;

import com.capitec.securefile.common.exception.ErrorResponse;
import com.capitec.securefile.model.response.AdminCustomerResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

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
                            description = "Current user is not allowed to access administrator endpoints.",
                            content = {
                                    @Content(
                                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = ErrorResponse.class))
                            })
            })
    ResponseEntity<List<AdminCustomerResponse>> listCustomers();
}
