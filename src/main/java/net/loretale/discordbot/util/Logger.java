package net.loretale.discordbot.util;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.loretale.discordbot.Constants;

public class Logger {
    private static TextChannel adminLog;
    private static TextChannel modLog;

    public static void init(Guild guild) {
        adminLog = guild.getTextChannelById(Constants.ADMIN_LOG_CHANNEL_ID);
        modLog = guild.getTextChannelById(Constants.LOG_CHANNEL_ID);
    }

    public static void LogAdmin(MessageEmbed embed) {
        adminLog.sendMessageEmbeds(embed).queue();
    }

    public static void LogAdmin(String message) {
        adminLog.sendMessage(message).queue();
    }

    public static void Log(MessageEmbed embed) {
        modLog.sendMessageEmbeds(embed).queue();
    }
}
