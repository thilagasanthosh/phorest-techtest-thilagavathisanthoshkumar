package com.thilaga.fruitmachine.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.thilaga.fruitmachine.domain.FloatPolicyType;
import com.thilaga.fruitmachine.domain.Machine;
import com.thilaga.fruitmachine.domain.MachineConfig;
import com.thilaga.fruitmachine.domain.MachineState;
import com.thilaga.fruitmachine.domain.MachineTotals;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MachineMapperTest {

  private static final List<String> COLOURS = List.of("black", "white", "green", "yellow");

  @Test
  void machineRoundTripKeepsConfigFloatFreePlaysAndTotals() {
    MachineConfig config = MachineConfig.of(4, COLOURS, 2, 100, 1_000, FloatPolicyType.MIN_RESERVE);
    UUID id = UUID.randomUUID();
    MachineTotals totals = new MachineTotals(7, 450, 1_000);

    MachineState state =
        MachineMapper.toState(MachineMapper.toEntity(id, new Machine(config, 1_200, 3), totals));

    assertEquals(id, state.id());
    assertEquals(config, state.machine().config());
    assertEquals(1_200, state.machine().floatPence());
    assertEquals(3, state.machine().freePlayBalance());
    assertEquals(totals, state.totals());
  }

  @Test
  void spinIdsMapToColourNamesAndBack() {
    List<String> names = MachineMapper.slotNames(new int[] {0, 2, 2, 3}, COLOURS);

    assertEquals(List.of("black", "green", "green", "yellow"), names);
    assertArrayEquals(new int[] {0, 2, 2, 3}, MachineMapper.spinIds(names, COLOURS));
  }
}
