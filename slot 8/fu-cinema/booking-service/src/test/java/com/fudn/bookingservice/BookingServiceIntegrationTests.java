package com.fudn.bookingservice;

import com.fudn.bookingservice.repository.BookingRepository;
import com.fudn.bookingservice.stub.MovieServiceStubs;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.mysql.MySQLContainer;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

import java.time.LocalDateTime;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// WireMock thay movie-service, URL cua no duoc ghi vao property movie.service.url (MovieClient dung)
@EnableWireMock(@ConfigureWireMock(baseUrlProperties = "movie.service.url"))
class BookingServiceIntegrationTests {

    @ServiceConnection
    static MySQLContainer mySQLContainer = new MySQLContainer("mysql:8.3.0");

    static {
        mySQLContainer.start();
    }

    private static final LocalDateTime FUTURE = LocalDateTime.now().plusDays(7).withNano(0);

    @LocalServerPort
    private Integer port;

    @Autowired
    private BookingRepository bookingRepository;

    @BeforeEach
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    private static String items(String showtimeId, String... seats) {
        StringBuilder sb = new StringBuilder("{\"items\":[");
        for (int i = 0; i < seats.length; i++) {
            if (i > 0) sb.append(',');
            sb.append("{\"showtimeId\":\"").append(showtimeId).append("\",\"seatCode\":\"").append(seats[i]).append("\"}");
        }
        return sb.append("]}").toString();
    }

    @Test
    void shouldCreateBookingWithServerSidePriceAndSnapshot() {
        String showtimeId = "66f3000000000000000000a1";
        MovieServiceStubs.stubShowtime(showtimeId, "SCHEDULED", FUTURE);

        RestAssured.given()
                .header("X-User-Id", 1)
                .contentType("application/json")
                .body(items(showtimeId, "E5", "E6"))
                .when()
                .post("/api/bookings")
                .then()
                .log().all()
                .statusCode(201)
                .body("bookingStatus", is("CONFIRMED"))
                .body("customerId", is(1))
                .body("totalPrice", is(190000))            // 2 x ticketPrice tu movie-service (BR10)
                .body("details", hasSize(2))
                .body("details[0].movieTitle", is("Galaxy Rangers"))
                .body("details[0].roomName", is("Room 01"));

        // 2 ve cung 1 suat chieu -> chi goi movie-service 1 lan (cache trong request)
        verify(1, getRequestedFor(urlEqualTo("/api/showtimes/" + showtimeId)));
    }

    @Test
    void shouldRejectSeatAlreadyBookedAndShowItInSeatMap() {
        String showtimeId = "66f3000000000000000000a2";
        MovieServiceStubs.stubShowtime(showtimeId, "SCHEDULED", FUTURE);

        RestAssured.given().header("X-User-Id", 1).contentType("application/json")
                .body(items(showtimeId, "A1")).post("/api/bookings")
                .then().statusCode(201);
        long before = bookingRepository.count();

        RestAssured.given().header("X-User-Id", 2).contentType("application/json")
                .body(items(showtimeId, "A1")).post("/api/bookings")
                .then().log().all()
                .statusCode(409)
                .body("message", containsString("A1"));

        assertThat(bookingRepository.count(), is(before));

        RestAssured.get("/api/bookings/showtimes/" + showtimeId + "/seats")
                .then().log().all()
                .statusCode(200)
                .body("totalSeats", is(40))
                .body("availableSeats", is(39))
                .body("bookedSeats", contains("A1"));
    }

    @Test
    void shouldRejectSeatOutsideRoomLayout() {
        String showtimeId = "66f3000000000000000000a3";
        MovieServiceStubs.stubShowtime(showtimeId, "SCHEDULED", FUTURE);

        RestAssured.given().header("X-User-Id", 1).contentType("application/json")
                .body(items(showtimeId, "F1")).post("/api/bookings")
                .then().log().all()
                .statusCode(400)
                .body("message", containsString("does not exist"));
    }

    @Test
    void shouldRejectCancelledShowtime() {
        String showtimeId = "66f3000000000000000000a4";
        MovieServiceStubs.stubShowtime(showtimeId, "CANCELLED", FUTURE);

        RestAssured.given().header("X-User-Id", 1).contentType("application/json")
                .body(items(showtimeId, "A1")).post("/api/bookings")
                .then().log().all()
                .statusCode(400);
    }

    @Test
    void shouldReturn404WhenShowtimeDoesNotExist() {
        String showtimeId = "66f9ffffffffffffffffffff";
        MovieServiceStubs.stubShowtimeNotFound(showtimeId);

        RestAssured.given().header("X-User-Id", 1).contentType("application/json")
                .body(items(showtimeId, "A1")).post("/api/bookings")
                .then().log().all()
                .statusCode(404);
    }

    @Test
    void shouldReturn503WhenMovieServiceIsDown() {
        String showtimeId = "66f3000000000000000000a5";
        MovieServiceStubs.stubMovieServiceDown(showtimeId);
        long before = bookingRepository.count();

        RestAssured.given().header("X-User-Id", 1).contentType("application/json")
                .body(items(showtimeId, "A1")).post("/api/bookings")
                .then().log().all()
                .statusCode(503)
                .body("message", containsString("Movie service is unavailable"));

        assertThat(bookingRepository.count(), is(before));
    }

    @Test
    void shouldCancelBookingAndReleaseSeat() {
        String showtimeId = "66f3000000000000000000a6";
        MovieServiceStubs.stubShowtime(showtimeId, "SCHEDULED", FUTURE);

        Integer bookingId = RestAssured.given().header("X-User-Id", 5).contentType("application/json")
                .body(items(showtimeId, "B2")).post("/api/bookings")
                .then().statusCode(201).extract().path("bookingId");

        // customer khac -> 403 (BR11)
        RestAssured.given().header("X-User-Id", 6).header("X-User-Role", "CUSTOMER")
                .put("/api/bookings/" + bookingId + "/cancel")
                .then().statusCode(403);

        RestAssured.given().header("X-User-Id", 5).header("X-User-Role", "CUSTOMER")
                .put("/api/bookings/" + bookingId + "/cancel")
                .then().log().all()
                .statusCode(200)
                .body("bookingStatus", is("CANCELLED"));

        RestAssured.get("/api/bookings/showtimes/" + showtimeId + "/seats")
                .then().statusCode(200).body("bookedSeats", empty());
    }

    @Test
    void shouldRequireUserHeaderWhenCalledWithoutGateway() {
        RestAssured.get("/api/bookings/my")
                .then().log().all()
                .statusCode(401)
                .body("message", containsString("X-User-Id"));
    }
}
