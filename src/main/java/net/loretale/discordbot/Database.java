package net.loretale.discordbot;

import net.loretale.discordbot.migrations.MigrationManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
    private static final String DB_URL = "jdbc:sqlite:discordbot.db";
    private static Connection connection;

    public static void init() throws SQLException {
        connection = DriverManager.getConnection(DB_URL);

        MigrationManager.migrate(connection);
    }

    public static Connection getConnection() {
        return connection;
    }
}
