package net.loretale.discordbot.commands;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.loretale.discordbot.model.PersistentMessage;
import net.loretale.discordbot.util.Permissions;

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

        Member member = event.getMember();
        if (!Permissions.isAdmin(member)) {
            event.reply("You do not have permission to do this.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        OptionMapping idOption = event.getOption(optionIdName);
        OptionMapping contentOption = event.getOption(optionContentName);

        if (contentOption == null || idOption == null) {
            event.reply("Missing required option(s).")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        String messageId = idOption.getAsString();
        String content = contentOption.getAsString().replace("\\n", "\n");

        String channelId = PersistentMessage.getChannelId(messageId);

        if (channelId == null) {
            event.reply("No message found with that ID.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        TextChannel channel = event.getJDA().getTextChannelById(channelId);

        if (channel == null) {
            event.reply("Channel no longer exists.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        channel.retrieveMessageById(messageId).queue(message -> {
            message.editMessage(content).queue(
                    __ -> {
                        event.reply("Message updated.")
                                .setEphemeral(true)
                                .queue();
                    },
                    __ -> {
                        event.reply("Something went wrong while trying to edit the message.")
                                .setEphemeral(true)
                                .queue();
                    }
            );

            }, __ -> {
                event.reply("Failed to retrieve message (it may have been deleted).")
                        .setEphemeral(true)
                        .queue();
            });
    }
}
