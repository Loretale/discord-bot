package net.loretale.discordbot.migrations;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class V3_Applications implements Migration {
    @Override
    public int version() {
        return 3;
    }

    @Override
    public void apply(Connection connection) throws SQLException {
        try (Statement s = connection.createStatement()) {
            s.execute("""
                CREATE TABLE IF NOT EXISTS applications (
                    id SERIAL PRIMARY KEY,
                    thread_id TEXT NOT NULL,
                    user_id TEXT NOT NULL,
                    username TEXT NOT NULL,
                    age TEXT NOT NULL,
                    metagaming_powergaming TEXT NOT NULL,
                    persona TEXT NOT NULL,
                    prompt TEXT NOT NULL,
                    status TEXT NOT NULL
                )
            """);
        }
    }
}
