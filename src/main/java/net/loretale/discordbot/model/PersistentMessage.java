package net.loretale.discordbot.model;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.loretale.discordbot.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PersistentMessage {
    public static void createMessage(String message, TextChannel channel, SlashCommandInteractionEvent event) {
        String content = message.replace("\\n", "\n");

        channel.sendMessage(content).queue(m -> {
            try (PreparedStatement s = Database.getConnection().prepareStatement("""
                INSERT INTO persistent_messages (channel_id, message_id)
                VALUES (?, ?)
            """)) {
                s.setString(1, channel.getId());
                s.setString(2, m.getId());
                s.executeUpdate();

                event.reply("Persistent message created!")
                        .setEphemeral(true)
                        .queue();
            } catch (SQLException e) {
                e.printStackTrace();

                event.reply("Something went wrong creating your message.")
                        .setEphemeral(true)
                        .queue();
            }
        });
    }

    public static String getChannelId(String messageId) {
        try (PreparedStatement s = Database.getConnection().prepareStatement("""
                SELECT channel_id
                FROM persistent_messages
                WHERE message_id = ?
            """)) {
            s.setString(1, messageId);
            ResultSet rs = s.executeQuery();

            if (!rs.next()) {
                return null;
            }

            return rs.getString("channel_id");
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
