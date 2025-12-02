-- ============================================
-- V2__seed_data.sql
-- Mental Health Clinic - Seed Data for Development
-- ============================================
-- This migration creates initial test data for development.
-- In production, this migration should be skipped or modified.
-- ============================================

-- Note: Passwords are BCrypt encoded. Default password is '123' for all users.
-- BCrypt hash for '123': $2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqBuBjZa/L5E6Nxj1NQgPHpR1CGKO

-- === Insert Admin User ===
INSERT INTO "users" (username, password, role)
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqBuBjZa/L5E6Nxj1NQgPHpR1CGKO', 'ROLE_ADMIN')
ON CONFLICT (username) DO NOTHING;

-- === Insert Psychologist Users ===
INSERT INTO "users" (username, password, role)
VALUES ('doc', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqBuBjZa/L5E6Nxj1NQgPHpR1CGKO', 'ROLE_PSYCHOLOGIST')
ON CONFLICT (username) DO NOTHING;

INSERT INTO "users" (username, password, role)
VALUES ('dr.martinez', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqBuBjZa/L5E6Nxj1NQgPHpR1CGKO', 'ROLE_PSYCHOLOGIST')
ON CONFLICT (username) DO NOTHING;

-- === Insert Patient Users ===
INSERT INTO "users" (username, password, role)
VALUES ('pepe@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqBuBjZa/L5E6Nxj1NQgPHpR1CGKO', 'ROLE_PATIENT')
ON CONFLICT (username) DO NOTHING;

INSERT INTO "users" (username, password, role)
VALUES ('grillo@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqBuBjZa/L5E6Nxj1NQgPHpR1CGKO', 'ROLE_PATIENT')
ON CONFLICT (username) DO NOTHING;

INSERT INTO "users" (username, password, role)
VALUES ('maria@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqBuBjZa/L5E6Nxj1NQgPHpR1CGKO', 'ROLE_PATIENT')
ON CONFLICT (username) DO NOTHING;

-- === Insert Psychologists ===
INSERT INTO "psychologists" (id, name, specialty, email, phone, dni)
SELECT id, 'Dr. Strange', 'Misticismo y Trauma', 'strange@clinic.com', '+51999111222', '99887766'
FROM "users" WHERE username = 'doc'
ON CONFLICT (id) DO NOTHING;

INSERT INTO "psychologists" (id, name, specialty, email, phone, dni)
SELECT id, 'Dra. Ana Martínez', 'Psicología Infantil', 'martinez@clinic.com', '+51999111333', '88776655'
FROM "users" WHERE username = 'dr.martinez'
ON CONFLICT (id) DO NOTHING;

-- === Insert Patients ===
INSERT INTO "patients" (id, name, email, phone, dni)
SELECT id, 'Pepe Grillo', 'pepe@test.com', '+51999333444', '12345678'
FROM "users" WHERE username = 'pepe@test.com'
ON CONFLICT (id) DO NOTHING;

INSERT INTO "patients" (id, name, email, phone, dni)
SELECT id, 'Grillo Pepito', 'grillo@test.com', '+51999555666', '87654321'
FROM "users" WHERE username = 'grillo@test.com'
ON CONFLICT (id) DO NOTHING;

INSERT INTO "patients" (id, name, email, phone, dni)
SELECT id, 'María García López', 'maria@test.com', '+51999777888', '11223344'
FROM "users" WHERE username = 'maria@test.com'
ON CONFLICT (id) DO NOTHING;

-- === Insert Rooms ===
INSERT INTO "rooms" (name, description, capacity)
VALUES
    ('Sala Suprema', 'Consultorio principal con vista al jardín', 3),
    ('Sala Zen', 'Espacio de meditación y relajación', 2),
    ('Sala Familiar', 'Consultorio amplio para terapia familiar', 6),
    ('Sala de Niños', 'Equipada con juegos y materiales didácticos', 4)
ON CONFLICT DO NOTHING;

-- === Insert Sample Appointments (for next week) ===
-- Note: These appointments are for testing purposes
DO $$
DECLARE
    v_patient_id BIGINT;
    v_psychologist_id BIGINT;
    v_room_id BIGINT;
    v_next_monday TIMESTAMP;
BEGIN
    -- Get IDs
    SELECT id INTO v_patient_id FROM "patients" WHERE dni = '12345678';
    SELECT id INTO v_psychologist_id FROM "psychologists" WHERE dni = '99887766';
    SELECT id INTO v_room_id FROM "rooms" WHERE name = 'Sala Suprema';

    -- Calculate next Monday at 10:00
    v_next_monday := date_trunc('week', CURRENT_DATE + INTERVAL '7 days') + INTERVAL '10 hours';

    -- Insert sample appointment if all IDs exist
    IF v_patient_id IS NOT NULL AND v_psychologist_id IS NOT NULL AND v_room_id IS NOT NULL THEN
        INSERT INTO "appointments" (start_time, end_time, patient_id, psychologist_id, room_id, status, notes)
        VALUES (
            v_next_monday,
            v_next_monday + INTERVAL '1 hour',
            v_patient_id,
            v_psychologist_id,
            v_room_id,
            'SCHEDULED',
            'Sesión de seguimiento - Cita creada automáticamente para pruebas'
        )
        ON CONFLICT DO NOTHING;

        -- Second appointment on Tuesday
        INSERT INTO "appointments" (start_time, end_time, patient_id, psychologist_id, room_id, status, notes)
        VALUES (
            v_next_monday + INTERVAL '1 day' + INTERVAL '4 hours', -- Tuesday 14:00
            v_next_monday + INTERVAL '1 day' + INTERVAL '5 hours', -- Tuesday 15:00
            v_patient_id,
            v_psychologist_id,
            v_room_id,
            'SCHEDULED',
            'Sesión de terapia cognitiva'
        )
        ON CONFLICT DO NOTHING;
    END IF;
END $$;

-- === Log successful seed ===
DO $$
BEGIN
    RAISE NOTICE 'V2__seed_data.sql: Seed data inserted successfully';
    RAISE NOTICE 'Test credentials:';
    RAISE NOTICE '  - Admin: username=admin, password=123';
    RAISE NOTICE '  - Doctor: username=doc, password=123';
    RAISE NOTICE '  - Patient: username=pepe@test.com, password=123';
END $$;
