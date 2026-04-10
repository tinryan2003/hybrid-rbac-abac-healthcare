package org.vgu.userservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Doctor entity - ROLE_DOCTOR
 * ABAC Attributes: hospitalId, departmentId, positionLevel, field (specialization)
 *
 * Note: doctors table uses email_address column (not email) for the email field.
 * @AttributeOverride remaps BaseStaffProfile.email → email_address column.
 */
@Entity
@Table(name = "doctors")
@AttributeOverride(name = "email", column = @Column(name = "email_address", length = 100))
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Doctor extends BaseStaffProfile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "doctor_id")
    private Long doctorId;

    @Column(name = "field", length = 100)
    private String field; // Specialization/Chuyên khoa

    // ABAC Attributes
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    @JsonIgnore // Prevent serialization of lazy-loaded Department entity
    private Department department;
}
