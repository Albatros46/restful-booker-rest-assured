package com.servet.booker.model;

/** toString is overridden: a record would print the password in logs and assertion messages. */
public record Credentials(String username, String password) {

    @Override
    public String toString() {
        return "Credentials[username=" + username + ", password=******]";
    }
}
