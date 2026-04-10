-- Add real creator columns for ABAC created_by_is_user constraint.
-- Run once on existing databases.

ALTER TABLE hospital_patients.patients
    ADD COLUMN created_by_keycloak_id VARCHAR(255) NULL COMMENT 'Keycloak subject of creator';

ALTER TABLE hospital_pharmacy.prescriptions
    ADD COLUMN created_by_keycloak_id VARCHAR(255) NULL COMMENT 'Keycloak subject who created prescription';

ALTER TABLE hospital_lab.lab_orders
    ADD COLUMN created_by_keycloak_id VARCHAR(255) NULL COMMENT 'Keycloak subject who created order';

