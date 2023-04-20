package cz.it4i.fiji.datastore.TestUI;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import cz.it4i.fiji.datastore.security.OAuthServer;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class OAuthServerIF {
    static String path="http://localhost:8443/";
    private static List<OAuthServer> loadServers() {
        try {
            URL url = new URL(path + "oauth-servers");
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
                return gson.fromJson(response.toString(), new TypeToken<List<OAuthServer>>() {}.getType());
            } else {
                connection.disconnect();
                return new ArrayList<>();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    private static OAuthServer loadServerById(String id) {
        try {
            URL url = new URL(path + "oauth-servers/" + id);
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
                return gson.fromJson(response.toString(), OAuthServer.class);
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

        List<OAuthServer> servers = loadServers();
        if (servers.size() < 1) {
            System.out.println("No Servers to update");
            return;
        }

        for (OAuthServer server : servers) {
            System.out.println(server.getId() + ". " + server.getName());
        }

        System.out.println("");
        System.out.println("Enter the id of the server to update, or type 'back' to return to the main menu:");
        scanner.nextLine();
        String input = scanner.nextLine();

        if (input.equalsIgnoreCase("back")) {
            return; // Return to the main menu
        }

        String id = input;

        OAuthServer serverToUpdate = loadServerById(id);
        if (serverToUpdate == null) {
            System.out.println("Server with id " + id + " does not exist.");
            return;
        }

        System.out.println("Server Details:");
        System.out.println("1. Name: " + serverToUpdate.getName());
        System.out.println("2. Auth URI: " + serverToUpdate.getAuthURI());
        System.out.println("3. Redirect URI: " + serverToUpdate.getRedirectURI());
        System.out.println("4. User Info URI: " + serverToUpdate.getUserInfoURI());
        System.out.println("5. Token Endpoint URI: " + serverToUpdate.getTokenEndpointURI());
        System.out.println("6. Client ID: " + serverToUpdate.getClientID());
        System.out.println("7. Client Secret: " + serverToUpdate.getClientSecret());
        System.out.println("8. Back");

        System.out.println("Enter the number of the parameter to update, or type 'back' to return to the main menu:");
        input = scanner.nextLine();

        if (input.equalsIgnoreCase("back")) {
            return; // Return to the main menu
        }

        int parameterNumber;
        try {
            parameterNumber = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Invalid input.");
            return;
        }

        switch (parameterNumber) {
            case 1:
                System.out.println("Enter the new name:");
                String newName = scanner.nextLine();
                serverToUpdate.setName(newName);
                break;
            case 2:
                System.out.println("Enter the new Auth URI:");
                String newAuthURI = scanner.nextLine();
                serverToUpdate.setAuthURI(URI.create(newAuthURI));
                break;
            case 3:
                System.out.println("Enter the new Redirect URI:");
                String newRedirectURI = scanner.nextLine();
                serverToUpdate.setRedirectURI(URI.create(newRedirectURI));
                break;
            case 4:
                System.out.println("Enter the new User Info URI:");
                String newUserInfoURI = scanner.nextLine();
                serverToUpdate.setUserInfoURI(URI.create(newUserInfoURI));
                break;
            case 5:
                System.out.println("Enter the new Token Endpoint URI:");
                String newTokenEndpointURI = scanner.nextLine();
                serverToUpdate.setTokenEndpointURI(URI.create(newTokenEndpointURI));
                break;
            case 6:
                System.out.println("Enter the new Client ID:");
                String newClientID = scanner.nextLine();
                serverToUpdate.setClientID(newClientID);
                break;
            case 7:
                System.out.println("Enter the new Client Secret:");
                String newClientSecret = scanner.nextLine();
                serverToUpdate.setClientSecret(newClientSecret);
                break;
            case 8:
                // Back option, do nothing and return to the main menu
                return;
            default:
                System.out.println("Invalid parameter number.");
                return;
        }

        try {
            URL url = new URL(path + "oauth-servers/" + id);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            Gson gson = new GsonBuilder().create();
            String json = gson.toJson(serverToUpdate);

            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(json.getBytes());
            outputStream.flush();
            outputStream.close();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                System.out.println("OAuth server updated successfully.");
            } else {
                System.out.println("Something went wrong");
            }

            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void deleteForm(Scanner scanner) {
        List<OAuthServer> servers = loadServers();
        if(servers.size()<1)
        {
            System.out.println("No Servers to delete");
            return;
        }
        for (int i = 0; i < servers.size(); i++) {
            System.out.println(servers.get(i).getId() + ". " + servers.get(i).getName());
        }
        System.out.println("");
        System.out.println("Enter id for delete:");
        String id = String.valueOf(scanner.nextInt());

        try {
            URL url = new URL(path + "oauth-servers/" + id);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("DELETE");

            if (connection.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT) {
                System.out.println("OAuth server deleted successfully.");
            } else {
                System.out.println("Something went wrong");
            }

            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    static void createServerForm(Scanner scanner) {
        scanner.nextLine();
        OAuthServer oauthServer = new OAuthServer();

        System.out.println("Enter Server Alias:");
        String name = scanner.nextLine();
        if (TestUI.checkTerminate(name)) {
            return;
        }
        oauthServer.setName(name);

        System.out.println("Enter authURI:");
        String authURI = scanner.nextLine();
        if (TestUI.checkTerminate(authURI)) {
            return;
        }
        oauthServer.setAuthURI(URI.create(authURI));

        System.out.println("Enter redirectURI:");
        String redirectURI = scanner.nextLine();
        if (TestUI.checkTerminate(redirectURI)) {
            return;
        }
        oauthServer.setRedirectURI(URI.create(redirectURI));

        System.out.println("Enter userInfoURI:");
        String userInfoURI = scanner.nextLine();
        if (TestUI.checkTerminate(userInfoURI)) {
            return;
        }
        oauthServer.setUserInfoURI(URI.create(userInfoURI));

        System.out.println("Enter tokenEndpointURI:");
        String tokenEndpointURI = scanner.nextLine();
        if (TestUI.checkTerminate(tokenEndpointURI)) {
            return;
        }
        oauthServer.setTokenEndpointURI(URI.create(tokenEndpointURI));

        System.out.println("Enter Service ID:");
        String clientID = scanner.nextLine();
        if (TestUI.checkTerminate(clientID)) {
            return;
        }
        oauthServer.setClientID(clientID);

        System.out.println("Enter Service Secret:");
        String clientSecret = scanner.nextLine();
        if (TestUI.checkTerminate(clientSecret)) {
            return;
        }
        oauthServer.setClientSecret(clientSecret);

        try {
            URL url = new URL(path+"oauth-servers/create");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            Gson gson = new GsonBuilder().create();
            String json = gson.toJson(oauthServer);

            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(json.getBytes());
            outputStream.flush();
            outputStream.close();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_CREATED) {
                System.out.println("OAuth server created successfully.");
            } else {
               System.out.println("Something went wrong");
            }

            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
