package com.servet.booker.model;

import java.math.BigDecimal;

/**
 * Request AND response body of a booking. BigDecimal for the price: a double would hide exactly the
 * kind of rounding the API does (BR-07: 99.99 comes back as 99).
 */
public record Booking(
        String firstname,
        String lastname,
        BigDecimal totalprice,
        boolean depositpaid,
        BookingDates bookingdates,
        String additionalneeds) {

    public Booking withTotalprice(BigDecimal price) {
        return new Booking(firstname, lastname, price, depositpaid, bookingdates, additionalneeds);
    }

    public Booking withDepositpaid(boolean paid) {
        return new Booking(firstname, lastname, totalprice, paid, bookingdates, additionalneeds);
    }

    public Booking withAdditionalneeds(String needs) {
        return new Booking(firstname, lastname, totalprice, depositpaid, bookingdates, needs);
    }

    public Booking withDates(BookingDates dates) {
        return new Booking(firstname, lastname, totalprice, depositpaid, dates, additionalneeds);
    }
}
