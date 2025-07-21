package me.txmc.core.timestats;

import java.io.File;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static java.lang.System.currentTimeMillis;
import java.util.Date;

/**
 *
 * @author 5aks
 * @since 7/19/2025 5:30 PM This file was created as a part of 8b8tCore
 *
 */

public class Database {

    private static TimeStatsSection main;
    private String url = "";
    private final ExecutorService databaseExecutor;
    private static final String DB_USERNAME = "sa"; // Default H2 username                                                      //Prepare DB info.
    private static final String DB_PASSWORD = ""; // Default H2 password

    public Database(String pluginPath, TimeStatsSection main) {
        this.main = main;
        String databasePath = pluginPath + "/TimeStats.db";
        this.url = "jdbc:h2:file:" + databasePath;
        this.databaseExecutor = Executors.newCachedThreadPool();                                                                //Instantiate.

        File databaseDir = new File(databasePath);
        if (!databaseDir.exists()) {
            databaseDir.mkdirs();
        }
        createTables();
    }

    private void createTables() {
        String createSQLTable = "CREATE TABLE IF NOT EXISTS PLAYERS (" //Create DB table.
                + "name VARCHAR NOT NULL PRIMARY KEY,"
                + "joindate BIGINT,"
                + "seen BIGINT NOT NULL, "
                + "playtime BIGINT NOT NULL "
                + ");";
        try (Connection connection = DriverManager.getConnection(url, DB_USERNAME, DB_PASSWORD); Statement stmt = connection.createStatement()) {
            stmt.execute(createSQLTable);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertPlayer(String player, long last, long total) {                                                            //Updates DB on player join/leave
        String name = player;
        long seen = last;
        long playtime = total;

        try (Connection connection = DriverManager.getConnection(url, DB_USERNAME, DB_PASSWORD)) {
            try (PreparedStatement insertStatement = connection.prepareStatement(                                               //Set statement to execute
                    "MERGE INTO PLAYERS (name, seen, playtime) KEY(name) VALUES (?, ?, ?)" 
            )) {
                insertStatement.setString(1, name);
                insertStatement.setLong(2, seen);
                insertStatement.setLong(3, playtime);
                insertStatement.executeUpdate();                                                                                //Executes statement after assigning rows
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertNewPlayer(String player, long last, long total) {                                                         //Updates on new player join
        String name = player;
        long seen = last;
        long playtime = total;

        try (Connection connection = DriverManager.getConnection(url, DB_USERNAME, DB_PASSWORD)) {
            try (PreparedStatement insertStatement = connection.prepareStatement(                                               //Set statement to execute
                    "INSERT INTO PLAYERS (name, joindate, seen, playtime) VALUES (?, ?, ?, ?)"                      
            )) {
                insertStatement.setString(1, name);
                insertStatement.setLong(2, new Date().getTime());
                insertStatement.setLong(3, seen);
                insertStatement.setLong(4, playtime);
                insertStatement.executeUpdate();                                                                                //Execute statement after assigning rows
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public PlayerStats fetchPlayer(String name) {
        try {
            try (Connection connection = DriverManager.getConnection(url, DB_USERNAME, DB_PASSWORD); PreparedStatement stmt = connection.prepareStatement("SELECT * FROM PLAYERS WHERE name = ?")) {
                stmt.setString(1, name);
                ResultSet resultSet = stmt.executeQuery();                                                                      //Executes statement to find instead of add
                if (resultSet.next()) {
                    long date = resultSet.getLong("joindate");
                    long seen = resultSet.getLong("seen");
                    long playtime = resultSet.getLong("playtime");
                    PlayerStats playerStats = new PlayerStats(name, date, seen, playtime);                                      //Create new PlayerStats to return

                    return playerStats;
                }
                return null;                                                                                                    //Reached when no player is found
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
