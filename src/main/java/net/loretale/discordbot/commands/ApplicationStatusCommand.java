package net.loretale.discordbot.commands;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.loretale.discordbot.Constants;
import net.loretale.discordbot.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ApplicationStatusCommand extends ListenerAdapter {
    public static final String name = "app-status";
    public static final String description = "Set status of an application";

    public static final String optionStatusName = "status";
    public static final String optionStatusDesc = "The status of the application";

    public static final String optionReasonName = "reason";
    public static final String optionReasonDesc = "Reason for status change";

    private final String[] states = new String[] {"pending", "accepted", "denied"};

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (event.getName().equals(name) && event.getFocusedOption().getName().equals(optionStatusName)) {
            List<Command.Choice> options = Stream.of(states)
                    .filter(word -> word.startsWith(event.getFocusedOption().getValue()))
                    .map(word -> new Command.Choice(word, word))
                    .toList();
            event.replyChoices(options).queue();
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || !event.getName().equals(name)) return;

        if (!(event.getChannel() instanceof ThreadChannel thread)) {
            event.reply("This command must be used inside an application thread.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        Member staff = event.getMember();
        if (!isStaff(staff)) {
            event.reply("You do not have permission to do this.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        OptionMapping statusOption = event.getOption(optionStatusName);

        if (statusOption == null) {
            event.reply("Missing status.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        String userId = "";

        try (ResultSet rs = getApplicationByThread(thread.getId())) {
            if (!rs.next()) {
                event.reply("Application not found.")
                        .setEphemeral(true)
                        .queue();
                return;
            }

            userId = rs.getString("user_id");
        } catch (SQLException e) {
            e.printStackTrace();
            event.reply("Something went wrong trying to find the application.")
                    .setEphemeral(true)
                    .queue();
        }



        OptionMapping reasonOption = event.getOption(optionReasonName);

        String status = statusOption.getAsString();

        switch (status) {
            case "pending" -> handlePending(event, thread, reasonOption, userId);
            case "accepted" -> handleAccepted(event, thread, userId);
            case "denied" -> handleDenied(event, thread, reasonOption, userId);
            default -> event.reply("Invalid status.")
                    .setEphemeral(true)
                    .queue();
        }
    }

    private void handleDenied(SlashCommandInteractionEvent event, ThreadChannel thread, OptionMapping reason, String userId) {
        if (reason == null) {
            event.reply("Must give a reason.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        try {
            updateStatus(thread.getId(), "DENIED");

            notifyApplicant(event.getGuild(), userId, "Denied", reason.getAsString());

            event.reply("Application denied.")
                    .setEphemeral(true)
                    .queue();

            thread.sendMessage("Application denied for reason:\n" + reason.getAsString())
                    .queue(_ -> {
                        thread.getManager().setName(thread.getName() + "\uD83D\uDFE5").queue(_ -> {
                            thread.getManager().setLocked(true).setArchived(true).queue();
                        });
                    });
        } catch (SQLException e) {
            e.printStackTrace();

            event.reply("Something went wrong.")
                    .setEphemeral(true)
                    .queue();
        }
    }

    private void handleAccepted(SlashCommandInteractionEvent event, ThreadChannel thread, String userId) {
        Guild guild = event.getGuild();
        assert guild != null;

        try (ResultSet rs = getApplicationByThread(thread.getId())) {
            if (!rs.next()) {
                event.reply("Application not found.")
                        .setEphemeral(true)
                        .queue();
                return;
            }

            String username = rs.getString("username");

            updateStatus(thread.getId(), "ACCEPTED");
            notifyApplicant(guild, userId, "Accepted", "");

            guild.retrieveMemberById(userId).queue(member -> {
                        guild.modifyNickname(member, username).queue();
                        Role role = guild.getRoleById(Constants.ACCEPTED_ROLE_ID);
                        guild.addRoleToMember(member, role).queue();
                    }
            );

            event.reply("Application **Accepted**.")
                    .setEphemeral(true)
                    .queue();

            thread.sendMessage("Accepted.")
                    .queue(_ -> {
                        thread.getManager().setName(thread.getName() + "\uD83D\uDFE9").queue(_ -> {
                            thread.getManager().setLocked(true).setArchived(true).queue();
                        });
                    });

        } catch (SQLException e) {
            e.printStackTrace();
            event.reply("Database error.")
                    .setEphemeral(true)
                    .queue();
        }
    }

    private void handlePending(SlashCommandInteractionEvent event, ThreadChannel thread, OptionMapping reason, String userId) {
        if (reason == null) {
            event.reply("Must give a reason.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        try {
            updateStatus(thread.getId(), "PENDING");

            notifyApplicant(event.getGuild(), userId, "Pending", reason.getAsString());

            thread.sendMessage("Application put on pending for reason:\n" + reason.getAsString())
                            .queue(_ -> {
                                thread.getManager().setName(thread.getName() + "\uD83D\uDFE8").queue();
                            });

            event.reply("Application set to pending.")
                    .setEphemeral(true)
                    .queue();
        } catch (SQLException e) {
            e.printStackTrace();

            event.reply("Something went wrong.")
                    .setEphemeral(true)
                    .queue();
        }
    }

    private boolean isStaff(Member member) {
        return member != null
                && member.getRoles().stream()
                .anyMatch(r -> r.getId().equals(Constants.STAFF_ROLE_ID));
    }

    private ResultSet getApplicationByThread(String threadId) throws SQLException {
        PreparedStatement ps = Database.getConnection().prepareStatement("""
            SELECT * FROM applications WHERE thread_id = ?
        """);
        ps.setString(1, threadId);
        return ps.executeQuery();
    }

    private void updateStatus(String threadId, String status) throws SQLException {
        try (PreparedStatement ps = Database.getConnection().prepareStatement("""
            UPDATE applications SET status = ? WHERE thread_id = ?
        """)) {
            ps.setString(1, status);
            ps.setString(2, threadId);
            ps.executeUpdate();
        }
    }

    private void notifyApplicant(Guild guild, String userId, String status, String reason) {
        guild.retrieveMemberById(userId).queue(member ->
                member.getUser().openPrivateChannel().queue(channel ->
                        channel.sendMessage(
                                reason.isEmpty() ?
                                "Your application status has been updated to **" + status + "**." :
                                "Your application status has been updated to **" + status + "**.\nReason: " + reason
                        ).queue()
                )
        );
    }


}
