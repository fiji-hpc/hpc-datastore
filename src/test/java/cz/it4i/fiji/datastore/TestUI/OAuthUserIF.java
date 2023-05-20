package cz.it4i.fiji.datastore.TestUI;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import cz.it4i.fiji.datastore.security.User;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class OAuthUserIF {
    static String path = "http://localhost:8443/";

    public static List<User> loadUsers() {
        try {
            URL url = new URL(path + "oauth-users");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();
                connection.disconnect();

                Gson gson = new GsonBuilder().create();
                return gson.fromJson(response.toString(), new TypeToken<List<User>>() {}.getType());
            } else {
                connection.disconnect();
                return new ArrayList<>();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static User loadUserById(String id) {
        try {
            URL url = new URL(path + "oauth-users/" + id);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();
                connection.disconnect();

                Gson gson = new GsonBuilder().create();
                return gson.fromJson(response.toString(), User.class);
            } else if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                connection.disconnect();
                return null;
            } else {
                connection.disconnect();
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void updateForm(Scanner scanner) {
        //scanner.nextLine();
        List<User> users = loadUsers();
        if (users.isEmpty()) {
            System.out.println("No users to update.");
            return;
        }

        for (User user : users) {
            System.out.println(user.getId() + ". " + user.getOauthAlias());
        }

        System.out.println("");
        System.out.println("Enter the id of the user to update, or type 'back' to return to the main menu:");
        scanner.nextLine();
        String input = scanner.nextLine();

        if (input.equalsIgnoreCase("back")) {
            return; // Return to the main menu
        }

        String id = input;

        User userToUpdate = loadUserById(id);
        if (userToUpdate == null) {
            System.out.println("User with id " + id + " does not exist.");
            return;
        }

        System.out.println("User Details:");
        System.out.println("1. Alias: " + userToUpdate.getOauthAlias());
        System.out.println("2. Client Token: " + userToUpdate.getClientToken());
        System.out.println("3. Client ID: " + userToUpdate.getClientID());
        System.out.println("4. Back");

        System.out.println("Enter the number corresponding to the field you want to update:");
        input = scanner.nextLine();

        switch (input) {
            case "1":
                System.out.println("Enter the new alias:");
                String alias = scanner.nextLine();
                userToUpdate.setOauthAlias(alias);
                break;
            case "2":
                System.out.println("Enter the new client token:");
                String clientToken = scanner.nextLine();
                userToUpdate.setClientToken(clientToken);
                break;
            case "3":
                System.out.println("Enter the new client ID:");
                String clientID = scanner.nextLine();
                userToUpdate.setClientID(clientID);
                break;
            case "4":
                return; // Return to the main menu
            default:
                System.out.println("Invalid input.");
                return;
        }

        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(userToUpdate);

        try {
            URL url = new URL(path + "oauth-users/" + id);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            connection.getOutputStream().write(json.getBytes());
            connection.getOutputStream().flush();
            connection.getOutputStream().close();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT) {
                System.out.println("User updated successfully.");
            } else {
                System.out.println("Failed to update user.");
            }

            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void deleteUser(Scanner scanner) {
        List<User> users = loadUsers();
        if (users.isEmpty()) {
            System.out.println("No users to delete.");
            return;
        }

        for (User user : users) {
            System.out.println(user.getId() + ". " + user.getOauthAlias());
        }

        System.out.println("");
        System.out.println("Enter the id of the user to delete, or type 'back' to return to the main menu:");
        scanner.nextLine();
        String input = scanner.nextLine();

        if (input.equalsIgnoreCase("back")) {
            return; // Return to the main menu
        }

        String id = input;

        User userToDelete = loadUserById(id);
        if (userToDelete == null) {
            System.out.println("User with id " + id + " does not exist.");
            return;
        }

        try {
            URL url = new URL(path + "oauth-users/" + id);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("DELETE");

            if (connection.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT) {
                System.out.println("User deleted successfully.");
            } else {
                System.out.println("Failed to delete user.");
            }

            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void createUser(Scanner scanner) {
        scanner.nextLine();
        System.out.println("Enter the user details:");

        System.out.println("Alias of Server:");
        String alias = scanner.nextLine();

        System.out.println("Client Token:");
        String clientToken = scanner.nextLine();

        System.out.println("Client ID:");
        String clientID = scanner.nextLine();

        User newUser = User.builder()
                .oauthAlias(alias)
                .clientToken(clientToken)
                .clientID(clientID)
                .build();

        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(newUser);

        try {
            URL url = new URL(path + "oauth-users");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            connection.getOutputStream().write(json.getBytes());
            connection.getOutputStream().flush();
            connection.getOutputStream().close();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_CREATED) {
                System.out.println("User created successfully.");
            } else {
                System.out.println("Failed to create user.");
            }

            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
