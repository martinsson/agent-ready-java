package com.example.wallet.application.port.in;

import com.example.wallet.domain.Wallet;
import com.example.wallet.domain.WalletId;

/** Driving port: take money out of an existing wallet. */
public interface WithdrawUseCase {

  Wallet withdraw(WalletId id, long amountCents);
}
