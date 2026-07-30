package org.skarvex.auth.core.repository.jdbc;

import org.skarvex.auth.core.model.User;
import org.skarvex.auth.core.repository.UserRepository;

import javax.sql.DataSource;import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class JdbcUserRepository
        implements UserRepository {

    private final DataSource dataSource;
    public JdbcUserRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<User> findById(UUID uuid) {

        String sql = "SELECT * FROM users WHERE uuid = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, uuid.toString());

            ResultSet resultSet = statement.executeQuery();

            if (!resultSet.next())
                return Optional.empty();

            return Optional.of(
                    new User(
                            UUID.fromString(resultSet.getString("uuid")),
                            resultSet.getString("password_hash"),
                            resultSet.getString("registration_ip"),
                            resultSet.getString("last_login_ip"),
                            resultSet.getBoolean("remember_session")
                    )
            );

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void save(User user) {
        String sql = "INSERT INTO users(uuid, password_hash, registration_ip, last_login_ip, remember_session) VALUES(?,?,?,?,?)";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, user.id().toString());
            statement.setString(2, user.passwordHash());
            statement.setString(3, user.registrationIp());
            statement.setString(4, user.lastLoginIp());
            statement.setBoolean(5, user.autoLogin());

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean updatePassword(UUID uuid, String passwordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE uuid = ?";

        try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, passwordHash);
            statement.setString(2, uuid.toString());

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean updateLastLoginIp(UUID uuid, String ip) {

        String sql = "UPDATE users SET last_login_ip = ? WHERE uuid = ?";

        try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, ip);
            statement.setString(2, uuid.toString());

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setAutoLogin(UUID uuid, boolean rememberSession) {

        String sql = "UPDATE users SET remember_session = ? WHERE uuid = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setBoolean(1, rememberSession);
            statement.setString(2, uuid.toString());

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
