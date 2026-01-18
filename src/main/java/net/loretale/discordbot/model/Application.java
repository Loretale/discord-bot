package net.loretale.discordbot.model;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.loretale.discordbot.Constants;
import net.loretale.discordbot.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Application {
    public static void create(
            ThreadChannel thread,
            Member member,
            String username,
            String age,
            String metagaming_powergaming,
            String character,
            String prompt) {
        try (PreparedStatement ps = Database.getConnection().prepareStatement("""
                INSERT INTO applications (
                    thread_id,
                    user_id,
                    username,
                    age,
                    metagaming_powergaming,
                    persona,
                    prompt,
                    status
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """)) {
            ps.setString(1, thread.getId());
            ps.setString(2, member.getId());
            ps.setString(3, username);
            ps.setString(4, age);
            ps.setString(5, metagaming_powergaming);
            ps.setString(6, character); // persona
            ps.setString(7, prompt);
            ps.setString(8, "OPEN");

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static int count() {
        try (PreparedStatement ps = Database.getConnection().prepareStatement("""
            SELECT COUNT(*) FROM applications
        """)) {
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static boolean hasAcceptedApplication(Member member, String username) {
        try (PreparedStatement ps = Database.getConnection().prepareStatement(
                "SELECT 1 FROM applications WHERE (user_id = ? OR username = ?) AND status = 'ACCEPTED' LIMIT 1"
        )) {
            ps.setString(1, member.getId());
            ps.setString(2, username);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean exists(ThreadChannel thread) {
        try(PreparedStatement ps = Database.getConnection().prepareStatement("""
            SELECT 1 FROM applications WHERE thread_id = ? LIMIT 1
        """)) {
            ps.setString(1, thread.getId());
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String getUserName(ThreadChannel thread) {
        try(PreparedStatement ps = Database.getConnection().prepareStatement("""
            SELECT username FROM applications WHERE thread_id = ? LIMIT 1
        """)) {
            ps.setString(1, thread.getId());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            return rs.getString("username");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getUserId(ThreadChannel thread) {
        try(PreparedStatement ps = Database.getConnection().prepareStatement("""
            SELECT user_id FROM applications WHERE thread_id = ? LIMIT 1
        """)) {
            ps.setString(1, thread.getId());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            return rs.getString("user_id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean updateStatus(String threadId, String status) {
        try (PreparedStatement ps = Database.getConnection().prepareStatement("""
            UPDATE applications SET status = ? WHERE thread_id = ?
        """)) {
            ps.setString(1, status);
            ps.setString(2, threadId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static ArrayList<ThreadChannel> getPreviousApplications(Guild guild, Member member, String username) {
        ArrayList<ThreadChannel> threadChannels = new ArrayList<>();

        try (PreparedStatement ps = Database.getConnection().prepareStatement(
                "SELECT thread_id FROM applications WHERE (user_id = ? OR username = ?)"
        )) {
            ps.setString(1, member.getId());
            ps.setString(2, username);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                threadChannels.add(guild.getThreadChannelById(rs.getString("thread_id")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return threadChannels;
    }

    public static String getAcceptedUsername(String userId) {
        try (PreparedStatement ps = Database.getConnection().prepareStatement(
                "SELECT username FROM applications WHERE user_id = ? AND status = 'ACCEPTED' LIMIT 1"
        )) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
               return rs.getString("username");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void applyNicknameAndRole(Guild guild, String userId, String username) {
        if (guild == null) return;
        guild.retrieveMemberById(userId).queue(member -> {
            guild.modifyNickname(member, username).queue();
            guild.addRoleToMember(member, guild.getRoleById(Constants.ACCEPTED_ROLE_ID)).queue();
        });
    }
}
