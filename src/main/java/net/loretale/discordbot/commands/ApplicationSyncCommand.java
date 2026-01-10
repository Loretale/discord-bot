package net.loretale.discordbot.commands;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.loretale.discordbot.Constants;
import net.loretale.discordbot.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ApplicationSyncCommand extends ListenerAdapter {
    public static final String name = "sync";
    public static final String description = "Sync a user's accepted application (nickname + role)";

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        // automatically try to sync when someone rejoins
        Guild guild = event.getGuild();
        String userId = event.getMember().getId();
        try {
            String username = getAcceptedUsername(userId);
            if (username != null) {
                applyNicknameAndRole(guild, userId, username);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || !event.getName().equals(name)) return;

        OptionMapping userOption = event.getOption("user");
        String targetId;
        boolean isStaff = event.getMember() != null && event.getMember().getRoles().stream()
                .anyMatch(r -> r.getId().equals(Constants.STAFF_ROLE_ID));

        if (userOption != null) {
            if (!isStaff) {
                event.reply("You do not have permission to sync other users.").setEphemeral(true).queue();
                return;
            }
            targetId = userOption.getAsUser().getId();
        } else {
            targetId = event.getUser().getId();
        }

        try {
            String username = getAcceptedUsername(targetId);
            if (username == null) {
                event.reply("No accepted application found for that user.").setEphemeral(true).queue();
                return;
            }

            applyNicknameAndRole(event.getGuild(), targetId, username);

            event.reply("Synced accepted application for <@" + targetId + ">.").setEphemeral(true).queue();
        } catch (SQLException e) {
            e.printStackTrace();
            event.reply("Database error while syncing.").setEphemeral(true).queue();
        }
    }

    private String getAcceptedUsername(String userId) throws SQLException {
        try (PreparedStatement ps = Database.getConnection().prepareStatement(
                "SELECT username FROM applications WHERE user_id = ? AND status = 'ACCEPTED' LIMIT 1"
        )) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("username");
                } else {
                    return null;
                }
            }
        }
    }

    private void applyNicknameAndRole(Guild guild, String userId, String username) {
        if (guild == null) return;
        guild.retrieveMemberById(userId).queue(member -> {
            guild.modifyNickname(member, username).queue(nickSuccess -> {
            }, nickFailure -> {
            });

            guild.addRoleToMember(member, guild.getRoleById(Constants.ACCEPTED_ROLE_ID)).queue(roleSuccess -> {
            }, roleFailure -> {
            });
        });
    }

    public static void syncIfAccepted(Guild guild, String userId) throws SQLException {
        ApplicationSyncCommand cmd = new ApplicationSyncCommand();
        String username = cmd.getAcceptedUsername(userId);
        if (username != null) {
            cmd.applyNicknameAndRole(guild, userId, username);
        }
    }

}