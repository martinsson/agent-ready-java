package com.example.wallet.adapter.out.persistence;

/** Unchecked wrapper for storage failures, so ports stay free of {@code java.sql}. */
public final class PersistenceException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public PersistenceException(String message, Throwable cause) {
    super(message, cause);
  }
}
