package com.example.wallet.adapter.out.persistence;

import com.example.wallet.application.port.out.WalletRepository;
import com.example.wallet.domain.Wallet;
import com.example.wallet.domain.WalletId;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import javax.sql.DataSource;

/** JDBC adapter for the {@link WalletRepository} port. Verified by JdbcWalletRepositoryIT. */
public final class JdbcWalletRepository implements WalletRepository {

  public static final String SCHEMA =
      "CREATE TABLE IF NOT EXISTS wallet (id TEXT PRIMARY KEY, balance_cents BIGINT NOT NULL)";

  private static final String SELECT = "SELECT balance_cents FROM wallet WHERE id = ?";

  private static final String UPSERT =
      "INSERT INTO wallet (id, balance_cents) VALUES (?, ?) "
          + "ON CONFLICT (id) DO UPDATE SET balance_cents = EXCLUDED.balance_cents";

  private final DataSource dataSource;

  public JdbcWalletRepository(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public Optional<Wallet> findById(WalletId id) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT)) {
      statement.setString(1, id.value());
      try (ResultSet rows = statement.executeQuery()) {
        if (rows.next()) {
          return Optional.of(new Wallet(id, rows.getLong("balance_cents")));
        }
        return Optional.empty();
      }
    } catch (SQLException e) {
      throw new PersistenceException("could not load wallet " + id.value(), e);
    }
  }

  @Override
  public void save(Wallet wallet) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(UPSERT)) {
      statement.setString(1, wallet.id().value());
      statement.setLong(2, wallet.balanceCents());
      statement.executeUpdate();
    } catch (SQLException e) {
      throw new PersistenceException("could not save wallet " + wallet.id().value(), e);
    }
  }
}
