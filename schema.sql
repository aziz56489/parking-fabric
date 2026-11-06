-- MySQL 8.0.16+ schema for the parking class diagram.
-- Usage: CREATE DATABASE parking_db; USE parking_db; then run this file.

SET NAMES utf8mb4;

-- User, Admin and TicketCounter share one table; `role` says which one a row is.
CREATE TABLE users (
  id             INT          NOT NULL AUTO_INCREMENT,
  name           VARCHAR(100) NOT NULL,
  email          VARCHAR(255) NOT NULL,
  password       VARCHAR(255) NOT NULL,           -- store a hash (e.g. BCrypt), never the plain password
  role           ENUM('USER', 'ADMIN', 'TICKET_COUNTER', 'EMPLOYEE') NOT NULL DEFAULT 'USER',
  is_handicapped BOOLEAN      NOT NULL DEFAULT FALSE,
  is_ev_owner    BOOLEAN      NOT NULL DEFAULT FALSE,
  is_blocked     BOOLEAN      NOT NULL DEFAULT FALSE,
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Admin 1 --manages-- 0..* ParkingArea
CREATE TABLE parking_area (
  id       INT          NOT NULL AUTO_INCREMENT,
  name     VARCHAR(255) NOT NULL,
  layout   LONGTEXT,                               -- the editor's JSON plan
  capacity INT          NOT NULL DEFAULT 0,
  admin_id INT,
  PRIMARY KEY (id),
  UNIQUE KEY uk_parking_area_name (name),
  CONSTRAINT fk_parking_area_admin FOREIGN KEY (admin_id) REFERENCES users (id) ON DELETE SET NULL,
  CONSTRAINT ck_parking_area_capacity CHECK (capacity >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ParkingArea 1 --contains (composition)-- 1..* ParkingSpot: spots are deleted with their area.
CREATE TABLE parking_spot (
  id              INT         NOT NULL AUTO_INCREMENT,
  parking_area_id INT         NOT NULL,
  number          VARCHAR(20) NOT NULL,
  type            ENUM('REGULAR', 'EV', 'HANDICAPPED', 'PERSONNEL') NOT NULL DEFAULT 'REGULAR',
  status          ENUM('AVAILABLE', 'RESERVED', 'OCCUPIED', 'NOT_AVAILABLE') NOT NULL DEFAULT 'AVAILABLE',
  PRIMARY KEY (id),
  UNIQUE KEY uk_parking_spot_number (parking_area_id, number),
  CONSTRAINT fk_parking_spot_area FOREIGN KEY (parking_area_id) REFERENCES parking_area (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- User 1 --makes-- 0..* Reservation, Reservation 0..* --for-- 1 ParkingSpot
CREATE TABLE reservation (
  id              INT      NOT NULL AUTO_INCREMENT,
  user_id         INT      NOT NULL,
  parking_spot_id INT      NOT NULL,
  start_time      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  end_time        DATETIME,                        -- NULL until the user marks the exit
  active          BOOLEAN  NOT NULL DEFAULT TRUE,
  -- Filled only while active, so the UNIQUE keys allow one active reservation per spot and per user.
  active_spot_id  INT AS (IF(active, parking_spot_id, NULL)) STORED,
  active_user_id  INT AS (IF(active, user_id, NULL)) STORED,
  PRIMARY KEY (id),
  UNIQUE KEY uk_reservation_active_spot (active_spot_id),
  UNIQUE KEY uk_reservation_active_user (active_user_id),
  KEY idx_reservation_user (user_id, start_time),
  CONSTRAINT fk_reservation_user FOREIGN KEY (user_id) REFERENCES users (id),
 CONSTRAINT fk_reservation_spot FOREIGN KEY (parking_spot_id) REFERENCES parking_spot (id),
  CONSTRAINT ck_reservation_times CHECK (end_time IS NULL OR end_time >= start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Reservation 1 --produces-- 0..1 Receipt
CREATE TABLE receipt (
  id             INT           NOT NULL AUTO_INCREMENT,
  reservation_id INT           NOT NULL,
  amount         DECIMAL(10,3) NOT NULL,           -- DECIMAL instead of double: exact money (3 decimals for TND)
  issued_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_receipt_reservation (reservation_id),
  CONSTRAINT fk_receipt_reservation FOREIGN KEY (reservation_id) REFERENCES reservation (id),
  CONSTRAINT ck_receipt_amount CHECK (amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE payment (
  id             INT           NOT NULL AUTO_INCREMENT,
  reservation_id INT           NOT NULL,
  amount         DECIMAL(10,3) NOT NULL,
  status         ENUM('PENDING', 'VALIDATED', 'UNPAID') NOT NULL DEFAULT 'PENDING',
  validated_by   INT,                              -- the ticket counter user who validated it
  PRIMARY KEY (id),
  UNIQUE KEY uk_payment_reservation (reservation_id),
  CONSTRAINT fk_payment_reservation FOREIGN KEY (reservation_id) REFERENCES reservation (id),
  CONSTRAINT fk_payment_validated_by FOREIGN KEY (validated_by) REFERENCES users (id) ON DELETE SET NULL,
  CONSTRAINT ck_payment_amount CHECK (amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- User 1 --files-- 0..* IssueReport, IssueReport 0..* --about-- 1 ParkingSpot
CREATE TABLE issue_report (
  id              INT           NOT NULL AUTO_INCREMENT,
  user_id         INT           NOT NULL,
  parking_spot_id INT           NOT NULL,
  description     VARCHAR(1000) NOT NULL,
  created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  resolved        BOOLEAN       NOT NULL DEFAULT FALSE,
  PRIMARY KEY (id),
  KEY idx_issue_report_open (resolved, created_at),
  CONSTRAINT fk_issue_report_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_issue_report_spot FOREIGN KEY (parking_spot_id) REFERENCES parking_spot (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Report generated for the admin (Admin ..receives..> UserReport).
CREATE TABLE user_report (
  id           INT      NOT NULL AUTO_INCREMENT,
  generated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  content      TEXT     NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- TicketCounter 1 --files-- 0..* NonPayingReport, NonPayingReport 0..* --targets-- 1 User
CREATE TABLE non_paying_report (
  id                INT           NOT NULL AUTO_INCREMENT,
  ticket_counter_id INT           NOT NULL,
  user_id           INT           NOT NULL,
  reported_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  note              VARCHAR(1000),
  PRIMARY KEY (id),
  CONSTRAINT fk_non_paying_report_counter FOREIGN KEY (ticket_counter_id) REFERENCES users (id),
  CONSTRAINT fk_non_paying_report_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
