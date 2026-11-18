package com.thilaga.fruitmachine.infrastructure.persistence;

import com.thilaga.fruitmachine.domain.PrizeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** ERD-01 {@code play} row: one history record per play. */
@Entity
@Table(name = "play")
class PlayEntity {

  @Id UUID id;

  @Column(name = "machine_id")
  UUID machineId;

  @JdbcTypeCode(SqlTypes.JSON)
  List<String> slots;

  @Enumerated(EnumType.STRING)
  PrizeType prize;

  @Column(name = "used_free_play")
  boolean usedFreePlay;

  @Column(name = "nominal_pence")
  long nominalPence;

  @Column(name = "paid_pence")
  long paidPence;

  @Column(name = "free_plays_credited")
  int freePlaysCredited;

  @Column(name = "float_after_pence")
  long floatAfterPence;

  protected PlayEntity() {}
}
