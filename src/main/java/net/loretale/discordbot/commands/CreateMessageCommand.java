package net.loretale.discordbot.commands;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.loretale.discordbot.model.PersistentMessage;
import net.loretale.discordbot.util.Permissions;

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

        Member member = event.getMember();
        if (!Permissions.isAdmin(member)) {
            event.reply("You do not have permission to do this.")
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

        String content = contentOption.getAsString();

        PersistentMessage.createMessage(content, channel, event);
    }
}
