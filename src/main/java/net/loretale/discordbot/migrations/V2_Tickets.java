package net.loretale.discordbot.migrations;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class V2_Tickets implements Migration {
    @Override
    public int version() {
        return 2;
    }

    @Override
    public void apply(Connection connection) throws SQLException {
        try (Statement s = connection.createStatement()) {
            s.execute("""
                CREATE TABLE IF NOT EXISTS discord_tickets (
                    id SERIAL PRIMARY KEY,
                    thread_id TEXT NOT NULL,
                    user_id TEXT NOT NULL,
                    subject TEXT NOT NULL,
                    description TEXT NOT NULL,
                    status TEXT NOT NULL,
                    close_reason TEXT
                )
            """);
        }
    }
}
