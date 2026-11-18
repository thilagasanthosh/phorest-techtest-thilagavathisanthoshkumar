package com.thilaga.fruitmachine.domain;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/** Everything a paid or free play reports back (AC-15, design 5.3). */
public record PlayResult(
    UUID playId,
    int[] spin,
    PrizeType prize,
    long nominalPence,
    long paidPence,
    long freePlaysEarned,
    long freePlayBalanceAfter,
    long floatAfterPence,
    boolean paidWithFreePlay,
    MachineStatus status) {

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PlayResult other)) {
      return false;
    }
    return Objects.equals(playId, other.playId)
        && Arrays.equals(spin, other.spin)
        && prize == other.prize
        && nominalPence == other.nominalPence
        && paidPence == other.paidPence
        && freePlaysEarned == other.freePlaysEarned
        && freePlayBalanceAfter == other.freePlayBalanceAfter
        && floatAfterPence == other.floatAfterPence
        && paidWithFreePlay == other.paidWithFreePlay
        && status == other.status;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        playId,
        Arrays.hashCode(spin),
        prize,
        nominalPence,
        paidPence,
        freePlaysEarned,
        freePlayBalanceAfter,
        floatAfterPence,
        paidWithFreePlay,
        status);
  }

  @Override
  public String toString() {
    return "PlayResult[playId="
        + playId
        + ", spin="
        + Arrays.toString(spin)
        + ", prize="
        + prize
        + ", nominalPence="
        + nominalPence
        + ", paidPence="
        + paidPence
        + ", freePlaysEarned="
        + freePlaysEarned
        + ", freePlayBalanceAfter="
        + freePlayBalanceAfter
        + ", floatAfterPence="
        + floatAfterPence
        + ", paidWithFreePlay="
        + paidWithFreePlay
        + ", status="
        + status
        + "]";
  }
}
