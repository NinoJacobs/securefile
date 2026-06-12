package com.capitec.securefile.api;

import com.capitec.securefile.model.response.StatementSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@GlobalApi
public interface AdminStatementsApi {

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
}
