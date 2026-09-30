# Kết quả test Part 3 (OpenFeign · WireMock · API Gateway)

> Theo `slot 7/part3-4_Test.md`, mục T1–T5 (Phần 3, Gateway chưa bảo mật).
> Phiên bản: order-service và api-gateway dùng Spring Boot 4.1.0 + Spring Cloud 2025.1.3; product-service và inventory-service giữ Spring Boot 3.5.14.
> Import `part3.postman_collection.json` + `part3-local.postman_environment.json` vào Postman, chọn environment `part3-local`, rồi chạy bằng Collection Runner.

## Chuẩn bị

- Container `mysql` (3306), `mongodb` (27017) đang `Up`; `mongo-express` chạy ở cổng 8090
- Inventory (8082), Product (8080), Order (8081), API Gateway (9000) đều khởi động thành công
- Log order-service: `Successfully validated 1 migration`, `Tomcat started on port 8081`, không có `NoSuchBeanDefinitionException`

## Test tự động (Maven)

| Project | Lệnh | Kết quả |
|---|---|---|
| order-service | `mvnw clean test` | `Tests run: 2, Failures: 0, Errors: 0` — `BUILD SUCCESS` |
| api-gateway | `mvnw test` | `Tests run: 5, Failures: 0, Errors: 0` — `BUILD SUCCESS` |

| Test | Kiểm tra | Đạt? |
|---|---|---|
| `OrderServiceApplicationTests.shouldSubmitOrder` | Stub WireMock trả `true` → `201`, `Order Placed Successfully`, WireMock nhận đúng `GET /api/inventory?skuCode=iphone_15&quantity=1` | ✅ |
| `OrderServiceApplicationTests.shouldFailOrderWhenProductIsNotInStock` | Stub WireMock trả `false` → `500`, số bản ghi `t_orders` không đổi | ✅ |
| `ApiGatewayRoutingTests.shouldRouteGetProductsToProductService` | `GET /api/products` được forward tới Product | ✅ |
| `ApiGatewayRoutingTests.shouldRouteSubPathToProductService` | `PUT /api/products/abc123` (path con + body) được forward | ✅ |
| `ApiGatewayRoutingTests.shouldRoutePostOrderToOrderService` | `POST /api/order` forward cả body → `201` | ✅ |
| `ApiGatewayRoutingTests.shouldForwardQueryParamsToInventoryService` | Query `skuCode`, `quantity` được forward nguyên vẹn | ✅ |
| `ApiGatewayRoutingTests.shouldReturn404WhenNoRouteMatches` | Path không có route → `404`, không gọi service nào | ✅ |

## Test thủ công

| # | Test case | Request | Kỳ vọng | Thực tế | Đạt? |
|---|---|---|---|---|---|
| T2.1 | Inventory còn hàng | `GET :8082/api/inventory?skuCode=iphone_15&quantity=100` | 200, `true` | 200, `true` | ✅ |
| T2.2 | Inventory hết hàng | `GET :8082/api/inventory?skuCode=iphone_15&quantity=101` | 200, `false` | 200, `false` | ✅ |
| T2.3 | SKU không tồn tại | `GET :8082/api/inventory?skuCode=khong_ton_tai&quantity=1` | 200, `false` | 200, `false` | ✅ |
| T3.1 | Đặt hàng qty 100 | `POST :8081/api/order` | 201 | 201, `Order Placed Successfully` | ✅ |
| T3.2 | Dữ liệu đã lưu | `SELECT ... FROM t_orders` | Có bản ghi `iphone_15`, qty 100, UUID | Có (id 1) | ✅ |
| T3.3 | Hết hàng qty 101 | `POST :8081/api/order` | 500, không có bản ghi mới | 500, log `Product with SkuCode iphone_15 is not in stock`, không có bản ghi mới | ✅ |
| T3.4 | SKU `nokia_3310` | `POST :8081/api/order` | 500 | 500, log `Product with SkuCode nokia_3310 is not in stock` | ✅ |
| T3.5 | Inventory ngừng chạy | `POST :8081/api/order` (qty 1) | 500 | 500, log `feign.RetryableException: Connection refused ... executing GET http://localhost:8082/api/inventory?skuCode=iphone_15&quantity=1` | ✅ |
| T3.6 | Thiếu `@EnableFeignClients` | Comment annotation rồi chạy | App không khởi động | `APPLICATION FAILED TO START` — `required a bean of type '...InventoryClient' that could not be found` (đã hoàn tác) | ✅ |
| T5.1 | Products qua Gateway | `GET :9000/api/products` | 200, cùng JSON với `:8080` | 200, JSON trùng khớp | ✅ |
| T5.2 | Tạo product qua Gateway | `POST :9000/api/products` | 201, có `id` | 201, có `id` | ✅ |
| T5.3 | Đặt hàng qua Gateway | `POST :9000/api/order` | 201 | 201, `Order Placed Successfully` | ✅ |
| T5.4 | Inventory qua Gateway | `GET :9000/api/inventory?skuCode=iphone_15&quantity=1` | 200, `true` | 200, `true` | ✅ |
| T5.5 | Path không có route | `GET :9000/api/khong-co-route` | 404 | 404 | ✅ |
| T5.6 | Path con | `PUT` / `DELETE :9000/api/products/{id}` | Giống gọi trực tiếp | 200 / 204 | ✅ |
| T5.7 | Service đích chết | `GET :9000/api/inventory?...` khi Inventory tắt | 500/502 | 500 | ✅ |

Kết quả chạy collection bằng newman: 13 request, 22 assertion, 0 lỗi.

## Dữ liệu trong `t_orders` sau khi test

| id | sku_code | price | quantity | Từ test case |
|---|---|---|---|---|
| 1 | iphone_15 | 1000.00 | 100 | T3.1 (curl) |
| 2 | iphone_15 | 1000.00 | 1 | T5.3 (curl) |
| 3 | iphone_15 | 1000.00 | 100 | T3.1 (newman) |
| 4 | iphone_15 | 1000.00 | 1 | T5.3 (newman) |

- Các request lỗi (T3.3, T3.4, T3.5) không tạo bản ghi nào.

## Nhận xét

- T5.7 được thực hiện với Inventory thay vì Product (cùng cơ chế: Gateway không kết nối được service đích).
- T3.3/T3.4 trả `500` vì `RuntimeException` chưa được handle. Hướng cải tiến: thêm `@RestControllerAdvice` map sang `409` như mục Mở rộng Phần 3.
- Order Service chỉ kiểm tra tồn kho, không trừ kho, nên T3.1 (qty 100) chạy lại nhiều lần vẫn `201`.
- Volume `product-service_mongo-data` được khởi tạo từ slot 3 với mật khẩu root khác với giá trị `password` trong `application.properties` copy từ slot 6, nên product-service trả `500` (`Exception authenticating MongoCredential`). `spring.data.mongodb.password` của product-service trong slot 7 đã được sửa cho khớp với volume.
