package net.loretale.discordbot.migrations;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class V1_PersistentMessages implements Migration {
    @Override
    public int version() { return 1; }

    @Override
    public void apply(Connection connection) throws SQLException {
        try (Statement s = connection.createStatement()) {
            s.execute("""
                    CREATE TABLE IF NOT EXISTS persistent_messages (
                        id SERIAL PRIMARY KEY,
                        channel_id TEXT NOT NULL,
                        message_id TEXT NOT NULL
                    )
                    """);
        }
    }
}
