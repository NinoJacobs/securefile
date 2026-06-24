package com.capitec.securefile.common.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTests {

    @Test
    void shouldListSwaggerTagsInDisplayOrder() {
        var openAPI = new OpenApiConfig().openAPI();

        assertThat(openAPI.getTags())
                .extracting("name")
                .containsExactly("Auth", "Admin", "Customer");
    }
}
