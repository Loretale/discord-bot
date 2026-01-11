package net.loretale.discordbot.migrations;

import java.sql.*;
import java.util.List;

public class MigrationManager {
    private static final List<Migration> MIGRATIONS = List.of(
            new V1_PersistentMessages(),
            new V2_Tickets(),
            new V3_Applications()
    );

    public static void migrate(Connection connection) throws SQLException {
        try (Statement s = connection.createStatement()) {
            s.execute("""
                    CREATE TABLE IF NOT EXISTS schema_version (
                        version INTEGER PRIMARY KEY
                    )
                    """);
        }

        int currentVersion = getCurrentVersion(connection);

        for (Migration migration : MIGRATIONS) {
            if (migration.version() > currentVersion) {
                connection.setAutoCommit(false);

                try {
                    migration.apply(connection);
                    setVersion(connection, migration.version());
                    connection.commit();
                } catch (SQLException e) {
                    connection.rollback();
                    throw e;
                } finally {
                    connection.setAutoCommit(true);
                }
            }
        }
    }

    private static int getCurrentVersion(Connection connection) throws SQLException {
        try (Statement s = connection.createStatement()) {
            ResultSet rs = s.executeQuery("SELECT MAX(version) FROM schema_version");
            rs.next();
            return rs.getInt(1);
        }
    }

    private static void setVersion(Connection connection, int version) throws SQLException {
        try (PreparedStatement s = connection.prepareStatement("""
                INSERT INTO schema_version (version) VALUES (?)
                """)) {
            s.setInt(1, version);
            s.executeUpdate();
        }
    }
}
