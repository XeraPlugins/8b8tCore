package me.txmc.core.database;

import java.io.File;
import java.sql.*;

public class GeneralDatabase {
    private final String url;

    public GeneralDatabase(String pluginFolderPath) {
        String databasePath = pluginFolderPath + "/Database/8b8tCorePlayerDB.db";
        this.url = "jdbc:sqlite:" + databasePath;

        File databaseDir = new File(pluginFolderPath + "/Database");
        if (!databaseDir.exists()) {
            databaseDir.mkdirs();
        }

        createTables();
    }

    private void createTables() {
        String createNicknamesTableSQL = "CREATE TABLE IF NOT EXISTS playerdata (" +
                "username TEXT PRIMARY KEY NOT NULL, " +
                "displayname TEXT NOT NULL" +
                ");";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            stmt.execute(createNicknamesTableSQL);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertNickname(String username, String displayname) {
        String insertSQL = "INSERT OR REPLACE INTO playerdata (username, displayname) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            pstmt.setString(1, username);
            pstmt.setString(2, displayname);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getNickname(String username) {
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT displayname FROM playerdata WHERE username = ?")) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getString("displayname");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

}
