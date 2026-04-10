package org.vgu.userservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Receptionist entity - ROLE_RECEPTIONIST
 * ABAC Attributes: hospitalId
 */
@Entity
@Table(name = "receptionists")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Receptionist extends BaseStaffProfile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "receptionist_id")
    private Long receptionistId;

    // ABAC Attributes: hospitalId inherited from BaseStaffProfile
}
