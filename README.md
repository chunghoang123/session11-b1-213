# RikkeiPay Assistant — Langfuse tracing chống nghẽn

Project độc lập minh họa Spring Boot gửi trace sang Langfuse bằng OTLP/HTTP qua một `BatchSpanProcessor` có hàng đợi hữu hạn. Luồng xử lý giao dịch chỉ đưa span đã kết thúc vào hàng đợi trong RAM; I/O mạng được worker nền thực hiện.

## Thành phần và cách chạy

- Java 21, Spring Boot 3.4.2, Spring AI 1.0.0-M6.
- Các dependency bắt buộc nằm trong `build.gradle`: Web, Actuator, Spring AI OpenAI, Micrometer OTel bridge, OTLP exporter và Langfuse Java.
- Langfuse UI/OTLP: `http://localhost:3000`.
- API tạo thêm trace: `GET http://localhost:8080/api/payments/ping`.

```powershell
Copy-Item .env.example .env
# Thay toàn bộ placeholder trong .env trước khi chạy.
docker compose -f docker-compose-langfuse.yml up -d --wait
.\gradlew.bat bootRun
```

Thông tin đăng nhập và API key được đọc từ `.env`; file này bị Git bỏ qua và không được đẩy lên repository.

## Vì sao Compose có 6 service

Ba service mà đề bài nêu (`postgres`, `clickhouse`, `langfuse-server`) đều có mặt; `langfuse-server` chờ PostgreSQL khỏe bằng `condition: service_healthy`. Tuy nhiên Langfuse v3/v4 không còn là kiến trúc ba container: OTLP ingestion ghi event vào object storage và hàng đợi, sau đó worker nạp dữ liệu vào ClickHouse. Vì vậy `redis`, `minio`, `langfuse-worker` là bắt buộc để file chạy thật, không phải thành phần tùy chọn. Cắt ba service này có thể làm web khởi động nhưng ingest trace không hoạt động đúng.

PostgreSQL giữ user/project/API key/prompt và metadata; ClickHouse giữ lượng lớn traces/spans và dữ liệu phân tích. Tất cả database chạy UTC theo yêu cầu của Langfuse.

## BatchSpanProcessor và Drop Span

Khi một span kết thúc, `BatchSpanProcessor.onEnd()` thử đưa nó vào hàng đợi bounded có tối đa 2.048 phần tử. Thao tác này không chờ một slot trống. Nếu exporter bị chậm, Langfuse mất mạng, hoặc tốc độ sinh span vượt tốc độ xuất đến mức hàng đợi đầy, span mới bị loại ngay. OpenTelemetry ghi cảnh báo/telemetry nội bộ về span bị loại, nhưng không quay lại chặn request.

Worker nền thức dậy tối đa sau 2 giây hoặc khi đủ batch, lấy tối đa 512 span và gọi OTLP exporter. Mỗi lần export chỉ được phép kéo dài 3 giây. Bốn giá trị trong `application.yml` tạo ba lớp phòng thủ:

1. `max-queue-size: 2048` chặn tăng RAM vô hạn.
2. `max-export-batch-size: 512` giới hạn kích thước mỗi request OTLP.
3. `schedule-delay: 2s` cân bằng độ trễ và số request mạng.
4. `export-timeout: 3s` giải phóng worker khi collector treo.

Drop span là lựa chọn **mất observability để giữ availability**. Với giao dịch ngân hàng, trace là dữ liệu phụ trợ; thread servlet đang xác thực/chuyển tiền không được phụ thuộc vào DNS, TCP, TLS, Langfuse, ClickHouse hay thời gian chờ collector. Nếu dùng exporter đồng bộ, sự cố giám sát có thể chiếm toàn bộ thread pool, kéo dài latency, gây timeout dây chuyền và biến lỗi ở hệ thống quan sát thành lỗi giao dịch. Batch bất đồng bộ giữ ranh giới lỗi đó; chi phí đánh đổi là trace có thể thiếu khi sự cố kéo dài.

Hàng đợi không phải kho bền vững: tiến trình dừng đột ngột cũng làm mất span chưa gửi. Với tracing đây là thiết kế có chủ đích. Không được dùng cơ chế này thay cho audit log hoặc sổ cái giao dịch bắt buộc phải lưu.

## Xác thực kết nối thực tế

Ứng dụng tạo span `langfuse-startup-probe` sau khi Spring sẵn sàng. Wrapper exporter chỉ in dòng thành công sau khi Langfuse trả kết quả OTLP thành công:

```text
Started It213Session11Bai1Application in ... seconds
OTLP export succeeded: batchSize=1, totalExported=1, endpoint=http://localhost:3000/api/public/otel/v1/traces
```

Không coi dòng “configured” hoặc việc TCP port mở là bằng chứng. Nếu endpoint lỗi/timeout, log sẽ là `OTLP export failed or timed out`; ứng dụng vẫn tiếp tục phục vụ `/api/payments/ping`, chứng minh đường giao dịch không phụ thuộc exporter.

## Kiểm thử

```powershell
.\gradlew.bat test
docker compose -f docker-compose-langfuse.yml config --quiet
```

`OtelBatchPropertiesTests` kiểm tra bốn giới hạn an toàn được bind đúng từ YAML.

Log đã xác minh end-to-end được lưu tại [`docs/RUN_EVIDENCE.md`](docs/RUN_EVIDENCE.md).
