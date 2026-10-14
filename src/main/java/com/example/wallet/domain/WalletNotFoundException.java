package com.example.wallet.domain;

/** Raised when an operation targets a wallet that does not exist. */
public final class WalletNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public WalletNotFoundException(WalletId id) {
    super("wallet not found: " + id.value());
  }
}
