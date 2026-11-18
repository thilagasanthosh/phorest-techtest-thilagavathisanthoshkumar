package com.thilaga.fruitmachine.api;

import jakarta.validation.constraints.NotNull;

/** API-02 body: {"useFreePlay": false|true}. */
public record PlayRequest(@NotNull Boolean useFreePlay) {}
