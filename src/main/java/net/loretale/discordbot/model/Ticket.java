package net.loretale.discordbot.model;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.loretale.discordbot.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Ticket {
    public static boolean isOpen(ThreadChannel thread) {
        try (PreparedStatement ps = Database.getConnection().prepareStatement("""
            SELECT status FROM discord_tickets WHERE thread_id = ?
        """)) {
            ps.setString(1, thread.getId());
            ResultSet rs = ps.executeQuery();
            return rs.next() && "OPEN".equals(rs.getString("status"));
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean exists(ThreadChannel thread) {
        try (PreparedStatement ps = Database.getConnection().prepareStatement("""
            SELECT 1 FROM discord_tickets WHERE thread_id = ? LIMIT 1
        """)) {
            ps.setString(1, thread.getId());
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void create(ThreadChannel thread, Member member, String subject, String description) {
        try (PreparedStatement ps = Database.getConnection().prepareStatement("""
            INSERT INTO discord_tickets (
                thread_id,
                user_id,
                subject,
                description,
                status,
                close_reason
            ) VALUES (?, ?, ?, ?, ?, NULL)
        """)) {
            ps.setString(1, thread.getId());
            ps.setString(2, member.getId());
            ps.setString(3, subject);
            ps.setString(4, description);
            ps.setString(5, "OPEN");
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static int getCount() {
        try (PreparedStatement ps = Database.getConnection().prepareStatement("""
            SELECT COUNT(*) FROM discord_tickets
        """)) {
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static void close(ThreadChannel thread, String reason) {
        try (PreparedStatement ps = Database.getConnection().prepareStatement("""
            UPDATE discord_tickets
            SET status = 'CLOSED',
                close_reason = ?
            WHERE thread_id = ?
        """)) {
            ps.setString(1, reason);
            ps.setString(2, thread.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void reopen(ThreadChannel thread) {
        try (PreparedStatement ps = Database.getConnection().prepareStatement("""
            UPDATE discord_tickets
            SET status = 'OPEN'
            WHERE thread_id = ?
        """)) {
            ps.setString(1, thread.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
