package net.loretale.discordbot.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class PingCommand extends ListenerAdapter {
    public static final String name = "ping";
    public static final String description = "Check the bot's latency";


    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent e) {
        if (!e.getName().equals(name)) return;

        long gatewayPing = e.getJDA().getGatewayPing();

        e.reply("Pong! " + gatewayPing + "ms.")
                .setEphemeral(true)
                .queue();
    }
}
