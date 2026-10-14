package com.example.wallet.application.fake;

import com.example.wallet.application.port.out.WalletRepository;
import com.example.wallet.domain.Wallet;
import com.example.wallet.domain.WalletId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Hand-written fake of the persistence port. This is what we use instead of Mockito. */
public final class InMemoryWalletRepository implements WalletRepository {

  private final Map<WalletId, Wallet> store = new HashMap<>();

  @Override
  public Optional<Wallet> findById(WalletId id) {
    return Optional.ofNullable(store.get(id));
  }

  @Override
  public void save(Wallet wallet) {
    store.put(wallet.id(), wallet);
  }
}
