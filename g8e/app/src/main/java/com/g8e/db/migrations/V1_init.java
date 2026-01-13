package com.g8e.db.migrations;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.g8e.util.Logger;

import io.github.cdimascio.dotenv.Dotenv;

public class V1_init {
    Dotenv dotenv = Dotenv.load();

    String createAccountsTable = "CREATE TABLE IF NOT EXISTS accounts ("
            + "account_id INT AUTO_INCREMENT PRIMARY KEY,"
            + "username VARCHAR(50) NOT NULL UNIQUE,"
            + "password VARCHAR(255) NOT NULL,"
            + "login_token VARCHAR(255),"
            + "registration_ip VARCHAR(50),"
            + "registration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
            + ");";

    String createPlayersTable = "CREATE TABLE IF NOT EXISTS players ("
            + "player_id INT AUTO_INCREMENT PRIMARY KEY,"
            + "account_id INT,"
            + "FOREIGN KEY (account_id) REFERENCES accounts(account_id),"
            + "skin_color INT,"
            + "hair_color INT,"
            + "shirt_color INT,"
            + "pants_color INT,"
            + "world_x INT,"
            + "world_y INT,"
            + "weapon INT,"
            + "shield INT,"
            + "inventory JSON,"
            + "inventoryAmounts JSON,"
            + "quest_progress JSON,"
            + "attack_experience INT,"
            + "defence_experience INT,"
            + "strength_experience INT,"
            + "hitpoints_experience INT"
            + ");";

    public void up() {
        try {
            Connection connection = DriverManager.getConnection(dotenv.get("DB_URL"), dotenv.get("DB_USERNAME"),
                    dotenv.get("DB_PASSWORD"));
            connection.createStatement().executeUpdate(createAccountsTable);
            Logger.printInfo("Created accounts table");

            connection.createStatement().executeUpdate(createPlayersTable);
            Logger.printInfo("Created players table");

        } catch (SQLException e) {
            Logger.printError(e.getMessage());
        }
    }

    public void down() {
        try {
            Connection connection = DriverManager.getConnection(dotenv.get("DB_URL"), dotenv.get("DB_USERNAME"),
                    dotenv.get("DB_PASSWORD"));
            connection.createStatement().executeUpdate("DROP TABLE IF EXISTS players;");
            Logger.printInfo("Dropped players table");

            connection.createStatement().executeUpdate("DROP TABLE IF EXISTS accounts;");
            Logger.printInfo("Dropped accounts table");

        } catch (SQLException e) {
            Logger.printError(e.getMessage());
        }
    }
}
