package org.vgu.userservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Lab Technician entity - ROLE_LAB_TECH
 * ABAC Attributes: hospitalId, departmentId, specialization
 */
@Entity
@Table(name = "lab_technicians")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class LabTechnician extends BaseStaffProfile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lab_tech_id")
    private Long labTechId;

    // ABAC Attributes
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    @JsonIgnore // Prevent serialization of lazy-loaded Department entity
    private Department department;

    @Column(name = "specialization", length = 100)
    private String specialization; // Lab specialization
}
