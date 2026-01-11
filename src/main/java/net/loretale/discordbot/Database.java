package net.loretale.discordbot;

import net.loretale.discordbot.migrations.MigrationManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

    private static Connection connection;

    private static String getJdbcUrl() {
        String url = System.getenv("DATABASE_URL");
        if (url == null || url.isEmpty()) {
            throw new IllegalStateException("DATABASE_URL environment variable is not set");
        }
        return url;
    }

    private static String getDbUser() {
        String user = System.getenv("DATABASE_USER");
        if (user == null || user.isEmpty()) {
            throw new IllegalStateException("DATABASE_USER environment variable is not set");
        }
        return user;
    }

    private static String getDbPassword() {
        String password = System.getenv("DATABASE_PASSWORD");
        if (password == null) {
            throw new IllegalStateException("DATABASE_PASSWORD environment variable is not set");
        }
        return password;
    }

    public static void init() throws SQLException {
        connection = DriverManager.getConnection(
                getJdbcUrl(),
                getDbUser(),
                getDbPassword()
        );

        MigrationManager.migrate(connection);
    }

    public static Connection getConnection() {
        if (connection == null) {
            throw new IllegalStateException("Database not initialized");
        }
        return connection;
    }
}

