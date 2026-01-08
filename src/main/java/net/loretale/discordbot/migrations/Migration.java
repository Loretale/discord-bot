package net.loretale.discordbot.migrations;

import java.sql.Connection;
import java.sql.SQLException;

public interface Migration {
    int version();
    void apply(Connection connection) throws SQLException;
}
