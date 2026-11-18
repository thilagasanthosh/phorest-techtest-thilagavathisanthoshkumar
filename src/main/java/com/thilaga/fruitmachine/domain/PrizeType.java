package com.thilaga.fruitmachine.domain;

/** The one outcome a spin is classified into (BR-04). Only the highest-priority one ever pays. */
public enum PrizeType {
  JACKPOT,
  FULL_HOUSE,
  SMALL_PRIZE,
  NONE
}
