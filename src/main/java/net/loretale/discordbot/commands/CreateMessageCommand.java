package net.loretale.discordbot.commands;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.loretale.discordbot.Database;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CreateMessageCommand extends ListenerAdapter {
    public static final String name = "create";
    public static final String description = "Create a persistent message";

    public static final String optionContentName = "content";
    public static final String optionContentDesc = "The content of the message";

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("message") ||
                event.getSubcommandName() == null ||
                !event.getSubcommandName().equals(name)) return;

        if (!event.isFromGuild()) {
            event.reply("This command must be used in the server.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        TextChannel channel = event.getChannel().asTextChannel();

        OptionMapping contentOption = event.getOption(optionContentName);

        if (contentOption == null) {
            event.reply("Missing content.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        String content = contentOption.getAsString().replace("\\n", "\n");;

        channel.sendMessage(content).queue(message -> {
            try (PreparedStatement s = Database.getConnection().prepareStatement("""
                INSERT INTO persistent_messages (channel_id, message_id, content)
                VALUES (?, ?, ?)
            """)) {
                s.setString(1, channel.getId());
                s.setString(2, message.getId());
                s.setString(3, content);
                s.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            event.reply("Persistent message created!")
                    .setEphemeral(true)
                    .queue();
        });
    }
}
