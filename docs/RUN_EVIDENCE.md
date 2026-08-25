# Minh chứng chạy thực tế

Thời điểm xác minh: 2026-08-25, múi giờ Asia/Saigon.

## Langfuse health

```text
GET http://localhost:3000/api/public/health
{"status":"OK","version":"4.18.0"}
```

Docker Compose báo cả PostgreSQL, ClickHouse, Redis và MinIO ở trạng thái `healthy`; `langfuse-server` và `langfuse-worker` đều `Up`.

## Spring Boot startup và OTLP

Đây là log console thật của lần chạy `gradlew.bat bootRun --console=plain`:

```text
:: Spring Boot :: (v3.4.2)

2026-08-25T19:00:21.827+07:00  INFO ... Starting It213Session11Bai1Application using Java 21.0.10
2026-08-25T19:00:25.731+07:00  INFO ... Tomcat started on port 8080 (http) with context path '/'
2026-08-25T19:00:25.752+07:00  INFO ... Started It213Session11Bai1Application in 4.432 seconds
2026-08-25T19:00:28.197+07:00  INFO ... OTLP export succeeded: batchSize=1, totalExported=1, endpoint=http://localhost:3000/api/public/otel/v1/traces
```

Sau đó gọi endpoint giao dịch mẫu:

```text
GET http://localhost:8080/api/payments/ping
{"status":"accepted","timestamp":"2026-08-25T12:00:45.783217100Z"}

2026-08-25T19:00:46.284+07:00  INFO ... OTLP export succeeded: batchSize=2, totalExported=3, endpoint=http://localhost:3000/api/public/otel/v1/traces
```

Dòng `OTLP export succeeded` chỉ được ghi trong callback khi `CompletableResultCode.isSuccess()` là `true`, tức là exporter đã nhận phản hồi thành công từ endpoint; đây không phải log cấu hình giả lập.

## Build/test

```text
> Task :compileJava
> Task :test
BUILD SUCCESSFUL in 42s
4 actionable tasks: 4 executed
```
