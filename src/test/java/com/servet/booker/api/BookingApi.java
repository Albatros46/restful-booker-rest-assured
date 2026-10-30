package com.servet.booker.api;

import static io.restassured.RestAssured.given;

import java.util.Map;

import com.servet.booker.config.ApiConfig;
import com.servet.booker.model.Booking;
import com.servet.booker.model.Credentials;
import com.servet.booker.spec.Specs;

import io.restassured.response.Response;

/**
 * API object (like a page object, for an API): tests call business actions, the HTTP details live
 * here. Methods return the raw Response, so a test can check a wrong status code too, not only the
 * happy path.
 */
public class BookingApi {

    public Response ping() {
        return given().spec(Specs.plain()).get("/ping");
    }

    public Response createToken(Credentials credentials) {
        return given().spec(Specs.json()).body(credentials).post("/auth");
    }

    /** Logs in with the configured account and returns the token. */
    public String token() {
        return createToken(new Credentials(ApiConfig.USERNAME, ApiConfig.password()))
                .then().spec(Specs.okJson())
                .extract().path("token");
    }

    public Response create(Object booking) {
        return given().spec(Specs.json()).body(booking).post("/booking");
    }

    public Response get(Object id) {
        return given().spec(Specs.json()).pathParam("id", id).get("/booking/{id}");
    }

    public Response findByName(String firstname, String lastname) {
        return given().spec(Specs.json())
                .queryParam("firstname", firstname)
                .queryParam("lastname", lastname)
                .get("/booking");
    }

    public Response update(int id, Booking booking, String token) {
        return withToken(token).body(booking).pathParam("id", id).put("/booking/{id}");
    }

    public Response partialUpdate(int id, Map<String, ?> fields, String token) {
        return withToken(token).body(fields).pathParam("id", id).patch("/booking/{id}");
    }

    public Response delete(int id, String token) {
        var request = given().spec(Specs.plain()).pathParam("id", id);
        if (token != null) {
            request.cookie("token", token);
        }
        return request.delete("/booking/{id}");
    }

    private io.restassured.specification.RequestSpecification withToken(String token) {
        var request = given().spec(Specs.json());
        return token == null ? request : request.cookie("token", token);
    }
}
