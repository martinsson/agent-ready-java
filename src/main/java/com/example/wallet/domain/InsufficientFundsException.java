package com.example.wallet.domain;

/** Raised when a withdrawal exceeds the wallet balance. */
public final class InsufficientFundsException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public InsufficientFundsException(WalletId id, long balanceCents, long requestedCents) {
    super(
        "wallet "
            + id.value()
            + " has "
            + balanceCents
            + " cents, cannot withdraw "
            + requestedCents);
  }
}
