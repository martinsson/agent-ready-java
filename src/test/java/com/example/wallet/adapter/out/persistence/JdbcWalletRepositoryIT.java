package com.example.wallet.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.wallet.domain.Wallet;
import com.example.wallet.domain.WalletId;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Runs against a real PostgreSQL in Docker (./verify --full). No mocks, no H2 look-alikes. */
@Testcontainers
class JdbcWalletRepositoryIT {

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  private static final WalletId ID = new WalletId("w-1");

  private JdbcWalletRepository repository;

  @BeforeEach
  void setUp() throws SQLException {
    PGSimpleDataSource dataSource = new PGSimpleDataSource();
    dataSource.setUrl(POSTGRES.getJdbcUrl());
    dataSource.setUser(POSTGRES.getUsername());
    dataSource.setPassword(POSTGRES.getPassword());
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute("DROP TABLE IF EXISTS wallet");
      statement.execute(JdbcWalletRepository.SCHEMA);
    }
    repository = new JdbcWalletRepository(dataSource);
  }

  @Test
  void savedWalletCanBeFoundAgain() {
    repository.save(new Wallet(ID, 120));

    assertThat(repository.findById(ID)).contains(new Wallet(ID, 120));
  }

  @Test
  void unknownWalletIsEmpty() {
    assertThat(repository.findById(ID)).isEmpty();
  }

  @Test
  void savingTwiceOverwritesTheBalance() {
    repository.save(new Wallet(ID, 120));
    repository.save(new Wallet(ID, 80));

    assertThat(repository.findById(ID)).contains(new Wallet(ID, 80));
  }
}
