package com.thilaga.fruitmachine.api;

import com.thilaga.fruitmachine.domain.MachineStatus;
import com.thilaga.fruitmachine.domain.PlayResult;
import com.thilaga.fruitmachine.domain.PrizeType;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** Play shape of design 5.3 (AC-15). */
public record PlayResponse(
    UUID playId,
    List<String> slots,
    PrizeType prize,
    long nominalPayout,
    long paidOut,
    long freePlaysCredited,
    long freePlayBalance,
    long floatAfter,
    boolean paidWithFreePlay,
    MachineStatus status) {

  /** Maps colour ids to colour names using the machine's colours (design 5.3). */
  static PlayResponse from(PlayResult result, List<String> colours) {
    return new PlayResponse(
        result.playId(),
        Arrays.stream(result.spin()).mapToObj(colours::get).toList(),
        result.prize(),
        result.nominalPence(),
        result.paidPence(),
        result.freePlaysEarned(),
        result.freePlayBalanceAfter(),
        result.floatAfterPence(),
        result.paidWithFreePlay(),
        result.status());
  }
}
