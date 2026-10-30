package com.servet.booker.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import com.servet.booker.model.Booking;
import com.servet.booker.model.BookingDates;
import com.servet.booker.model.CreatedBooking;
import com.servet.booker.spec.Specs;
import com.servet.booker.support.TestData;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;

@Epic("Restful Booker")
@Feature("Validation")
class BookingValidationTest extends ApiTest {

    @ParameterizedTest(name = "unknown booking id {0} answers 404")
    @ValueSource(strings = { "999999999", "abc", "-1" })
    void unknown_booking_is_404(String id) {
        api.get(id).then().spec(Specs.inTime()).statusCode(404);
    }

    /**
     * Found while writing these tests: REST Assured's ContentType.JSON sends a list of four JSON types
     * in Accept. Postman sends only application/json, so the Postman collection never saw this.
     */
    @ParameterizedTest(name = "Accept: {0} answers 418")
    @Tag("known-bug")
    @ValueSource(strings = { "application/json, application/javascript, text/javascript, text/json", "text/plain" })
    @DisplayName("[KNOWN BUG BR-09] an Accept list containing JSON, or an unsupported type, answers 418 I'm a Teapot")
    void accept_header_is_negotiated(String accept) {
        io.restassured.RestAssured.given().spec(Specs.json()).accept(accept).body(TestData.booking())
                .post("/booking")
                .then().spec(Specs.inTime())
                .statusCode(418); // expected: 200 for the list (it contains application/json), 406 for text/plain
    }

    @Test
    @Tag("known-bug")
    @DisplayName("[KNOWN BUG BR-02] missing required fields answer 500 instead of 400")
    void missing_fields_are_a_client_error() {
        api.create(Map.of("firstname", "OnlyFirstName")).then().spec(Specs.inTime()).statusCode(500);
    }

    @Test
    @Tag("known-bug")
    @DisplayName("[KNOWN BUG BR-03] negative price and checkout before checkin are stored")
    void impossible_booking_is_rejected() {
        Booking valid = TestData.booking();
        Booking impossible = valid
                .withTotalprice(new BigDecimal("-5"))
                .withDates(new BookingDates(valid.bookingdates().checkout(), valid.bookingdates().checkin()));

        CreatedBooking stored = api.create(impossible).then().spec(Specs.okJson()).extract().as(CreatedBooking.class);
        cleanUpLater(stored.bookingid());

        // expected: 400; actual: stored as sent
        assertThat(stored.booking()).isEqualTo(impossible);
    }
}
