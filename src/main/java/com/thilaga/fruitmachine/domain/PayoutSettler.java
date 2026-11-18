package com.thilaga.fruitmachine.domain;

/**
 * Pays a prize from the float and turns any shortfall into free plays (BR-05, BR-06, D2, D3, D5,
 * D9). The caller decides the stake: 0 for a free play (D7), the play cost for a paid play.
 */
public final class PayoutSettler {

  private static final int FULL_HOUSE_DIVISOR = 2;
  private static final int SMALL_PRIZE_MULTIPLIER = 5;

  private PayoutSettler() {}

  public static Settlement settle(
      long floatBeforeStakePence, long stakePence, long playCostPence, PrizeType prize) {
    long floatAfterStake = floatBeforeStakePence + stakePence;

    long nominal =
        switch (prize) {
          case JACKPOT -> floatAfterStake;
          case FULL_HOUSE -> floatAfterStake / FULL_HOUSE_DIVISOR;
          case SMALL_PRIZE -> SMALL_PRIZE_MULTIPLIER * playCostPence;
          case NONE -> 0;
        };

    long paid = Math.min(nominal, floatAfterStake);
    long shortfall = nominal - paid;
    // A jackpot pays the entire float, so shortfall is always 0 for it already; the explicit
    // guard matches BR-06/AC-24's "a jackpot never creates free plays" directly.
    long freePlaysEarned =
        (prize == PrizeType.JACKPOT || shortfall == 0) ? 0 : ceilDiv(shortfall, playCostPence);

    return new Settlement(paid, freePlaysEarned, floatAfterStake - paid);
  }

  private static long ceilDiv(long numerator, long denominator) {
    return (numerator + denominator - 1) / denominator;
  }
}
