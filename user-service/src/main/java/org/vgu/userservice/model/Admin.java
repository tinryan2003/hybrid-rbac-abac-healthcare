package org.vgu.userservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Admin entity - ROLE_ADMIN
 * ABAC Attributes: hospitalId, adminLevel
 * Note: hospitalId can be NULL for system admin
 */
@Entity
@Table(name = "admins")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Admin extends BaseStaffProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "admin_id")
    private Long adminId;

    // ABAC Attributes
    // hospitalId inherited from BaseStaffProfile (can be NULL = system admin)

    @Convert(converter = AdminLevelConverter.class)
    @Column(name = "admin_level")
    private AdminLevel adminLevel = AdminLevel.HOSPITAL;

    /**
     * Admin seniority level used for ABAC and mapping to position_level.
     * SYSTEM > HOSPITAL > DEPARTMENT.
     */
    public enum AdminLevel {
        SYSTEM(3),
        HOSPITAL(2),
        DEPARTMENT(1);

        private final int code;

        AdminLevel(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static AdminLevel fromCode(Integer code) {
            if (code == null)
                return null;
            for (AdminLevel level : values()) {
                if (level.code == code)
                    return level;
            }
            throw new IllegalArgumentException("Unknown admin level code: " + code);
        }
    }
}
