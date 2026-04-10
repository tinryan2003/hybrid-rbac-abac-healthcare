package org.vgu.userservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Billing Clerk entity - ROLE_BILLING_CLERK
 * ABAC Attributes: hospitalId
 */
@Entity
@Table(name = "billing_clerks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class BillingClerk extends BaseStaffProfile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "billing_clerk_id")
    private Long billingClerkId;

    // ABAC Attributes: hospitalId inherited from BaseStaffProfile
}
