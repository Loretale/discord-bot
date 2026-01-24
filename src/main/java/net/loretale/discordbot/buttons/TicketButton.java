package net.loretale.discordbot.buttons;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.modals.Modal;
import net.loretale.discordbot.Constants;
import net.loretale.discordbot.model.Ticket;

import java.awt.*;
import java.util.Objects;

public class TicketButton extends ListenerAdapter {
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getGuild() == null) {
            event.reply("Command must be used in server.")
                    .setEphemeral(true)
                    .queue();

            return;
        }

        String id = event.getComponentId();

        if (!id.startsWith("ticket:create:")) return;

        String type = id.substring("ticket:create:".length()); // Strip off the "ticket:create:"

        Modal modal = Modal.create("ticket:modal:"+type, "Create" + type + " ticket")
                .addComponents(
                        Label.of("Subject", TextInput.create("subject", TextInputStyle.SHORT)
                                .setPlaceholder("Subject (Rule broken/bug/other)")
                                .setMinLength(3)
                                .setMaxLength(100)
                                .build()),
                        Label.of("Description", TextInput.create("description", TextInputStyle.PARAGRAPH)
                                .setPlaceholder("Explain the reason for your ticket (add details in thread, 1000 characters max).")
                                .setMinLength(10)
                                .setMaxLength(1000)
                                .build())
                ).build();

        event.replyModal(modal).queue();
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (event.getGuild() == null) {
            event.reply("Command must be used in server.")
                    .setEphemeral(true)
                    .queue();

            return;
        }

        String id = event.getModalId();
        if (!id.startsWith("ticket:modal:")) return;

        String type = id.substring("ticket:modal:".length()); // Strip off the "ticket:create:"


        TextChannel ticketChannel = Objects.requireNonNull(event.getGuild()).getTextChannelById(Constants.TICKET_CHANNEL_ID);
        Member member = event.getMember();

        if (ticketChannel == null || member == null) {
            event.reply("Ticket system misconfigured. Does channel not exist anymore?")
                    .setEphemeral(true)
                    .queue();
            return;
        }


        event.deferReply(true).queue();

        String threadName = type + "-ticket-" + member.getEffectiveName() + "-" + (Ticket.getCount() + 1);

        ticketChannel.createThreadChannel(threadName, true)
                .setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_1_WEEK)
                .queue(thread -> {
                    handleThreadSetup(event, thread, type);
                });

    }

    private void handleThreadSetup(ModalInteractionEvent event, ThreadChannel thread, String type) {
        Member member = event.getMember();
        Guild guild = event.getGuild();

        if (guild == null) {
            event.reply("Command must be used in server.")
                    .setEphemeral(true)
                    .queue();

            return;
        }

        Role role = switch (type) {
            case "admin" -> guild.getRoleById(Constants.ADMIN_ROLE_ID);
            case "staff" -> guild.getRoleById(Constants.STAFF_ROLE_ID);
            default -> null;
        };

        String mention = role != null ? role.getAsMention() : "";

        String reason = Objects.requireNonNull(event.getValue("subject")).getAsString();
        String description = Objects.requireNonNull(event.getValue("description")).getAsString();

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("New " + type + " ticket")
                .setColor(Color.GREEN)
                .addField("User", member.getAsMention(), false)
                .addField("Subject", reason, false)
                .addField("Description", description, false)
                .addField("Please note", """
                        @ mentioning someone in a thread will add them.
                        Please do not mention people or roles if you do not mean for them
                        to get added to the channel.""", false)
                .setFooter("We will get to you as soon as possible.");

        thread.sendMessageEmbeds(embed.build()).queue();

        thread.sendMessage(mention).queue();

        thread.addThreadMember(member).queue();

        Ticket.create(thread, member, reason, description);

        event.getHook()
                .editOriginal("Your ticket has been created: " + thread.getAsMention())
                .queue();

    }
}
