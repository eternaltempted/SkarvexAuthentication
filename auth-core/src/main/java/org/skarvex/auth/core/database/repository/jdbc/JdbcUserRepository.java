package org.skarvex.auth.core.database.repository.jdbc;

import org.skarvex.auth.core.model.User;
import org.skarvex.auth.core.database.repository.UserRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class JdbcUserRepository implements UserRepository {

    private final DataSource dataSource;
    public JdbcUserRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<User> findById(UUID uuid) {

        String sql = "SELECT uuid, password_hash, registration_ip, last_login_ip, auto_login, email, is_email_verified FROM users WHERE uuid = ?";

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
                            resultSet.getBoolean("auto_login"),
                            resultSet.getString("email"),
                            resultSet.getBoolean("is_email_verified")
                    )
            );

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user: " + uuid, e);
        }
    }

    @Override
    public void save(User user) {
        String sql = "INSERT INTO users(uuid, password_hash, registration_ip, last_login_ip, auto_login) VALUES(?,?,?,?,?)";

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
            throw new RuntimeException("Failed to save user: " + user.id().toString(), e);
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
            throw new RuntimeException("Failed to update password for user: " + uuid, e);
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
            throw new RuntimeException("Failed to update last login ip for user: " + uuid, e);
        }
    }

    @Override
    public boolean setAutoLogin(UUID uuid, boolean isAutoLogged) {

        String sql = "UPDATE users SET auto_login = ? WHERE uuid = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setBoolean(1, isAutoLogged);
            statement.setString(2, uuid.toString());

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update auto-login for user " + uuid, e);
        }
    }

    @Override
    public boolean updateEmail(UUID uuid, String email) {
        boolean isVerified = (email != null && !email.isBlank());
        String sql = "UPDATE users SET email = ?, is_email_verified = ? WHERE uuid = ?";

        try (Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, email);
            statement.setBoolean(2, isVerified);
            statement.setString(3, uuid.toString());

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update email for user: " + uuid, e);
        }
    }

    @Override
    public boolean isEmailLinked(String email) {
        String sql = "SELECT EXISTS(SELECT 1 FROM users WHERE email = ?)";

        try (Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet set = statement.executeQuery()) {
                if (set.next()) return set.getBoolean(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error to check the email: ", e);
        }
        return false;
    }

}
