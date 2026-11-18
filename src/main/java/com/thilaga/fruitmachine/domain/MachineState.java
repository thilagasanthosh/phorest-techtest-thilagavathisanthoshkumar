package com.thilaga.fruitmachine.domain;

import java.util.UUID;

/** A machine as loaded from storage: its id, its current state and its lifetime totals. */
public record MachineState(UUID id, Machine machine, MachineTotals totals) {}
