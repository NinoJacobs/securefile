package com.capitec.securefile.api;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.MediaType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ApiResponses(
        value = {
                @ApiResponse(
                        responseCode = "400",
                        description = "Request does not adhere to the expected standard.",
                        content = @Content),
                @ApiResponse(
                        responseCode = "409",
                        description = "Request cannot be processed.",
                        content = {
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                                        schema = @Schema(implementation = Exception.class)
                                )
                        }),
                @ApiResponse(
                        responseCode = "412",
                        description = "Server dependency is not available or a handled server error.",
                        content = {
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                                        schema = @Schema(implementation = Exception.class)
                                )
                        })
        })
public @interface GlobalApi {
}
