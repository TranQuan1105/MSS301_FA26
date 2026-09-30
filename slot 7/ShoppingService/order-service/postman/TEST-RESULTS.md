# Kết quả test Order Service (Postman)

> Theo `slot 5/order-service_test.md`. Base URL `http://localhost:8081`, MySQL 8.3.0 (Docker), bảng `t_orders` rỗng trước khi test.
> Import `order-service.postman_collection.json` + `order-service-local.postman_environment.json` vào Postman, chọn environment `order-service-local`, rồi chạy bằng Collection Runner.

## Chuẩn bị

- Container `mysql` đang `Up`, database `order_service` đã tồn tại
- Log khởi động: `Successfully applied 1 migration to schema order_service, now at version v1`
- Bảng `t_orders` và `flyway_schema_history` đã được tạo

## Tổng hợp

| # | Test case | Method | Status kỳ vọng | Status thực tế | Response | Đạt? |
|---|---|---|---|---|---|---|
| 1 | Đặt hàng hợp lệ | POST | 201 | 201 | `Order Placed Successfully` (`text/plain;charset=UTF-8`) | ✅ |
| 2 | Số lượng lớn, giá thập phân | POST | 201 | 201 | `Order Placed Successfully` | ✅ |
| 3 | Thiếu `skuCode` | POST | 201 (hiện tại) / nên là 400 | 201 | `Order Placed Successfully`, `sku_code` = NULL | ✅ (ghi nhận lỗ hổng) |
| 4 | Sai kiểu `quantity` (`"abc"`) | POST | 400 | 400 | `{"status":400,"error":"Bad Request"}` | ✅ |
| 5 | Thiếu `Content-Type` | POST | 415 | 415 | `{"status":415,"error":"Unsupported Media Type"}` | ✅ |
| 6 | Body rỗng `{}` | POST | 201 (hiện tại) / nên là 400 | 201 | `Order Placed Successfully`, mọi field NULL | ✅ (ghi nhận lỗ hổng) |
| 7 | Sai method (GET) | GET | 405 | 405 | `{"status":405,"error":"Method Not Allowed"}` | ✅ |
| 8 | Sai path `/api/orders` | POST | 404 | 404 | `{"status":404,"error":"Not Found"}` | ✅ |

## Dữ liệu trong `t_orders` sau khi test

| id | order_number | sku_code | price | quantity | Từ test case |
|---|---|---|---|---|---|
| 1 | 3a65b1ae-e0e9-4a89-b503-e3ca2edb6c63 | iphone_15 | 1000.00 | 1 | TC1 |
| 2 | f026b53e-021e-4cec-a563-a5e3dc3c06a2 | pixel_8 | 899.99 | 5 | TC2 |
| 3 | 98f5c5f8-7dd9-4e70-a4fb-aff6f4db2e3b | NULL | 1000.00 | 1 | TC3 |
| 4 | e4adf996-88f6-4edc-8f0e-74df59e95944 | NULL | NULL | NULL | TC6 |

- `order_number` được sinh tự động dạng UUID
- `price` 899.99 lưu đúng, không bị làm tròn (`decimal(19,2)`)
- Các request lỗi (TC4, TC5, TC7, TC8) không tạo bản ghi nào

## Nhận xét

- TC4 bị chặn ở tầng Jackson (không parse được `"abc"` thành `Integer`) nên chưa vào tới Controller.
- TC5: trong Postman phải đổi body sang `raw` + `Text` (hoặc bỏ header) thì mới tái hiện được, vì chọn `JSON` Postman sẽ tự gắn `application/json`.
- TC3 và TC6 cho thấy service chưa validate input. Hướng cải tiến: thêm `spring-boot-starter-validation`, `@NotBlank`/`@NotNull`/`@Positive` vào `OrderRequest`, `@Valid` ở Controller để trả về `400 Bad Request`.
