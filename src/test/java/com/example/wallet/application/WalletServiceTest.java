package com.example.wallet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.example.wallet.application.fake.InMemoryWalletRepository;
import com.example.wallet.domain.InsufficientFundsException;
import com.example.wallet.domain.Wallet;
import com.example.wallet.domain.WalletId;
import com.example.wallet.domain.WalletNotFoundException;
import org.junit.jupiter.api.Test;

class WalletServiceTest {

  private static final WalletId ID = new WalletId("w-1");

  private final InMemoryWalletRepository repository = new InMemoryWalletRepository();
  private final WalletService service = new WalletService(repository);

  @Test
  void depositCreatesTheWalletOnFirstUse() {
    Wallet result = service.deposit(ID, 100);

    assertThat(result.balanceCents()).isEqualTo(100);
    assertThat(repository.findById(ID)).contains(result);
  }

  @Test
  void depositsAccumulate() {
    service.deposit(ID, 100);
    service.deposit(ID, 50);

    assertThat(repository.findById(ID)).contains(new Wallet(ID, 150));
  }

  @Test
  void withdrawPersistsTheNewBalance() {
    service.deposit(ID, 100);

    Wallet result = service.withdraw(ID, 30);

    assertThat(result.balanceCents()).isEqualTo(70);
    assertThat(repository.findById(ID)).contains(result);
  }

  @Test
  void withdrawFromAnUnknownWalletFails() {
    assertThatExceptionOfType(WalletNotFoundException.class)
        .isThrownBy(() -> service.withdraw(ID, 10));
    assertThat(repository.findById(ID)).isEmpty();
  }

  @Test
  void failedWithdrawalLeavesTheStoredBalanceUntouched() {
    service.deposit(ID, 50);

    assertThatExceptionOfType(InsufficientFundsException.class)
        .isThrownBy(() -> service.withdraw(ID, 51));
    assertThat(repository.findById(ID)).contains(new Wallet(ID, 50));
  }
}
