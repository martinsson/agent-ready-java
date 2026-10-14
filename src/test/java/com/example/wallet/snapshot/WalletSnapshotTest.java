package com.example.wallet.snapshot;

import com.diffplug.selfie.Selfie;
import com.example.wallet.domain.Wallet;
import com.example.wallet.domain.WalletId;
import org.junit.jupiter.api.Test;

/**
 * Selfie snapshot test: the expected value lives inline in the source. When behaviour changes on
 * purpose, run {@code ./verify --snapshots} (Selfie overwrite mode) and review the diff. Use this
 * style to pin down the current output of legacy code before refactoring it (characterization).
 */
class WalletSnapshotTest {

  @Test
  void walletAfterDepositAndWithdrawal() {
    Wallet wallet = Wallet.empty(new WalletId("w-1")).deposit(500).withdraw(150);

    Selfie.expectSelfie(wallet.toString()).toBe("Wallet[id=WalletId[value=w-1], balanceCents=350]");
  }
}
