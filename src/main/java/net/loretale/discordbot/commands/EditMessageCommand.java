package net.loretale.discordbot.commands;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.loretale.discordbot.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class EditMessageCommand extends ListenerAdapter {
    public static final String name = "edit";
    public static final String description = "Edit a persistent message";

    public static final String optionIdName = "id";
    public static final String optionIdDesc = "The ID of the message";

    public static final String optionContentName = "content";
    public static final String optionContentDesc = "The new content of the message";

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("message") ||
                event.getSubcommandName() == null ||
                !event.getSubcommandName().equals(name)) return;

        OptionMapping idOption = event.getOption(optionIdName);
        OptionMapping contentOption = event.getOption(optionContentName);

        if (contentOption == null || idOption == null) {
            event.reply("Missing required option(s).")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        String id = idOption.getAsString();
        String content = contentOption.getAsString().replace("\\n", "\n");

        try (PreparedStatement s = Database.getConnection().prepareStatement("""
                SELECT id, channel_id, message_id
                FROM persistent_messages
                WHERE message_id = ?
            """)) {
            s.setString(1, id);
            ResultSet rs = s.executeQuery();

            if (!rs.next()) {
                event.reply("No message found with that ID.")
                        .setEphemeral(true)
                        .queue();
                return;
            }

            String channelId = rs.getString("channel_id");
            String messageId = rs.getString("message_id");
            int dbId = rs.getInt("id");

            TextChannel channel = event.getJDA().getTextChannelById(channelId);

            if (channel == null) {
                event.reply("Channel no longer exists.")
                        .setEphemeral(true)
                        .queue();
                return;
            }

            channel.retrieveMessageById(messageId).queue(message -> {
                message.editMessage(content).queue();

                try (PreparedStatement update = Database.getConnection().prepareStatement("""
                        UPDATE persistent_messages
                        SET content = ?
                        WHERE id = ?
                    """)) {
                    update.setString(1, content);
                    update.setInt(2, dbId);
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                event.reply("Message updated.")
                        .setEphemeral(true)
                        .queue();
            }, failure -> {
                event.reply("Failed to retrieve message (it may have been deleted).")
                        .setEphemeral(true)
                        .queue();
            });
        } catch (SQLException e) {
            e.printStackTrace();
            event.reply("Database error occurred.")
                    .setEphemeral(true)
                    .queue();
        }
    }
}
