package com.fudn.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
// Trỏ cả 3 route sang cùng 1 WireMock server (port ngẫu nhiên)
@EnableWireMock(@ConfigureWireMock(baseUrlProperties = {
        "services.product.url", "services.order.url", "services.inventory.url"}))
class ApiGatewayRoutingTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRouteGetProductsToProductService() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo("/api/products"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"id\":\"1\",\"name\":\"iPhone 15\",\"description\":\"Apple\",\"price\":1000}]")));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("iPhone 15")));

        verify(getRequestedFor(urlEqualTo("/api/products")));
    }

    @Test
    void shouldRouteSubPathToProductService() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.put(urlEqualTo("/api/products/abc123"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"abc123\",\"name\":\"iPhone 15 Pro\"}")));

        mockMvc.perform(put("/api/products/abc123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"iPhone 15 Pro\",\"description\":\"Apple\",\"price\":1200}"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("iPhone 15 Pro")));

        verify(putRequestedFor(urlEqualTo("/api/products/abc123"))
                .withRequestBody(containing("iPhone 15 Pro")));
    }

    @Test
    void shouldRoutePostOrderToOrderService() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/api/order"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withBody("Order Placed Successfully")));

        mockMvc.perform(post("/api/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skuCode\":\"iphone_15\",\"price\":1000,\"quantity\":1}"))
                .andExpect(status().isCreated())
                .andExpect(content().string("Order Placed Successfully"));

        verify(postRequestedFor(urlEqualTo("/api/order"))
                .withRequestBody(containing("iphone_15")));
    }

    @Test
    void shouldForwardQueryParamsToInventoryService() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlPathEqualTo("/api/inventory"))
                .withQueryParam("skuCode", equalTo("iphone_15"))
                .withQueryParam("quantity", equalTo("1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("true")));

        mockMvc.perform(get("/api/inventory")
                        .param("skuCode", "iphone_15")
                        .param("quantity", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void shouldReturn404WhenNoRouteMatches() throws Exception {
        mockMvc.perform(get("/api/khong-co-route"))
                .andExpect(status().isNotFound());

        verify(0, anyRequestedFor(anyUrl()));
    }
}
