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
