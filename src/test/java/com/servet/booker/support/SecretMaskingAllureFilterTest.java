package com.servet.booker.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class SecretMaskingAllureFilterTest {

    @Test
    void password_and_token_in_json_are_masked() {
        String masked = SecretMaskingAllureFilter.mask(
                "{\"username\":\"admin\",\"password\":\"s3cr3t\"} {\"token\" : \"abc123def456789\"}");

        assertThat(masked)
                .contains("\"username\":\"admin\"")
                .contains("\"password\":\"******\"")
                .contains("\"token\" : \"******\"")
                .doesNotContain("s3cr3t")
                .doesNotContain("abc123def456789");
    }

    @Test
    void token_cookie_is_masked() {
        assertThat(SecretMaskingAllureFilter.mask("token=abc123def456789; Path=/")).isEqualTo("token=******; Path=/");
    }

    @Test
    void auth_headers_are_masked_others_kept() {
        assertThat(SecretMaskingAllureFilter.maskHeader("Cookie", "token=abc")).isEqualTo("******");
        assertThat(SecretMaskingAllureFilter.maskHeader("authorization", "Bearer x")).isEqualTo("******");
        assertThat(SecretMaskingAllureFilter.maskHeader("Accept", "application/json")).isEqualTo("application/json");
    }

    @Test
    void other_fields_are_untouched() {
        String body = "{\"firstname\":\"Anna\",\"totalprice\":250}";
        assertThat(SecretMaskingAllureFilter.mask(body)).isEqualTo(body);
    }
}
