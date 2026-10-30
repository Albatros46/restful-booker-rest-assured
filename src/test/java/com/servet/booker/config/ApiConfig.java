package com.servet.booker.config;

/**
 * Where the API is and how to log in. The password comes from the RB_PASSWORD environment variable
 * (a CI secret). Without it the public demo password from the Restful Booker docs is used, so the
 * project runs out of the box.
 */
public final class ApiConfig {

    public static final String BASE_URI = System.getProperty("baseUri", "https://restful-booker.herokuapp.com");
    public static final String USERNAME = "admin";
    /** Every response must arrive within this time (ms); -DmaxResponseMs=5000 for a slow network. */
    public static final long MAX_RESPONSE_MS = Long.getLong("maxResponseMs", 3000);

    private static final String DEMO_PASSWORD = "password123"; // published at https://restful-booker.herokuapp.com/apidoc

    private ApiConfig() {
    }

    public static String password() {
        String fromEnv = System.getenv("RB_PASSWORD");
        return fromEnv != null && !fromEnv.isBlank() ? fromEnv : DEMO_PASSWORD;
    }
}
