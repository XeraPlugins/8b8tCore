package me.txmc.core.timestats;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONObject;
/**
 *
 * @author 5aks
 * @since 7/18/2025 8:05 AM This file was created as a part of 8b8tCore
 *
 */
public class NameLookup {

    public static boolean nameLookUp(String name) {
        try {
            URI uri = URI.create("https://api.mojang.com/users/profiles/minecraft/" + name);
            URL url = uri.toURL();
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("user-Agent", "MinecraftServer/1.20.1");
            connection.setRequestProperty("Accept", "application/json");
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String in;
                StringBuilder sb = new StringBuilder();
                while ((in = reader.readLine()) != null) {
                    sb.append(in);
                }
                reader.close();
                connection.disconnect();
                String jsonResponse = sb.toString().trim();
                if (!jsonResponse.isEmpty() && jsonResponse.startsWith("{")) {
                    JSONObject json = new JSONObject(jsonResponse);
                    return json.has("name");
                }

                return false;
            } else {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                String in;
                StringBuilder sb = new StringBuilder();
                while((in = reader.readLine()) != null){
                    sb.append(in);
                }
                reader.close();
                connection.disconnect();
                String errorStream = sb.toString().trim();
                System.out.println("\n\n\n\nRESPONSE CODE: " + responseCode + "\n\n\n\n");
                System.out.println(errorStream);
                System.out.println("\n\n\n\nRESPONSE CODE: " + responseCode + "\n\n\n\n");
            }
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return false;
    }
}
