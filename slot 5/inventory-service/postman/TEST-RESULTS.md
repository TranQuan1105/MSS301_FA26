# Kết quả test Inventory Service (Postman)

> Theo `slot 5/inventory-service_test.md`. Base URL `http://localhost:8082`, MySQL 8.3.0 (Docker, dùng chung container với order-service).
> Import `inventory-service.postman_collection.json` + `inventory-service-local.postman_environment.json` vào Postman, chọn environment `inventory-service-local`, rồi chạy bằng Collection Runner.

## Chuẩn bị

- Container `mysql` đang `Up`, database `inventory_service` đã tồn tại
- Log khởi động: `Successfully applied 2 migrations to schema inventory_service, now at version v2`
- Bảng `t_inventory` có 4 dòng dữ liệu mẫu:

| id | sku_code | quantity |
|---|---|---|
| 1 | iphone_15 | 100 |
| 2 | pixel_8 | 100 |
| 3 | galaxy_24 | 100 |
| 4 | oneplus_12 | 100 |

## Tổng hợp

| # | Test case | Params | Status kỳ vọng | Body kỳ vọng | Status thực tế | Body thực tế | Đạt? |
|---|---|---|---|---|---|---|---|
| 1 | Đủ hàng | skuCode=iphone_15, quantity=100 | 200 | true | 200 | `true` | ✅ |
| 2 | Không đủ hàng | skuCode=iphone_15, quantity=200 | 200 | false | 200 | `false` | ✅ |
| 3 | Boundary `=` tồn kho | skuCode=pixel_8, quantity=100 | 200 | true | 200 | `true` | ✅ |
| 4 | Boundary vượt 1 đơn vị | skuCode=pixel_8, quantity=101 | 200 | false | 200 | `false` | ✅ |
| 5 | SKU không tồn tại | skuCode=not_exist_sku, quantity=1 | 200 | false | 200 | `false` | ✅ |
| 6 | Thiếu `quantity` | skuCode=iphone_15 | 400 | — | 400 | `{"error":"Bad Request"}` | ✅ |
| 7 | Thiếu `skuCode` | quantity=10 | 400 | — | 400 | `{"error":"Bad Request"}` | ✅ |
| 8 | Sai kiểu `quantity` | quantity=abc | 400 | — | 400 | `{"error":"Bad Request"}` | ✅ |
| 9 | `quantity` âm | quantity=-5 | 200 (hiện tại) / nên 400 | true (hiện tại) | 200 | `true` | ✅ (ghi nhận lỗ hổng) |
| 10 | Sai method (POST) | — | 405 | — | 405 | `{"error":"Method Not Allowed"}` | ✅ |

- Endpoint chỉ đọc, dữ liệu `t_inventory` sau khi test không thay đổi (vẫn 4 dòng, mỗi SKU 100).

## Nhận xét

- TC3 và TC4 xác nhận query dùng đúng `>=` (`GreaterThanEqual`): 100 trả `true`, 101 trả `false`.
- TC5 trả `false` chứ không lỗi 404/500, vì `exists...` chỉ trả về không có record nào khớp.
- TC6, TC7, TC8 bị Spring chặn trước khi vào Controller (`@RequestParam` bắt buộc, không convert được `"abc"` sang `Integer`).
- TC9 cho thấy chưa validate input: số âm vẫn thỏa `quantity >= -5`. Hướng cải tiến: thêm `spring-boot-starter-validation`, `@Validated` trên Controller và `@Min(1)` cho `quantity` để trả `400 Bad Request` (giống lỗ hổng ở order-service TC3/TC6).
