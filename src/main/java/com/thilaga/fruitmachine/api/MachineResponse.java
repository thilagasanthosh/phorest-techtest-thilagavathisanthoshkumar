package com.thilaga.fruitmachine.api;

import com.thilaga.fruitmachine.domain.FloatPolicyType;
import com.thilaga.fruitmachine.domain.MachineConfig;
import com.thilaga.fruitmachine.domain.MachineState;
import com.thilaga.fruitmachine.domain.MachineStatus;
import java.util.List;
import java.util.UUID;

/** Machine shape of design 5.3. Reserve and status are derived, never stored (A3, A6). */
public record MachineResponse(
    UUID id,
    int slotCount,
    List<String> colours,
    int k,
    long playCostPence,
    FloatPolicyType floatPolicy,
    long floatPence,
    long reservePence,
    MachineStatus status,
    long freePlays,
    long totalPlays,
    long totalPaidPence) {

  static MachineResponse from(MachineState state) {
    MachineConfig config = state.machine().config();
    return new MachineResponse(
        state.id(),
        config.slots(),
        config.colours(),
        config.k(),
        config.playCostPence(),
        config.floatPolicyType(),
        state.machine().floatPence(),
        state.machine().reservePence(),
        state.machine().status(),
        state.machine().freePlayBalance(),
        state.totals().totalPlays(),
        state.totals().totalPaidPence());
  }
}
