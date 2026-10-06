package com.fudn.gateway;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.web.servlet.MockMvc;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
// 3 route cung tro sang 1 WireMock server (port ngau nhien) dong vai customer/movie/booking-service
@EnableWireMock(@ConfigureWireMock(baseUrlProperties = {
        "services.customer.url", "services.movie.url", "services.booking.url"}))
class ApiGatewaySecurityIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Value("${app.jwt.secret}")
    private String secret;

    /** Ky token giong customer-service (HS256, claims sub/uid/role). */
    private String token(long uid, String email, String role, Instant expiresAt) {
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(
                new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("fu-cinema").subject(email)
                .issuedAt(expiresAt.minus(60, ChronoUnit.MINUTES)).expiresAt(expiresAt)
                .claim("uid", uid).claim("role", role)
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private String bearer(long uid, String email, String role) {
        return "Bearer " + token(uid, email, role, Instant.now().plus(60, ChronoUnit.MINUTES));
    }

    @Test
    void publicGetMoviesIsRoutedWithoutToken() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlPathEqualTo("/api/movies"))
                .willReturn(okJson("[{\"movieId\":\"66f200000000000000000001\",\"title\":\"Galaxy Rangers\"}]")));

        mockMvc.perform(get("/api/movies").param("keyword", "galaxy"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Galaxy Rangers")));

        verify(getRequestedFor(urlEqualTo("/api/movies?keyword=galaxy")).withoutHeader("X-User-Id"));
    }

    @Test
    void loginIsPublicAndRoutedToCustomerService() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/api/auth/login"))
                .willReturn(okJson("{\"accessToken\":\"x.y.z\",\"role\":\"ADMIN\"}")));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@fucinema.com\",\"password\":\"@@abc123@@\"}"))
                .andExpect(status().isOk());

        verify(postRequestedFor(urlEqualTo("/api/auth/login")).withRequestBody(containing("admin@fucinema.com")));
    }

    @Test
    void protectedEndpointWithoutTokenReturns401AndIsNotForwarded() throws Exception {
        mockMvc.perform(get("/api/customers/me"))
                .andExpect(status().isUnauthorized());

        verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void invalidOrExpiredTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/bookings/my").header(HttpHeaders.AUTHORIZATION, "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized());

        String expired = token(1, "an@gmail.com", "CUSTOMER", Instant.now().minus(5, ChronoUnit.MINUTES));
        mockMvc.perform(get("/api/bookings/my").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
                .andExpect(status().isUnauthorized());

        verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void customerTokenIsTranslatedToUserHeadersAndSpoofedHeaderIsOverridden() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo("/api/customers/me"))
                .willReturn(okJson("{\"customerId\":1,\"email\":\"an@gmail.com\"}")));

        mockMvc.perform(get("/api/customers/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1, "an@gmail.com", "CUSTOMER"))
                        .header("X-User-Id", "2")                 // client co tinh gia mao
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk());

        verify(getRequestedFor(urlEqualTo("/api/customers/me"))
                .withHeader("X-User-Id", equalTo("1"))
                .withHeader("X-User-Email", equalTo("an@gmail.com"))
                .withHeader("X-User-Role", equalTo("CUSTOMER")));
    }

    @Test
    void customerCannotCallAdminEndpoints() throws Exception {
        String customer = bearer(1, "an@gmail.com", "CUSTOMER");

        mockMvc.perform(get("/api/customers").header(HttpHeaders.AUTHORIZATION, customer))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/bookings/report").param("startDate", "2026-10-01").param("endDate", "2026-10-31")
                        .header(HttpHeaders.AUTHORIZATION, customer))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/genres").contentType(MediaType.APPLICATION_JSON).content("{\"genreName\":\"X\"}")
                        .header(HttpHeaders.AUTHORIZATION, customer))
                .andExpect(status().isForbidden());

        verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void adminCannotUseCustomerOnlyEndpoints() throws Exception {
        String admin = bearer(0, "admin@fucinema.com", "ADMIN");

        mockMvc.perform(get("/api/customers/me").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content("{\"items\":[]}")
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isForbidden());

        verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void adminReportIsRoutedToBookingServiceWithAdminHeaders() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlPathEqualTo("/api/bookings/report"))
                .willReturn(okJson("{\"totalBookings\":2,\"totalRevenue\":285000}")));

        mockMvc.perform(get("/api/bookings/report").param("startDate", "2026-10-01").param("endDate", "2026-10-31")
                        .header(HttpHeaders.AUTHORIZATION, bearer(0, "admin@fucinema.com", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("285000")));

        verify(getRequestedFor(urlPathEqualTo("/api/bookings/report"))
                .withQueryParam("startDate", equalTo("2026-10-01"))
                .withHeader("X-User-Id", equalTo("0"))
                .withHeader("X-User-Role", equalTo("ADMIN")));
    }
}
