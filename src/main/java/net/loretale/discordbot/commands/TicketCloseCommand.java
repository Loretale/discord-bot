package net.loretale.discordbot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.loretale.discordbot.Database;

import java.awt.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TicketCloseCommand extends ListenerAdapter {
    public static final String name = "ticket-close";
    public static final String description = "Close the current ticket";

    public static final String optionReasonName = "reason";
    public static final String optionReasonDesc = "The reason for closing the ticket";

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || !event.getName().equals(name)) return;

        if (!(event.getChannel() instanceof ThreadChannel thread)) {
            event.reply("This command can only be used inside a ticket channel")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        OptionMapping reasonOption = event.getOption(optionReasonName);
        if (reasonOption == null) {
            event.reply("You must provide a reason.")
                    .setEphemeral(true)
                    .queue();
            return;
        }


        if (!isTicketOpen(thread.getId())) {
            event.reply("Thread is not a ticket or ticket is already closed.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        String reason = reasonOption.getAsString();

        event.reply("Ticket closed.")
                .setEphemeral(true)
                .queue();

        closeTicket(thread, reason);
    }

    private boolean isTicketOpen(String threadId) {
        try (PreparedStatement ps = Database.getConnection().prepareStatement("""
            SELECT status FROM tickets WHERE thread_id = ?
        """)) {
            ps.setString(1, threadId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && "OPEN".equals(rs.getString("status"));
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void closeTicket(ThreadChannel thread, String reason) {
        try (PreparedStatement ps = Database.getConnection().prepareStatement("""
        UPDATE tickets
        SET status = 'CLOSED',
            close_reason = ?
        WHERE thread_id = ?
    """)) {
            ps.setString(1, reason);
            ps.setString(2, thread.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Ticket Closed")
                .setColor(Color.RED)
                .addField("Reason", reason, false);

        thread.sendMessageEmbeds(embed.build()).queue(_ -> {
            thread.getManager()
                    .setLocked(true)
                    .setArchived(true)
                    .queue();
        });
    }
}
