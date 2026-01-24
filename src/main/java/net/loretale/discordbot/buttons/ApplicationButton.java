package net.loretale.discordbot.buttons;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.modals.Modal;
import net.loretale.discordbot.Constants;
import net.loretale.discordbot.commands.ApplicationSyncCommand;
import net.loretale.discordbot.model.Application;

import java.awt.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;

public class ApplicationButton extends ListenerAdapter {
    public static final String buttonId = "application:create:button";
    public static final String modalId = "application:create:modal";

    public static final String usernameKey = "username";
    public static final String ageKey = "age";
    public static final String metagamingKey = "metagaming";
    public static final String characterKey = "character";
    public static final String promptKey = "prompt";

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getGuild() == null) {
            event.reply("Command must be used in server.")
                    .setEphemeral(true)
                    .queue();

            return;
        }

        String id = event.getComponentId();

        if (!id.equals(buttonId)) return;

        if(ApplicationSyncCommand.syncIfAccepted(event.getGuild(), event.getUser().getId())) {
            event.reply("This account is already linked to an accepted account. Synced.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        Modal modal = Modal.create(modalId, "Create application")
                .addComponents(
                        Label.of("Hytale Username", TextInput.create(usernameKey, TextInputStyle.SHORT)
                                .setPlaceholder("Username")
                                .setMinLength(3)
                                .setMaxLength(16)
                                .build()),
                        Label.of("Are you 16 or older?", StringSelectMenu.create(ageKey)
                                .addOption("Yes", "Yes")
                                .addOption("No", "No")
                                .build()),
                        Label.of("Define metagaming & powergaming", "1-2 sentences", TextInput.create(metagamingKey, TextInputStyle.PARAGRAPH)
                                .setPlaceholder("Metagaming is... Powergaming is...")
                                .setMinLength(10)
                                .setMaxLength(200)
                                .build()),
                        Label.of("Describe your character (~ 1 paragraph)", TextInput.create(characterKey, TextInputStyle.PARAGRAPH)
                                .setPlaceholder("[Name] is from [place] and...")
                                .setMinLength(100)
                                .setMaxLength(750)
                                .build()),
                        Label.of("Respond to the prompt in character:",
                                "\"How was your travels?\" The bearded man asks you as you step off the ship.",
                                TextInput.create(promptKey, TextInputStyle.PARAGRAPH)
                                        .setPlaceholder("Eric steps off the ship and...")
                                        .setMinLength(100)
                                        .setMaxLength(500)
                                        .build()
                                )
                ).build();

        event.replyModal(modal).queue();
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        Guild guild = event.getGuild();

        if (guild == null) {
            event.reply("Command must be used in server.")
                    .setEphemeral(true)
                    .queue();

            return;
        }

        String id = event.getModalId();

        if (!id.equals(modalId)) return;

        TextChannel applicationChannel = guild.getTextChannelById(Constants.APPLICATION_CHANNEL_ID);
        Member member = event.getMember();

        if (applicationChannel == null || member == null) {
            event.reply("Application system misconfigured. Does channel not exist anymore?")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (event.getValue(usernameKey) == null) {
            event.reply("Username input missing.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        String username = Objects.requireNonNull(event.getValue(usernameKey)).getAsString();

        boolean hasAcceptedRole = member.getRoles().stream()
                .anyMatch(r -> r.getId().equals(Constants.ACCEPTED_ROLE_ID));
        if (hasAcceptedRole) {
            event.reply("You already have the accepted role and cannot make an application.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (Application.hasAcceptedApplication(member, username)) {
            event.reply("An accepted application already exists for that account or username.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        event.deferReply(true).queue();

        String threadName = username + "-" + (Application.count() + 1);

        applicationChannel.createThreadChannel(threadName, true)
                .setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_1_WEEK)
                .queue(thread -> {
                    handleThreadSetup(event, thread);
                });
    }

    private void handleThreadSetup(ModalInteractionEvent event, ThreadChannel thread) {
        Member member = event.getMember();
        Guild guild = event.getGuild();

        if (guild == null) {
            event.reply("Interaction must happen in server.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (member == null) {
            event.reply("Could not find member.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        String username = Objects.requireNonNull(event.getValue(usernameKey)).getAsString();
        String age = Objects.requireNonNull(event.getValue(ageKey)).getAsStringList().toString();
        String metagaming = Objects.requireNonNull(event.getValue(metagamingKey)).getAsString();
        String character = Objects.requireNonNull(event.getValue(characterKey)).getAsString();
        String prompt = Objects.requireNonNull(event.getValue(promptKey)).getAsString();

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(username + " Application")
                .setColor(Color.BLUE)
                .addField("Age", age, true)
                .addField("Metagaming", metagaming, true)
                .addField("Character", character, false)
                .addField("Prompt Response", prompt, false)
                .setFooter("We will get to you as soon as possible.");

        thread.sendMessageEmbeds(embed.build()).queue();

        ArrayList<ThreadChannel> previousApplications = Application.getPreviousApplications(guild, member, username);

        if (!previousApplications.isEmpty()) {
            thread.sendMessage("Previous applications: " +
                    previousApplications
                            .stream()
                            .map(ThreadChannel::getAsMention)
                            .collect(Collectors.joining(", ")) + ".")
                    .queue();
        }

        thread.sendMessage(Objects.requireNonNull(guild.getRoleById(Constants.STAFF_ROLE_ID))
                .getAsMention())
                .queue();

        thread.addThreadMember(member).queue();

        Application.create(thread, member, username, age, metagaming, character, prompt);

        event.getHook()
                .editOriginal("Your application has been created: " + thread.getAsMention())
                .queue();
    }
}
