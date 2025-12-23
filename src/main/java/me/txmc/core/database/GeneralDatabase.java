package me.txmc.core.database;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.util.concurrent.*;

/**
 * @author MindComplexity (aka Libalpm)
 * @since 2025/12/21
 * This file was created as a part of 8b8tCore
*/

public class GeneralDatabase {
    private static GeneralDatabase instance;
    private final String url;
    private final ExecutorService databaseExecutor;

    private GeneralDatabase(String pluginFolderPath) {
        String databasePath = pluginFolderPath + "/Database/8b8tCorePlayerDB.db";
        this.url = "jdbc:sqlite:" + databasePath;
        this.databaseExecutor = Executors.newFixedThreadPool(2);
        File databaseDir = new File(pluginFolderPath + "/Database");
        if (!databaseDir.exists()) {
            databaseDir.mkdirs();
        }

        createTables();
    }

    /**
     * Initialize the database singleton.
     */
    public static synchronized void initialize(String pluginFolderPath) {
        if (instance == null) {
            instance = new GeneralDatabase(pluginFolderPath);
        }
    }

    /**
     * Get the database instance. (Intialized first)
     */
    public static GeneralDatabase getInstance() {
        if (instance == null) {
            throw new IllegalStateException("GeneralDatabase not initialized! Call initialize() first.");
        }
        return instance;
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url);
    }

    private void createTables() {
        String createNicknamesTableSQL = "CREATE TABLE IF NOT EXISTS playerdata (" +
                "username TEXT PRIMARY KEY NOT NULL, " +
                "displayname TEXT, " +
                "muted INTEGER" +
                ");";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createNicknamesTableSQL);

            addColumnIfNotExists(conn, stmt, "showJoinMsg", "BOOLEAN DEFAULT TRUE");
            addColumnIfNotExists(conn, stmt, "muted", "INTEGER");
            addColumnIfNotExists(conn, stmt, "hidePrefix", "BOOLEAN DEFAULT FALSE");
            addColumnIfNotExists(conn, stmt, "hideDeathMessages", "BOOLEAN DEFAULT FALSE");
            addColumnIfNotExists(conn, stmt, "hideAnnouncements", "BOOLEAN DEFAULT FALSE");
            addColumnIfNotExists(conn, stmt, "hideBadges", "BOOLEAN DEFAULT FALSE");
            addColumnIfNotExists(conn, stmt, "selectedRank", "TEXT");
            addColumnIfNotExists(conn, stmt, "customGradient", "TEXT");
            addColumnIfNotExists(conn, stmt, "hideCustomTab", "BOOLEAN DEFAULT FALSE");
            addColumnIfNotExists(conn, stmt, "useVanillaLeaderboard", "BOOLEAN DEFAULT FALSE");
            addColumnIfNotExists(conn, stmt, "gradient_animation", "TEXT DEFAULT 'none'");
            addColumnIfNotExists(conn, stmt, "gradient_speed", "INTEGER DEFAULT 5");
            addColumnIfNotExists(conn, stmt, "nameDecorations", "TEXT");
            addColumnIfNotExists(conn, stmt, "preventPhantomSpawn", "BOOLEAN DEFAULT TRUE");
            addColumnIfNotExists(conn, stmt, "prefixGradient", "TEXT");
            addColumnIfNotExists(conn, stmt, "prefix_animation", "TEXT DEFAULT 'none'");
            addColumnIfNotExists(conn, stmt, "prefix_speed", "INTEGER DEFAULT 5");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addColumnIfNotExists(Connection conn, Statement stmt, String columnName, String columnDef) throws SQLException {
        if (!columnExists(conn, "playerdata", columnName)) {
            stmt.execute("ALTER TABLE playerdata ADD COLUMN " + columnName + " " + columnDef + ";");
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

    private CompletableFuture<Void> executeUpdate(String sql, Object... params) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    pstmt.setObject(i + 1, params[i]);
                }
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, databaseExecutor);
    }

    private <T> T executeQuery(String sql, ResultSetMapper<T> mapper, T defaultValue, Object... params) {
        Callable<T> task = () -> {
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    pstmt.setObject(i + 1, params[i]);
                }
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return mapper.map(rs);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return defaultValue;
        };

        Future<T> future = databaseExecutor.submit(task);
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    @FunctionalInterface
    private interface ResultSetMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    public CompletableFuture<Void> upsertPlayer(String username, String column, Object value) {
        String sql = "INSERT INTO playerdata (username, displayname, " + column + ") VALUES (?, ?, ?) " +
                     "ON CONFLICT(username) DO UPDATE SET " + column + " = excluded." + column + ";";
        return executeUpdate(sql, username, username, value);
    }

    public CompletableFuture<Void> insertNickname(String username, String displayname) {
        String sql = "INSERT INTO playerdata (username, displayname) VALUES (?, ?) " +
                     "ON CONFLICT(username) DO UPDATE SET displayname = excluded.displayname;";
        return executeUpdate(sql, username, displayname);
    }

    public String getNickname(String username) {
        return executeQuery(
                "SELECT displayname FROM playerdata WHERE username = ?",
                rs -> rs.getString("displayname"),
                null,
                username
        );
    }

    public String getPlayerData(String username, String column) {
        return executeQuery(
                "SELECT " + column + " FROM playerdata WHERE username = ?",
                rs -> rs.getString(column),
                null,
                username
        );
    }

    public void updateShowJoinMsg(String username, boolean showJoinMsg) {
        upsertPlayer(username, "showJoinMsg", showJoinMsg);
    }

    public boolean getPlayerShowJoinMsg(String username) {
        return executeQuery(
                "SELECT showJoinMsg FROM playerdata WHERE username = ?",
                rs -> rs.getInt("showJoinMsg") == 1,
                true,
                username
        );
    }

    public void updateHidePrefix(String username, boolean hidePrefix) {
        upsertPlayer(username, "hidePrefix", hidePrefix);
    }

    public boolean getPlayerHidePrefix(String username) {
        return executeQuery(
                "SELECT hidePrefix FROM playerdata WHERE username = ?",
                rs -> rs.getInt("hidePrefix") == 1,
                false,
                username
        );
    }

    public void updateHideDeathMessages(String username, boolean hide) {
        upsertPlayer(username, "hideDeathMessages", hide);
    }

    public boolean getPlayerHideDeathMessages(String username) {
        return executeQuery(
                "SELECT hideDeathMessages FROM playerdata WHERE username = ?",
                rs -> rs.getInt("hideDeathMessages") == 1,
                false,
                username
        );
    }

    public void updateHideAnnouncements(String username, boolean hide) {
        upsertPlayer(username, "hideAnnouncements", hide);
    }

    public boolean getPlayerHideAnnouncements(String username) {
        return executeQuery(
                "SELECT hideAnnouncements FROM playerdata WHERE username = ?",
                rs -> rs.getInt("hideAnnouncements") == 1,
                false,
                username
        );
    }

    public void updateHideBadges(String username, boolean hide) {
        upsertPlayer(username, "hideBadges", hide);
    }

    public boolean getPlayerHideBadges(String username) {
        return executeQuery(
                "SELECT hideBadges FROM playerdata WHERE username = ?",
                rs -> rs.getInt("hideBadges") == 1,
                false,
                username
        );
    }

    public boolean isMuted(String username) {
        Callable<Boolean> task = () -> {
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("SELECT muted FROM playerdata WHERE username = ?")) {
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
            return future.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void mute(String username, long timestamp) {
        upsertPlayer(username, "muted", timestamp);
    }

    public void unmute(String username) {
        String sql = "UPDATE playerdata SET muted = NULL WHERE username = ?;";
        executeUpdate(sql, username);
    }

    public CompletableFuture<Void> updateSelectedRank(String username, String rank) {
        return upsertPlayer(username, "selectedRank", rank);
    }

    public String getSelectedRank(String username) {
        return executeQuery(
                "SELECT selectedRank FROM playerdata WHERE username = ?",
                rs -> rs.getString("selectedRank"),
                null,
                username
        );
    }

    public CompletableFuture<Void> updateCustomGradient(String username, String gradient) {
        return upsertPlayer(username, "customGradient", gradient);
    }
 
    public CompletableFuture<Void> updateGradient(String username, String gradient) {
        return updateCustomGradient(username, gradient);
    }

    public String getCustomGradient(String username) {
        return executeQuery(
                "SELECT customGradient FROM playerdata WHERE username = ?",
                rs -> rs.getString("customGradient"),
                null,
                username
        );
    }

    public String getGradient(String username) {
        return getCustomGradient(username);
    }

    public void updateHideCustomTab(String username, boolean hide) {
        upsertPlayer(username, "hideCustomTab", hide);
    }

    public boolean getHideCustomTab(String username) {
        return executeQuery(
                "SELECT hideCustomTab FROM playerdata WHERE username = ?",
                rs -> rs.getBoolean("hideCustomTab"),
                false,
                username
        );
    }

    public void setVanillaLeaderboard(String username, boolean useVanilla) {
        upsertPlayer(username, "useVanillaLeaderboard", useVanilla);
    }

    public boolean isVanillaLeaderboard(String username) {
        return executeQuery(
                "SELECT useVanillaLeaderboard FROM playerdata WHERE username = ?",
                rs -> rs.getBoolean("useVanillaLeaderboard"),
                false,
                username
        );
    }

    public CompletableFuture<Void> updateGradientAnimation(String username, String animation) {
        return upsertPlayer(username, "gradient_animation", animation);
    }

    public String getGradientAnimation(String username) {
        return executeQuery(
                "SELECT gradient_animation FROM playerdata WHERE username = ?",
                rs -> rs.getString("gradient_animation"),
                "none",
                username
        );
    }

    public CompletableFuture<Void> updateGradientSpeed(String username, int speed) {
        return upsertPlayer(username, "gradient_speed", speed);
    }

    public int getGradientSpeed(String username) {
        return executeQuery(
                "SELECT gradient_speed FROM playerdata WHERE username = ?",
                rs -> rs.getInt("gradient_speed"),
                5,
                username
        );
    }

    public void updatePreventPhantomSpawn(String username, boolean prevent) {
        upsertPlayer(username, "preventPhantomSpawn", prevent);
    }

    public boolean getPreventPhantomSpawn(String username) {
        return executeQuery(
                "SELECT preventPhantomSpawn FROM playerdata WHERE username = ?",
                rs -> rs.getBoolean("preventPhantomSpawn"),
                true,
                username
        );
    }

    public CompletableFuture<Void> updatePrefixGradient(String username, String gradient) {
        return upsertPlayer(username, "prefixGradient", gradient);
    }

    public String getPrefixGradient(String username) {
        return executeQuery(
                "SELECT prefixGradient FROM playerdata WHERE username = ?",
                rs -> rs.getString("prefixGradient"),
                null,
                username
        );
    }

    public CompletableFuture<Void> updatePrefixAnimation(String username, String animation) {
        return upsertPlayer(username, "prefix_animation", animation);
    }

    public String getPrefixAnimation(String username) {
        return executeQuery(
                "SELECT prefix_animation FROM playerdata WHERE username = ?",
                rs -> rs.getString("prefix_animation"),
                "none",
                username
        );
    }

    public CompletableFuture<Void> updatePrefixSpeed(String username, int speed) {
        return upsertPlayer(username, "prefix_speed", speed);
    }

    public int getPrefixSpeed(String username) {
        return executeQuery(
                "SELECT prefix_speed FROM playerdata WHERE username = ?",
                rs -> rs.getInt("prefix_speed"),
                5,
                username
        );
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