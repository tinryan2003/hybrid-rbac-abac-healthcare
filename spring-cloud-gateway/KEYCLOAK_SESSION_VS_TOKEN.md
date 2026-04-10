# Keycloak: Session vs Access Token — Tại sao Gateway vẫn chấp nhận khi "session expired"?

## Hai khái niệm khác nhau

| Khái niệm | Ý nghĩa | Gateway có kiểm tra? |
|-----------|----------|----------------------|
| **Keycloak SSO Session** | Phiên đăng nhập trên Keycloak (idle/max timeout). Keycloak Admin hiển thị "session expired" theo cài đặt này. | **Không** — Gateway không gọi Keycloak để hỏi session còn sống hay không. |
| **JWT Access Token** | Token có claim `exp` (expiration). Gateway validate **chữ ký** và **exp** khi decode JWT. | **Có** — Nếu JWT hết hạn (`exp` trong quá khứ), Gateway trả 401. |

Gateway **chỉ** xem JWT: kiểm tra chữ ký (JWK từ Keycloak) và thời hạn `exp`. Nó **không** biết trạng thái session trên Keycloak. Nên:

- Keycloak báo "session expired" (theo SSO Session Idle/Max).
- Access token vẫn còn thời hạn (`exp` chưa tới) → Gateway vẫn chấp nhận.

## Cách xử lý

### 1. Rút ngắn Access Token Lifespan (đơn giản)

Trong Keycloak: **Realm Settings → Tokens**:

- **Access Token Lifespan**: đặt ngắn (ví dụ **5 phút**).
- **SSO Session Idle / Max**: giữ theo chính sách (ví dụ 30 phút).

Khi đó token hết hạn nhanh; frontend dùng refresh token để lấy access token mới. Khi session thật sự hết (refresh fail), frontend nhận 401 và có thể redirect sang trang session expired.

### 2. Bật Token Introspection (khớp với trạng thái Keycloak)

Để Gateway **từ chối** ngay khi Keycloak coi token/session không còn hợp lệ (logout, revoke, session expired):

1. Trong Keycloak tạo client (ví dụ `gateway-client`):
   - **Access Type**: confidential
   - **Service accounts roles**: ON
   - Vào **Service account roles** → client **realm-management** → gán role **introspection**.

2. Trong `application.yml` (hoặc profile tương ứng):

```yaml
keycloak:
  introspection:
    enabled: true
    uri: http://localhost:8180/realms/hospital-realm/protocol/openid-connect/token/introspect
    client-id: gateway-client
    client-secret: "<client-secret từ Keycloak>"
```

3. Khởi động lại Gateway.

Khi bật, mỗi request có JWT sẽ được Gateway gửi lên Keycloak introspect. Nếu Keycloak trả `active: false` (session expired, token revoked, v.v.) → Gateway trả **401 Unauthorized**.

**Lưu ý**: Introspection tăng một gọi HTTP tới Keycloak mỗi request; có thể cân nhắc cache kết quả (theo tài liệu Spring / best practice) nếu cần giảm tải.

## Tóm tắt

- **"Session expired" trên Keycloak** = trạng thái phiên SSO trên Keycloak.
- **Gateway chỉ kiểm tra JWT** (chữ ký + `exp`), không tự hỏi Keycloak session còn hay không.
- Muốn Gateway từ chối đúng lúc Keycloak coi là hết session: rút ngắn **Access Token Lifespan** và/hoặc bật **Token Introspection** như trên.
