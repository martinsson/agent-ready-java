package com.example.wallet.application;

import com.example.wallet.application.port.in.DepositUseCase;
import com.example.wallet.application.port.in.WithdrawUseCase;
import com.example.wallet.application.port.out.WalletRepository;
import com.example.wallet.domain.Wallet;
import com.example.wallet.domain.WalletId;
import com.example.wallet.domain.WalletNotFoundException;

/** Application service orchestrating the wallet use cases. Contains no business rules itself. */
public final class WalletService implements DepositUseCase, WithdrawUseCase {

  private final WalletRepository wallets;

  public WalletService(WalletRepository wallets) {
    this.wallets = wallets;
  }

  @Override
  public Wallet deposit(WalletId id, long amountCents) {
    Wallet updated = wallets.findById(id).orElseGet(() -> Wallet.empty(id)).deposit(amountCents);
    wallets.save(updated);
    return updated;
  }

  @Override
  public Wallet withdraw(WalletId id, long amountCents) {
    Wallet current = wallets.findById(id).orElseThrow(() -> new WalletNotFoundException(id));
    Wallet updated = current.withdraw(amountCents);
    wallets.save(updated);
    return updated;
  }
}
