package com.servet.booker.tests;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import com.servet.booker.model.Booking;
import com.servet.booker.model.BookingDates;
import com.servet.booker.model.CreatedBooking;
import com.servet.booker.spec.Specs;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;

/** The same 9 scenarios as the Postman data-driven collection (Week 13): one CSV row = one test. */
@Epic("Restful Booker")
@Feature("Data-driven bookings")
class BookingDataDrivenTest extends ApiTest {

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/data/bookings.csv", numLinesToSkip = 1, encoding = "UTF-8")
    void booking_is_stored_as_sent(String scenario, String firstname, String lastname, BigDecimal totalprice,
            boolean depositpaid, LocalDate checkin, LocalDate checkout, String additionalneeds,
            BigDecimal expectedPrice) {
        // an empty CSV cell is null for JUnit; the API stores it as "" (that is what we send)
        Booking sent = new Booking(firstname, lastname, totalprice, depositpaid, new BookingDates(checkin, checkout),
                additionalneeds == null ? "" : additionalneeds);

        CreatedBooking created = api.create(sent)
                .then().spec(Specs.okJson())
                .body(matchesJsonSchemaInClasspath("schemas/created-booking.json"))
                .extract().as(CreatedBooking.class);
        cleanUpLater(created.bookingid());

        // compareTo, not equals: 250 and 250.0 are the same price
        assertThat(created.booking().totalprice()).as(scenario).isEqualByComparingTo(expectedPrice);
        assertThat(created.booking().withTotalprice(totalprice)).as(scenario).isEqualTo(sent);
        assertThat(api.get(created.bookingid()).as(Booking.class).lastname()).isEqualTo(lastname);
    }
}
