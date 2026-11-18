package com.thilaga.fruitmachine.api;

import com.thilaga.fruitmachine.domain.FloatPolicyType;
import com.thilaga.fruitmachine.domain.MachineConfig;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

/** API-01 body (design 5.3). Omitted optionals take the original game's defaults (BR-01). */
public record CreateMachineRequest(
    Integer slotCount,
    List<String> colours,
    Integer k,
    @NotNull @Positive Long playCostPence,
    @NotNull Long startingFloatPence,
    FloatPolicyType floatPolicy) {

  MachineConfig toConfig() {
    return MachineConfig.of(
        slotCount != null ? slotCount : MachineConfig.DEFAULT_SLOTS,
        colours != null ? colours : MachineConfig.DEFAULT_COLOURS,
        k != null ? k : MachineConfig.DEFAULT_K,
        playCostPence,
        startingFloatPence,
        floatPolicy != null ? floatPolicy : MachineConfig.DEFAULT_POLICY);
  }
}
