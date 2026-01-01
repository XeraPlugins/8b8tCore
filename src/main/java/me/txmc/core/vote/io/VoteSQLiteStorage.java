package me.txmc.core.vote.io;

import me.txmc.core.util.GlobalUtils;
import me.txmc.core.vote.VoteEntry;

import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.logging.Level;

/**
 * SQLite storage implementation for voter storage. 
 * @author MindComplexity 
 * @since 2025/08/30
 * This file was created as a part of 8b8tCore
 */

public class VoteSQLiteStorage {
    private final File databaseFile;
    private Connection connection;

    public VoteSQLiteStorage(File databaseFile) {
        this.databaseFile = databaseFile;
        initializeDatabase();
    }

    private void initializeDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            
            String url = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);
            
            String createTableSQL = "CREATE TABLE IF NOT EXISTS votes (" +
                "username TEXT PRIMARY KEY, " +
                "times_voted INTEGER NOT NULL DEFAULT 0, " +
                "timestamp INTEGER NOT NULL" +
                ")";
            
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(createTableSQL);
            }
            
            // GlobalUtils.log(Level.INFO, "SQLite vote database initialized: " + databaseFile.getAbsolutePath());
            
        } catch (Exception e) {
            GlobalUtils.log(Level.SEVERE, "Failed to initialize SQLite vote database");
            e.printStackTrace();
        }
    }

    public void save(java.util.Map<String, VoteEntry> voteMap) {
        if (connection == null) {
            GlobalUtils.log(Level.WARNING, "SQLite connection is null, cannot save votes");
            return;
        }
        try {
            connection.setAutoCommit(false);
            try (Statement clearStmt = connection.createStatement()) {
                clearStmt.execute("DELETE FROM votes");
            }

            String insertSQL = "INSERT INTO votes (username, times_voted, timestamp) VALUES (?, ?, ?)";

            try (PreparedStatement stmt = connection.prepareStatement(insertSQL)) {
                for (var entry : voteMap.entrySet()) {
                    stmt.setString(1, entry.getKey());
                    stmt.setInt(2, entry.getValue().getCount());
                    stmt.setLong(3, entry.getValue().getTimestamp());
                    stmt.addBatch();
                }
                
                stmt.executeBatch();
            }
            connection.commit();
        } catch (SQLException e) {
            GlobalUtils.log(Level.SEVERE, "Failed to save votes to SQLite");
            e.printStackTrace();
            try {
                connection.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public HashMap<String, VoteEntry> load() {
        HashMap<String, VoteEntry> voteMap = new HashMap<>();
        
        if (connection == null) {
            GlobalUtils.log(Level.WARNING, "SQLite connection is null, returning empty vote map");
            return voteMap;
        }

        String selectSQL = "SELECT username, times_voted, timestamp FROM votes";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(selectSQL)) {
            
            while (rs.next()) {
                String username = rs.getString("username");
                int timesVoted = rs.getInt("times_voted");
                long timestamp = rs.getLong("timestamp");
                
                voteMap.put(username, new VoteEntry(timesVoted, timestamp));
            }
            
            // GlobalUtils.log(Level.INFO, "Loaded " + voteMap.size() + " vote entries from SQLite");
            
        } catch (SQLException e) {
            GlobalUtils.log(Level.SEVERE, "Failed to load votes from SQLite");
            e.printStackTrace();
        }
        
        return voteMap;
    }

    /**
     * Close database connection
     */
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                GlobalUtils.log(Level.INFO, "SQLite vote database connection closed");
            } catch (SQLException e) {
                GlobalUtils.log(Level.WARNING, "Error closing SQLite connection");
                e.printStackTrace();
            }
        }
    }


}
