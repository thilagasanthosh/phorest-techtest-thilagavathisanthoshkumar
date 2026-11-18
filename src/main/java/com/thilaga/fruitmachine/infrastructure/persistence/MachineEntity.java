package com.thilaga.fruitmachine.infrastructure.persistence;

import com.thilaga.fruitmachine.domain.FloatPolicyType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** ERD-01 {@code machine} row. Colours are names; the mapper converts them to ids. */
@Entity
@Table(name = "machine")
class MachineEntity {

  @Id UUID id;

  @Column(name = "slot_count")
  int slotCount;

  @JdbcTypeCode(SqlTypes.JSON)
  List<String> colours;

  int k;

  @Column(name = "play_cost_pence")
  long playCostPence;

  @Column(name = "starting_float_pence")
  long startingFloatPence;

  @Enumerated(EnumType.STRING)
  @Column(name = "float_policy")
  FloatPolicyType floatPolicy;

  @Column(name = "float_pence")
  long floatPence;

  @Column(name = "free_plays")
  int freePlays;

  @Column(name = "total_plays")
  long totalPlays;

  @Column(name = "total_paid_pence")
  long totalPaidPence;

  @Column(name = "total_refilled_pence")
  long totalRefilledPence;

  @Version long version;

  protected MachineEntity() {}
}
