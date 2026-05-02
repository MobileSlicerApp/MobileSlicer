CREATE TABLE IF NOT EXISTS beta_registrations_new (
  id TEXT PRIMARY KEY,
  email TEXT,
  device TEXT NOT NULL,
  printer TEXT NOT NULL,
  testing TEXT NOT NULL,
  created_at TEXT NOT NULL
);

INSERT INTO beta_registrations_new
  (id, email, device, printer, testing, created_at)
SELECT id, email, device, printer, testing, created_at
FROM beta_registrations;

DROP TABLE beta_registrations;

ALTER TABLE beta_registrations_new RENAME TO beta_registrations;

CREATE INDEX IF NOT EXISTS idx_beta_registrations_created_at
  ON beta_registrations(created_at);
