package com.thilaga.fruitmachine.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** API-04 body: {"amountPence": n}, n > 0 (AC-20). */
public record RefillRequest(@NotNull @Positive Long amountPence) {}
