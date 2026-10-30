package com.servet.booker.tests;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.servet.booker.config.ApiConfig;
import com.servet.booker.model.Credentials;
import com.servet.booker.model.TokenResponse;
import com.servet.booker.spec.Specs;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;

@Epic("Restful Booker")
@Feature("Health and authentication")
class HealthAndAuthTest extends ApiTest {

    @Test
    @Tag("smoke")
    @Tag("known-bug")
    @DisplayName("[KNOWN BUG BR-08] ping answers 201 Created instead of 200")
    void ping() {
        api.ping().then().spec(Specs.inTime()).statusCode(201);
    }

    @Test
    @Tag("smoke")
    void valid_credentials_return_a_token_matching_the_schema() {
        api.createToken(new Credentials(ApiConfig.USERNAME, ApiConfig.password()))
                .then().spec(Specs.okJson())
                .body(matchesJsonSchemaInClasspath("schemas/token.json"));
    }

    @Test
    @Tag("known-bug")
    @DisplayName("[KNOWN BUG BR-01] wrong password answers 200 (expected 401) and no token")
    void wrong_password_is_rejected() {
        TokenResponse answer = api.createToken(new Credentials(ApiConfig.USERNAME, "wrong-password"))
                .then().spec(Specs.okJson()) // 200: this is the bug
                .body("token", nullValue())
                .body("reason", equalTo("Bad credentials"))
                .extract().as(TokenResponse.class);

        assertThat(answer.token()).isNull();
    }
}
