-- Idempotency-Key for POST /plays: a retried key returns the stored result, never plays twice.
-- Keys never expire (no cleanup job); one row per (machine, key).

CREATE TABLE play_idempotency (
  id              BINARY(16)   NOT NULL,
  machine_id      BINARY(16)   NOT NULL,
  idempotency_key VARCHAR(128) NOT NULL,
  result          JSON         NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_play_idempotency_key (machine_id, idempotency_key),
  CONSTRAINT fk_play_idempotency_machine FOREIGN KEY (machine_id) REFERENCES machine (id)
);
