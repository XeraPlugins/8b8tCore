package me.txmc.core.database;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.*;

public class GeneralDatabase {
    private final String url;
    private final ExecutorService databaseExecutor;

    public GeneralDatabase(String pluginFolderPath) {
        String databasePath = pluginFolderPath + "/Database/8b8tCorePlayerDB.db";
        this.url = "jdbc:sqlite:" + databasePath;
        this.databaseExecutor = Executors.newCachedThreadPool();

        File databaseDir = new File(pluginFolderPath + "/Database");
        if (!databaseDir.exists()) {
            databaseDir.mkdirs();
        }

        createTables();
    }

    private void createTables() {
        String createNicknamesTableSQL = "CREATE TABLE IF NOT EXISTS playerdata (" +
                "username TEXT PRIMARY KEY NOT NULL, " +
                "displayname TEXT, " +
                "muted INTEGER" +
                ");";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            stmt.execute(createNicknamesTableSQL);

            if (!columnExists(conn, "playerdata", "showJoinMsg")) {
                stmt.execute("ALTER TABLE playerdata ADD COLUMN showJoinMsg BOOLEAN DEFAULT TRUE;");
            }

            if (!columnExists(conn, "playerdata", "muted")) {
                stmt.execute("ALTER TABLE playerdata ADD COLUMN muted INTEGER;");
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
        String insertSQL = "INSERT INTO playerdata (username, displayname) VALUES (?, ?) ON CONFLICT(username) DO UPDATE SET displayname = excluded.displayname;";

        databaseExecutor.execute(() -> {
            try (Connection conn = DriverManager.getConnection(url);
                 PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
                pstmt.setString(1, username);
                pstmt.setString(2, displayname);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public String getNickname(String username) {
        Callable<String> task = () -> {
            try (Connection conn = DriverManager.getConnection(url);
                 PreparedStatement pstmt = conn.prepareStatement("SELECT displayname FROM playerdata WHERE username = ?")) {
                pstmt.setString(1, username);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    return rs.getString("displayname");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        };

        Future<String> future = databaseExecutor.submit(task);
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void updateShowJoinMsg(String username, boolean showJoinMsg) {
        String updateSQL = "INSERT INTO playerdata (username, displayname, showJoinMsg) " +
                "VALUES (?, ?, ?) " +
                "ON CONFLICT(username) DO UPDATE SET showJoinMsg = excluded.showJoinMsg;";
        databaseExecutor.execute(() -> {
            try (Connection conn = DriverManager.getConnection(url);
                 PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
                pstmt.setString(1, username);
                pstmt.setString(2, username);
                pstmt.setBoolean(3, showJoinMsg);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public boolean getPlayerShowJoinMsg(String username) {
        Callable<Boolean> task = () -> {
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
        };

        Future<Boolean> future = databaseExecutor.submit(task);
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return true;
        }
    }

    public boolean isMuted(String username) {
        Callable<Boolean> task = () -> {
            String sql = "SELECT muted FROM playerdata WHERE username = ?";

            try (Connection conn = DriverManager.getConnection(url);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    long mutedUntil = rs.getLong("muted");
                    if (mutedUntil > Instant.now().getEpochSecond()) {
                        return true;
                    } else {
                        unmute(username);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return false;
        };

        Future<Boolean> future = databaseExecutor.submit(task);
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void mute(String username, long timestamp) {
        String sql = "INSERT INTO playerdata (username, displayname, muted) VALUES (?, ?, ?) ON CONFLICT(username) DO UPDATE SET muted = excluded.muted;";

        databaseExecutor.execute(() -> {
            try (Connection conn = DriverManager.getConnection(url);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, username);
                pstmt.setLong(3, timestamp);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void unmute(String username) {
        String sql = "INSERT INTO playerdata (username, displayname, muted) VALUES (?, ?, NULL) ON CONFLICT(username) DO UPDATE SET muted = NULL;";
        databaseExecutor.execute(() -> {
            try (Connection conn = DriverManager.getConnection(url);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, username);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void close() {
        databaseExecutor.shutdown();
        try {
            if (!databaseExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                databaseExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            databaseExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}