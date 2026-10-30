package com.servet.booker.tests;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;

/**
 * Tests for the schemas themselves, offline. A schema that accepts everything would make every
 * schema assertion pass for nothing: these prove that the schemas really reject wrong bodies.
 */
@Epic("Restful Booker")
@Feature("JSON Schema")
@Tag("unit")
class SchemaContractTest {

    private static final String VALID = """
            {"firstname":"Anna","lastname":"Schmidt","totalprice":250,"depositpaid":true,
             "bookingdates":{"checkin":"2026-12-01","checkout":"2026-12-05"},"additionalneeds":"Breakfast"}""";

    @Test
    void valid_booking_passes() {
        assertThat(VALID, matchesJsonSchemaInClasspath("schemas/booking.json"));
    }

    @ParameterizedTest(name = "rejected: {0}")
    @ValueSource(strings = {
            "extra field|\"additionalneeds\":\"Breakfast\"|\"additionalneeds\":\"Breakfast\",\"vip\":true",
            "price as text|\"totalprice\":250|\"totalprice\":\"250\"",
            "date with time|\"checkin\":\"2026-12-01\"|\"checkin\":\"2026-12-01T10:00\"",
            "missing lastname|\"lastname\":\"Schmidt\",|",
            "deposit as number|\"depositpaid\":true|\"depositpaid\":1" })
    void broken_booking_is_rejected(String change) {
        String[] parts = change.split("\\|", -1);
        String broken = VALID.replace(parts[1], parts[2]);

        assertThat(parts[0], broken, not(matchesJsonSchemaInClasspath("schemas/booking.json")));
    }

    @Test
    void token_must_be_15_hex_characters() {
        assertThat("{\"token\":\"abc123abc123abc\"}", matchesJsonSchemaInClasspath("schemas/token.json"));
        assertThat("{\"token\":\"not-a-token\"}", not(matchesJsonSchemaInClasspath("schemas/token.json")));
    }
}
