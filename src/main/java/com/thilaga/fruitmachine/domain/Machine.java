package com.thilaga.fruitmachine.domain;

/** One configured fruit machine: its float, free-play balance, and the play it offers. */
public final class Machine {

  private final MachineConfig config;
  private final FloatPolicy policy;
  private long floatPence;
  private long freePlayBalance;

  public Machine(MachineConfig config) {
    this(config, config.startingFloatPence(), 0);
  }

  /**
   * Rehydrates a machine at its current float and free-play balance - these drift from {@link
   * MachineConfig#startingFloatPence()} as soon as the machine is played, so persistence (Issue 3)
   * reloads a machine through this constructor, not the single-arg one.
   */
  public Machine(MachineConfig config, long floatPence, long freePlayBalance) {
    this.config = config;
    this.policy = config.floatPolicyType().toPolicy();
    this.floatPence = floatPence;
    this.freePlayBalance = freePlayBalance;
  }

  public PlayResult play(boolean useFreePlay, SpinGenerator generator) {
    if (useFreePlay && freePlayBalance == 0) {
      throw new NoFreePlayAvailableException();
    }
    if (!policy.canPlay(floatPence, config.playCostPence())) {
      throw new MachineNeedsRefillException();
    }

    if (useFreePlay) {
      freePlayBalance--; // D7: one credit is used, no stake is added.
    }
    long stake = useFreePlay ? 0 : config.playCostPence();

    int[] spin = generator.generate(config.slots(), config.colours().size());
    SpinProfile profile = SpinProfile.of(spin);
    PrizeType prize = PrizeEvaluator.evaluate(profile, config);
    Settlement settlement = PayoutSettler.settle(floatPence, stake, config.playCostPence(), prize);

    floatPence = settlement.floatAfterPence();
    freePlayBalance += settlement.freePlaysEarned();

    return new PlayResult(
        spin,
        prize,
        settlement.nominalPence(),
        settlement.paidPence(),
        settlement.freePlaysEarned(),
        freePlayBalance,
        floatPence,
        policy.status(floatPence, config.playCostPence()));
  }

  public void refill(long amountPence) {
    if (amountPence <= 0) {
      throw new IllegalArgumentException("refill amount must be greater than 0");
    }
    floatPence += amountPence;
  }

  public MachineConfig config() {
    return config;
  }

  public long floatPence() {
    return floatPence;
  }

  public long freePlayBalance() {
    return freePlayBalance;
  }

  public MachineStatus status() {
    return policy.status(floatPence, config.playCostPence());
  }

  /** The float the policy holds back before a play (D10); 0 under NONE. */
  public long reservePence() {
    return policy.reservePence(config.playCostPence());
  }
}
