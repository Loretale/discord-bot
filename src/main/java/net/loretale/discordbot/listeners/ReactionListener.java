package net.loretale.discordbot.listeners;

import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.regex.Pattern;

public class ReactionListener extends ListenerAdapter {
    private static final Pattern FLAG_EMOJI =
            Pattern.compile("[\\x{1F1E6}-\\x{1F1FF}]{2}");

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (event.getUser() == null || event.getUser().isBot()) return;

        UnicodeEmoji emoji = event.getReaction().getEmoji().asUnicode();

        if (FLAG_EMOJI.matcher(emoji.getFormatted()).matches()) {
            event.getReaction().removeReaction(event.getUser()).queue();
        }
    }
}
