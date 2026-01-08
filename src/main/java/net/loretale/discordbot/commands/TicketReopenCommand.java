package net.loretale.discordbot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.loretale.discordbot.Database;

import java.awt.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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

        if (!isTicketClosed(thread.getId())) {
            event.reply("Thread is not a ticket or ticket is already open.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        openTicket(thread, event.getUser());

        event.reply("Ticket closed.")
                .setEphemeral(true)
                .queue();
    }

    private boolean isTicketClosed(String threadId) {
        try (PreparedStatement ps = Database.getConnection().prepareStatement("""
            SELECT status FROM tickets WHERE thread_id = ?
        """)) {
            ps.setString(1, threadId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && "CLOSED".equals(rs.getString("status"));
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void openTicket(ThreadChannel thread, User closedBy) {
        // Update DB
        try (PreparedStatement ps = Database.getConnection().prepareStatement("""
        UPDATE tickets
        SET status = 'OPEN'
        WHERE thread_id = ?
    """)) {
            ps.setString(1, thread.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Notify thread
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Ticket Reopened")
                .setColor(Color.GREEN)
                .addField("Opened by", closedBy.getAsMention(), false);

        thread.sendMessageEmbeds(embed.build()).queue();

        // Lock + archive
        thread.getManager()
                .setLocked(false)
                .setArchived(false)
                .queue();
    }
}
