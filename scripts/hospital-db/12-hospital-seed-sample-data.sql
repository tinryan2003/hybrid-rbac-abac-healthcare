-- =====================================================
-- Sample Data for Testing
-- Dữ liệu mẫu để test các chức năng
-- =====================================================

-- =====================================================
-- 1. Sample Prescriptions (Pharmacy)
-- =====================================================
USE hospital_pharmacy;

-- Prescription 1: PENDING status
INSERT INTO prescriptions (doctor_id, patient_id, prescription_date, diagnosis, notes, status, hospital_id) VALUES
(1, 1, CURDATE(), 'Common cold', 'Patient has symptoms of common cold', 'PENDING', 'HOSPITAL_A');

SET @prescription_id_1 = LAST_INSERT_ID();

-- Prescription items for prescription 1
INSERT INTO prescription_items (prescription_id, medicine_id, dosage, frequency, duration_days, start_date, end_date, quantity, instructions, before_after_meal, unit_price, total_price) VALUES
(@prescription_id_1, 1, '500mg', '3 times daily', 5, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 5 DAY), 15, 'Take with water', 'AFTER', 2000.00, 30000.00),
(@prescription_id_1, 3, '400mg', '2 times daily', 5, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 5 DAY), 10, 'Take with food', 'AFTER', 3000.00, 30000.00);

-- Prescription 2: APPROVED status
INSERT INTO prescriptions (doctor_id, patient_id, prescription_date, diagnosis, notes, status, hospital_id) VALUES
(1, 1, DATE_SUB(CURDATE(), INTERVAL 2 DAY), 'Bacterial infection', 'Patient needs antibiotic treatment', 'APPROVED', 'HOSPITAL_A');

SET @prescription_id_2 = LAST_INSERT_ID();

INSERT INTO prescription_items (prescription_id, medicine_id, dosage, frequency, duration_days, start_date, end_date, quantity, instructions, before_after_meal, unit_price, total_price) VALUES
(@prescription_id_2, 2, '500mg', '3 times daily', 7, DATE_SUB(CURDATE(), INTERVAL 2 DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 2 DAY), INTERVAL 7 DAY), 21, 'Complete full course', 'BEFORE', 5000.00, 105000.00);

-- =====================================================
-- 2. Sample Lab Orders (Lab Service)
-- =====================================================
USE hospital_lab;

-- Lab Order 1: PENDING
INSERT INTO lab_orders (patient_id, doctor_id, order_date, order_type, clinical_diagnosis, urgency, status, hospital_id, department_id) VALUES
(1, 1, CURDATE(), 'LAB', 'Routine checkup', 'ROUTINE', 'PENDING', 'HOSPITAL_A', 1);

SET @lab_order_id_1 = LAST_INSERT_ID();

-- Lab order items
INSERT INTO lab_order_items (lab_order_id, test_id, status, priority, price) VALUES
(@lab_order_id_1, 1, 'PENDING', 1, 150000.00),
(@lab_order_id_1, 2, 'PENDING', 2, 50000.00);

-- Lab Order 2: COMPLETED
INSERT INTO lab_orders (patient_id, doctor_id, order_date, order_type, clinical_diagnosis, urgency, status, hospital_id, department_id, completed_at) VALUES
(1, 1, DATE_SUB(CURDATE(), INTERVAL 3 DAY), 'LAB', 'Diabetes monitoring', 'ROUTINE', 'COMPLETED', 'HOSPITAL_A', 1, DATE_SUB(CURDATE(), INTERVAL 1 DAY));

SET @lab_order_id_2 = LAST_INSERT_ID();

INSERT INTO lab_order_items (lab_order_id, test_id, status, priority, price) VALUES
(@lab_order_id_2, 3, 'COMPLETED', 1, 200000.00);

-- Lab results for completed order
INSERT INTO lab_results (lab_order_id, order_item_id, test_id, result_value, result_unit, reference_range, result_status, interpretation, result_date) VALUES
(@lab_order_id_2, LAST_INSERT_ID(), 3, '6.5', '%', '4.0-6.0', 'ABNORMAL', 'Slightly elevated, monitor closely', DATE_SUB(CURDATE(), INTERVAL 1 DAY));

-- =====================================================
-- 3. Sample Invoices and Payments (Billing)
-- =====================================================
USE hospital_billing;

-- Invoice 1: PENDING
INSERT INTO invoices (invoice_number, patient_id, invoice_date, due_date, subtotal, discount_amount, tax_amount, total_amount, status, outstanding_amount, hospital_id) VALUES
('INV-2025-001', 1, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 30 DAY), 200000.00, 0.00, 0.00, 200000.00, 'PENDING', 200000.00, 'HOSPITAL_A');

SET @invoice_id_1 = LAST_INSERT_ID();

INSERT INTO invoice_items (invoice_id, item_type, item_code, item_description, quantity, unit_price, total_price) VALUES
(@invoice_id_1, 'CONSULTATION', 'CONS-GEN', 'General Consultation', 1, 200000.00, 200000.00);

-- Invoice 2: PAID
INSERT INTO invoices (invoice_number, patient_id, invoice_date, due_date, subtotal, discount_amount, tax_amount, total_amount, status, paid_amount, outstanding_amount, paid_date, hospital_id) VALUES
('INV-2025-002', 1, DATE_SUB(CURDATE(), INTERVAL 5 DAY), DATE_SUB(CURDATE(), INTERVAL 5 DAY), 150000.00, 0.00, 0.00, 150000.00, 'PAID', 150000.00, 0.00, DATE_SUB(CURDATE(), INTERVAL 4 DAY), 'HOSPITAL_A');

SET @invoice_id_2 = LAST_INSERT_ID();

INSERT INTO invoice_items (invoice_id, item_type, item_code, item_description, quantity, unit_price, total_price) VALUES
(@invoice_id_2, 'LAB', 'CBC', 'Complete Blood Count', 1, 150000.00, 150000.00);

-- Payment for invoice 2
INSERT INTO payments (invoice_id, payment_date, payment_method, amount, status, transaction_id, received_by_keycloak_id) VALUES
(@invoice_id_2, DATE_SUB(CURDATE(), INTERVAL 4 DAY), 'CASH', 150000.00, 'COMPLETED', 'TXN-001', 'billing_clerk1-keycloak-uuid');

-- Invoice 3: PARTIALLY_PAID
INSERT INTO invoices (invoice_number, patient_id, invoice_date, due_date, subtotal, discount_amount, tax_amount, total_amount, status, paid_amount, outstanding_amount, hospital_id) VALUES
('INV-2025-003', 1, DATE_SUB(CURDATE(), INTERVAL 2 DAY), DATE_ADD(CURDATE(), INTERVAL 28 DAY), 500000.00, 0.00, 0.00, 500000.00, 'PARTIALLY_PAID', 200000.00, 300000.00, 'HOSPITAL_A');

SET @invoice_id_3 = LAST_INSERT_ID();

INSERT INTO invoice_items (invoice_id, item_type, item_code, item_description, quantity, unit_price, total_price) VALUES
(@invoice_id_3, 'CONSULTATION', 'CONS-SPEC', 'Specialist Consultation', 1, 500000.00, 500000.00);

INSERT INTO payments (invoice_id, payment_date, payment_method, amount, status, transaction_id, received_by_keycloak_id) VALUES
(@invoice_id_3, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 'BANK_TRANSFER', 200000.00, 'COMPLETED', 'TXN-002', 'billing_clerk1-keycloak-uuid');

-- =====================================================
-- 4. Sample Appointments (Appointment Service)
-- =====================================================
USE hospital_appointments;

-- Appointment 1: CONFIRMED
INSERT INTO appointments (doctor_id, patient_id, doctor_specialization, appointment_date, appointment_time, duration_minutes, reason, status, hospital_id, department_id, created_by_keycloak_id, approve_date) VALUES
(1, 1, 'Cardiology', CURDATE(), '09:00:00', 30, 'Regular checkup', 'CONFIRMED', 'HOSPITAL_A', 1, 'ccb3c2cf-f647-44a6-b416-fc1faaf0a307', NOW());

-- Appointment 2: PENDING
INSERT INTO appointments (doctor_id, patient_id, doctor_specialization, appointment_date, appointment_time, duration_minutes, reason, status, hospital_id, department_id, created_by_keycloak_id) VALUES
(1, 1, 'Cardiology', DATE_ADD(CURDATE(), INTERVAL 7 DAY), '10:00:00', 30, 'Follow-up appointment', 'PENDING', 'HOSPITAL_A', 1, 'ccb3c2cf-f647-44a6-b416-fc1faaf0a307');

-- Appointment 3: COMPLETED
INSERT INTO appointments (doctor_id, patient_id, doctor_specialization, appointment_date, appointment_time, duration_minutes, reason, status, hospital_id, department_id, created_by_keycloak_id, approve_date, completed_date) VALUES
(1, 1, 'Cardiology', DATE_SUB(CURDATE(), INTERVAL 3 DAY), '14:00:00', 30, 'Previous consultation', 'COMPLETED', 'HOSPITAL_A', 1, 'ccb3c2cf-f647-44a6-b416-fc1faaf0a307', DATE_SUB(CURDATE(), INTERVAL 3 DAY), DATE_SUB(CURDATE(), INTERVAL 3 DAY));

SELECT 'Sample data for testing added successfully!' AS status;
