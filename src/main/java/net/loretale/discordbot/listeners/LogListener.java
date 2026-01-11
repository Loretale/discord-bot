package net.loretale.discordbot.listeners;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateTimeOutEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.loretale.discordbot.util.Logger;

import java.awt.*;
import java.time.Instant;
import java.util.stream.Collectors;

public class LogListener extends ListenerAdapter {
    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        Logger.Log(new EmbedBuilder()
                .setTitle("Member Joined")
                .setDescription(event.getMember().getAsMention())
                .setColor(Color.GREEN)
                .setTimestamp(Instant.now())
                .build());
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        if (event.getMember() == null) {
            Logger.Log(new EmbedBuilder()
                    .setTitle("Member Left")
                    .setColor(Color.RED)
                    .setTimestamp(Instant.now())
                    .build());

        } else {
            Logger.Log(new EmbedBuilder()
                    .setTitle("Member Left")
                    .setDescription(event.getMember().getAsMention())
                    .setColor(Color.RED)
                    .setTimestamp(Instant.now())
                    .build());

        }
    }

    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {
        Logger.Log(new EmbedBuilder()
                .setTitle("Message Edited")
                .addField("Author", event.getAuthor().getAsTag(), true)
                .addField("Channel", event.getChannel().getAsMention(), true)
                .addField("Message ID", event.getMessageId(), false)
                .setColor(Color.ORANGE)
                .setTimestamp(Instant.now())
                .build());
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        Logger.Log(new EmbedBuilder()
                .setTitle("Message Deleted")
                .addField("Channel", event.getChannel().getAsMention(), true)
                .addField("Message", event.getMessageId(), true)
                .setColor(Color.RED)
                .setTimestamp(Instant.now())
                .build());
    }

    @Override
    public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) {
        String roles = event.getRoles().stream()
                .map(Role::getAsMention)
                .collect(Collectors.joining(", "));

        Logger.Log(new EmbedBuilder()
                .setTitle("Role Added")
                .addField("User", event.getMember().getAsMention(), true)
                .addField("Role(s)", roles, true)
                .setColor(Color.GREEN)
                .setTimestamp(Instant.now())
                .build());
    }

    @Override
    public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {
        String roles = event.getRoles().stream()
                .map(Role::getAsMention)
                .collect(Collectors.joining(", "));

        Logger.Log(new EmbedBuilder()
                .setTitle("Role Removed")
                .addField("User", event.getMember().getAsMention(), true)
                .addField("Role(s)", roles, true)
                .setColor(Color.RED)
                .setTimestamp(Instant.now())
                .build());
    }

    @Override
    public void onGuildBan(GuildBanEvent event) {
        Logger.LogAdmin(new EmbedBuilder()
                .setTitle("User Banned")
                .addField("User", event.getUser().getAsTag(), true)
                .addField("User ID", event.getUser().getId(), true)
                .setColor(Color.RED)
                .setTimestamp(Instant.now())
                .build());
    }

    @Override
    public void onGuildUnban(GuildUnbanEvent event) {
        Logger.LogAdmin(new EmbedBuilder()
                .setTitle("User Unbanned")
                .addField("User", event.getUser().getAsTag(), true)
                .setColor(Color.GREEN)
                .setTimestamp(Instant.now())
                .build());
    }

    @Override
    public void onGuildMemberUpdateNickname(GuildMemberUpdateNicknameEvent event) {
        Logger.Log(new EmbedBuilder()
                .setTitle("Nickname Changed")
                .addField("User", event.getMember().getAsMention(), true)
                .addField("Old", event.getOldNickname() == null ? "None" : event.getOldNickname(), true)
                .addField("New", event.getNewNickname() == null ? "None" : event.getNewNickname(), true)
                .setColor(Color.ORANGE)
                .setTimestamp(Instant.now())
                .build());
    }

    @Override
    public void onGuildMemberUpdateTimeOut(GuildMemberUpdateTimeOutEvent event) {
        Logger.Log(new EmbedBuilder()
                .setTitle("Timeout Updated")
                .addField("User", event.getMember().getAsMention(), true)
                .addField(
                        "Until",
                        event.getNewTimeOutEnd() == null
                                ? "Removed"
                                : "<t:" + event.getNewTimeOutEnd().toEpochSecond() + ":R>",
                        true
                )
                .setColor(Color.DARK_GRAY)
                .setTimestamp(Instant.now())
                .build());
    }
}
