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
            addColumnIfNotExists(conn, stmt, "prefixDecorations", "TEXT");

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

    private <T> CompletableFuture<T> executeQueryAsync(String sql, ResultSetMapper<T> mapper, T defaultValue, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
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
        }, databaseExecutor);
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

    public CompletableFuture<String> getNicknameAsync(String username) {
        return executeQueryAsync(
                "SELECT displayname FROM playerdata WHERE username = ?",
                rs -> {
                    String val = rs.getString("displayname");
                    return rs.wasNull() ? null : val;
                },
                null,
                username
        );
    }

    public String getNickname(String username) {
        return getNicknameAsync(username).join();
    }

    public CompletableFuture<String> getPlayerDataAsync(String username, String column) {
        return executeQueryAsync(
                "SELECT " + column + " FROM playerdata WHERE username = ?",
                rs -> {
                    String val = rs.getString(column);
                    return rs.wasNull() ? null : val;
                },
                null,
                username
        );
    }

    public void updateShowJoinMsg(String username, boolean showJoinMsg) {
        upsertPlayer(username, "showJoinMsg", showJoinMsg);
    }

    public CompletableFuture<Boolean> getPlayerShowJoinMsgAsync(String username) {
        return executeQueryAsync(
                "SELECT showJoinMsg FROM playerdata WHERE username = ?",
                rs -> rs.getInt("showJoinMsg") == 1,
                true,
                username
        );
    }

    public boolean getPlayerShowJoinMsg(String username) {
        return getPlayerShowJoinMsgAsync(username).join();
    }

    public void updateHidePrefix(String username, boolean hidePrefix) {
        upsertPlayer(username, "hidePrefix", hidePrefix);
    }

    public CompletableFuture<Boolean> getPlayerHidePrefixAsync(String username) {
        return executeQueryAsync(
                "SELECT hidePrefix FROM playerdata WHERE username = ?",
                rs -> rs.getInt("hidePrefix") == 1,
                false,
                username
        );
    }

    /** Sync wrappers (we async tasks to mitigate blocking these need to be handwritten a lot of stuff is still sync which is really bad for Foldenor in ver 1.21.11) */
    public boolean getPlayerHidePrefix(String username) {
        return getPlayerHidePrefixAsync(username).join();
    }

    public void updateHideDeathMessages(String username, boolean hide) {
        upsertPlayer(username, "hideDeathMessages", hide);
    }

    public CompletableFuture<Boolean> getPlayerHideDeathMessagesAsync(String username) {
        return executeQueryAsync(
                "SELECT hideDeathMessages FROM playerdata WHERE username = ?",
                rs -> rs.getInt("hideDeathMessages") == 1,
                false,
                username
        );
    }

    public boolean getPlayerHideDeathMessages(String username) {
        return getPlayerHideDeathMessagesAsync(username).join();
    }

    public void updateHideAnnouncements(String username, boolean hide) {
        upsertPlayer(username, "hideAnnouncements", hide);
    }

    public CompletableFuture<Boolean> getPlayerHideAnnouncementsAsync(String username) {
        return executeQueryAsync(
                "SELECT hideAnnouncements FROM playerdata WHERE username = ?",
                rs -> rs.getInt("hideAnnouncements") == 1,
                false,
                username
        );
    }

    public boolean getPlayerHideAnnouncements(String username) {
        return getPlayerHideAnnouncementsAsync(username).join();
    }

    public void updateHideBadges(String username, boolean hide) {
        upsertPlayer(username, "hideBadges", hide);
    }

    public CompletableFuture<Boolean> getPlayerHideBadgesAsync(String username) {
        return executeQueryAsync(
                "SELECT hideBadges FROM playerdata WHERE username = ?",
                rs -> rs.getInt("hideBadges") == 1,
                false,
                username
        );
    }
    public boolean getPlayerHideBadges(String username) {
        return getPlayerHideBadgesAsync(username).join();
    }

    public CompletableFuture<Boolean> isMutedAsync(String username) {
        return CompletableFuture.supplyAsync(() -> {
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
        }, databaseExecutor);
    }

    public CompletableFuture<Long> getMutedUntilAsync(String username) {
        return executeQueryAsync(
                "SELECT muted FROM playerdata WHERE username = ?",
                rs -> {
                    long val = rs.getLong("muted");
                    return rs.wasNull() ? 0L : val;
                },
                0L,
                username
        );
    }

    // more sync blocks
    public boolean isMuted(String username) {
        return isMutedAsync(username).join();
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

    public CompletableFuture<String> getSelectedRankAsync(String username) {
        return executeQueryAsync(
                "SELECT selectedRank FROM playerdata WHERE username = ?",
                rs -> {
                    String val = rs.getString("selectedRank");
                    return rs.wasNull() ? null : val;
                },
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

    public CompletableFuture<String> getCustomGradientAsync(String username) {
        return executeQueryAsync(
                "SELECT customGradient FROM playerdata WHERE username = ?",
                rs -> {
                    String val = rs.getString("customGradient");
                    return rs.wasNull() ? null : val;
                },
                null,
                username
        );
    }

    public CompletableFuture<String> getGradientAsync(String username) {
        return getCustomGradientAsync(username);
    }

    public void updateHideCustomTab(String username, boolean hide) {
        upsertPlayer(username, "hideCustomTab", hide);
    }

    public CompletableFuture<Boolean> getHideCustomTabAsync(String username) {
        return executeQueryAsync(
                "SELECT hideCustomTab FROM playerdata WHERE username = ?",
                rs -> rs.getBoolean("hideCustomTab"),
                false,
                username
        );
    }

    public void setVanillaLeaderboard(String username, boolean useVanilla) {
        upsertPlayer(username, "useVanillaLeaderboard", useVanilla);
    }

    public CompletableFuture<Boolean> isVanillaLeaderboardAsync(String username) {
        return executeQueryAsync(
                "SELECT useVanillaLeaderboard FROM playerdata WHERE username = ?",
                rs -> rs.getBoolean("useVanillaLeaderboard"),
                false,
                username
        );
    }

    // more sync blocks
    public boolean isVanillaLeaderboard(String username) {
        return isVanillaLeaderboardAsync(username).join();
    }

    public CompletableFuture<Void> updateGradientAnimation(String username, String animation) {
        return upsertPlayer(username, "gradient_animation", animation);
    }

    public CompletableFuture<String> getGradientAnimationAsync(String username) {
        return executeQueryAsync(
                "SELECT gradient_animation FROM playerdata WHERE username = ?",
                rs -> rs.getString("gradient_animation"),
                "none",
                username
        );
    }

    public CompletableFuture<Void> updateGradientSpeed(String username, int speed) {
        return upsertPlayer(username, "gradient_speed", speed);
    }

    public CompletableFuture<Integer> getGradientSpeedAsync(String username) {
        return executeQueryAsync(
                "SELECT gradient_speed FROM playerdata WHERE username = ?",
                rs -> rs.getInt("gradient_speed"),
                5,
                username
        );
    }

    public void updatePreventPhantomSpawn(String username, boolean prevent) {
        upsertPlayer(username, "preventPhantomSpawn", prevent);
    }

    public CompletableFuture<Boolean> getPreventPhantomSpawnAsync(String username) {
        return executeQueryAsync(
                "SELECT preventPhantomSpawn FROM playerdata WHERE username = ?",
                rs -> rs.getBoolean("preventPhantomSpawn"),
                true,
                username
        );
    }

    // more sync blocks
    public boolean getPreventPhantomSpawn(String username) {
        return getPreventPhantomSpawnAsync(username).join();
    }

    public CompletableFuture<Void> updatePrefixGradient(String username, String gradient) {
        return upsertPlayer(username, "prefixGradient", gradient);
    }

    public CompletableFuture<String> getPrefixGradientAsync(String username) {
        return executeQueryAsync(
                "SELECT prefixGradient FROM playerdata WHERE username = ?",
                rs -> {
                    String val = rs.getString("prefixGradient");
                    return rs.wasNull() ? null : val;
                },
                null,
                username
        );
    }

    public CompletableFuture<Void> updatePrefixAnimation(String username, String animation) {
        return upsertPlayer(username, "prefix_animation", animation);
    }

    public CompletableFuture<String> getPrefixAnimationAsync(String username) {
        return executeQueryAsync(
                "SELECT prefix_animation FROM playerdata WHERE username = ?",
                rs -> rs.getString("prefix_animation"),
                "none",
                username
        );
    }

    public CompletableFuture<Void> updatePrefixSpeed(String username, int speed) {
        return upsertPlayer(username, "prefix_speed", speed);
    }

    public CompletableFuture<Integer> getPrefixSpeedAsync(String username) {
        return executeQueryAsync(
                "SELECT prefix_speed FROM playerdata WHERE username = ?",
                rs -> rs.getInt("prefix_speed"),
                5,
                username
        );
    }

    public CompletableFuture<Void> updatePrefixDecorations(String username, String decorations) {
        return upsertPlayer(username, "prefixDecorations", decorations);
    }

    public CompletableFuture<String> getPrefixDecorationsAsync(String username) {
        return executeQueryAsync(
                "SELECT prefixDecorations FROM playerdata WHERE username = ?",
                rs -> rs.getString("prefixDecorations"),
                null,
                username
        );
    }

    public CompletableFuture<Void> updateNameDecorations(String username, String decorations) {
        return upsertPlayer(username, "nameDecorations", decorations);
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