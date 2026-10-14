package com.example.wallet.domain;

/** Identity of a {@link Wallet}. */
public record WalletId(String value) {

  public WalletId {
    if (value.isBlank()) {
      throw new IllegalArgumentException("wallet id must not be blank");
    }
  }
}
