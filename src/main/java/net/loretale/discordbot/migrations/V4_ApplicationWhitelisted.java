package net.loretale.discordbot.migrations;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class V4_ApplicationWhitelisted implements Migration {

    @Override
    public int version() {
        return 4;
    }

    @Override
    public void apply(Connection connection) throws SQLException {
        try (Statement s = connection.createStatement()) {
            s.execute("""
            ALTER TABLE applications
            ADD COLUMN IF NOT EXISTS whitelisted BOOLEAN NOT NULL DEFAULT FALSE
        """);
        }
    }
}
