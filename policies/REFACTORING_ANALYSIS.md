# Policy Refactoring Analysis: Hardcode → Dynamic

## 🔍 **Phân tích các phần còn Hardcode**

### **1. ✅ ĐÃ FIX: Working Hours trong `hospital_dynamic.rego`**

**Trước (BUG):**
```rego
constraint.type == "WORKING_HOURS"
current_hour >= 2    # ❌ Hardcode
current_hour < 17    # ❌ Hardcode
```

**Sau (FIXED):**
```rego
constraint.type == "WORKING_HOURS"
current_hour >= constraint.start_hour  # ✅ Đọc từ data
current_hour < constraint.end_hour     # ✅ Đọc từ data
```

**Lý do:** `OpaDataTransformer` đã push `start_hour: 8, end_hour: 17` vào constraint, nhưng Rego không đọc!

---

### **2. ⚠️ CẦN XEM XÉT: Helper Functions trong `hospital.rego`**

#### **A. `is_work_hours` (dòng 34-37)**

```rego
# Check if current time is within working hours (8:00 - 16:00)
is_work_hours if {
    input.context.hour >= 8    # ❌ Hardcode
    input.context.hour < 16    # ❌ Hardcode
}
```

**Vấn đề:**
- Được dùng trong nhiều rules cũ (dòng 122, 139, 155, 197, 214, 497)
- Nếu muốn thay đổi working hours → phải sửa code

**Giải pháp:**
- ✅ **Option 1**: Giữ nguyên cho backward compatibility (nếu vẫn dùng `hospital.rego`)
- ✅ **Option 2**: Migrate tất cả sang `hospital_dynamic.rego` → xóa `hospital.rego`
- ✅ **Option 3**: Đưa vào config data:

```rego
# Read from data.config (if exists)
is_work_hours if {
    default_work_start := 8
    default_work_end := 16
    
    work_start := data.config.working_hours.start_hour if data.config.working_hours.start_hour
    work_start := default_work_start
    
    work_end := data.config.working_hours.end_hour if data.config.working_hours.end_hour
    work_end := default_work_end
    
    input.context.hour >= work_start
    input.context.hour < work_end
}
```

---

#### **B. `nurse_or_work_hours` (dòng 61-66)**

```rego
# True if user is not NURSE, or if within work hours
nurse_or_work_hours if {
    input.user.role != "NURSE"
}
nurse_or_work_hours if {
    is_work_hours  # ❌ Depends on hardcoded is_work_hours
}
```

**Vấn đề:**
- Logic phức tạp: "NURSE chỉ làm trong giờ hành chính, các role khác không bị giới hạn"
- Được dùng trong nhiều rules (dòng 122, 139, 155)

**Giải pháp:**
- ✅ **Option 1**: Tạo policy dynamic riêng cho NURSE với time constraint
- ✅ **Option 2**: Giữ helper này nhưng refactor `is_work_hours` để đọc từ config

---

### **3. 📋 SO SÁNH: `hospital.rego` vs `hospital_dynamic.rego`**

| Aspect | `hospital.rego` | `hospital_dynamic.rego` |
|--------|----------------|------------------------|
| **Policies** | Hardcoded rules | Đọc từ `data.dynamic_policies` |
| **Working Hours** | Hardcode `8-16` | ✅ Đọc từ constraint (đã fix) |
| **Department Check** | Helper `same_department` | ✅ Đọc từ constraint |
| **Hospital Check** | Helper `same_hospital` | ✅ Đọc từ constraint |
| **Flexibility** | ❌ Phải sửa code | ✅ Thay đổi qua API |
| **Performance** | ✅ Nhanh (no data lookup) | ⚠️ Chậm hơn một chút (iterate policies) |
| **Maintainability** | ❌ Khó maintain | ✅ Dễ maintain |

---

## 🎯 **Đề xuất Migration Strategy**

### **Phase 1: Hybrid Approach (Hiện tại)**

**Giữ cả 2 files:**
- `hospital.rego` → Legacy policies (backward compatibility)
- `hospital_dynamic.rego` → New dynamic policies

**OPA sẽ evaluate cả 2:**
```rego
# Trong main entry point
allow := result if {
    # Try dynamic first
    result := dynamic_allow
}

allow := result if {
    # Fallback to static
    result := static_allow
}
```

**Ưu điểm:**
- ✅ Không break existing policies
- ✅ Có thể migrate từng policy một
- ✅ Test dynamic system trước khi chuyển hết

**Nhược điểm:**
- ⚠️ Có thể conflict giữa 2 systems
- ⚠️ Khó debug (2 nguồn truth)

---

### **Phase 2: Full Migration (Recommended)**

**Bước 1: Convert tất cả policies từ `hospital.rego` → Database**

Ví dụ:
```java
// Policy từ hospital.rego dòng 186-200
Policy nurseReadVitals = Policy.builder()
    .policyId("nurse_read_vitals_legacy")
    .policyName("NURSE Reading Patient Vitals")
    .effect("Allow")
    .targetRoles(List.of("NURSE"))
    .actions(List.of("read"))
    .resourceObject("patient_vitals")
    .constraints(PolicyConstraintDto.builder()
        .workingHoursOnly(true)  // 8-17
        .sameDepartment(true)
        .sameHospital(true)
        .build())
    .build();
```

**Bước 2: Sync tất cả vào OPA**

```bash
# Trigger bulk sync
POST /api/policies/opa-sync/resync
```

**Bước 3: Disable `hospital.rego`**

```rego
# Comment out hoặc rename
# package hospital.authz.static  # Old namespace
```

**Bước 4: Chỉ dùng `hospital_dynamic.rego`**

---

### **Phase 3: Enhanced Dynamic System**

**Thêm config data cho system-wide settings:**

```json
// Push to OPA: PUT /v1/data/config
{
  "working_hours": {
    "default_start": 8,
    "default_end": 17,
    "roles": {
      "NURSE": {"start": 8, "end": 16},
      "DOCTOR": {"start": 7, "end": 19}
    }
  },
  "emergency_override": {
    "enabled": true,
    "allowed_roles": ["DOCTOR", "PRIMARY_DOCTOR", "NURSE"]
  }
}
```

**Rego đọc từ config:**
```rego
# Helper đọc từ config (fallback to default)
get_working_hours(role) := hours if {
    hours := data.config.working_hours.roles[role]
} else := {
    "start_hour": data.config.working_hours.default_start,
    "end_hour": data.config.working_hours.default_end
}
```

---

## 🔧 **Các Hardcode Còn Lại Cần Xem Xét**

### **1. Role Hierarchy (Có thể giữ)**

```rego
# PRIMARY_DOCTOR inherits all DOCTOR permissions
effective_roles contains "DOCTOR" if {
    input.user.role == "PRIMARY_DOCTOR"
}
```

**Đánh giá:** ✅ **NÊN GIỮ** - Đây là business logic cốt lõi, không nên dynamic

---

### **2. Fallback Policies (Có thể giữ)**

```rego
# SYSTEM_ADMIN always has full access (override)
allow := {...} if {
    input.user.role == "SYSTEM_ADMIN"
}
```

**Đánh giá:** ✅ **NÊN GIỮ** - Security-critical, không nên disable

---

### **3. Emergency Override (Có thể giữ)**

```rego
allow := {...} if {
    input.context.emergency == true
    input.user.role in ["DOCTOR", "PRIMARY_DOCTOR", "NURSE"]
}
```

**Đánh giá:** ✅ **NÊN GIỮ** - Life-critical, cần hardcode để đảm bảo hoạt động

---

## 📊 **Recommendation Matrix**

| Component | Current State | Recommendation | Priority |
|-----------|--------------|-----------------|----------|
| **Working Hours** | ❌ Hardcode `8-16` | ✅ **FIXED** - Đọc từ constraint | 🔴 HIGH |
| **Time Range** | ✅ Dynamic | ✅ **OK** - Đã đúng | ✅ |
| **Department** | ✅ Dynamic | ✅ **OK** - Đã đúng | ✅ |
| **Hospital** | ✅ Dynamic | ✅ **OK** - Đã đúng | ✅ |
| **Position Level** | ✅ Dynamic | ✅ **OK** - Đã đúng | ✅ |
| **Role Hierarchy** | ⚠️ Hardcode | ✅ **KEEP** - Business logic | 🟡 MEDIUM |
| **Fallback Policies** | ⚠️ Hardcode | ✅ **KEEP** - Security-critical | 🟡 MEDIUM |
| **Helper Functions** | ⚠️ Hardcode | 🔄 **MIGRATE** - Convert to policies | 🟢 LOW |

---

## ✅ **Action Items**

### **Immediate (Done ✅)**
- [x] Fix `WORKING_HOURS` constraint trong `hospital_dynamic.rego`
- [x] Verify constraint đọc đúng từ `constraint.start_hour` và `constraint.end_hour`

### **Short-term (Next Sprint)**
- [ ] Test dynamic policies với working hours constraint
- [ ] Document migration path từ `hospital.rego` → dynamic
- [ ] Create migration script để convert legacy policies

### **Long-term (Future)**
- [ ] Migrate tất cả policies từ `hospital.rego` → Database
- [ ] Deprecate `hospital.rego` (hoặc rename thành `hospital_legacy.rego`)
- [ ] Add config data support cho system-wide settings
- [ ] Performance testing: dynamic vs static

---

## 🧪 **Testing Checklist**

Sau khi fix, test các scenarios:

### **Test 1: Working Hours Constraint**
```bash
# Policy với working_hours_only: true
# Expected: start_hour=8, end_hour=17 từ OpaDataTransformer

# Test case 1: Request lúc 10:00 → Should ALLOW
# Test case 2: Request lúc 19:00 → Should DENY
```

### **Test 2: Custom Time Range**
```bash
# Policy với time_range: "09:00-18:00"
# Expected: start_hour=9, end_hour=18

# Test case 1: Request lúc 10:00 → Should ALLOW
# Test case 2: Request lúc 19:00 → Should DENY
```

### **Test 3: No Time Constraint**
```bash
# Policy không có time constraint
# Expected: Always allow (không check time)

# Test case: Request bất kỳ giờ nào → Should ALLOW
```

---

## 📚 **References**

- **Paper**: "Detecting Conflicts in ABAC Policies with Rule Reduction and Binary-Search Techniques"
- **Pattern**: Role-centric RBAC-A (Kuhn et al. 2010)
- **OPA Docs**: https://www.openpolicyagent.org/docs/latest/policy-language/

---

## 🎯 **Conclusion**

**Status:** ✅ **FIXED** - Working hours constraint giờ đã đọc từ data thay vì hardcode

**Next Steps:**
1. Test fix với real policies
2. Plan migration từ `hospital.rego` → dynamic
3. Consider config data cho system-wide settings

**Key Principle:** 
> "Make it work, make it right, make it fast" - Kent Beck
> 
> ✅ **Work**: Dynamic policies functional
> 🔄 **Right**: Migrate hardcoded helpers → dynamic
> ⚡ **Fast**: Optimize performance nếu cần
