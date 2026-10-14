package com.example.wallet.application.port.out;

import com.example.wallet.domain.Wallet;
import com.example.wallet.domain.WalletId;
import java.util.Optional;

/** Driven port: persistence of wallets. Implemented by adapters, faked in tests. */
public interface WalletRepository {

  Optional<Wallet> findById(WalletId id);

  void save(Wallet wallet);
}
