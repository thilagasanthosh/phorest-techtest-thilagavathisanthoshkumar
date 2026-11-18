package com.thilaga.fruitmachine.domain;

/**
 * Lifetime counters of one machine (ERD-01 totals, AC-28). Updated in the same transaction as each
 * play or refill.
 */
public record MachineTotals(long totalPlays, long totalPaidPence, long totalRefilledPence) {}
