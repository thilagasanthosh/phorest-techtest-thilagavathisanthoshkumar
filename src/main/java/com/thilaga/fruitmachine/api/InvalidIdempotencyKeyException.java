package com.thilaga.fruitmachine.api;

/** An Idempotency-Key header that is empty or longer than the 128 characters the store holds. */
class InvalidIdempotencyKeyException extends RuntimeException {

  InvalidIdempotencyKeyException() {
    super("Idempotency-Key must be 1 to 128 characters");
  }
}
