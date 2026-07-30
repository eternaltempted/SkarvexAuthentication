package org.skarvex.auth.core.repository.jdbc;

import org.skarvex.auth.core.model.RestrictedIp;
import org.skarvex.auth.core.repository.RestrictedIpRepository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Optional;

public class JdbcRestrictedIpRepository implements RestrictedIpRepository {

    private final DataSource dataSource;

    public JdbcRestrictedIpRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void block(RestrictedIp restrictedIp) {
        String sql = "INSERT INTO restricted_ips(ip, blocked_until) VALUES(?,?)";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, restrictedIp.ip());
            statement.setTimestamp(2, Timestamp.from(restrictedIp.blockedUntil()));
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<RestrictedIp> findByIp(String ip) {
        String sql = "SELECT ip, blocked_until FROM restricted_ips WHERE ip = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ip);

            ResultSet resultSet = statement.executeQuery();

            if (!resultSet.next()) return Optional.empty();

            return Optional.of(new RestrictedIp(
                    resultSet.getString("ip"),
                    resultSet.getTimestamp("blocked_until").toInstant()
            ));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void unblock(String ip) {
        String sql = "DELETE FROM restricted_ips WHERE ip = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, ip);
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
