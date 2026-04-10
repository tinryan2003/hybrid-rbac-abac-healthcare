# Policy JSON schema – Single rule vs Multi rule

## 1. Hai cách gửi policy (single-rule vs multi-rule)

API **POST/PUT /api/policies** chấp nhận **hai** kiểu body: **policy có mảng `rules`** (multi-rule) hoặc single-rule (top-level effect/subjects/actions/resources; backend tạo 1 rule r1). Nếu có rules (non-empty) thì dùng rules.

| Cách | Mô tả |
|------|--------|
| **Multi-rule** | Gửi `rules` với 1 hoặc nhiều rule (format chính). |
| **Single-rule (flat)** | Không gửi `rules`, gửi top-level effect/subjects/actions/resources/conditions; backend tạo 1 rule `r1`. |

---

## 2. So sánh JSON trong ảnh với schema backend

### 2.1 Tên field khác nhau

| Trong ảnh (JSON bạn gửi) | Trong project (PolicyCreateUpdateRequest / PolicyRuleItemDto) | Ghi chú |
|---------------------------|----------------------------------------------------------------|---------|
| `id` | `policyId` | Bắt buộc, unique. |
| `name` | `policyName` | Bắt buộc. |
| (không có) | `tenantId` | Bắt buộc (vd: `"HOSPITAL_A"`). |
| `effect` | `effect` | Giống (Allow/Deny). |
| `priority` | `priority` | Giống. |
| `combiningAlgorithm` | `combiningAlgorithm` | Giống (vd: `"deny-overrides"`). |
| `rules` | `rules` | Giống – mảng rule. |
| **Trong từng rule:** | | |
| `ruleId` | `ruleId` | Giống. |
| `ruleName` | `ruleName` | Giống. |
| **`roles`** (mảng trực tiếp) | **`subjects`** (object) | Backend cần `subjects: { "roles": ["ADMIN"] }`, không có field `roles` ở rule. |
| **`resource_type`** (string) | **`resources`** (object) | Backend cần `resources: { "type": "appointment" }`, không có field `resource_type`. |
| `actions` | `actions` | Giống (mảng string). |
| **`condition`** (số ít) | **`conditions`** (số nhiều) | Backend dùng `conditions`. Cấu trúc tree `operator` / `children` với `field`, `operator`, `value` thì **đúng** (OPA/transformer hiểu). |
| `priority` (trong rule) | `priority` | Giống. |

### 2.2 JSON trong ảnh chưa đúng schema backend

- **Rule dùng `roles`** → backend mong **`subjects`**: `"subjects": { "roles": ["ADMIN"] }`.
- **Rule dùng `resource_type`** → backend mong **`resources`**: `"resources": { "type": "appointment" }`.
- **Rule dùng `condition`** → backend mong **`conditions`** (và tree AND/children với `field`/`operator`/`value` là hợp lệ).
- **Thiếu `tenantId`** ở top-level → bắt buộc phải thêm.
- **`id` / `name`** trong ảnh → map thành **`policyId`** / **`policyName`**.

### 2.3 Ví dụ chuyển từ JSON ảnh sang đúng API

**Trong ảnh (rút gọn):**
```json
{
  "id": "",
  "name": "",
  "effect": "allow",
  "priority": 10,
  "combiningAlgorithm": "deny-overrides",
  "rules": [
    {
      "ruleId": "r1",
      "ruleName": "Rule 1",
      "effect": "allow",
      "roles": ["ADMIN"],
      "resource_type": "appointment",
      "actions": [],
      "condition": { "operator": "AND", "children": [...] },
      "priority": 10
    }
  ]
}
```

**Đúng schema backend (ví dụ):**
```json
{
  "tenantId": "HOSPITAL_A",
  "policyId": "policy-appointment-admin",
  "policyName": "Appointment admin rule",
  "effect": "allow",
  "priority": 10,
  "combiningAlgorithm": "deny-overrides",
  "rules": [
    {
      "ruleId": "r1",
      "ruleName": "Rule 1",
      "effect": "Allow",
      "subjects": { "roles": ["ADMIN"] },
      "resources": { "type": "appointment" },
      "actions": ["read", "update"],
      "conditions": {
        "operator": "AND",
        "children": [
          { "field": "env.time", "operator": "eq", "value": "08:00-17:00" }
        ]
      },
      "priority": 10
    }
  ]
}
```

- `actions: []` trong ảnh: backend/conflict detection thường coi rule không có action là không tham gia conflict; nên chỉ rõ action (vd: `["read","update"]`) cho đúng ngữ nghĩa.
- `env.ip`: operator nên có giá trị (vd: `"eq"` hoặc format IP/CIDR mà OPA/transformer hỗ trợ).

---

## 3. Kết luận

- **Chỉ có một schema API**: luôn là **multi-rule** với mảng `rules`; “single rule” = gửi `rules: [ { một rule } ]`.
- **JSON trong ảnh là multi-rule đúng ý tưởng**, nhưng **chưa đúng schema backend** vì:
  - Dùng `roles` thay vì `subjects: { "roles": [...] }`,
  - Dùng `resource_type` thay vì `resources: { "type": "..." }`,
  - Dùng `condition` thay vì `conditions`,
  - Thiếu `tenantId`,
  - Top-level dùng `id`/`name` thay vì `policyId`/`policyName`.
- Sau khi đổi tên field và thêm `tenantId` (và điền `actions`/operator cho `env.ip`) thì JSON đó **đúng** với schema hiện tại và dùng được cho conflict detection + resolution (deny-overrides, priority).

Nếu bạn muốn, bước tiếp theo có thể là: (1) thêm hỗ trợ **single-rule** thật (khi `rules` null/empty thì build 1 rule từ top-level effect/subjects/actions/resources/conditions), hoặc (2) chuẩn hóa một bản “policy JSON mẫu” đúng schema để copy-paste.
