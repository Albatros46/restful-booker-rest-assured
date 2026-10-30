package com.servet.booker.tests;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;

import com.servet.booker.api.BookingApi;
import com.servet.booker.model.Booking;
import com.servet.booker.model.CreatedBooking;
import com.servet.booker.spec.Specs;

/**
 * Logs in once per test class and deletes every booking a test created, also when the test fails:
 * the public API should not fill up with our test data.
 */
abstract class ApiTest {

    protected static final BookingApi api = new BookingApi();
    protected static String token;

    private final Queue<Integer> created = new ConcurrentLinkedQueue<>();

    @BeforeAll
    static void logIn() {
        token = api.token();
    }

    /** Creates a booking, checks the happy-path rules and remembers it for cleanup. */
    protected CreatedBooking createBooking(Booking booking) {
        CreatedBooking result = api.create(booking).then().spec(Specs.okJson()).extract().as(CreatedBooking.class);
        created.add(result.bookingid());
        return result;
    }

    /** For bookings created through a raw call (e.g. invalid data the API accepts anyway). */
    protected void cleanUpLater(int bookingId) {
        created.add(bookingId);
    }

    @AfterEach
    void deleteCreatedBookings() {
        Integer id;
        while ((id = created.poll()) != null) {
            api.delete(id, token); // best effort: an already deleted booking answers 405 (BR-06)
        }
    }
}
