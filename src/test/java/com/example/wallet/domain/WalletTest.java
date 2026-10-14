package com.example.wallet.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class WalletTest {

  private static final WalletId ID = new WalletId("w-1");

  @Test
  void depositIncreasesBalance() {
    assertThat(Wallet.empty(ID).deposit(250).balanceCents()).isEqualTo(250);
  }

  @Test
  void withdrawDecreasesBalance() {
    assertThat(new Wallet(ID, 500).withdraw(200).balanceCents()).isEqualTo(300);
  }

  @Test
  void withdrawingTheWholeBalanceIsAllowed() {
    assertThat(new Wallet(ID, 100).withdraw(100).balanceCents()).isZero();
  }

  @Test
  void withdrawingMoreThanTheBalanceFails() {
    assertThatExceptionOfType(InsufficientFundsException.class)
        .isThrownBy(() -> new Wallet(ID, 100).withdraw(101))
        .withMessageContaining("w-1")
        .withMessageContaining("100")
        .withMessageContaining("101");
  }

  @ParameterizedTest
  @ValueSource(longs = {0, -1, Long.MIN_VALUE})
  void depositRejectsNonPositiveAmounts(long amount) {
    assertThatIllegalArgumentException().isThrownBy(() -> Wallet.empty(ID).deposit(amount));
  }

  @ParameterizedTest
  @ValueSource(longs = {0, -1, Long.MIN_VALUE})
  void withdrawRejectsNonPositiveAmounts(long amount) {
    assertThatIllegalArgumentException().isThrownBy(() -> new Wallet(ID, 100).withdraw(amount));
  }

  @Test
  void negativeBalanceIsRejected() {
    assertThatIllegalArgumentException().isThrownBy(() -> new Wallet(ID, -1));
  }

  @Test
  void zeroBalanceIsValid() {
    assertThat(new Wallet(ID, 0).balanceCents()).isZero();
  }

  @Test
  void depositOverflowIsRejected() {
    assertThatExceptionOfType(ArithmeticException.class)
        .isThrownBy(() -> new Wallet(ID, Long.MAX_VALUE).deposit(1));
  }
}
