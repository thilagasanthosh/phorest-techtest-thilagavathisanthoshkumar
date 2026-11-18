package com.thilaga.fruitmachine.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** ERD-01 {@code refill} row: one operator refill (D11). */
@Entity
@Table(name = "refill")
class RefillEntity {

  @Id UUID id;

  @Column(name = "machine_id")
  UUID machineId;

  @Column(name = "amount_pence")
  long amountPence;

  @Column(name = "float_after_pence")
  long floatAfterPence;

  protected RefillEntity() {}
}
