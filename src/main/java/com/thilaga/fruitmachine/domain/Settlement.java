package com.thilaga.fruitmachine.domain;

/** Result of settling one prize against the float (BR-05, BR-06). */
public record Settlement(
    long paidPence, long nominalPence, long freePlaysEarned, long floatAfterPence) {}
