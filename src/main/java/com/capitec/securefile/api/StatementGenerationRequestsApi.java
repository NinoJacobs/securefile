package com.capitec.securefile.api;

import com.capitec.securefile.model.request.CreateGenerationRequestRequest;
import com.capitec.securefile.model.response.GenerationRequestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@GlobalApi
public interface StatementGenerationRequestsApi {

    @Operation(summary = "Create a statement generation request")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "201",
                        description = "Successfully created a statement generation request.",
                        content = {
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = GenerationRequestResponse.class))
                        })
            })
    ResponseEntity<GenerationRequestResponse> createGenerationRequest(
            @Valid @RequestBody CreateGenerationRequestRequest request);

    @Operation(summary = "Get a statement generation request by ID")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successfully retrieved the statement generation request.",
                        content = {
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = GenerationRequestResponse.class))
                        }),
                @ApiResponse(
                        responseCode = "404",
                        description = "Generation request not found.",
                        content = @Content)
            })
    ResponseEntity<GenerationRequestResponse> getGenerationRequest(@Valid @NotBlank String requestId);

    @Operation(summary = "Retry a statement generation request")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successfully retried the statement generation request.",
                        content = {
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = GenerationRequestResponse.class))
                        }),
                @ApiResponse(
                        responseCode = "404",
                        description = "Generation request not found.",
                        content = @Content)
            })
    ResponseEntity<GenerationRequestResponse> retryGenerationRequest(@Valid @NotBlank String requestId);
}
