package com.servet.booker.model;

/** "token" on success, "reason" on failure (the API answers both with 200, see BR-01). */
public record TokenResponse(String token, String reason) {

    @Override
    public String toString() {
        return "TokenResponse[token=" + (token == null ? null : "******") + ", reason=" + reason + "]";
    }
}
