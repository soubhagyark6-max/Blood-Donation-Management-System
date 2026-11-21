-- ============================================================================
-- LifeLink - MySQL 8 schema (optional: Hibernate can also create these tables
-- automatically when DDL_AUTO=update, which is the default).
-- Run:  mysql -u root -p < database/schema.sql
-- ============================================================================
CREATE DATABASE IF NOT EXISTS lifelink CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE lifelink;

CREATE TABLE IF NOT EXISTS users (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  full_name     VARCHAR(100) NOT NULL,
  email         VARCHAR(150) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,                     -- BCrypt hash only
  role          VARCHAR(20)  NOT NULL,                     -- ADMIN, STAFF, DONOR, RECIPIENT
  enabled       BIT NOT NULL DEFAULT 1,
  demo          BIT NOT NULL DEFAULT 0,
  created_at    DATETIME(6) NOT NULL,
  CONSTRAINT uk_users_email UNIQUE (email),
  CONSTRAINT chk_users_role CHECK (role IN ('ADMIN','STAFF','DONOR','RECIPIENT')),
  INDEX idx_users_role (role)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS donors (
  id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id             BIGINT NULL,
  full_name           VARCHAR(100) NOT NULL,
  email               VARCHAR(150) NOT NULL,
  phone               VARCHAR(15)  NOT NULL,
  date_of_birth       DATE NOT NULL,
  gender              VARCHAR(10) NOT NULL,
  blood_group         VARCHAR(10) NOT NULL,
  city                VARCHAR(80) NOT NULL,
  address             VARCHAR(300) NULL,
  last_donation_date  DATE NULL,
  available           BIT NOT NULL DEFAULT 1,
  contact_preference  VARCHAR(20) NOT NULL DEFAULT 'PHONE_AND_EMAIL',
  verified            BIT NOT NULL DEFAULT 0,
  demo                BIT NOT NULL DEFAULT 0,
  created_at          DATETIME(6) NOT NULL,
  CONSTRAINT uk_donor_email UNIQUE (email),
  CONSTRAINT uk_donor_phone UNIQUE (phone),
  CONSTRAINT uk_donor_user UNIQUE (user_id),
  CONSTRAINT fk_donor_user FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_donor_group (blood_group),
  INDEX idx_donor_city (city)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS donations (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  donor_id       BIGINT NOT NULL,
  donation_date  DATE NOT NULL,
  component      VARCHAR(20) NOT NULL,
  units          INT NOT NULL DEFAULT 1,
  location       VARCHAR(150) NULL,
  notes          VARCHAR(300) NULL,
  staff_verified BIT NOT NULL DEFAULT 0,
  recorded_by    BIGINT NULL,
  demo           BIT NOT NULL DEFAULT 0,
  created_at     DATETIME(6) NOT NULL,
  CONSTRAINT fk_donation_donor FOREIGN KEY (donor_id) REFERENCES donors(id),
  CONSTRAINT fk_donation_user  FOREIGN KEY (recorded_by) REFERENCES users(id),
  CONSTRAINT chk_donation_units CHECK (units BETWEEN 1 AND 4),
  INDEX idx_donation_date (donation_date)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS blood_requests (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  requester_id      BIGINT NOT NULL,
  patient_reference VARCHAR(100) NOT NULL,
  blood_group       VARCHAR(10) NOT NULL,
  component         VARCHAR(20) NOT NULL,
  units             INT NOT NULL,
  hospital          VARCHAR(150) NOT NULL,
  city              VARCHAR(80) NOT NULL,
  contact_phone     VARCHAR(15) NOT NULL,
  required_date     DATE NOT NULL,
  urgency           VARCHAR(15) NOT NULL,
  status            VARCHAR(20) NOT NULL,
  staff_remarks     VARCHAR(500) NULL,
  demo              BIT NOT NULL DEFAULT 0,
  created_at        DATETIME(6) NOT NULL,
  updated_at        DATETIME(6) NOT NULL,
  CONSTRAINT fk_req_user FOREIGN KEY (requester_id) REFERENCES users(id),
  CONSTRAINT chk_req_units CHECK (units BETWEEN 1 AND 20),
  INDEX idx_req_status (status),
  INDEX idx_req_group (blood_group),
  INDEX idx_req_created (created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS request_status_history (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  request_id  BIGINT NOT NULL,
  status      VARCHAR(20) NOT NULL,
  remarks     VARCHAR(500) NULL,
  changed_by  VARCHAR(100) NULL,
  changed_at  DATETIME(6) NOT NULL,
  CONSTRAINT fk_hist_request FOREIGN KEY (request_id) REFERENCES blood_requests(id),
  INDEX idx_hist_request (request_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS blood_inventory (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  batch_code      VARCHAR(30) NOT NULL,
  blood_group     VARCHAR(10) NOT NULL,
  component       VARCHAR(20) NOT NULL,
  units           INT NOT NULL,
  units_received  INT NOT NULL,
  collection_date DATE NOT NULL,
  expiry_date     DATE NOT NULL,
  demo            BIT NOT NULL DEFAULT 0,
  created_at      DATETIME(6) NOT NULL,
  version         BIGINT NULL,
  CONSTRAINT chk_inv_units_non_negative CHECK (units >= 0),          -- stock can never go negative
  CONSTRAINT chk_inv_dates CHECK (expiry_date > collection_date),
  INDEX idx_inv_group_comp (blood_group, component),
  INDEX idx_inv_expiry (expiry_date)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS inventory_transactions (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  type         VARCHAR(15) NOT NULL,                  -- RECEIVED, ISSUED, DISCARDED
  blood_group  VARCHAR(10) NOT NULL,
  component    VARCHAR(20) NOT NULL,
  units        INT NOT NULL,
  batch_code   VARCHAR(30) NULL,
  request_id   BIGINT NULL,
  note         VARCHAR(300) NULL,
  performed_by VARCHAR(100) NULL,
  demo         BIT NOT NULL DEFAULT 0,
  created_at   DATETIME(6) NOT NULL,
  CONSTRAINT chk_tx_units CHECK (units > 0),
  INDEX idx_tx_created (created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS donation_camps (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  name          VARCHAR(150) NOT NULL,
  camp_date     DATE NOT NULL,
  start_time    TIME NOT NULL,
  end_time      TIME NOT NULL,
  venue         VARCHAR(200) NOT NULL,
  city          VARCHAR(80) NOT NULL,
  contact_name  VARCHAR(100) NOT NULL,
  contact_phone VARCHAR(15) NOT NULL,
  capacity      INT NOT NULL DEFAULT 50,
  description   VARCHAR(500) NULL,
  active        BIT NOT NULL DEFAULT 1,
  demo          BIT NOT NULL DEFAULT 0,
  created_at    DATETIME(6) NOT NULL,
  CONSTRAINT chk_camp_times CHECK (end_time > start_time),
  INDEX idx_camp_date (camp_date)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS appointments (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  donor_id   BIGINT NOT NULL,
  camp_id    BIGINT NOT NULL,
  status     VARCHAR(15) NOT NULL,
  reference  VARCHAR(20) NOT NULL,
  demo       BIT NOT NULL DEFAULT 0,
  booked_at  DATETIME(6) NOT NULL,
  CONSTRAINT uk_appt_reference UNIQUE (reference),
  CONSTRAINT fk_appt_donor FOREIGN KEY (donor_id) REFERENCES donors(id),
  CONSTRAINT fk_appt_camp  FOREIGN KEY (camp_id)  REFERENCES donation_camps(id),
  INDEX idx_appt_camp (camp_id),
  INDEX idx_appt_donor (donor_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS notifications (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT NOT NULL,
  type        VARCHAR(20) NOT NULL,
  message     VARCHAR(300) NOT NULL,
  link        VARCHAR(200) NULL,
  read_flag   BIT NOT NULL DEFAULT 0,
  dedupe_key  VARCHAR(100) NULL,
  created_at  DATETIME(6) NOT NULL,
  CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_notif_user (user_id, read_flag)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS audit_logs (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  actor       VARCHAR(150) NOT NULL,
  action      VARCHAR(60) NOT NULL,
  details     VARCHAR(500) NULL,
  created_at  DATETIME(6) NOT NULL,
  INDEX idx_audit_created (created_at)
) ENGINE=InnoDB;
