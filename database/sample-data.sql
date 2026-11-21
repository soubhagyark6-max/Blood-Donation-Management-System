-- ============================================================================
-- OPTIONAL sample data (DEMO). You normally do NOT need this file:
-- the application loads richer demo data automatically on first start
-- (DEMO_DATA_ENABLED=true) and creates real BCrypt hashes for the demo accounts.
--
-- If you prefer to load data manually, only do so on an EMPTY database and
-- start the application with DEMO_DATA_ENABLED=false. User accounts are NOT
-- included here on purpose (a BCrypt hash cannot be written by hand) - create
-- an admin by starting once with demo data, or register accounts in the UI.
-- All rows below are flagged demo = 1.
-- ============================================================================
USE lifelink;

INSERT INTO donation_camps (name, camp_date, start_time, end_time, venue, city, contact_name, contact_phone, capacity, description, active, demo, created_at)
VALUES
 ('Community Blood Donation Drive', DATE_ADD(CURDATE(), INTERVAL 6 DAY),  '09:00:00', '16:00:00', 'Jayanagar Community Hall', 'Bengaluru', 'Ravi Kumar', '9000000101', 60, 'DEMO camp', 1, 1, NOW(6)),
 ('College Campus Blood Camp',      DATE_ADD(CURDATE(), INTERVAL 14 DAY), '09:00:00', '16:00:00', 'UBDT College Auditorium', 'Mysuru',    'Dr. Meena',  '9000000102', 80, 'DEMO camp', 1, 1, NOW(6));

INSERT INTO blood_inventory (batch_code, blood_group, component, units, units_received, collection_date, expiry_date, demo, created_at, version)
VALUES
 ('LL-DEMO-0001', 'O_POS', 'WHOLE_BLOOD', 12, 12, DATE_SUB(CURDATE(), INTERVAL 5 DAY), DATE_ADD(CURDATE(), INTERVAL 30 DAY), 1, NOW(6), 0),
 ('LL-DEMO-0002', 'A_POS', 'WHOLE_BLOOD',  9,  9, DATE_SUB(CURDATE(), INTERVAL 4 DAY), DATE_ADD(CURDATE(), INTERVAL 31 DAY), 1, NOW(6), 0),
 ('LL-DEMO-0003', 'B_POS', 'RED_CELLS',    6,  6, DATE_SUB(CURDATE(), INTERVAL 3 DAY), DATE_ADD(CURDATE(), INTERVAL 39 DAY), 1, NOW(6), 0),
 ('LL-DEMO-0004', 'AB_NEG','PLATELETS',    2,  2, DATE_SUB(CURDATE(), INTERVAL 1 DAY), DATE_ADD(CURDATE(), INTERVAL 4 DAY),  1, NOW(6), 0);

INSERT INTO inventory_transactions (type, blood_group, component, units, batch_code, note, performed_by, demo, created_at)
VALUES
 ('RECEIVED', 'O_POS',  'WHOLE_BLOOD', 12, 'LL-DEMO-0001', 'DEMO opening stock', 'sample-data.sql', 1, NOW(6)),
 ('RECEIVED', 'A_POS',  'WHOLE_BLOOD',  9, 'LL-DEMO-0002', 'DEMO opening stock', 'sample-data.sql', 1, NOW(6)),
 ('RECEIVED', 'B_POS',  'RED_CELLS',    6, 'LL-DEMO-0003', 'DEMO opening stock', 'sample-data.sql', 1, NOW(6)),
 ('RECEIVED', 'AB_NEG', 'PLATELETS',    2, 'LL-DEMO-0004', 'DEMO opening stock', 'sample-data.sql', 1, NOW(6));
