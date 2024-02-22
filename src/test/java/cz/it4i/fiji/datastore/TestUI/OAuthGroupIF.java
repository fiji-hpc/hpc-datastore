package cz.it4i.fiji.datastore.TestUI;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import cz.it4i.fiji.datastore.security.OAuthGroupDTO;
import cz.it4i.fiji.datastore.security.User;
import cz.it4i.fiji.datastore.security.OAuthGroup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class OAuthGroupIF {
    static String path = "http://localhost:8443/";

    private static List<OAuthGroup> loadGroups() {
        try {
            URL url = new URL(path + "oauth-groups");
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
                return gson.fromJson(response.toString(), new TypeToken<List<OAuthGroup>>() {}.getType());
            } else {
                connection.disconnect();
                return new ArrayList<>();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private static OAuthGroup loadGroupById(String id) {
        try {
            URL url = new URL(path + "oauth-groups/" + id);
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
                return gson.fromJson(response.toString(), OAuthGroup.class);
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

    public static void updateGroupForm(Scanner scanner) {
        List<OAuthGroup> groups = loadGroups();
        if (groups.isEmpty()) {
            System.out.println("No groups to update.");
            return;
        }

        for (OAuthGroup group : groups) {
            System.out.println(group.getId() + ". " + group.getName());
        }

        System.out.println("");
        System.out.println("Enter the id of the group to update, or type 'back' to return to the main menu:");
        scanner.nextLine();
        String input = scanner.nextLine();

        if (input.equalsIgnoreCase("back")) {
            return; // Return to the main menu
        }

        String id = input;

        OAuthGroup groupToUpdate = loadGroupById(id);
        if (groupToUpdate == null) {
            System.out.println("Group with id " + id + " does not exist.");
            return;
        }

        System.out.println("Group Details:");
        System.out.println("1. Name: " + groupToUpdate.getName());
        System.out.println("2. Owner: " + groupToUpdate.getOwner());
        System.out.println("3. Back");

        System.out.println("Enter the number corresponding to the field you want to update:");
        input = scanner.nextLine();

        switch (input) {
            case "1":
                System.out.println("Enter the new name:");
                String name = scanner.nextLine();
                groupToUpdate.setName(name);
                break;
            case "2":
                List<User> users=OAuthUserIF.loadUsers();
                for (User user : users) {
                    System.out.println(user.getId() + ". " + user.getOauthAlias());
                }
                System.out.println("Enter the new owner id:");
                String owner = scanner.nextLine();
                groupToUpdate.setOwner(OAuthUserIF.loadUserById(owner));
                break;
            case "3":
                return; // Return to the main menu
            default:
                System.out.println("Invalid input.");
                return;
        }

        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(groupToUpdate);

        try {
            URL url = new URL(path + "oauth-groups/" + id);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            connection.getOutputStream().write(json.getBytes());
            connection.getOutputStream().flush();
            connection.getOutputStream().close();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT) {
                System.out.println("Group updated successfully.");
            } else {
                System.out.println("Failed to update group.");
            }

            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void deleteGroup(Scanner scanner) {
        List<OAuthGroup> groups = loadGroups();
        if (groups.isEmpty()) {
            System.out.println("No groups to delete.");
            return;
        }

        for (OAuthGroup group : groups) {
            System.out.println(group.getId() + ". " + group.getName());
        }

        System.out.println("");
        System.out.println("Enter the id of the group to delete, or type 'back' to return to the main menu:");
        scanner.nextLine();
        String input = scanner.nextLine();

        if (input.equalsIgnoreCase("back")) {
            return; // Return to the main menu
        }

        String id = input;

        OAuthGroup groupToDelete = loadGroupById(id);
        if (groupToDelete == null) {
            System.out.println("Group with id " + id + " does not exist.");
            return;
        }

        try {
            URL url = new URL(path + "oauth-groups/" + id);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("DELETE");

            if (connection.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT) {
                System.out.println("Group deleted successfully.");
            } else {
                System.out.println("Failed to delete group.");
            }

            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void createGroup(Scanner scanner) {
        scanner.nextLine();
        System.out.println("Enter the group details:");

        System.out.println("Name:");
        String name = scanner.nextLine();

        List<User> users=OAuthUserIF.loadUsers();
        for (User user : users) {
            System.out.println(user.getId() + ". " + user.getOauthAlias());
        }
        System.out.println("Enter the new owner id:");
        String owner = scanner.nextLine();

        OAuthGroupDTO newGroup = new OAuthGroupDTO();
        newGroup.setName(name);
        newGroup.setOwnerId(owner);

        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(newGroup);

        try {
            URL url = new URL(path + "oauth-groups");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            connection.getOutputStream().write(json.getBytes());
            connection.getOutputStream().flush();
            connection.getOutputStream().close();
            System.out.println(path+ "oauth-groups");
            System.out.println(connection.getResponseCode());
            if (connection.getResponseCode() == 200) {
                System.out.println("Group created successfully.");
            } else {
                System.out.println("Failed to create group.");
            }

            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
