package net.loretale.discordbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.loretale.discordbot.buttons.ApplicationButton;
import net.loretale.discordbot.buttons.TicketButton;
import net.loretale.discordbot.commands.*;
import net.loretale.discordbot.listeners.LogListener;
import net.loretale.discordbot.listeners.ReactionListener;
import net.loretale.discordbot.util.Logger;

import java.awt.*;
import java.util.Arrays;
import java.util.Objects;

public class Main {
    static void main() throws Exception {
        String token = System.getenv("DISCORD_TOKEN");

        Database.init();

        JDA jda = JDABuilder.createDefault(token)
                .enableIntents(Arrays.asList(GatewayIntent.values()))
                .addEventListeners(new PingCommand())
                .addEventListeners(new CreateMessageCommand())
                .addEventListeners(new EditMessageCommand())
                .addEventListeners(new TicketAddCommand())
                .addEventListeners(new TicketCloseCommand())
                .addEventListeners(new TicketReopenCommand())
                .addEventListeners(new TicketButton())
                .addEventListeners(new ApplicationButton())
                .addEventListeners(new ApplicationStatusCommand())
                .addEventListeners(new ApplicationSyncCommand())
                .addEventListeners(new LogListener())
                .addEventListeners(new ReactionListener())
                .build();

        jda.awaitReady();

        System.out.println("Bot is online!");

        Guild guild = Objects.requireNonNull(jda.getGuildById(Constants.GUILD_ID));

        Logger.init(guild);

        createTicketEmbed(guild);
        createApplicationEmbed(guild);

        guild.updateCommands()
                .addCommands(
                        Commands.slash(ApplicationSyncCommand.name, ApplicationSyncCommand.description)
                                .addOption(OptionType.USER, "user", "User to sync (staff only)", false),
                        Commands.slash(PingCommand.name, PingCommand.description),
                        Commands.slash("message", "Manage persistent messages")
                                .setDefaultPermissions(DefaultMemberPermissions.DISABLED)
                                .addSubcommands(
                                        new SubcommandData(CreateMessageCommand.name, CreateMessageCommand.description)
                                                .addOption(OptionType.STRING, CreateMessageCommand.optionContentName, CreateMessageCommand.optionContentDesc, true),
                                        new SubcommandData(EditMessageCommand.name, EditMessageCommand.description)
                                                .addOption(OptionType.STRING, EditMessageCommand.optionIdName, EditMessageCommand.optionIdDesc, true)
                                                .addOption(OptionType.STRING, EditMessageCommand.optionContentName, EditMessageCommand.optionContentDesc, true)
                                ),
                        Commands.slash(TicketAddCommand.name, TicketAddCommand.description)
                                .addOption(OptionType.USER, TicketAddCommand.optionUserName, TicketAddCommand.optionUserDesc),
                        Commands.slash(TicketCloseCommand.name, TicketCloseCommand.description)
                                .addOption(OptionType.STRING, TicketCloseCommand.optionReasonName, TicketCloseCommand.optionReasonDesc),
                        Commands.slash(TicketReopenCommand.name, TicketReopenCommand.description),
                        Commands.slash(ApplicationStatusCommand.name, ApplicationStatusCommand.description)
                                .addOption(OptionType.STRING, ApplicationStatusCommand.optionStatusName, ApplicationStatusCommand.optionStatusDesc, true, true)
                                .addOption(OptionType.STRING, ApplicationStatusCommand.optionReasonName, ApplicationStatusCommand.optionReasonDesc)

                ).queue();
    }

    static void createTicketEmbed(Guild guild) {
        TextChannel channel = guild.getTextChannelById(Constants.TICKET_CHANNEL_ID);

        if (channel == null) {
            throw new IllegalStateException("Ticket channel not found");
        }

        String botId = channel.getJDA().getSelfUser().getId();


        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Create a ticket")
                .setDescription("""
                Click a button below to create a ticket. A thread will be opened for you.
                - **Admin tickets** are for sensitive or confidential content, but have slower response time.
                - **Staff tickets** can be seen by all staff.
                - Report bugs on our GitHub or in <#1441862118138843387>.
                """)
                .setColor(Color.GREEN);

        ActionRow buttons = ActionRow.of(
                Button.danger("ticket:create:admin", "Create Admin Ticket"),
                Button.primary("ticket:create:staff", "Create Staff Ticket")
        );

        boolean found = false;

        for (Message message : channel.getIterableHistory()) {
            if (message.getAuthor().getId().equals(botId)) {
                channel.editMessageEmbedsById(message.getId(), embed.build())
                        .setComponents(buttons)
                        .queue();

                found = true;
            }
        }

        if (!found) {
            channel.sendMessageEmbeds(embed.build())
                    .addComponents(buttons)
                    .queue();
        }
    }

    static void createApplicationEmbed(Guild guild) {
        TextChannel channel = guild.getTextChannelById(Constants.APPLICATION_CHANNEL_ID);

        if (channel == null) {
            throw new IllegalStateException("Application channel not found");
        }

        String botId = channel.getJDA().getSelfUser().getId();


        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Create your application")
                .setDescription("""
                Click the button below to start making your application.
                You'll be asked to fill out your username, confirm you're older than 16,
                and be asked to define metagaming and powergaming. After this, you are
                asked to describe a character and respond to a prompt.
                
                When you're done, a channel should be created. Someone will review your
                application as soon as possible after this.
                """)
                .setColor(Color.GREEN);

        ActionRow buttons = ActionRow.of(
                Button.success(ApplicationButton.buttonId, "Apply")
        );

        boolean found = false;

        for (Message message : channel.getIterableHistory()) {
            if (message.getAuthor().getId().equals(botId)) {
                channel.editMessageEmbedsById(message.getId(), embed.build())
                        .setComponents(buttons)
                        .queue();

                found = true;
            }
        }

        if (!found) {
            channel.sendMessageEmbeds(embed.build())
                    .addComponents(buttons)
                    .queue();
        }
    }
}
