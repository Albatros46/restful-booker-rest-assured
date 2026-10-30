package com.servet.booker.support;

import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.qameta.allure.Allure;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

/**
 * Attaches every request and response to the Allure report, with secrets replaced by ******:
 * the password in the login body, the token in its response, and the Cookie / Authorization headers.
 *
 * Why not Allure's own AllureRestAssured filter: it writes bodies and headers as they are, so the
 * password and the token would end up in target/allure-results (the lesson from Week 11).
 */
public class SecretMaskingAllureFilter implements Filter {

    static final String MASK = "******";
    private static final Pattern SECRET_JSON_FIELD =
            Pattern.compile("(\"(?:password|token)\"\\s*:\\s*\")[^\"]*(\")");
    private static final Pattern TOKEN_COOKIE = Pattern.compile("(token=)[^;\\s]+");

    @Override
    public Response filter(FilterableRequestSpecification request, FilterableResponseSpecification response,
            FilterContext context) {
        Response result = context.next(request, response);
        String requestText = request.getMethod() + " " + request.getURI() + "\n"
                + request.getHeaders().asList().stream()
                        .map(h -> h.getName() + ": " + maskHeader(h.getName(), h.getValue()))
                        .collect(Collectors.joining("\n"))
                + (request.getBody() == null ? "" : "\n\n" + mask(String.valueOf((Object) request.getBody())));
        String responseText = result.getStatusLine() + " (" + result.getTime() + " ms)\n\n"
                + mask(result.getBody().asString());
        Allure.addAttachment("Request " + request.getMethod() + " " + request.getDerivedPath(), "text/plain",
                requestText);
        Allure.addAttachment("Response " + result.getStatusCode(), "text/plain", responseText);
        return result;
    }

    static String mask(String text) {
        if (text == null) {
            return "";
        }
        return TOKEN_COOKIE.matcher(SECRET_JSON_FIELD.matcher(text).replaceAll("$1" + MASK + "$2"))
                .replaceAll("$1" + MASK);
    }

    static String maskHeader(String name, String value) {
        return name.equalsIgnoreCase("Authorization") || name.equalsIgnoreCase("Cookie") ? MASK : value;
    }
}
