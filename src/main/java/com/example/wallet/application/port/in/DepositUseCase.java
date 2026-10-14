package com.example.wallet.application.port.in;

import com.example.wallet.domain.Wallet;
import com.example.wallet.domain.WalletId;

/** Driving port: put money into a wallet, creating it on first deposit. */
public interface DepositUseCase {

  Wallet deposit(WalletId id, long amountCents);
}
