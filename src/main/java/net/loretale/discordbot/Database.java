package net.loretale.discordbot;

import net.loretale.discordbot.migrations.MigrationManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
    private static final String DB_URL = "jdbc:sqlite:" + getDatabasePath();
    private static Connection connection;

    private static String getDatabasePath() {
        // Check for environment variable first
        String envPath = System.getenv("DATABASE_PATH");
        if (envPath != null && !envPath.isEmpty()) {
            return envPath;
        }
        // Fall back to project root with absolute path
        return System.getProperty("user.dir") + "/discordbot.db";
    }

    public static void init() throws SQLException {
        connection = DriverManager.getConnection(DB_URL);

        MigrationManager.migrate(connection);
    }

    public static Connection getConnection() {
        return connection;
    }
}
