package org.vgu.userservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Pharmacist entity - ROLE_PHARMACIST
 * ABAC Attributes: hospitalId, licenseNumber
 */
@Entity
@Table(name = "pharmacists")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Pharmacist extends BaseStaffProfile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pharmacist_id")
    private Long pharmacistId;

    // ABAC Attributes
    // hospitalId inherited from BaseStaffProfile
    
    @Column(name = "license_number", length = 50)
    private String licenseNumber;
}
