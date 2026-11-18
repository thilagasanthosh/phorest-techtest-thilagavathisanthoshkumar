package com.thilaga.fruitmachine.domain;

import java.util.Arrays;
import java.util.Objects;

/** Everything a paid or free play reports back (AC-15). */
public record PlayResult(
    int[] spin,
    PrizeType prize,
    long paidPence,
    long freePlaysEarned,
    long freePlayBalanceAfter,
    long floatAfterPence,
    MachineStatus status) {

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PlayResult other)) {
      return false;
    }
    return Arrays.equals(spin, other.spin)
        && prize == other.prize
        && paidPence == other.paidPence
        && freePlaysEarned == other.freePlaysEarned
        && freePlayBalanceAfter == other.freePlayBalanceAfter
        && floatAfterPence == other.floatAfterPence
        && status == other.status;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        Arrays.hashCode(spin),
        prize,
        paidPence,
        freePlaysEarned,
        freePlayBalanceAfter,
        floatAfterPence,
        status);
  }

  @Override
  public String toString() {
    return "PlayResult[spin="
        + Arrays.toString(spin)
        + ", prize="
        + prize
        + ", paidPence="
        + paidPence
        + ", freePlaysEarned="
        + freePlaysEarned
        + ", freePlayBalanceAfter="
        + freePlayBalanceAfter
        + ", floatAfterPence="
        + floatAfterPence
        + ", status="
        + status
        + "]";
  }
}
