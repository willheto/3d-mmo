package com.g8e.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.g8e.db.models.DBAccount;
import com.g8e.db.models.DBPlayer;
import com.g8e.gameserver.util.SkillUtils;
import com.google.gson.Gson;

public class CommonQueries {

    public static DBAccount getAccountByLoginToken(String socketId) throws SQLException {
        String query = "SELECT * FROM accounts WHERE login_token = ?";
        try (Connection conn = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, socketId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new DBAccount(rs.getInt("account_id"), rs.getString("username"), rs.getString("password"),
                        rs.getString("login_token"), rs.getString("registration_ip"),
                        rs.getString("registration_date"));
            }
            return null;
        }
    }

    public static DBPlayer getPlayerByAccountId(int accountId) throws SQLException {
        String query = "SELECT * FROM players WHERE account_id = ?";
        try (Connection conn = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, accountId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                // Use getObject to properly handle null values
                Integer weapon = (Integer) rs.getObject("weapon");
                Integer shield = (Integer) rs.getObject("shield");
                Integer helmet = (Integer) rs.getObject("helmet");
                Integer bodyArmor = (Integer) rs.getObject("body_armor");
                Integer legArmor = (Integer) rs.getObject("leg_armor");
                Integer gloves = (Integer) rs.getObject("gloves");
                Integer boots = (Integer) rs.getObject("boots");
                Integer neckwear = (Integer) rs.getObject("neckwear");
                Integer ring = (Integer) rs.getObject("ring");

                return new DBPlayer(
                        rs.getInt("player_id"),
                        rs.getInt("account_id"),
                        rs.getInt("skin_color"),
                        rs.getInt("hair_color"),
                        rs.getInt("shirt_color"),
                        rs.getInt("pants_color"),
                        rs.getInt("world_x"),
                        rs.getInt("world_y"),
                        weapon, // weapon will remain null if the database value is null
                        shield,
                        helmet,
                        bodyArmor,
                        legArmor,
                        gloves,
                        boots,
                        neckwear,
                        ring,
                        parseIntArray(rs.getString("inventory")),
                        parseIntArray(rs.getString("inventoryAmounts")),
                        parseIntArray(rs.getString("quest_progress")),
                        rs.getInt("attack_experience"),
                        rs.getInt("strength_experience"),
                        rs.getInt("defence_experience"),
                        rs.getInt("hitpoints_experience"),
                        rs.getInt("magic_experience"));
            }
            return null;
        }
    }

    private static int[] parseIntArray(String str) {
        return (new Gson().fromJson(str, int[].class));

    }

    public static void savePlayerPositionByAccountId(int accountId, int x, int y) throws SQLException {
        String query = "UPDATE players SET world_x = ?, world_y = ? WHERE account_id = ?";
        try (Connection conn = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, x);
            stmt.setInt(2, y);
            stmt.setInt(3, accountId);
            stmt.executeUpdate();
        }
    }

    public static void savePlayerXpByAccountId(int accountId, int skill, int xp) throws SQLException {
        String skillName = SkillUtils.getSkillNameByNumber(skill);
        String query = "UPDATE players SET " + skillName + "_experience = ? WHERE account_id = ?";
        try (Connection conn = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, xp);
            stmt.setInt(2, accountId);
            stmt.executeUpdate();
        }
    }

    public static void savePlayerQuestProgressByAccountId(int accountId, String questProgress) throws SQLException {
        String query = "UPDATE players SET quest_progress = ? WHERE account_id = ?";
        try (Connection conn = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, questProgress);
            stmt.setInt(2, accountId);
            stmt.executeUpdate();
        }
    }

    public static void savePlayerInventoryByAccountId(int accountId, String inventory, String inventoryAmounts)
            throws SQLException {
        String query = "UPDATE players SET inventory = ?, inventoryAmounts = ? WHERE account_id = ?";
        try (Connection conn = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, inventory);
            stmt.setString(2, inventoryAmounts);
            stmt.setInt(3, accountId);
            stmt.executeUpdate();
        }
    }

    public static void savePlayerWieldablesByAccountId(int accountId, Integer weaponID, Integer shieldID,
            Integer helmetID, Integer bodyArmorID, Integer legArmorID, Integer glovesID, Integer bootsID,
            Integer neckwearID, Integer ringID) throws SQLException {
        String query = "UPDATE players SET weapon = ?, shield = ?, helmet = ?, body_armor = ?, leg_armor = ?, gloves = ?, boots = ?, neckwear = ?, ring = ? WHERE account_id = ?";
        try (Connection conn = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {

            if (weaponID != null) {
                stmt.setInt(1, weaponID);
            } else {
                stmt.setNull(1, java.sql.Types.INTEGER);
            }

            if (shieldID != null) {
                stmt.setInt(2, shieldID);
            } else {
                stmt.setNull(2, java.sql.Types.INTEGER);
            }

            if (helmetID != null) {
                stmt.setInt(3, helmetID);
            } else {
                stmt.setNull(3, java.sql.Types.INTEGER);
            }

            if (bodyArmorID != null) {
                stmt.setInt(4, bodyArmorID);
            } else {
                stmt.setNull(4, java.sql.Types.INTEGER);
            }

            if (legArmorID != null) {
                stmt.setInt(5, legArmorID);
            } else {
                stmt.setNull(5, java.sql.Types.INTEGER);
            }

            if (glovesID != null) {
                stmt.setInt(6, glovesID);
            } else {
                stmt.setNull(6, java.sql.Types.INTEGER);
            }

            if (bootsID != null) {
                stmt.setInt(7, bootsID);
            } else {
                stmt.setNull(7, java.sql.Types.INTEGER);
            }

            if (neckwearID != null) {
                stmt.setInt(8, neckwearID);
            } else {
                stmt.setNull(8, java.sql.Types.INTEGER);
            }

            if (ringID != null) {
                stmt.setInt(9, ringID);
            } else {
                stmt.setNull(9, java.sql.Types.INTEGER);
            }

            // Account ID is the last parameter (10th)
            stmt.setInt(10, accountId);

            stmt.executeUpdate();
        }
    }

    public static void savePlayerAppearanceByAccountId(int accountId, int skinColor, int hairColor, int shirtColor,
            int pantsColor)
            throws SQLException {
        String query = "UPDATE players SET skin_color = ?, hair_color = ?, shirt_color = ?, pants_color = ? WHERE account_id = ?";
        try (Connection conn = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, skinColor);
            stmt.setInt(2, hairColor);
            stmt.setInt(3, shirtColor);
            stmt.setInt(4, pantsColor);
            stmt.setInt(5, accountId);
            stmt.executeUpdate();
        }

    }

}
