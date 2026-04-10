# Phân tích Section 3.4.6 – Policy governance and misuse prevention

**Đã áp dụng:** Phần học thuật (định nghĩa misuse, invariant, pre/post) đã bổ sung vào Section 3.4.6 trong \texttt{chap-Table.tex}; subsection ``Policy governance and misuse prevention (implementation)'' và demo scenarios đã thêm vào \texttt{chap-Formula.tex} (Chapter 4). Mô tả version history đã sửa cho khớp implementation (metadata + future work cho snapshot).

---

## Nhận xét của supervisor

- Các quy tắc access control đề xuất ở mức cơ bản, phù hợp bài toán nhiều vai trò trong HMS, đạt mức luận văn cử nhân.
- **Chưa giải quyết**: "the potential for conflicts and abuse of power if hybrid systems (RBAC & ABAC) implementation is not effectively controlled".
- Thầy nói: hiện chỉ **nhắc đến** vấn đề, chưa **giải quyết** — cần giải quyết từ **học thuật** tới **demo**.

---

## 1. Hiện trạng Section 3.4.6 (chap-Table.tex)

### 1.1 Nội dung hiện có

- **Vấn đề được nêu**: policy governance risks; privileged users có thể abuse (tạo Allow quá rộng, làm yếu constraint); false-permit nguy hiểm trong bệnh viện.
- **Năm biện pháp** (chỉ mô tả, không formal):
  1. Deterministic decision semantics
  2. Restricted policy administration (chỉ admin được sửa policy)
  3. Change traceability and policy version history
  4. Conflict checks on policy update (reject nếu conflict)
  5. Audit evidence cho sensitive access và privileged operations

### 1.2 Khoảng trống so với yêu cầu “giải quyết”

| Khía cạnh | Hiện tại | Thiếu |
|-----------|----------|--------|
| **Học thuật** | Liệt kê cơ chế, không định nghĩa formal | Định nghĩa rõ “misuse/abuse”; invariant (vd: tập policy không chứa AUTH_CONFLICT); điều kiện tiền/hậu cho thao tác cập nhật policy; cách các control ngăn abuse. |
| **Demo** | Chương 4 không có subsection riêng cho governance | Không có đoạn “Governance implementation” hay “Misuse prevention demo”; không chỉ rõ conflict-on-save bật strict; không chứng minh bằng kịch bản (409 khi conflict, 403 khi non-admin, audit). |
| **Implementation** | Một số phần đã có, một số chưa khớp luận văn | Conflict-on-save: có (strict), nhưng luận văn không nói rõ chế độ strict; policy version **history (snapshot)** chưa có trong code; policy create/update **chưa gửi audit event** tới audit-service. |

---

## 2. Đề xuất bổ sung từ học thuật tới demo

### 2.1 Học thuật (Chapter 3 – Section 3.4.6)

**A. Định nghĩa rõ “misuse” / “abuse” trong ngữ cảnh hybrid**

- Thêm 1 đoạn ngắn (hoặc definition): *Misuse/abuse* = (i) đưa vào tập policy một cặp rule xung đột (Allow vs Deny cho cùng request), hoặc (ii) mở rộng quyền không có kiểm soát (vd. thêm Allow quá rộng, bỏ constraint) mà không bị ràng buộc bởi conflict check hoặc audit.

**B. Formal invariant và pre/post cho policy update**

- **Invariant** (có thể viết dạng công thức hoặc bullet):
  - *Conflict-freedom*: Mọi tập policy enabled tại mỗi thời điểm không chứa cặp rule AUTH_CONFLICT (định nghĩa AUTH_CONFLICT đã có ở 3.4.5).
  - *Write-restriction*: Chỉ subject có role ADMIN mới được thực hiện thao tác ghi (create/update/delete) trên policy.
- **Pre-condition cho PolicyUpdate** (trước khi lưu):
  - Subject có role ADMIN, và  
  - Nếu bật conflict-on-save (strict): tập policy sau khi áp dụng thay đổi không được chứa AUTH_CONFLICT.
- **Post-condition**: Nếu lưu thành công thì invariant conflict-freedom vẫn đúng; mọi thay đổi có created_by/updated_by và timestamp (traceability).

**C. Nối từng control tới “giải quyết” conflict & abuse**

- Thêm 1 câu rõ ràng cho từng control, kiểu:
  - *Deterministic semantics* → tránh hành vi không xác định, giảm “accidental permission”.
  - *Restricted administration* → chỉ ADMIN mới có thể thay đổi policy → giới hạn abuse (định nghĩa abuse như trên).
  - *Conflict check on update* → đảm bảo invariant conflict-freedom được duy trì → trực tiếp ngăn đưa conflict vào.
  - *Traceability & version* → ai thay gì khi nào → hỗ trợ phát hiện và truy vết abuse.
  - *Audit* → bằng chứng cho sensitive access và privileged operations (policy update) → accountability.

Như vậy section 3.4.6 không chỉ “nhắc” risk mà còn nêu **định nghĩa**, **invariant**, **điều kiện** và **cách mỗi control giải quyết** conflict/abuse.

### 2.2 Demo / Implementation (Chapter 4)

**A. Subsection mới: “Policy governance and misuse prevention (implementation)”**

- Đặt ngay sau phần Policy Conflict Detection Implementation (Section 4.x), tham chiếu Section 3.4.6.
- Nội dung gợi ý:
  - Conflict-on-save: prototype cấu hình `policy.conflict-on-save: strict`; khi create/update policy mà gây AUTH_CONFLICT thì service trả 409 và conflict report; không lưu → **duy trì conflict-freedom**.
  - Restricted administration: gateway và policy-service chỉ cho phép role ADMIN gọi POST/PUT/DELETE policy; non-admin nhận 403.
  - Traceability: policy có version, created_at/updated_at, created_by_keycloak_id/updated_by_keycloak_id (nếu có snapshot history thì nói rõ; nếu chưa có thì nói “metadata for traceability …; full snapshot history có thể mở rộng sau”).
  - Audit: nếu đã tích hợp policy events vào audit-service thì mô tả và có thể kèm hình audit log cho policy update; nếu chưa thì nói rõ “policy create/update có thể gửi event tới audit service để đáp ứng governance” và liệt kê bước mở rộng.

**B. Kịch bản demo rõ ràng (trong Chương 4 hoặc Evaluation)**

- **Scenario 1 – Conflict prevention**: Thêm/sửa policy để tạo cặp Allow/Deny trùng điều kiện → API trả 409, UI hiển thị conflict report → chứng minh “conflict không bị đưa vào hệ thống”.
- **Scenario 2 – Abuse prevention (restriction)**: User không phải ADMIN gọi POST /api/policies (hoặc PUT) → 403 → chứng minh “chỉ admin mới thay đổi được policy”.
- **Scenario 3 – Traceability**: Sau khi admin cập nhật policy, kiểm tra version/timestamp/created_by (và audit log nếu có) → chứng minh “ai thay gì khi nào”.

Những scenario này chứng minh governance không chỉ được “nhắc” mà còn **được triển khai và kiểm chứng** trong demo.

### 2.3 Implementation (code) – tùy thời gian

- **Conflict-on-save**: Đã có; chỉ cần đảm bảo luận văn và cấu hình mặc định (application.yml) nói rõ dùng `strict` cho môi trường governance.
- **Policy version history (snapshot)**: Luận văn hiện nói “policy-version history is maintained as snapshots”. Trong code chỉ có `version` (integer) và created_by/updated_by, **chưa có bảng lưu snapshot nội dung policy**. Cần một trong hai:
  - **Option A**: Thêm bảng `policy_snapshots` (policy_id, version, snapshot_json, created_at, created_by), lưu mỗi lần create/update → luận văn giữ nguyên mô tả.
  - **Option B**: Sửa luận văn: “Policies are stored with metadata (version, timestamps, created_by, updated_by) for traceability; full content snapshots for rollback can be added as future work.”
- **Audit cho policy update**: Policy-service hiện không gửi event (RabbitMQ) khi create/update policy. Để “audit evidence for policy updates” đúng như Section 3.4.6:
  - Policy-service gửi event (e.g. POLICY_CREATED, POLICY_UPDATED) tới audit exchange; audit-service có ResourceType.POLICY / POLICY_MANAGEMENT và event type tương ứng, lưu vào audit_logs. Sau đó trong luận văn có thể trích dẫn và có screenshot audit log cho policy update.

---

## 3. Tóm tắt hành động

| Ưu tiên | Việc cần làm |
|--------|-------------------------------|
| **Cao** | Bổ sung vào **Section 3.4.6**: định nghĩa misuse/abuse; invariant (conflict-freedom, write-restriction); pre/post cho policy update; và câu nối từng control → giải quyết conflict/abuse. |
| **Cao** | Thêm vào **Chapter 4**: subsection “Policy governance and misuse prevention (implementation)” + 3 scenario demo (409 conflict, 403 non-admin, traceability). |
| **Trung bình** | Chuẩn hóa mô tả **conflict-on-save**: nói rõ prototype dùng chế độ strict và reject (409) khi có AUTH_CONFLICT. |
| **Trung bình** | Sửa mô tả **policy version history**: hoặc implement snapshot (bảng policy_snapshots), hoặc đổi thành “metadata for traceability; snapshot as future work”. |
| **Thấp (nếu có thời gian)** | Implement **audit event cho policy create/update** (policy-service → audit-service) và thêm 1–2 câu + hình trong luận văn. |

Làm xong các mục “Cao” và “Trung bình” thì Section 3.4.6 sẽ không chỉ nhắc vấn đề mà **trình bày cách giải quyết từ học thuật (định nghĩa, invariant, điều kiện) đến demo (implementation + scenario)**.
