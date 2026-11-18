-- ERD-01: machine, play and refill. Money is BIGINT pence (D4); colours and slots are JSON names.

CREATE TABLE machine (
  id                   BINARY(16)   NOT NULL,
  slot_count           INT          NOT NULL,
  colours              JSON         NOT NULL,
  k                    INT          NOT NULL,
  play_cost_pence      BIGINT       NOT NULL,
  starting_float_pence BIGINT       NOT NULL,
  float_policy         VARCHAR(20)  NOT NULL,
  float_pence          BIGINT       NOT NULL,
  free_plays           INT          NOT NULL,
  total_plays          BIGINT       NOT NULL,
  total_paid_pence     BIGINT       NOT NULL,
  total_refilled_pence BIGINT       NOT NULL,
  version              BIGINT       NOT NULL,
  created_at           TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at           TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT chk_machine_slot_count CHECK (slot_count >= 1),
  CONSTRAINT chk_machine_k CHECK (k >= 1 AND k <= slot_count),
  CONSTRAINT chk_machine_play_cost CHECK (play_cost_pence > 0),
  CONSTRAINT chk_machine_float CHECK (float_pence >= 0),
  CONSTRAINT chk_machine_free_plays CHECK (free_plays >= 0),
  CONSTRAINT chk_machine_policy CHECK (float_policy IN ('MIN_RESERVE', 'NONE'))
);

CREATE TABLE play (
  id                    BINARY(16)   NOT NULL,
  machine_id            BINARY(16)   NOT NULL,
  played_at             TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  slots                 JSON         NOT NULL,
  prize                 VARCHAR(20)  NOT NULL,
  used_free_play        BOOLEAN      NOT NULL,
  nominal_pence         BIGINT       NOT NULL,
  paid_pence            BIGINT       NOT NULL,
  free_plays_credited   INT          NOT NULL,
  float_after_pence     BIGINT       NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_play_machine FOREIGN KEY (machine_id) REFERENCES machine (id),
  CONSTRAINT chk_play_prize CHECK (prize IN ('JACKPOT', 'FULL_HOUSE', 'SMALL_PRIZE', 'NONE')),
  CONSTRAINT chk_play_paid CHECK (paid_pence >= 0),
  CONSTRAINT chk_play_float_after CHECK (float_after_pence >= 0)
);

CREATE TABLE refill (
  id                BINARY(16)   NOT NULL,
  machine_id        BINARY(16)   NOT NULL,
  refilled_at       TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  amount_pence      BIGINT       NOT NULL,
  float_after_pence BIGINT       NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_refill_machine FOREIGN KEY (machine_id) REFERENCES machine (id),
  CONSTRAINT chk_refill_amount CHECK (amount_pence > 0),
  CONSTRAINT chk_refill_float_after CHECK (float_after_pence >= 0)
);
