package com.example.wallet.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.LongRange;

/** Property-based tests: jqwik explores inputs an agent (or a human) would not think to write. */
class WalletPropertiesTest {

  private static final WalletId ID = new WalletId("w-prop");

  @Property(tries = 200)
  void depositThenWithdrawRestoresTheBalance(
      @ForAll @LongRange(min = 0, max = 1_000_000_000L) long balance,
      @ForAll @LongRange(min = 1, max = 1_000_000_000L) long amount) {
    Wallet start = new Wallet(ID, balance);

    assertThat(start.deposit(amount).withdraw(amount)).isEqualTo(start);
  }

  @Property(tries = 200)
  void withdrawalSucceedsExactlyWhenFundsSuffice(
      @ForAll @LongRange(min = 0, max = 1_000_000_000L) long balance,
      @ForAll @LongRange(min = 1, max = 2_000_000_000L) long amount) {
    Wallet start = new Wallet(ID, balance);

    if (amount > balance) {
      assertThatExceptionOfType(InsufficientFundsException.class)
          .isThrownBy(() -> start.withdraw(amount));
    } else {
      assertThat(start.withdraw(amount).balanceCents()).isEqualTo(balance - amount);
    }
  }
}
