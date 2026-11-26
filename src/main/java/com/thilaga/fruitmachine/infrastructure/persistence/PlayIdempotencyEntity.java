package com.thilaga.fruitmachine.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** {@code play_idempotency} row: the stored result of the play that used an Idempotency-Key. */
@Entity
@Table(name = "play_idempotency")
class PlayIdempotencyEntity {

  @Id UUID id;

  @Column(name = "machine_id")
  UUID machineId;

  @Column(name = "idempotency_key")
  String idempotencyKey;

  @JdbcTypeCode(SqlTypes.JSON)
  String result;

  protected PlayIdempotencyEntity() {}

  PlayIdempotencyEntity(UUID id, UUID machineId, String idempotencyKey, String result) {
    this.id = id;
    this.machineId = machineId;
    this.idempotencyKey = idempotencyKey;
    this.result = result;
  }
}
