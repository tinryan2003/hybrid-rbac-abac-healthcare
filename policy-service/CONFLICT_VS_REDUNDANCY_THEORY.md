# Lý thuyết Conflict vs Redundancy (theo paper)

## Hai loại bất thường chính

| Khái niệm | Định nghĩa (paper) | Mức độ quan trọng |
|-----------|--------------------|--------------------|
| **Conflict (AUTH_CONFLICT)** | Hai rule đưa ra **quyết định trái ngược** (Permit vs Deny) cho **cùng một request** (tập request match cả hai rule có giao khác rỗng). | **Quan trọng** – ảnh hưởng bảo mật / nhất quán chính sách. |
| **Redundancy** | Một rule **bị phủ bởi** (covered/subsumed by) rule khác cùng effect: mọi request match rule này cũng match rule kia → rule bị phủ có thể bỏ mà không đổi ngữ nghĩa. | **Thứ yếu** – tối ưu, dọn dẹp, không gây sai quyết định. |

---

## Conflict (Allow vs Deny)

- **Điều kiện:** Tồn tại ít nhất một request mà cả hai rule đều match, nhưng một rule **Allow** và một rule **Deny**.
- **Ví dụ:** “Admin can manage patient” (Allow) và “ADMIN is not permitted to manage patients” (Deny): cùng subject ADMIN, cùng resource patient_record, actions giao nhau → **AUTH_CONFLICT** (quan trọng, cần báo và xử lý).

---

## Redundancy (cùng effect)

- **Điều kiện (chuẩn paper):**
  1. Cùng effect (cả hai Allow hoặc cả hai Deny).
  2. **Tồn tại request match cả hai** → mọi attribute (subject, object, …) phải **giao khác rỗng** (full overlap).
  3. (Chặt hơn) Một rule **subsumes** rule kia: tập request của rule này là **tập con** của rule kia → rule bị phủ là “redundant”.

- **Ví dụ không phải redundancy:**  
  - Doctor Allow (role=DOCTOR, patient_record, read/create/update/delete) và Admin Allow (role=ADMIN, patient_record, …).  
  - Subject: roles DOCTOR vs ADMIN → **giao rỗng** (không có request vừa DOCTOR vừa ADMIN).  
  - → **Không có request nào match cả hai** → không redundancy theo lý thuyết; báo “redundancy” ở đây là **sai**.

- **Ví dụ đúng redundancy:**  
  - Rule A: Allow, role in [ADMIN, MANAGER], patient_record, [read, write].  
  - Rule B: Allow, role [ADMIN], patient_record, [read].  
  - Mọi request match B cũng match A → B redundant với A.

---

## Điều chỉnh implementation

- **Trước:** Redundancy = same effect + “overlap” theo nghĩa: có chung key attribute (vd. `roles`) hoặc một phần giao (PARTIAL_INTERSECT) → Doctor vs Admin bị báo REDUNDANCY_CONFLICT (không đúng lý thuyết).
- **Sau:** Chỉ báo redundancy khi **full overlap** = tồn tại request match cả hai rule:
  - Trong code: `checkAttributeConflict` trả về **EXCEPTION** (subject và object đều INTERSECT), không dùng **ASSOCIATION** (chỉ PARTIAL_INTERSECT, ví dụ cùng key nhưng value disjoint như DOCTOR vs ADMIN).
- Kết quả: Cặp Doctor Allow vs Admin Allow **không còn** bị báo REDUNDANCY_CONFLICT; AUTH_CONFLICT (Allow vs Deny, ví dụ Admin Allow vs Admin Deny) vẫn báo như cũ.

---

## Tóm tắt

- **AUTH_CONFLICT:** Overlap + effect khác nhau → luôn quan trọng, giữ nguyên.
- **REDUNDANCY_CONFLICT:** Chỉ báo khi same effect **và** full overlap (EXCEPTION), không báo khi chỉ PARTIAL (ASSOCIATION) như Doctor vs Admin. Có thể bổ sung bước “subsumption” (một rule là tập con của rule kia) để chỉ báo redundancy “thật” nếu cần sát paper hơn.
