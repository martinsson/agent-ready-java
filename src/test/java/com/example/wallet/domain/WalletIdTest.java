package com.example.wallet.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class WalletIdTest {

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "   "})
  void blankIdsAreRejected(String blank) {
    assertThatIllegalArgumentException().isThrownBy(() -> new WalletId(blank));
  }

  @Test
  void nonBlankIdIsKept() {
    assertThat(new WalletId("w-1").value()).isEqualTo("w-1");
  }
}
