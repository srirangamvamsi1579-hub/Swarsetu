-- Swarsetu server schema v1 (MySQL 8, InnoDB, utf8mb4)
-- Multi-college design: every college-owned table has college_id (= the college code).
-- Links between records use composite foreign keys (college_id, x_id) -> parent(college_id, id),
-- so the database itself refuses a record that points into another college.

CREATE TABLE colleges (
  id                VARCHAR(12)  NOT NULL,              -- college code, e.g. RAM-7K2Q (primary key)
  name              VARCHAR(150) NOT NULL,
  name_key          VARCHAR(200) NOT NULL,              -- normalised name: unique alternate key
  affiliation       VARCHAR(200) NULL,
  city              VARCHAR(80)  NULL,
  country           VARCHAR(80)  NULL,
  address           VARCHAR(300) NULL,
  phone             VARCHAR(30)  NULL,
  email             VARCHAR(120) NULL,
  group_size        TINYINT UNSIGNED NOT NULL DEFAULT 6,
  approve_signups   BOOLEAN      NOT NULL DEFAULT TRUE,
  require_gmail     BOOLEAN      NOT NULL DEFAULT FALSE,
  reminder_days     TINYINT UNSIGNED NOT NULL DEFAULT 1,
  reminder_external BOOLEAN      NOT NULL DEFAULT TRUE,
  working_start     TIME         NOT NULL DEFAULT '09:00:00',
  working_end       TIME         NOT NULL DEFAULT '18:00:00',
  working_days      VARCHAR(40)  NOT NULL DEFAULT 'Mon,Tue,Wed,Thu,Fri',
  drive_link        VARCHAR(300) NULL,
  created_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  version           INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uq_colleges_name_key (name_key),
  CONSTRAINT ck_colleges_code CHECK (REGEXP_LIKE(id, '^[A-Z]{3}-[A-Z0-9]{4}$', 'c')),
  CONSTRAINT ck_colleges_group CHECK (group_size BETWEEN 1 AND 50),
  CONSTRAINT ck_colleges_rem CHECK (reminder_days <= 30)
) ENGINE=InnoDB;

CREATE TABLE users (
  id                 VARCHAR(40)  NOT NULL,
  college_id         VARCHAR(12)  NOT NULL,
  username           VARCHAR(60)  NOT NULL,
  username_lc        VARCHAR(60)  GENERATED ALWAYS AS (LOWER(username)) STORED,
  email              VARCHAR(120) NOT NULL,
  mobile             CHAR(10)     NULL,
  country_code       VARCHAR(5)   NOT NULL DEFAULT '+91',
  first_name         VARCHAR(60)  NOT NULL,
  last_name          VARCHAR(60)  NULL,
  alt_contact        VARCHAR(30)  NULL,
  city               VARCHAR(80)  NULL,
  country            VARCHAR(80)  NULL,
  role               ENUM('admin','faculty','student') NOT NULL,
  status             ENUM('pending','active','inactive','rejected') NOT NULL DEFAULT 'pending',
  password_hash      VARCHAR(100) NOT NULL,
  must_change        BOOLEAN      NOT NULL DEFAULT FALSE,
  failed_count       TINYINT UNSIGNED NOT NULL DEFAULT 0,
  locked_until       DATETIME(3)  NULL,
  last_login_at      DATETIME(3)  NULL,
  prev_login_at      DATETIME(3)  NULL,
  password_set_at    DATETIME(3)  NULL,
  email_verified_at  DATETIME(3)  NULL,
  mobile_verified_at DATETIME(3)  NULL,
  student_id         VARCHAR(40)  NULL,                  -- linked enrolment (same college)
  reject_reason      VARCHAR(300) NULL,
  approved_by        VARCHAR(40)  NULL,
  approved_at        DATETIME(3)  NULL,
  created_at         DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at         DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  version            INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uq_users_college_id (college_id, id),
  UNIQUE KEY uq_users_username (username_lc),            -- sign-in IDs are unique across all colleges
  UNIQUE KEY uq_users_email (email),
  UNIQUE KEY uq_users_mobile (mobile),
  UNIQUE KEY uq_users_student (college_id, student_id),
  KEY ix_users_college_status (college_id, status),
  CONSTRAINT fk_users_college FOREIGN KEY (college_id) REFERENCES colleges(id) ON DELETE RESTRICT,
  CONSTRAINT ck_users_mobile CHECK (mobile IS NULL OR REGEXP_LIKE(mobile, '^[0-9]{10}$')),
  CONSTRAINT ck_users_email CHECK (email = LOWER(email))
) ENGINE=InnoDB;

CREATE TABLE role_permissions (
  college_id VARCHAR(12) NOT NULL,
  role       ENUM('faculty','student') NOT NULL,
  permission VARCHAR(40) NOT NULL,
  allowed    BOOLEAN     NOT NULL,
  PRIMARY KEY (college_id, role, permission),
  CONSTRAINT fk_perm_college FOREIGN KEY (college_id) REFERENCES colleges(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE courses (
  id          VARCHAR(80)  NOT NULL,
  college_id  VARCHAR(12)  NOT NULL,
  name        VARCHAR(120) NOT NULL,
  description VARCHAR(500) NULL,
  level       VARCHAR(40)  NULL,
  instructor  VARCHAR(120) NULL,
  duration    VARCHAR(60)  NULL,
  status      ENUM('Active','Inactive') NOT NULL DEFAULT 'Active',
  created_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  version     INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (college_id, id),
  UNIQUE KEY uq_courses_name (college_id, name),
  CONSTRAINT fk_courses_college FOREIGN KEY (college_id) REFERENCES colleges(id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE batches (
  id          VARCHAR(80)  NOT NULL,
  college_id  VARCHAR(12)  NOT NULL,
  course_id    VARCHAR(80)       NOT NULL,
  batch_key    VARCHAR(160) NOT NULL,                  -- course + weekdays + time + mode (+ student for 1:1)
  name         VARCHAR(200) NOT NULL,
  class_type   ENUM('Group','One-to-One') NOT NULL,
  mode         ENUM('Online','Offline') NOT NULL,
  weekdays     VARCHAR(40)  NOT NULL,
  start_time   TIME         NOT NULL,
  length_min   SMALLINT UNSIGNED NOT NULL DEFAULT 30,
  location     VARCHAR(150) NULL,
  meet_link    VARCHAR(300) NULL,
  created_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  version      INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (college_id, id),
  UNIQUE KEY uq_batches_key (college_id, batch_key),
  CONSTRAINT fk_batches_course FOREIGN KEY (college_id, course_id) REFERENCES courses(college_id, id) ON DELETE RESTRICT,
  CONSTRAINT ck_batches_link CHECK (meet_link IS NULL OR meet_link LIKE 'https://%')
) ENGINE=InnoDB;

CREATE TABLE students (
  id          VARCHAR(80)  NOT NULL,
  college_id  VARCHAR(12)  NOT NULL,
  first_name     VARCHAR(60)  NOT NULL,
  last_name      VARCHAR(60)  NULL,
  email          VARCHAR(120) NULL,
  mobile         CHAR(10)     NULL,
  parent         VARCHAR(120) NULL,
  age_group      VARCHAR(20)  NULL,
  city           VARCHAR(80)  NULL,
  country        VARCHAR(80)  NULL,
  course_id      VARCHAR(80)       NOT NULL,
  batch_id       VARCHAR(80)       NULL,
  level          VARCHAR(40)  NULL,
  class_type     ENUM('Group','One-to-One') NOT NULL,
  mode           ENUM('Online','Offline') NOT NULL,
  weekdays       VARCHAR(40)  NOT NULL,
  start_time     TIME         NOT NULL,
  length_min     SMALLINT UNSIGNED NOT NULL DEFAULT 30,
  monthly_fee    DECIMAL(10,2) NULL,
  joining_fee    DECIMAL(10,2) NULL,
  joining_date   DATE         NULL,
  status         ENUM('Active','Inactive') NOT NULL DEFAULT 'Active',
  created_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  version        INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (college_id, id),  -- id is the student code, e.g. STU0001-HIN
  KEY ix_students_batch (college_id, batch_id),
  CONSTRAINT fk_students_course FOREIGN KEY (college_id, course_id) REFERENCES courses(college_id, id) ON DELETE RESTRICT,
  CONSTRAINT fk_students_batch FOREIGN KEY (college_id, batch_id) REFERENCES batches(college_id, id) ON DELETE RESTRICT,
  CONSTRAINT ck_students_fee CHECK (monthly_fee IS NULL OR monthly_fee >= 0)
) ENGINE=InnoDB;

ALTER TABLE users
  ADD CONSTRAINT fk_users_student FOREIGN KEY (college_id, student_id) REFERENCES students(college_id, id) ON DELETE RESTRICT,
  ADD CONSTRAINT fk_users_approver FOREIGN KEY (college_id, approved_by) REFERENCES users(college_id, id) ON DELETE RESTRICT;

CREATE TABLE attendance_marks (
  college_id  VARCHAR(12)  NOT NULL,
  student_id    VARCHAR(80)      NOT NULL,
  class_date    DATE        NOT NULL,
  code          ENUM('P','L','A','V','H','DH','C') NOT NULL,
  previous_code ENUM('P','L','A','V','H','DH','C') NULL,
  saved_by      VARCHAR(80)      NOT NULL,
  saved_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (college_id, student_id, class_date),
  KEY ix_att_day (college_id, class_date),
  CONSTRAINT fk_att_student FOREIGN KEY (college_id, student_id) REFERENCES students(college_id, id) ON DELETE CASCADE,
  CONSTRAINT fk_att_user FOREIGN KEY (college_id, saved_by) REFERENCES users(college_id, id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE absence_notices (
  id          VARCHAR(80)  NOT NULL,
  college_id  VARCHAR(12)  NOT NULL,
  student_id  VARCHAR(80)       NOT NULL,
  class_date  DATE         NOT NULL,
  reason      VARCHAR(60)  NOT NULL,
  note        VARCHAR(200) NULL,
  status      ENUM('sent','seen','marked','cancelled') NOT NULL DEFAULT 'sent',
  sent_by     VARCHAR(80)       NOT NULL,
  sent_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  handled_by  VARCHAR(80)       NULL,
  handled_at  DATETIME(3)  NULL,
  PRIMARY KEY (college_id, id),
  KEY ix_notice_day (college_id, class_date),
  CONSTRAINT fk_notice_student FOREIGN KEY (college_id, student_id) REFERENCES students(college_id, id) ON DELETE CASCADE,
  CONSTRAINT fk_notice_sender FOREIGN KEY (college_id, sent_by) REFERENCES users(college_id, id) ON DELETE RESTRICT,
  CONSTRAINT fk_notice_handler FOREIGN KEY (college_id, handled_by) REFERENCES users(college_id, id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE payments (
  id          VARCHAR(80)  NOT NULL,
  college_id  VARCHAR(12)  NOT NULL,
  student_id    VARCHAR(80)        NOT NULL,
  batch_id      VARCHAR(80)        NULL,
  batch_name    VARCHAR(200)  NULL,                  -- kept for history if the batch is later removed
  fee_month     CHAR(7)       NOT NULL,              -- YYYY-MM
  payment_date  DATE          NULL,
  total_fee     DECIMAL(10,2) NOT NULL,
  paid          DECIMAL(10,2) NOT NULL DEFAULT 0,
  dues          DECIMAL(10,2) GENERATED ALWAYS AS (GREATEST(total_fee - paid, 0)) STORED,
  status        VARCHAR(20)   GENERATED ALWAYS AS (CASE WHEN paid <= 0 AND total_fee > 0 THEN 'Unpaid' WHEN paid >= total_fee THEN 'Paid' ELSE 'Partially paid' END) STORED,
  method        ENUM('UPI','Cash','Bank Transfer','Card','Cheque') NOT NULL DEFAULT 'UPI',
  remarks       VARCHAR(300)  NULL,
  updated_by    VARCHAR(80)        NULL,
  created_at    DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at    DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  version       INT           NOT NULL DEFAULT 0,
  PRIMARY KEY (college_id, id),
  KEY ix_pay_student_month (college_id, student_id, fee_month),
  KEY ix_pay_month (college_id, fee_month),
  CONSTRAINT fk_pay_student FOREIGN KEY (college_id, student_id) REFERENCES students(college_id, id) ON DELETE RESTRICT,
  CONSTRAINT fk_pay_batch FOREIGN KEY (college_id, batch_id) REFERENCES batches(college_id, id) ON DELETE RESTRICT,
  CONSTRAINT fk_pay_user FOREIGN KEY (college_id, updated_by) REFERENCES users(college_id, id) ON DELETE RESTRICT,
  CONSTRAINT ck_pay_month CHECK (REGEXP_LIKE(fee_month, '^[0-9]{4}-(0[1-9]|1[0-2])$')),
  CONSTRAINT ck_pay_amounts CHECK (total_fee >= 0 AND paid >= 0 AND paid <= total_fee)
) ENGINE=InnoDB;

CREATE TABLE payment_claims (
  id          VARCHAR(80)  NOT NULL,
  college_id  VARCHAR(12)  NOT NULL,
  student_id     VARCHAR(80)        NOT NULL,
  fee_month      CHAR(7)       NOT NULL,
  amount         DECIMAL(10,2) NOT NULL,
  method         ENUM('UPI','Bank transfer') NOT NULL,
  reference      VARCHAR(22)   NOT NULL,
  status         ENUM('pending','verified','rejected') NOT NULL DEFAULT 'pending',
  active_ref     VARCHAR(22)   GENERATED ALWAYS AS (IF(status <> 'rejected', UPPER(reference), NULL)) STORED,
  paid_on        DATE          NOT NULL,
  submitted_by   VARCHAR(80)        NOT NULL,
  submitted_at   DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  reviewed_by    VARCHAR(80)        NULL,
  reviewed_at    DATETIME(3)   NULL,
  review_note    VARCHAR(300)  NULL,
  payment_id     VARCHAR(80)        NULL,
  PRIMARY KEY (college_id, id),
  UNIQUE KEY uq_claim_ref (active_ref),               -- a payment reference is accepted once, across all colleges
  KEY ix_claim_status (college_id, status),
  CONSTRAINT fk_claim_student FOREIGN KEY (college_id, student_id) REFERENCES students(college_id, id) ON DELETE CASCADE,
  CONSTRAINT fk_claim_submitter FOREIGN KEY (college_id, submitted_by) REFERENCES users(college_id, id) ON DELETE RESTRICT,
  CONSTRAINT fk_claim_reviewer FOREIGN KEY (college_id, reviewed_by) REFERENCES users(college_id, id) ON DELETE RESTRICT,
  CONSTRAINT fk_claim_payment FOREIGN KEY (college_id, payment_id) REFERENCES payments(college_id, id) ON DELETE RESTRICT,
  CONSTRAINT ck_claim_amount CHECK (amount > 0 AND amount <= 200000),
  CONSTRAINT ck_claim_ref CHECK ((method = 'UPI' AND REGEXP_LIKE(reference, '^[0-9]{12}$')) OR (method = 'Bank transfer' AND REGEXP_LIKE(reference, '^[A-Za-z0-9]{6,22}$')))
) ENGINE=InnoDB;

CREATE TABLE college_payment_details (
  college_id    VARCHAR(12)  NOT NULL,
  payee         VARCHAR(80)  NOT NULL,
  upi_id        VARCHAR(80)  NULL,
  upi_mobile    CHAR(10)     NULL,
  apps          VARCHAR(100) NULL,
  bank          VARCHAR(80)  NULL,
  branch        VARCHAR(80)  NULL,
  account_no    VARCHAR(18)  NULL,
  ifsc          CHAR(11)     NULL,
  qr_mode       ENUM('gen','upload') NOT NULL DEFAULT 'gen',
  qr_image      MEDIUMBLOB   NULL,
  qr_vpa        VARCHAR(80)  NULL,
  changed_by    VARCHAR(80)       NOT NULL,
  changed_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (college_id),
  CONSTRAINT fk_paydet_college FOREIGN KEY (college_id) REFERENCES colleges(id) ON DELETE CASCADE,
  CONSTRAINT fk_paydet_user FOREIGN KEY (college_id, changed_by) REFERENCES users(college_id, id) ON DELETE RESTRICT,
  CONSTRAINT ck_paydet_upi CHECK (upi_id IS NULL OR REGEXP_LIKE(upi_id, '^[A-Za-z0-9._-]{2,256}@[A-Za-z]{2,64}$')),
  CONSTRAINT ck_paydet_ifsc CHECK (ifsc IS NULL OR REGEXP_LIKE(ifsc, '^[A-Z]{4}0[A-Z0-9]{6}$', 'c')),
  CONSTRAINT ck_paydet_acct CHECK (account_no IS NULL OR REGEXP_LIKE(account_no, '^[0-9]{9,18}$')),
  CONSTRAINT ck_paydet_some CHECK (upi_id IS NOT NULL OR account_no IS NOT NULL),
  CONSTRAINT ck_paydet_qr CHECK (qr_mode = 'gen' OR (qr_image IS NOT NULL AND qr_vpa IS NOT NULL AND LOWER(qr_vpa) = LOWER(upi_id)))
) ENGINE=InnoDB;

CREATE TABLE calendar_events (
  id          VARCHAR(80)  NOT NULL,
  college_id  VARCHAR(12)  NOT NULL,
  title       VARCHAR(150) NOT NULL,
  type        ENUM('Holiday','Vacation','Event','Other') NOT NULL,
  start_date  DATE         NOT NULL,
  end_date    DATE         NOT NULL,
  description VARCHAR(500) NULL,
  applies_to  ENUM('all','batches','students') NOT NULL DEFAULT 'all',
  created_by  VARCHAR(80)       NOT NULL,
  created_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  version     INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (college_id, id),
  KEY ix_cal_dates (college_id, start_date, end_date),
  CONSTRAINT fk_cal_user FOREIGN KEY (college_id, created_by) REFERENCES users(college_id, id) ON DELETE RESTRICT,
  CONSTRAINT ck_cal_dates CHECK (end_date >= start_date)
) ENGINE=InnoDB;

CREATE TABLE calendar_event_batches (
  college_id VARCHAR(12) NOT NULL,
  event_id   VARCHAR(80)      NOT NULL,
  batch_id   VARCHAR(80)      NOT NULL,
  PRIMARY KEY (college_id, event_id, batch_id),
  CONSTRAINT fk_ceb_event FOREIGN KEY (college_id, event_id) REFERENCES calendar_events(college_id, id) ON DELETE CASCADE,
  CONSTRAINT fk_ceb_batch FOREIGN KEY (college_id, batch_id) REFERENCES batches(college_id, id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE calendar_event_students (
  college_id VARCHAR(12) NOT NULL,
  event_id   VARCHAR(80)      NOT NULL,
  student_id VARCHAR(80)      NOT NULL,
  PRIMARY KEY (college_id, event_id, student_id),
  CONSTRAINT fk_ces_event FOREIGN KEY (college_id, event_id) REFERENCES calendar_events(college_id, id) ON DELETE CASCADE,
  CONSTRAINT fk_ces_student FOREIGN KEY (college_id, student_id) REFERENCES students(college_id, id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE inbox_messages (
  id          VARCHAR(80)  NOT NULL,
  college_id  VARCHAR(12)  NOT NULL,
  user_id     VARCHAR(80)       NULL,                       -- one person, or NULL for a role/permission audience
  audience    VARCHAR(40)  NULL,                       -- 'all', 'admin', or a permission name
  kind        ENUM('holiday','absence','payment','join','security','system') NOT NULL,
  title       VARCHAR(200) NOT NULL,
  body        VARCHAR(1000) NOT NULL,
  link        VARCHAR(40)  NULL,
  dedupe_key  VARCHAR(120) NULL,
  created_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (college_id, id),
  UNIQUE KEY uq_inbox_dedupe (college_id, dedupe_key),
  KEY ix_inbox_user (college_id, user_id, created_at),
  CONSTRAINT fk_inbox_user FOREIGN KEY (college_id, user_id) REFERENCES users(college_id, id) ON DELETE CASCADE,
  CONSTRAINT ck_inbox_target CHECK ((user_id IS NOT NULL AND audience IS NULL) OR (user_id IS NULL AND audience IS NOT NULL))
) ENGINE=InnoDB;

CREATE TABLE inbox_reads (
  college_id VARCHAR(12) NOT NULL,
  message_id VARCHAR(80)      NOT NULL,
  user_id    VARCHAR(80)      NOT NULL,
  read_at    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (college_id, message_id, user_id),
  CONSTRAINT fk_read_msg FOREIGN KEY (college_id, message_id) REFERENCES inbox_messages(college_id, id) ON DELETE CASCADE,
  CONSTRAINT fk_read_user FOREIGN KEY (college_id, user_id) REFERENCES users(college_id, id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Pending registrations and one-time codes (email / SMS). Codes are stored hashed.
CREATE TABLE verifications (
  id           CHAR(36)     NOT NULL,                  -- random UUID handed to the client
  purpose      ENUM('register','reset','change_contact') NOT NULL,
  user_id      VARCHAR(40)  NULL,
  payload      JSON         NULL,                      -- pending registration / new contact details
  email        VARCHAR(120) NULL,
  mobile       CHAR(10)     NULL,
  email_code_hash  VARCHAR(100) NULL,
  mobile_code_hash VARCHAR(100) NULL,
  attempts     TINYINT UNSIGNED NOT NULL DEFAULT 0,
  expires_at   DATETIME(3)  NOT NULL,
  consumed_at  DATETIME(3)  NULL,
  created_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY ix_verif_exp (expires_at),
  CONSTRAINT fk_verif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE refresh_tokens (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  user_id     VARCHAR(80)      NOT NULL,
  token_hash  CHAR(64)    NOT NULL,
  family      CHAR(36)    NOT NULL,                    -- rotation family: reuse of an old token revokes the family
  expires_at  DATETIME(3) NOT NULL,
  revoked_at  DATETIME(3) NULL,
  replaced_by BIGINT      NULL,
  created_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  user_agent  VARCHAR(200) NULL,
  ip          VARCHAR(45) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_rt_hash (token_hash),
  KEY ix_rt_family (family),
  CONSTRAINT fk_rt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Reliable delivery of email / SMS: written in the same transaction as the business change,
-- then sent by a background worker with retries.
CREATE TABLE outbox (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  college_id  VARCHAR(12)  NULL,
  channel     ENUM('email','sms') NOT NULL,
  recipient   VARCHAR(120) NOT NULL,
  subject     VARCHAR(200) NULL,
  body        TEXT         NOT NULL,
  status      ENUM('queued','sent','failed') NOT NULL DEFAULT 'queued',
  attempts    TINYINT UNSIGNED NOT NULL DEFAULT 0,
  next_try_at DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  last_error  VARCHAR(500) NULL,
  dedupe_key  VARCHAR(150) NULL,
  created_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  sent_at     DATETIME(3)  NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_outbox_dedupe (dedupe_key),
  KEY ix_outbox_due (status, next_try_at),
  CONSTRAINT fk_outbox_college FOREIGN KEY (college_id) REFERENCES colleges(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE audit_log (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  college_id  VARCHAR(12)  NULL,
  user_id     VARCHAR(40)  NULL,
  action      VARCHAR(60)  NOT NULL,
  entity      VARCHAR(40)  NULL,
  entity_id   VARCHAR(40)  NULL,
  detail      JSON         NULL,
  ip          VARCHAR(45)  NULL,
  at          DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY ix_audit_college (college_id, at)
) ENGINE=InnoDB;

-- Lists that do not have their own tables yet (classes, timetable changes, academic levels,
-- events, notification log, import history, faculty slots). Stored per college as JSON
-- documents, still keyed by college code and checked against permissions by the server.
CREATE TABLE college_documents (
  college_id  VARCHAR(12)  NOT NULL,
  collection  VARCHAR(40)  NOT NULL,
  id          VARCHAR(120) NOT NULL,
  data        JSON         NOT NULL,
  updated_by  VARCHAR(40)  NULL,
  updated_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (college_id, collection, id),
  CONSTRAINT fk_docs_college FOREIGN KEY (college_id) REFERENCES colleges(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE payment_detail_audit (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  college_id  VARCHAR(12)  NOT NULL,
  changed_by  VARCHAR(40)  NOT NULL,
  fields      VARCHAR(300) NOT NULL,
  old_upi     VARCHAR(80)  NULL,
  old_account_last4 CHAR(4) NULL,
  at          DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY ix_pda_college (college_id, at),
  CONSTRAINT fk_pda_college FOREIGN KEY (college_id) REFERENCES colleges(id) ON DELETE CASCADE
) ENGINE=InnoDB;
