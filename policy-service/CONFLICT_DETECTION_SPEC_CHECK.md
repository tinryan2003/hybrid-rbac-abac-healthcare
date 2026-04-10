# Policy Conflict Detection – So sánh spec 6 bước với implementation hiện tại

## Tóm tắt spec (baseline/naive)

| Bước | Nội dung |
|------|----------|
| 1 | **Format rule thống nhất**: `id`, `effect`, `actions`, `resource_type`, `conds` (eq/in/range, AND) |
| 2 | **Canonical domain**: ANY (không ràng buộc), SET (eq/in), INTERVAL (range); time → phút trong ngày |
| 3 | **Overlap domain**: ANY overlap mọi thứ; SET overlap ⇔ giao ≠ ∅; INTERVAL overlap ⇔ max(l1,l2) ≤ min(r1,r2) |
| 4 | **Rule overlap**: actions overlap + cùng resource_type + mọi attribute domain overlap |
| 5 | **Naive pairwise O(n²)**: nếu rule overlap và effect khác nhau → AUTH_CONFLICT |
| 6 | **Report + witness**: in conflict + “witness request” (mẫu request chứng minh overlap) |

---

## So sánh với implementation hiện tại

### Đã có (tương đương spec)

| Spec | Implementation | Ghi chú |
|------|----------------|---------|
| effect permit/deny | `Policy.effect` = Allow/Deny | ✅ |
| actions overlap | `actionsOverlap(rule1.actions, rule2.actions)` | ✅ |
| Chỉ conflict khi effect khác nhau | So sánh Allow vs Deny, bỏ qua cùng effect | ✅ |
| Pairwise (có tối ưu) | O(n²) nhưng index theo action → chỉ so Allow–Deny, cùng action | ✅ (mạnh hơn naive) |
| Report conflict | `ConflictPair`: policyId1, policyId2, reason, conflictType | ✅ |
| Subject/object attributes | `PolicyRule`: subjectAttributes, objectAttributes (Map<String, List<String>>) | ✅ Tương đương SET |

### Chưa có / khác spec

| Spec | Hiện tại | Cần bổ sung |
|------|----------|-------------|
| **resource_type** thống nhất | Có `resources` (JSON) nhưng conflict check **không** so resource_type | Thêm điều kiện: cùng resource_type (hoặc overlap resource) mới conflict |
| **conds** dạng chuẩn (eq/in/range) | `conditions` là Map<String, Object> (same_department, time_range, …) **chưa** đưa vào overlap | Canonical hóa conditions → domain (SET/INTERVAL); dùng trong overlap |
| **Domain ANY/SET/INTERVAL** | Chỉ so subject/object dạng list values (SET). Không có INTERVAL, không có ANY rõ ràng | Thêm: thiếu attribute → ANY; range (time, level) → INTERVAL |
| **Witness request** | Không có | Thêm: với mỗi conflict, build witness (1 phần tử trong giao SET, 1 điểm trong INTERVAL) |
| **Time → phút** | Không normalize time | Nếu thêm time vào conflict: "08:00"-"17:00" → IntervalDom(480, 1020) |

---

## Format rule hiện tại (Policy → PolicyRule)

- **id** ≈ `policyId`
- **effect** ≈ `effect` (Allow/Deny)
- **actions** ≈ parsed từ `policy.actions` (JSON array)
- **resource_type**: hiện không tách riêng; từ `policy.resources` có thể lấy `object` hoặc `type` (tùy frontend)
- **conds**: 
  - Subject/object: đã có qua `subjectAttributes`, `objectAttributes` (map attr → list values) → tương đương **SET**.
  - **Conditions** (time, department, hospital, …): đang parse vào `rule.conditions` nhưng **không dùng** trong `checkAttributeConflict` → cần đưa vào canonical conds và overlap.

---

## Đề xuất triển khai theo đúng spec (baseline)

1. **Chốt format rule (Bước 1)**  
   - Thêm/canonical hóa `resource_type` từ `resources` (ví dụ `resource.object` hoặc `resource.type`).  
   - Chuẩn hóa `conditions` thành `conds` với chỉ `eq`, `in`, `range` (AND):  
     - `same_department` → `sub.department` in [dept_ids] hoặc SET.  
     - `time_range` / working_hours → `env.time` range [start_min, end_min].  
     - Các attr khác tương tự.

2. **Canonical domain (Bước 2)**  
   - Trong service: map mỗi điều kiện → AnyDom / SetDom / IntervalDom.  
   - Time "HH:MM" → `time_to_min()` (0–1439).  
   - Thiếu attribute → AnyDom.

3. **Overlap + rule overlap (Bước 3–4)**  
   - Hàm `overlap(dom1, dom2)` cho ANY/SET/INTERVAL.  
   - `ruleOverlap(r1, r2)` = actions overlap + cùng resource_type + mọi attr (subject, object, conds) domain overlap.

4. **Naive pairwise (Bước 5)**  
   - Có thể thêm mode “baseline”: bỏ index, duyệt mọi cặp (i < j); hoặc giữ index nhưng đảm bảo định nghĩa conflict giống spec (effect khác nhau + rule overlap).

5. **Witness (Bước 6)**  
   - Với mỗi cặp conflict: với mỗi attribute, `witness(dom1, dom2)` → một giá trị cụ thể (SET: phần tử trong giao; INTERVAL: điểm trong khoảng; ANY: lấy từ rule kia).  
   - Thêm vào `ConflictPair`: `witnessRequest` (Map attribute → value hoặc object mô tả request mẫu).

6. **Vị trí chạy**  
   - Giữ như hiện tại: **offline/API** (sau khi thêm/sửa policy, gọi GET/POST conflict detection).  
   - Có thể thêm bước CI: nếu `conflictCount > 0` thì fail deploy hoặc cảnh báo.

---

## Kết luận

- **Spec 6 bước** mô tả đúng một **baseline/naive** conflict detection cho ABAC constraints (role-centric hybrid RBAC-A).
- **Implementation hiện tại** đã có: effect, actions overlap, subject/object attributes (SET), pairwise (có index), report.  
- **Thiếu so với spec**: (1) **resource_type** trong điều kiện conflict, (2) **conditions** (time, department, …) đưa vào canonical domain và overlap, (3) **witness request** trong report.
- Để “làm đúng spec”: cần canonical hóa rule (resource_type + conds eq/in/range), domain ANY/SET/INTERVAL, overlap theo domain, và sinh witness cho mỗi conflict.

File này dùng làm checklist khi triển khai đủ 6 bước trong `policy-service`.
