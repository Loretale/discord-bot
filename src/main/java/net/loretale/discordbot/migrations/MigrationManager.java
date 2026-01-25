package net.loretale.discordbot.migrations;

import java.sql.*;
import java.util.List;

public class MigrationManager {
    private static final String APP_NAME = "discordbot";

    private static final List<Migration> MIGRATIONS = List.of(
            new V1_PersistentMessages(),
            new V2_Tickets(),
            new V3_Applications(),
            new V4_ApplicationWhitelisted()
    );

    public static void migrate(Connection connection) throws SQLException {
        try (Statement s = connection.createStatement()) {
            s.execute("""
                CREATE TABLE IF NOT EXISTS schema_migrations (
                    app_name TEXT NOT NULL,
                    version INTEGER NOT NULL,
                    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (app_name, version)
                );
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
        try (PreparedStatement s = connection.prepareStatement("""
                SELECT COALESCE(MAX(version), 0)
                FROM schema_migrations
                WHERE app_name = ?
            """)) {
            s.setString(1, APP_NAME);

            ResultSet rs = s.executeQuery();
            rs.next();
            return rs.getInt(1);
        }
    }

    private static void setVersion(Connection connection, int version) throws SQLException {
        try (PreparedStatement s = connection.prepareStatement("""
                INSERT INTO schema_migrations (app_name, version)
                VALUES (?, ?)
                """)) {
            s.setString(1, APP_NAME);
            s.setInt(2, version);
            s.executeUpdate();
        }
    }
}
