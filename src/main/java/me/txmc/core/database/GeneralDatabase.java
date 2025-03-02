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

        String addShowJoinMsgColumnSQL = "ALTER TABLE playerdata ADD COLUMN showJoinMsg BOOLEAN DEFAULT TRUE;";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            stmt.execute(createNicknamesTableSQL);

            if (!columnExists(conn, "playerdata", "showJoinMsg")) {
                stmt.execute(addShowJoinMsgColumnSQL);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean columnExists(Connection conn, String tableName, String columnName) {
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, tableName, columnName)) {
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
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

    public void updateShowJoinMsg(String username, boolean showJoinMsg) {
        String updateSQL = "UPDATE playerdata SET showJoinMsg = ? WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
            pstmt.setBoolean(1, showJoinMsg);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean getPlayerShowJoinMsg(String username) {
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement("SELECT showJoinMsg FROM playerdata WHERE username = ?")) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("showJoinMsg") == 1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

}
