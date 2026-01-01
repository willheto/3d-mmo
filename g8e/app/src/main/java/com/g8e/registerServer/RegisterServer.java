package com.g8e.registerServer;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;

import at.favre.lib.crypto.bcrypt.BCrypt;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.g8e.db.DatabaseConnection;
import com.g8e.registerServer.models.RegistrationRequest;
import com.g8e.registerServer.models.RegistrationResponse;
import com.g8e.util.Logger;

public class RegisterServer {
    private static final int PORT = 8000;
    private final Gson gson = new Gson();

    public void startServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/create-account", this::handleRegisterRequest);
        server.setExecutor(null); // creates a default executor
        server.start();
        Logger.printInfo("Register server started on port " + PORT);
    }

    private void handleRegisterRequest(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 200, new RegistrationResponse(true)); // Just respond with OK
        } else if ("POST".equals(exchange.getRequestMethod())) {
            // Parse the request body
            String requestBody = new String(exchange.getRequestBody().readAllBytes());
            RegistrationRequest request = gson.fromJson(requestBody, RegistrationRequest.class);

            try {
                validateRegistrationRequest(request, exchange);
            } catch (IOException e) {
                sendResponse(exchange, 400, new RegistrationResponse(false, e.getMessage()));
                return;
            } catch (IllegalArgumentException e) {
                sendResponse(exchange, 400, new RegistrationResponse(false, e.getMessage()));
                return;
            }
            validateRegistrationRequest(request, exchange);
            registerUser(request, exchange);

        } else {
            sendResponse(exchange, 405, new RegistrationResponse(false, "Method not allowed"));
        }
    }

    private void validateRegistrationRequest(RegistrationRequest request, HttpExchange exchange)
            throws IllegalArgumentException, IOException {

        if (request.getUsername() == null || request.getPassword() == null) {
            throw new IllegalArgumentException("Username and password are required");
        }

        if (request.getUsername().length() < 3 || request.getUsername().length() > 20) {
            throw new IllegalArgumentException("Username must be between 3 and 20 characters");
        }

        if (request.getPassword().length() < 5 || request.getPassword().length() > 20) {
            throw new IllegalArgumentException("Password must be between 5 and 20 characters");
        }

    }

    private void registerUser(RegistrationRequest request, HttpExchange exchange) throws IOException {
        try {
            Connection connection = DatabaseConnection.createDatabaseConnection();
            String SQL_REGISTER_USER = "INSERT INTO accounts (username, password) VALUES (?, ?)";
            var statement = connection.prepareStatement(SQL_REGISTER_USER);

            statement.setString(1, request.getUsername());

            String hashedPassword = BCrypt.withDefaults().hashToString(12, request.getPassword().toCharArray());
            statement.setString(2, hashedPassword);
            statement.executeUpdate();

            String SQL_SELECT_ACCOUNT_ID = "SELECT account_id FROM accounts WHERE username = ?";
            var selectStatement = connection.prepareStatement(SQL_SELECT_ACCOUNT_ID);
            selectStatement.setString(1, request.getUsername());
            selectStatement.execute();

            // Move the cursor to the first row
            ResultSet resultSet = selectStatement.getResultSet();
            if (resultSet.next()) { // Check if there is a result
                int accountID = resultSet.getInt("account_id");
                createPlayer(accountID, exchange);
            } else {
                sendResponse(exchange, 404, new RegistrationResponse(false, "Account not found"));
            }

        } catch (SQLException e) {
            sendResponse(exchange, 500, new RegistrationResponse(false, e.getMessage()));
        }

    }

    private void createPlayer(int accountID, HttpExchange exchange) throws IOException {
        String SQL_INSERT_PLAYER = "INSERT INTO players "
                + "(account_id, skin_color, hair_color, shirt_color, pants_color, world_x, world_y, weapon, inventory, inventoryAmounts, "
                + "quest_progress, attack_experience, defence_experience, "
                + "strength_experience, hitpoints_experience, magic_experience) "
                + "VALUES (?, 0, 0, 0, 0, 0, 0, null, ?, ?, ?, 0, 0, 0, 1200, 0)";

        try (Connection connection = DatabaseConnection.createDatabaseConnection();
                var statement = connection.prepareStatement(SQL_INSERT_PLAYER)) {
            statement.setInt(1, accountID);
            int[] inventory = new int[20];
            int[] inventoryAmounts = new int[20];
            int[] questProgress = new int[5];

            inventory[0] = 102;
            inventoryAmounts[0] = 150;

            statement.setString(2, gson.toJson(inventory));
            statement.setString(3, gson.toJson(inventoryAmounts));
            statement.setString(4, gson.toJson(questProgress));
            statement.executeUpdate();

            sendResponse(exchange, 200, new RegistrationResponse(true));

        } catch (SQLException e) {
            sendResponse(exchange, 500, new RegistrationResponse(false, e.getMessage()));
        }

    }

    private void sendResponse(HttpExchange exchange, int statusCode, RegistrationResponse response) throws IOException {
        String jsonResponse = gson.toJson(response);
        exchange.getResponseHeaders().set("Content-Type", "application/json");

        // Add CORS headers
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*"); // Allow all origins
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, GET, OPTIONS"); // Allow specific //
                                                                                                 // methods
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type"); // Allow specific headers
        exchange.sendResponseHeaders(statusCode, jsonResponse.getBytes().length);

        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(jsonResponse.getBytes());
        }

        exchange.close();
    }

}
