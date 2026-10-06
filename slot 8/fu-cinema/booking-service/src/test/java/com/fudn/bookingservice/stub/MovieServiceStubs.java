package com.fudn.bookingservice.stub;

import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/** Gia lap GET /api/showtimes/{id} cua movie-service. */
@UtilityClass
public class MovieServiceStubs {

    public void stubShowtime(String showtimeId, String status, LocalDateTime startTime) {
        stubFor(get(urlEqualTo("/api/showtimes/" + showtimeId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "showtimeId": "%s",
                                  "movieId": "66f200000000000000000001",
                                  "movieTitle": "Galaxy Rangers",
                                  "roomId": "66f100000000000000000001",
                                  "roomName": "Room 01",
                                  "seatRows": 5,
                                  "seatsPerRow": 8,
                                  "startTime": "%s",
                                  "endTime": "%s",
                                  "ticketPrice": 95000,
                                  "showtimeStatus": "%s"
                                }
                                """.formatted(showtimeId, startTime, startTime.plusMinutes(125), status))));
    }

    public void stubShowtimeNotFound(String showtimeId) {
        stubFor(get(urlEqualTo("/api/showtimes/" + showtimeId))
                .willReturn(aResponse().withStatus(404)));
    }

    public void stubMovieServiceDown(String showtimeId) {
        stubFor(get(urlEqualTo("/api/showtimes/" + showtimeId))
                .willReturn(aResponse().withStatus(500)));
    }
}
