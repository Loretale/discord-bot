
package net.loretale.discordbot.commands;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.loretale.discordbot.model.Application;
import net.loretale.discordbot.util.Permissions;

public class ApplicationSyncCommand extends ListenerAdapter {
    public static final String name = "sync";
    public static final String description = "Sync a user's accepted application (nickname + role)";

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        Guild guild = event.getGuild();
        String userId = event.getMember().getId();
        String username = Application.getAcceptedUsername(userId);
        if (username != null) {
            Application.applyNicknameAndRole(guild, userId, username);
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || !event.getName().equals(name)) return;

        OptionMapping userOption = event.getOption("user");
        Member targetMember;
        boolean isStaff = Permissions.isStaff(event.getMember());

        if (userOption != null) {
            if (!isStaff) {
                event.reply("You do not have permission to sync other users.")
                        .setEphemeral(true)
                        .queue();
                return;
            }
            targetMember = userOption.getAsMember();
        } else {
            targetMember = event.getMember();
        }

        if (targetMember == null) {
            event.reply("Couldn't find member.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        String targetId = targetMember.getId();

        String username = Application.getAcceptedUsername(targetId);

        if (username == null) {
            event.reply("No accepted application found.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        Application.applyNicknameAndRole(event.getGuild(), targetId, username);

        event.reply("Synced accepted application for " + targetMember.getAsMention() + ".")
                .setEphemeral(true)
                .queue();
    }

    public static boolean syncIfAccepted(Guild guild, String userId) {
        String username = Application.getAcceptedUsername(userId);
        if (username != null) {
            Application.applyNicknameAndRole(guild, userId, username);
            return true;
        }
        return false;
    }

}