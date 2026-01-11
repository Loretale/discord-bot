package net.loretale.discordbot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.loretale.discordbot.model.Ticket;

import java.awt.*;

public class TicketReopenCommand extends ListenerAdapter {
    public static final String name = "ticket-reopen";
    public static final String description = "Reopen the current ticket";

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || !event.getName().equals(name)) return;

        if (!(event.getChannel() instanceof ThreadChannel thread)) {
            event.reply("This command can only be used inside a ticket channel")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (!Ticket.exists(thread)) {
            event.reply("Thread does not exist.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (Ticket.isOpen(thread)) {
            event.reply("Ticket is already open.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        openTicket(thread, event.getUser());

        event.reply("Ticket closed.")
                .setEphemeral(true)
                .queue();
    }

    private void openTicket(ThreadChannel thread, User closedBy) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Ticket Reopened")
                .setColor(Color.GREEN)
                .addField("Opened by", closedBy.getAsMention(), false);

        thread.sendMessageEmbeds(embed.build()).queue();

        thread.getManager()
                .setLocked(false)
                .setArchived(false)
                .queue();
    }
}
