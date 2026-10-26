# Kết quả kiểm thử Order Saga (Orchestration)

> Theo `slot 13/Saga_guide.md`, mục 6. Spring Boot 4.1.1, Java 21. Chạy theo **cách B**: cả 3 database và 3 service đều trong Docker (`docker compose up -d`).
> Import `Saga_Postman_Collection.json` vào Postman, chạy Collection Runner theo thứ tự `0 → KB1 → KB2 → KB3 → KB4 → KB5 → KB7`, bỏ thư mục "Thủ công".

## Hạ tầng (TODO-0, TODO-10)

| Container | Trạng thái | Cổng host |
|---|---|---|
| `saga-mysql` (mysql:8.4) | Up (healthy) | 3307 |
| `saga-sqlserver` (mssql/server:2022-latest) | Up (healthy) | 14330 |
| `saga-mongodb` (mongo:8.0, replica set `rs0`) | Up (healthy) | 27017 |
| `inventory-service` | Up | 8082 |
| `payment-service` | Up | 8083 |
| `order-service` | Up | 8081 |

Dữ liệu mẫu sau khi khởi động: `P1 = 10`, `P2 = 5`, `C1 = 5000`, `C2 = 100`.

## Collection Runner (KB1 → KB5, KB7)

Chạy bằng newman: **43 request, 89 assertion, 0 lỗi**.

| KB | Request `POST /api/orders` | `status` / `sagaStatus` | `failureReason` | Kho / ví | Đạt? |
|---|---|---|---|---|---|
| KB1 | `C1, P1, 2, 2000` | `CONFIRMED / COMPLETED` | `null` | P1 10 → 8, C1 5000 → 3000 | ✅ |
| KB2 | `C2, P1, 1, 1000` | `CANCELLED / COMPENSATED` | `402 - Số dư không đủ: còn 100.0, cần 1000.0` | P1 giữ 8, C2 giữ 100; reservation `RELEASED` | ✅ |
| KB3 | `C1, P2, 100, 2000` | `CANCELLED / COMPENSATED` | `409 - Không đủ hàng: còn 5, cần 100` | P2 giữ 5, C1 giữ 3000 | ✅ |
| KB4 | `C1, P9, 1, 10` | `CANCELLED / COMPENSATED` | `404 - Không tìm thấy sản phẩm P9` | không đổi | ✅ |
| KB5 | `C9, P2, 1, 20` | `CANCELLED / COMPENSATED` | `404 - Không tìm thấy ví của C9` | P2 giữ 5 (trừ rồi trả) | ✅ |
| KB7 | release ×2, reserve lại, pay lại, refund order chưa trả | – | – | tất cả không đổi | ✅ |

Thứ tự `step` trong saga-logs:

| KB | Saga logs |
|---|---|
| KB1 | `CREATE_ORDER → RESERVE_INVENTORY → PROCESS_PAYMENT → CONFIRM_ORDER` |
| KB2, KB5 | `CREATE_ORDER → RESERVE_INVENTORY → SAGA → COMPENSATION → CANCEL_ORDER` |
| KB3, KB4 | `CREATE_ORDER → SAGA → CANCEL_ORDER` (stack compensation rỗng, không gọi `release`) |

## Kịch bản thủ công

### KB6 – payment-service bị tắt

`docker compose stop payment-service` → đặt hàng `C1, P2, 1, 20` → `docker compose start payment-service`. Postman: 6 request, 12 assertion, 0 lỗi.

- Order: `CANCELLED / COMPENSATED`
- `failureReason`: `Service không phản hồi: I/O error on POST request for "http://payment-service:8083/api/payments/pay": null`
- P2 giữ 5 (trừ rồi trả), C1 giữ 3000
- Saga logs: `CREATE_ORDER → RESERVE_INVENTORY → SAGA → COMPENSATION → CANCEL_ORDER`

### KB8 – Compensation cũng lỗi

Khởi động lại order-service với `PAYMENT_DELAY_MS=15000`, gửi đơn `C1, P2, 1, 20`, trong 15 giây chờ chạy `docker compose stop payment-service inventory-service`, sau đó bật lại. Postman: 6 request, 13 assertion, 0 lỗi.

- Order: `PENDING / COMPENSATING` (không báo `COMPENSATED` giả)
- P2: 5 → 4 (bị giữ vì `release` chưa chạy được)
- Saga logs:

| step | result | message |
|---|---|---|
| CREATE_ORDER | SUCCESS | Order PENDING |
| RESERVE_INVENTORY | SUCCESS | Đã giữ 1 P2 |
| SAGA | FAILED | Service không phản hồi: I/O error on POST request for "http://payment-service:8083/..." |
| COMPENSATION | FAILED | Lần thử 1: I/O error on POST request for "http://inventory-service:8082/..." |
| COMPENSATION | FAILED | Lần thử 2: ... |
| COMPENSATION | FAILED | Lần thử 3: ... |

- Khôi phục: chạy lại order-service với `PAYMENT_DELAY_MS=0`, gọi tay `POST /api/inventory/release/8` → `RELEASED`, P2 trở về 5. Order 8 vẫn ở `COMPENSATING` vì chưa có cơ chế khôi phục Saga (EXT-4).

## Dữ liệu `orders` sau khi chạy

| id | Kịch bản | status | saga_status |
|---|---|---|---|
| 1 | KB1 | CONFIRMED | COMPLETED |
| 2 | KB2 | CANCELLED | COMPENSATED |
| 3 | KB3 | CANCELLED | COMPENSATED |
| 4 | KB4 | CANCELLED | COMPENSATED |
| 5 | KB5 | CANCELLED | COMPENSATED |
| 6, 7 | KB6 (chạy 2 lần) | CANCELLED | COMPENSATED |
| 8 | KB8 | PENDING | COMPENSATING |

Trạng thái cuối: `P1 = 8`, `P2 = 5`, `C1 = 3000`, `C2 = 100`.

## Ghi chú môi trường

- Máy chạy test có **Avast Web Shield** quét HTTPS, nên Maven bên trong container báo `PKIX path building failed` ở bước `mvn dependency:go-offline`. Dockerfile của project giữ đúng như hướng dẫn; khi test, image được build bằng một Dockerfile cục bộ có thêm bước import chứng chỉ gốc Avast vào JDK của stage build. Trên máy không quét HTTPS, `docker compose up -d --build` chạy trực tiếp.
- Docker Desktop được cấp 3,9 GB RAM (hướng dẫn khuyên ≥ 4 GB); cả 6 container vẫn chạy ổn định trong suốt quá trình test.
