package com.example.wallet.domain;

/** A wallet holding a non-negative balance, in cents. Immutable: operations return a new wallet. */
public record Wallet(WalletId id, long balanceCents) {

  public Wallet {
    if (balanceCents < 0) {
      throw new IllegalArgumentException("balance must not be negative: " + balanceCents);
    }
  }

  public static Wallet empty(WalletId id) {
    return new Wallet(id, 0);
  }

  public Wallet deposit(long amountCents) {
    requirePositive(amountCents);
    return new Wallet(id, Math.addExact(balanceCents, amountCents));
  }

  public Wallet withdraw(long amountCents) {
    requirePositive(amountCents);
    if (amountCents > balanceCents) {
      throw new InsufficientFundsException(id, balanceCents, amountCents);
    }
    return new Wallet(id, balanceCents - amountCents);
  }

  private static void requirePositive(long amountCents) {
    if (amountCents <= 0) {
      throw new IllegalArgumentException("amount must be positive: " + amountCents);
    }
  }
}
