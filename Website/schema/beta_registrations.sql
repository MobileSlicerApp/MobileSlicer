CREATE TABLE IF NOT EXISTS beta_registrations (
  id TEXT PRIMARY KEY,
  email TEXT,
  device TEXT NOT NULL,
  printer TEXT NOT NULL,
  testing TEXT NOT NULL,
  created_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_beta_registrations_created_at
  ON beta_registrations(created_at);

CREATE TABLE IF NOT EXISTS beta_feedback (
  id TEXT PRIMARY KEY,
  type TEXT NOT NULL,
  title TEXT NOT NULL,
  body TEXT NOT NULL,
  photo_keys TEXT NOT NULL,
  created_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_beta_feedback_created_at
  ON beta_feedback(created_at);
