package com.thilaga.fruitmachine.domain;

/** Everything a paid or free play reports back (AC-15). */
public record PlayResult(
    int[] spin,
    PrizeType prize,
    long paidPence,
    long freePlaysEarned,
    long freePlayBalanceAfter,
    long floatAfterPence,
    MachineStatus status) {}
