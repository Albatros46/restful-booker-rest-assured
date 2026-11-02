# API Testing · REST Assured + JUnit 6

[![API Tests](https://github.com/Albatros46/restful-booker-rest-assured/actions/workflows/api-tests.yml/badge.svg)](https://github.com/Albatros46/restful-booker-rest-assured/actions/workflows/api-tests.yml)
![Java](https://img.shields.io/badge/Java-21-orange) ![REST Assured](https://img.shields.io/badge/REST%20Assured-6.2-2C9E58) ![JUnit](https://img.shields.io/badge/JUnit-6-25A162) ![JSON Schema](https://img.shields.io/badge/JSON%20Schema-validated-blue) ![Tests](https://img.shields.io/badge/tests-40%20passing-brightgreen) ![Defects](https://img.shields.io/badge/defects%20found-9-red)

API tests for the [Restful Booker](https://restful-booker.herokuapp.com/apidoc/index.html) practice API in Java, with **request / response specifications, POJOs and JSON Schema**. The same API was tested with Postman in [Week 13](https://github.com/Albatros46/-H13-Postman-and-Newman) — this project covers the same scenarios in code, and found a defect the Postman collection could not see.

Part of my 52-week QA Automation roadmap — **Week 14**.

## Structure

```
src/test/java/com/servet/booker/
├── config/ApiConfig          base URI, credentials (RB_PASSWORD env), response-time limit
├── spec/Specs                RequestSpecification + ResponseSpecification, defined once
├── model/                    POJOs as Java records: Booking, BookingDates (LocalDate), CreatedBooking, …
├── api/BookingApi            API object: create, get, findByName, update, partialUpdate, delete, token
├── support/                  TestData (unique per test) · SecretMaskingAllureFilter
└── tests/                    ApiTest (login + cleanup) and 7 test classes
src/test/resources/
├── schemas/                  booking, created-booking ($ref), booking-ids, token — additionalProperties: false
└── data/bookings.csv         the 9 data-driven scenarios from Week 13
```

```java
@Test
void put_changes_price_and_deposit() {
    Booking original = TestData.booking();
    int id = createBooking(original).bookingid();               // cleaned up after the test
    Booking changed = original.withTotalprice(new BigDecimal("999")).withDepositpaid(false);

    Booking answer = api.update(id, changed, token).then().spec(Specs.okJson()).extract().as(Booking.class);

    assertThat(answer).isEqualTo(changed);                      // records: the whole body in one line
    assertThat(api.get(id).as(Booking.class)).isEqualTo(changed);
}
```

## The three building blocks

| | How | Why |
|---|---|---|
| **Specifications** | `Specs.json()` (base URI, JSON, masking filter) · `Specs.okJson()` (200 + JSON + response time < 3 s) | rules live in one place; a test only says what is special |
| **POJOs** | Java `record`s, Jackson with `LocalDate` and `FAIL_ON_UNKNOWN_PROPERTIES`; price as `BigDecimal` | no string-built JSON; an unexpected field fails; `double` would hide exactly the rounding bug BR-07 |
| **JSON Schema** | draft-04, `additionalProperties: false`, date pattern, `$ref` from the create response to the booking schema | the structure is checked separately from the values |

**Do the schemas really reject anything?** `SchemaContractTest` runs offline against the schema files: an extra field, the price as text, a date with time, a missing field and a number instead of a boolean are all **rejected**. A schema that accepts everything would make every schema assertion pass for nothing.

## What is tested (40 tests)

| Class | Tests | What |
|---|---|---|
| `HealthAndAuthTest` | 3 | ping · token matches the schema · wrong password |
| `BookingLifecycleTest` | 7 | create (schema + echo) · read · find by name · PUT · PUT without a field · PATCH · delete / gone / delete again |
| `BookingSecurityTest` | 3 | PUT / PATCH / DELETE without or with a wrong token → 403 **and the booking is unchanged afterwards** |
| `BookingValidationTest` | 7 | unknown, non-numeric and negative ids → 404 · Accept header (2) · missing fields · impossible booking |
| `BookingDataDrivenTest` | 9 | `@CsvFileSource`: boundaries, umlauts, Turkish characters, 60-char name, price above 32-bit int, decimal price |
| `SchemaContractTest` | 7 | the schemas themselves (offline) |
| `SecretMaskingAllureFilterTest` | 4 | the masking itself (offline) |

Tags: `smoke` (4), `known-bug` (8 tests for BR-01…06, 08, 09; BR-07 is a row of the data-driven CSV), `unit` (11, offline) → `mvn test -Dgroups=smoke`, `mvn test -DexcludedGroups=known-bug` (32).

## Defects

| ID | Severity | Summary | Found by |
|---|---|---|---|
| BR-01 | Medium | Wrong password → `200 OK` instead of `401` | Postman (Week 13) |
| BR-02 | Medium | Missing fields → `500` instead of `400` | Postman |
| BR-03 | High | Negative price and checkout before checkin are stored | Postman |
| BR-04 | Low | `PUT` keeps a field that was not sent | Postman |
| BR-05 | Low | `DELETE` → `201 Created` | Postman |
| BR-06 | Low | Second `DELETE` → `405` instead of `404` | Postman |
| BR-07 | High | `99.99` is silently stored as `99` | Postman |
| BR-08 | Low | `GET /ping` → `201 Created` | Postman |
| **BR-09** | **Medium** | **An `Accept` list that contains `application/json` → `418 I'm a Teapot`; an unsupported type → `418` instead of `406`** | **REST Assured (this project)** |

Every defect is pinned by a test named `[KNOWN BUG BR-xx]` that asserts today's behaviour: the pipeline stays green while the defect is open and turns red when the API changes. Details for BR-01…08: [DEFECTS.md of Week 13](https://github.com/Albatros46/-H13-Postman-and-Newman/blob/main/DEFECTS.md).

### How BR-09 was found

The first run: every test that created a booking failed with `418`. REST Assured's `setAccept(ContentType.JSON)` does not send one type but four: `application/json, application/javascript, text/javascript, text/json`. Proven with `curl`, changing only that header:

| `Accept` | Answer |
|---|---|
| `application/json` | 200 |
| `*/*` | 200 |
| `application/json, application/javascript, text/javascript, text/json` | **418** |
| `text/plain` | **418** (should be 406) |

Postman sends a single `application/json`, so the Week 13 collection never triggered it. A different client is a different test: real clients (browsers, mobile apps, other libraries) send `Accept` lists too.

## Secrets stay out of the report

`SecretMaskingAllureFilter` attaches every request and response to Allure, with the login password, the token in the response and the `Cookie` / `Authorization` headers replaced by `******`. Allure's own REST Assured filter would write them as they are. REST Assured's console logging is deliberately **off**: on a failure it would print the login body into the CI log.

**Checked with a control run:** with masking switched off, the same scan found the password in 2 files and 2 tokens in `target/allure-results`. With masking on: **0 and 0** (in all 218 attachments of a full run).

## Parallel

Every test creates and deletes its own bookings (unique last name per test, cleanup in `@AfterEach` even when the test fails), so JUnit runs classes and methods in parallel (4 threads, `junit-platform.properties`):

| | Run 1 | Run 2 |
|---|---|---|
| sequential | 39.2 s | 38.9 s |
| parallel (4) | 17.8 s | 17.9 s |

Whole `mvn test` including Maven start-up; 40/40 green in all four runs.

## Postman (Week 13) vs. REST Assured (Week 14)

| | Postman + Newman | REST Assured |
|---|---|---|
| Tests written in | JavaScript snippets inside JSON | Java, in the IDE, refactorable |
| Request bodies | JSON text with `{{variables}}` | POJOs (records), type-checked |
| Shared rules | collection-level scripts | `RequestSpecification` / `ResponseSpecification` |
| Data-driven | `newman -d data.csv` | `@ParameterizedTest @CsvFileSource` |
| Independent tests | requests depend on the previous one (chaining) | every test creates its own data → runs in parallel |
| Who can use it | also non-developers, by clicking | developers / SDETs |
| Found | BR-01 … BR-08 | the same 8 **+ BR-09** |

Both have their place: Postman for exploring an API and for the whole team, REST Assured for a test suite that lives next to the code.

## Run

```bash
mvn test                                  # 40 tests, about 18 s
mvn test -Dgroups=smoke                   # 4 quick checks
mvn test -DbaseUri=http://localhost:3001  # e.g. a local Restful Booker
mvn allure:serve                          # Allure report
```

The password comes from `RB_PASSWORD` (a GitHub secret in CI); without it the public demo password is used. CI runs on every push and every night (the public API can change without a commit here).
