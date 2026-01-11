package net.loretale.discordbot.commands;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.loretale.discordbot.model.Ticket;

public class TicketAddCommand extends ListenerAdapter {
    public static final String name = "ticket-add";
    public static final String description = "Add a user to a ticket";

    public static final String optionUserName = "user";
    public static final String optionUserDesc = "The user to be added";

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || !event.getName().equals(name)) return;

        if (!(event.getChannel() instanceof ThreadChannel thread) || Ticket.exists(thread)) {
            event.reply("This command can only be used inside a ticket channel")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        OptionMapping userOption = event.getOption(optionUserName);

        if (userOption == null) {
            event.reply("You must specify a user to add.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        Member target = userOption.getAsMember();

        if (target == null) {
            event.reply("User not found.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (!Ticket.isOpen(thread)) {
            event.reply("Ticket is close or this is not a ticket thread.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        thread.addThreadMember(target).queue(
                _ -> thread.sendMessage("Added " + target.getAsMention() + " to the ticket.").queue(),
                _ -> thread
                        .sendMessage("Failed to add user " + target.getAsMention() + " to the ticket.")
                        .queue()
        );

        event.reply("Done.")
                .setEphemeral(true)
                .queue();
    }
}
