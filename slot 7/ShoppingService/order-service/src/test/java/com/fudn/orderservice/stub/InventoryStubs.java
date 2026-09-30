package com.fudn.orderservice.stub;

import lombok.experimental.UtilityClass;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@UtilityClass
public class InventoryStubs {

    public void stubInventoryCall(String skuCode, Integer quantity) {
        stubInventory(skuCode, quantity, true);
    }

    public void stubInventoryOutOfStock(String skuCode, Integer quantity) {
        stubInventory(skuCode, quantity, false);
    }

    private void stubInventory(String skuCode, Integer quantity, boolean inStock) {
        stubFor(get(urlEqualTo("/api/inventory?skuCode=" + skuCode + "&quantity=" + quantity))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(String.valueOf(inStock))));
    }
}
