package com.thilaga.fruitmachine.infrastructure.persistence;

import com.thilaga.fruitmachine.domain.Machine;
import com.thilaga.fruitmachine.domain.MachineConfig;
import com.thilaga.fruitmachine.domain.MachineState;
import com.thilaga.fruitmachine.domain.MachineTotals;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Converts between domain objects and entities. Colour ids become names here (MachineConfig
 * Javadoc).
 */
final class MachineMapper {

  private MachineMapper() {}

  static MachineEntity toEntity(UUID id, Machine machine, MachineTotals totals) {
    MachineConfig config = machine.config();
    MachineEntity entity = new MachineEntity();
    entity.id = id;
    entity.slotCount = config.slots();
    entity.colours = config.colours();
    entity.k = config.k();
    entity.playCostPence = config.playCostPence();
    entity.startingFloatPence = config.startingFloatPence();
    entity.floatPolicy = config.floatPolicyType();
    entity.floatPence = machine.floatPence();
    entity.freePlays = Math.toIntExact(machine.freePlayBalance());
    entity.totalPlays = totals.totalPlays();
    entity.totalPaidPence = totals.totalPaidPence();
    entity.totalRefilledPence = totals.totalRefilledPence();
    return entity;
  }

  static MachineState toState(MachineEntity entity) {
    MachineConfig config =
        new MachineConfig(
            entity.slotCount,
            entity.colours,
            entity.k,
            entity.playCostPence,
            entity.startingFloatPence,
            entity.floatPolicy);
    Machine machine = new Machine(config, entity.floatPence, entity.freePlays);
    MachineTotals totals =
        new MachineTotals(entity.totalPlays, entity.totalPaidPence, entity.totalRefilledPence);
    return new MachineState(entity.id, machine, totals);
  }

  /** Spin colour ids to colour names, for the play history (JSON column). */
  static List<String> slotNames(int[] spin, List<String> colours) {
    List<String> names = new ArrayList<>(spin.length);
    for (int id : spin) {
      names.add(colours.get(id));
    }
    return names;
  }

  /** Colour names from the play history back to colour ids. */
  static int[] spinIds(List<String> names, List<String> colours) {
    int[] ids = new int[names.size()];
    for (int i = 0; i < ids.length; i++) {
      ids[i] = colours.indexOf(names.get(i));
    }
    return ids;
  }
}
