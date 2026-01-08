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
import net.loretale.discordbot.Database;

import java.awt.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

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
        String id = event.getComponentId();

        if (!id.equals(buttonId)) return;

        Modal modal = Modal.create(modalId, "Create application")
                .addComponents(
                        Label.of("Hytale Username", TextInput.create(usernameKey, TextInputStyle.SHORT)
                                .setPlaceholder("Username")
                                .setMinLength(3)
                                .setMaxLength(16)
                                .build()),
                        Label.of("Are you 16 or older?", StringSelectMenu.create(ageKey)
                                .addOption("yes", "Yes")
                                .addOption("no", "No")
                                .build()),
                        Label.of("Define metagaming & powergaming", "1-2 sentences", TextInput.create(metagamingKey, TextInputStyle.PARAGRAPH)
                                .setPlaceholder("Metagaming is...")
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
        String id = event.getModalId();

        if (!id.equals(modalId)) return;

        TextChannel applicationChannel = Objects.requireNonNull(event.getGuild())
                .getTextChannelById(Constants.APPLICATION_CHANNEL_ID);
        Member member = event.getMember();

        if (applicationChannel == null || member == null) {
            event.reply("Application system misconfigured. Does channel not exist anymore?")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        event.deferReply(true).queue();

        String username = Objects.requireNonNull(event.getValue(usernameKey)).getAsString();

        String threadName = username + "-" + getNextApplicationNumber();

        applicationChannel.createThreadChannel(threadName, true)
                .setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_1_WEEK)
                .queue(thread -> {
                    handleThreadSetup(event, thread);
                });
    }

    private void handleThreadSetup(ModalInteractionEvent event, ThreadChannel thread) {
        Member member = event.getMember();
        Guild guild = event.getGuild();

        assert guild != null;

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

        thread.sendMessage(Objects.requireNonNull(guild.getRoleById(Constants.STAFF_ROLE_ID)).getAsMention()).queue();

        thread.addThreadMember(member);

        try (PreparedStatement ps = Database.getConnection().prepareStatement("""
                INSERT INTO applications (
                    thread_id,
                    user_id,
                    username,
                    age,
                    metagaming,
                    persona,
                    prompt,
                    status
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """)) {
            ps.setString(1, thread.getId());
            ps.setString(2, member.getId());
            ps.setString(3, username);
            ps.setString(4, age);
            ps.setString(5, metagaming);
            ps.setString(6, character); // persona
            ps.setString(7, prompt);
            ps.setString(8, "OPEN");

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        event.getHook()
                .editOriginal("Your application has been created: " + thread.getAsMention())
                .queue();
    }

    private int getNextApplicationNumber() {
        try (PreparedStatement ps = Database.getConnection().prepareStatement("""
            SELECT COUNT(*) FROM applications
        """)) {
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) + 1 : 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return 1;
        }
    }
}
