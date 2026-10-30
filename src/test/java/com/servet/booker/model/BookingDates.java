package com.servet.booker.model;

import java.time.LocalDate;

public record BookingDates(LocalDate checkin, LocalDate checkout) {
}
