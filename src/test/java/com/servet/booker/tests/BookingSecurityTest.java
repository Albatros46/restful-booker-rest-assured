package com.servet.booker.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.servet.booker.model.Booking;
import com.servet.booker.spec.Specs;
import com.servet.booker.support.TestData;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;

/** A forbidden call must answer 403 AND change nothing: checking only the status is not enough. */
@Epic("Restful Booker")
@Feature("Authorization")
class BookingSecurityTest extends ApiTest {

    @Test
    void update_without_token_is_forbidden_and_changes_nothing() {
        Booking original = TestData.booking();
        int id = createBooking(original).bookingid();

        api.update(id, original.withDepositpaid(false), null).then().spec(Specs.inTime()).statusCode(403);

        assertThat(api.get(id).as(Booking.class)).isEqualTo(original);
    }

    @Test
    void patch_with_invalid_token_is_forbidden_and_changes_nothing() {
        Booking original = TestData.booking();
        int id = createBooking(original).bookingid();

        api.partialUpdate(id, Map.of("totalprice", 1), "invalid-token").then().spec(Specs.inTime()).statusCode(403);

        assertThat(api.get(id).as(Booking.class)).isEqualTo(original);
    }

    @Test
    void delete_without_token_is_forbidden_and_the_booking_stays() {
        int id = createBooking(TestData.booking()).bookingid();

        api.delete(id, null).then().spec(Specs.inTime()).statusCode(403);

        api.get(id).then().spec(Specs.okJson());
    }
}
