package com.servet.booker.support;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import com.servet.booker.model.Booking;
import com.servet.booker.model.BookingDates;

/**
 * Unique data per test: tests run in parallel and the API is public, so a test must never depend on
 * bookings it did not create. The last name carries a run id, which also makes searches exact.
 */
public final class TestData {

    private TestData() {
    }

    public static Booking booking() {
        LocalDate checkin = LocalDate.now().plusDays(30);
        return new Booking(
                "Test",
                "QA-" + UUID.randomUUID().toString().substring(0, 8),
                BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(100, 1000)),
                true,
                new BookingDates(checkin, checkin.plusDays(5)),
                "Breakfast");
    }
}
