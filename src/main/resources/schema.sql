-- Usamos comillas dobles "..." para forzar a H2 a respetar las minúsculas
-- Esto alinea la BD con tu @Table("patients") de Java.

CREATE TABLE IF NOT EXISTS "patients" (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    email VARCHAR(255) UNIQUE
);

CREATE TABLE IF NOT EXISTS "psychologists" (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    specialty VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS "rooms" (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS "users" (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) UNIQUE,
    password VARCHAR(255),
    role VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS "appointments" (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    patient_id BIGINT,
    psychologist_id BIGINT,
    room_id BIGINT,
    FOREIGN KEY (patient_id) REFERENCES "patients"(id),
    FOREIGN KEY (psychologist_id) REFERENCES "psychologists"(id),
    FOREIGN KEY (room_id) REFERENCES "rooms"(id)
);