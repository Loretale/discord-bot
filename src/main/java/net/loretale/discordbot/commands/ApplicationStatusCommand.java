package net.loretale.discordbot.commands;

import net.dv8tion.jda.api.Permission;
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
import net.loretale.discordbot.model.Application;
import net.loretale.discordbot.util.Permissions;

import java.util.Arrays;
import java.util.List;
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

        Member member = event.getMember();
        if (!Permissions.isStaff(member)) {
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

        if (Arrays.stream(states).noneMatch(s -> s.equals(statusOption.getAsString()))) {
            event.reply("Unknown status.")
                    .setEphemeral(true)
                    .queue();
        }

        String userId = Application.getUserId(thread);

        if (userId == null) {
            event.reply("Application not found.")
                    .setEphemeral(true)
                    .queue();
            return;
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

        if(!Application.updateStatus(thread.getId(), "DENIED")) {
            event.reply("Something went wrong trying to update status.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        notifyApplicant(event.getGuild(), userId, "Your application " + thread.getAsMention() + " has been " +
                "DENIED for reason: " + reason + ".\nNote that you can make a new application, unless otherwise mentioned.");

        event.reply("Application denied.")
                .setEphemeral(true)
                .queue();

        thread.sendMessage("Application denied for reason:\n" + reason.getAsString())
                .queue(_ -> {
                    thread.getManager().setName(thread.getName() + "\uD83D\uDFE5").queue(_ -> {
                        thread.getManager().setLocked(true).setArchived(true).queue();
                    });
                });
    }

    private void handleAccepted(SlashCommandInteractionEvent event, ThreadChannel thread, String userId) {
        Guild guild = event.getGuild();

        if (guild == null) {
            event.reply("Interaction must happen in server.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        String username = Application.getUserName(thread);

        if (username == null) {
            event.reply("Application not found.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if(!Application.updateStatus(thread.getId(), "ACCEPTED")) {
            event.reply("Something went wrong trying to update status.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        notifyApplicant(guild, userId, "Your application " + thread.getAsMention() + " has been " +
                "ACCEPTED, welcome to the server! When the server opens, you will be whitelisted and be able to join.");

        guild.retrieveMemberById(userId).queue(member -> {
                    guild.modifyNickname(member, username).queue();
                    Role role = guild.getRoleById(Constants.ACCEPTED_ROLE_ID);
                    guild.addRoleToMember(member, role).queue(
                            __ -> { System.out.println("Successfully assigned role to " + username);},
                            Throwable::printStackTrace
                    );
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
    }

    private void handlePending(SlashCommandInteractionEvent event, ThreadChannel thread, OptionMapping reason, String userId) {
        if (reason == null) {
            event.reply("Must give a reason.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if(!Application.updateStatus(thread.getId(), "PENDING")) {
            event.reply("Something went wrong trying to update status.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        notifyApplicant(event.getGuild(), userId, "Your application " + thread.getAsMention() +
                " has been put on PENDING, for reason: " + reason.getAsString() + "\n" +
                "Please respond with to the requested edit / questions.");

        thread.sendMessage("Application put on pending for reason:\n" + reason.getAsString())
                        .queue(_ -> {
                            thread.getManager().setName(thread.getName() + "\uD83D\uDFE8").queue();
                        });

        event.reply("Application set to pending.")
                .setEphemeral(true)
                .queue();
    }

    private void notifyApplicant(Guild guild, String userId, String message) {
        guild.retrieveMemberById(userId).queue(member ->
                member.getUser().openPrivateChannel().queue(channel ->
                        channel.sendMessage(message).queue()
                )
        );
    }
}
