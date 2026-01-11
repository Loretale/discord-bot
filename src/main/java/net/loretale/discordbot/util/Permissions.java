package net.loretale.discordbot.util;

import net.dv8tion.jda.api.entities.Member;
import net.loretale.discordbot.Constants;

public class Permissions {
    public static boolean isStaff(Member member) {
        return member != null
                && member.getRoles().stream()
                .anyMatch(r -> r.getId().equals(Constants.STAFF_ROLE_ID));
    }

    public static boolean isAdmin(Member member) {
        return member != null
                && member.getRoles().stream()
                .anyMatch(r -> r.getId().equals(Constants.ADMIN_ROLE_ID));
    }
}
