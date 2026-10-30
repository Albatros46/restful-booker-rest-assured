package com.servet.booker.spec;

import static org.hamcrest.Matchers.lessThan;

import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.servet.booker.config.ApiConfig;
import com.servet.booker.support.SecretMaskingAllureFilter;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

/**
 * Request and response rules shared by every test, defined once. A test only says what is special
 * about its call; base URI, JSON, reporting and the response-time limit come from here.
 */
public final class Specs {

    /** Jackson for the POJOs: LocalDate as "2026-12-01", unknown fields fail (the schema checks catch them). */
    private static final RestAssuredConfig CONFIG = RestAssured.config().objectMapperConfig(
            ObjectMapperConfig.objectMapperConfig().jackson2ObjectMapperFactory((type, charset) ->
                    new com.fasterxml.jackson.databind.ObjectMapper()
                            .registerModule(new JavaTimeModule())
                            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                            .setSerializationInclusion(JsonInclude.Include.NON_NULL)));

    private Specs() {
    }

    /**
     * JSON in, JSON out. Deliberately no REST Assured console logging: it would print the login body
     * (password) into CI logs. Requests and responses go to Allure, masked.
     */
    public static RequestSpecification json() {
        return new RequestSpecBuilder()
                .setBaseUri(ApiConfig.BASE_URI)
                .setConfig(CONFIG)
                .setContentType(ContentType.JSON)
                // NOT setAccept(ContentType.JSON): that sends "application/json, application/javascript,
                // text/javascript, text/json" and Restful Booker answers 418 (BR-09, found by this project)
                .setAccept("application/json")
                .addFilter(new SecretMaskingAllureFilter())
                .build();
    }

    /** Some endpoints (ping, delete) answer plain text: no Accept: application/json. */
    public static RequestSpecification plain() {
        return new RequestSpecBuilder()
                .setBaseUri(ApiConfig.BASE_URI)
                .setConfig(CONFIG)
                .addFilter(new SecretMaskingAllureFilter())
                .build();
    }

    /** Expected for every successful JSON answer. */
    public static ResponseSpecification okJson() {
        return new ResponseSpecBuilder()
                .expectStatusCode(200)
                .expectContentType(ContentType.JSON)
                .expectResponseTime(lessThan(ApiConfig.MAX_RESPONSE_MS), TimeUnit.MILLISECONDS)
                .build();
    }

    /** Only the time limit: for answers whose status code is the subject of the test. */
    public static ResponseSpecification inTime() {
        return new ResponseSpecBuilder()
                .expectResponseTime(lessThan(ApiConfig.MAX_RESPONSE_MS), TimeUnit.MILLISECONDS)
                .build();
    }
}
