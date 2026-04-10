# Policy Conflict Detection – Kết quả kiểm tra

## Tóm tắt

- **Logic chính:** Đúng với mô hình Allow vs Deny + overlap (action, resource_type, subject/object, time).
- **Đã sửa:** Bucket key dùng `resourceType` thay vì `resourceType|actionKey` để không bỏ sót conflict khi hai rule cùng resource type nhưng khác tập action (vd. [read] vs [read,write]).
- **Witness:** Đã có: `buildWitness` + `witnessValue`; `ConflictPair.witnessRequest` được gán đúng.
- **Edge cases / spec còn thiếu:** ANY (attribute null/empty), conditions ngoài TIME_RANGE chưa canonical, redundancy không gọi `detectConflictsInRules` cho POST by IDs.

---

## 1. Kiến trúc đã xem

| Thành phần | Vai trò |
|------------|--------|
| `ConflictDetectionService` | Reduction, bucket, index by action, AUTH + REDUNDANCY |
| `TimeInterval` / `TimeIntervalReducer` | TIME_RANGE → phút trong ngày, disjoint segments |
| `TimeIntervalBinarySearch` | Tìm interval overlap + policy IDs có effect khác (conflict) |
| `PolicyRule` | effect, actions, resourceType, subject/object attributes, conditions |
| `ConflictPair` | policyId1/2, reason, conflictType, overlappingActions, resourceType, witnessRequest |

---

## 2. Đúng so với spec / paper

- **Effect:** Allow vs Deny; cùng effect không tính AUTH conflict (chỉ redundancy).
- **Action overlap:** `actionsOverlap()` dùng set intersection.
- **Resource type:** `resourceTypesOverlap()` (null/blank = ANY); đã dùng trong `checkConflict` và redundancy.
- **Subject/object:** SET overlap; “một rule chia sẻ toàn bộ attribute với rule kia” + value sets giao nhau → INTERSECT; chỉ một phần → PARTIAL_INTERSECT.
- **TIME_RANGE:** Parse hour/minute → phút trong ngày; `timeRangesOverlap()`; nếu không có time constraint thì coi là overlap (không giới hạn thời gian).
- **Reduction:** Chỉ rule có actions và (subject hoặc object) mới tham gia conflict.
- **Index:** Bucket theo resource type; trong bucket partition Allow/Deny, index theo từng action; chỉ so cặp (Allow, Deny) có ít nhất một action chung.
- **Report:** ConflictPair có reason, conflictType (AUTH_CONFLICT / REDUNDANCY_CONFLICT), overlappingActions, resourceType.
- **Witness:** `buildWitness` lấy union attribute, `witnessValue` chọn một giá trị trong giao (SET); null/empty coi như ANY (lấy từ bên kia). Witness được đưa vào `ConflictPair.witnessRequest`.

---

## 3. Bug đã sửa

**Bucket key (bỏ sót conflict):**

- Trước: `bucketKey = resourceType + "|" + actionKey` (actionKey = toàn bộ actions nối bằng dấu phẩy).
- Hệ quả: Rule A [read, write] và rule B [read] cùng resource type → hai bucket khác nhau → không bao giờ so sánh → bỏ sót conflict.
- Sửa: Bucket chỉ theo `resourceType` (hoặc "ANY"). Trong bucket vẫn dùng index theo action để chỉ so (Allow, Deny) có ít nhất một action chung.

---

## 4. Edge cases & khác biệt so với spec 6 bước

| Vấn đề | Hiện trạng | Gợi ý |
|--------|------------|--------|
| **ANY attribute** | Subject/object null hoặc empty → `checkAttributeSetConflict` trả về DISJOINT. | Spec: thiếu attribute = ANY, overlap mọi thứ. Có thể: trong overlap attribute, nếu một bên null/empty thì coi attribute đó INTERSECT. |
| **Conditions khác TIME_RANGE** | Chỉ TIME_RANGE được parse và dùng trong overlap; same_department, v.v. chưa đưa vào conflict. | Nếu cần đủ spec: canonical hóa conditions (eq/in/range) và đưa vào overlap (SET/INTERVAL). |
| **POST /detect (policyIds)** | `detectConflicts(List<String> policyIds)` chỉ gọi `detectConflictsInRules`, không gọi `detectRedundancyConflicts`. | Nếu muốn redundancy khi detect theo list: gọi thêm `detectRedundancyConflicts(rules)` và merge vào result. |
| **TimeIntervalReducer** | Một segment có thể nhận cùng policyId từ nhiều interval (nếu sau này một rule có nhiều TIME_RANGE). | Hiện mỗi rule chỉ một interval → không lỗi. Nếu sau này hỗ trợ nhiều khoảng/rule, cần deduplicate policyIds/effects trong segment. |

---

## 5. TimeIntervalBinarySearch

- `findOverlappingPolicies`: binary search theo `end > query.start`, rồi quét tới khi `start >= query.end`; gom tất cả policyIds từ các interval overlap → đúng.
- `findConflictingPolicies`: lấy overlapping intervals rồi với mỗi (policyId, effect) trong interval, thêm policyId nếu effect khác queryEffect. Logic đúng; có thể tối ưu bằng cách dùng kết quả từ findOverlappingPolicies thay vì duyệt lại toàn bộ intervals (hiện vẫn đúng).

---

## 6. Conflict vs Redundancy theo lý thuyết (paper)

- **AUTH_CONFLICT:** Hai rule trái effect (Allow vs Deny) cho cùng request (overlap) → quan trọng (bảo mật). Giữ nguyên.
- **Redundancy:** Paper = một rule bị phủ bởi rule khác (cùng effect, subset of requests). Không phải "bất kỳ overlap nào cùng effect".
- **Vấn đề cũ:** Doctor Allow vs Admin Allow bị báo REDUNDANCY vì code coi overlap = PARTIAL_INTERSECT (cùng key roles nhưng value DOCTOR vs ADMIN disjoint) → không có request match cả hai → không phải redundancy.
- **Đã sửa:** Chỉ báo REDUNDANCY khi checkAttributeConflict = EXCEPTION (subject và object đều full intersect). Chi tiết: `CONFLICT_VS_REDUNDANCY_THEORY.md`.

---

## 7. Kết luận

- **Conflict detection (AUTH + REDUNDANCY):** Logic đúng; bucket key đã sửa; redundancy chỉ khi full overlap (EXCEPTION).
- **Time:** Disjoint intervals + binary search phù hợp paper; TIME_RANGE overlap và “không có = mọi thời điểm” nhất quán.
- **Witness:** Đã có và gán vào `witnessRequest`.
- **Cần làm thêm (tùy mức độ đúng spec):** (1) ANY cho attribute null/empty trong overlap, (2) conditions ngoài time canonical + overlap, (3) redundancy subsumption (một rule ⊆ rule kia) nếu muốn sát paper hơn.
