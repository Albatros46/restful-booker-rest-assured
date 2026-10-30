package com.servet.booker.tests;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.servet.booker.model.Booking;
import com.servet.booker.model.BookingId;
import com.servet.booker.model.CreatedBooking;
import com.servet.booker.spec.Specs;
import com.servet.booker.support.TestData;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;

/** Each test creates its own booking (ApiTest deletes it afterwards), so the tests are independent. */
@Epic("Restful Booker")
@Feature("Booking lifecycle")
class BookingLifecycleTest extends ApiTest {

    @Test
    @Tag("smoke")
    void created_booking_matches_the_schema_and_echoes_the_request() {
        Booking sent = TestData.booking();

        CreatedBooking created = api.create(sent)
                .then().spec(Specs.okJson())
                .body(matchesJsonSchemaInClasspath("schemas/created-booking.json"))
                .extract().as(CreatedBooking.class);
        cleanUpLater(created.bookingid());

        // records compare field by field: one line checks the whole body
        assertThat(created.booking()).isEqualTo(sent);
    }

    @Test
    @Tag("smoke")
    void stored_booking_can_be_read_back() {
        Booking sent = TestData.booking();
        int id = createBooking(sent).bookingid();

        Booking stored = api.get(id)
                .then().spec(Specs.okJson())
                .body(matchesJsonSchemaInClasspath("schemas/booking.json"))
                .extract().as(Booking.class);

        assertThat(stored).isEqualTo(sent);
    }

    @Test
    void booking_is_found_by_first_and_last_name() {
        Booking sent = TestData.booking();
        int id = createBooking(sent).bookingid();

        List<BookingId> found = List.of(api.findByName(sent.firstname(), sent.lastname())
                .then().spec(Specs.okJson())
                .body(matchesJsonSchemaInClasspath("schemas/booking-ids.json"))
                .extract().as(BookingId[].class));

        assertThat(found).containsExactly(new BookingId(id)); // the run id in the last name makes it unique
    }

    @Test
    void put_changes_price_and_deposit() {
        Booking original = TestData.booking();
        int id = createBooking(original).bookingid();
        Booking changed = original.withTotalprice(new BigDecimal("999")).withDepositpaid(false);

        Booking answer = api.update(id, changed, token).then().spec(Specs.okJson()).extract().as(Booking.class);

        assertThat(answer).isEqualTo(changed);
        assertThat(api.get(id).as(Booking.class)).isEqualTo(changed);
    }

    @Test
    @Tag("known-bug")
    @DisplayName("[KNOWN BUG BR-04] PUT keeps additionalneeds although it was not sent")
    void put_replaces_the_whole_booking() {
        Booking original = TestData.booking().withAdditionalneeds("Breakfast");
        int id = createBooking(original).bookingid();

        Booking answer = api.update(id, original.withAdditionalneeds(null), token)
                .then().spec(Specs.okJson()).extract().as(Booking.class);

        // expected: null (PUT replaces the resource); actual: the old value survives
        assertThat(answer.additionalneeds()).isEqualTo("Breakfast");
    }

    @Test
    void patch_changes_only_the_sent_field() {
        Booking original = TestData.booking();
        int id = createBooking(original).bookingid();

        Booking answer = api.partialUpdate(id, Map.of("totalprice", 123), token)
                .then().spec(Specs.okJson()).extract().as(Booking.class);

        assertThat(answer).isEqualTo(original.withTotalprice(new BigDecimal("123")));
    }

    @Test
    @Tag("known-bug")
    @DisplayName("[KNOWN BUG BR-05 / BR-06] delete answers 201, deleting again answers 405 instead of 404")
    void deleted_booking_is_gone() {
        int id = api.create(TestData.booking()).then().spec(Specs.okJson()).extract().path("bookingid");

        api.delete(id, token).then().spec(Specs.inTime()).statusCode(201);   // expected 204
        api.get(id).then().spec(Specs.inTime()).statusCode(404);
        api.delete(id, token).then().spec(Specs.inTime()).statusCode(405);   // expected 404
    }
}
