package me.txmc.core.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.*;

/**
 * @author MindComplexity (aka Libalpm)
 * @since 2025/12/21
 * This file was created as a part of 8b8tCore
*/

public class GeneralDatabase {
    private static GeneralDatabase instance;
    private final HikariDataSource dataSource;
    private final ExecutorService databaseExecutor;

    private static final Set<String> VALID_COLUMNS = Set.of(
        "displayname", "muted", "showJoinMsg", "hidePrefix", "hideDeathMessages",
        "hideAnnouncements", "hideBadges", "selectedRank", "customGradient",
        "hideCustomTab", "useVanillaLeaderboard", "gradient_animation", 
        "gradient_speed", "nameDecorations", "preventPhantomSpawn", 
        "prefixGradient", "prefix_animation", "prefix_speed", "prefixDecorations"
    );

    private GeneralDatabase(String pluginFolderPath) {
        File databaseDir = new File(pluginFolderPath + "/Database");
        if (!databaseDir.exists()) {
            databaseDir.mkdirs();
        }

        String databasePath = pluginFolderPath + "/Database/8b8tCorePlayerDB.db";
        
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + databasePath);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(600000);
        config.setConnectionTimeout(30000);
        config.setPoolName("8b8tCore-SQLite-Pool");        
        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("synchronous", "NORMAL");
        config.addDataSourceProperty("busy_timeout", "5000");
        
        this.dataSource = new HikariDataSource(config);
        this.databaseExecutor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "8b8tCore-Database-Thread");
            t.setDaemon(true);
            return t;
        });

        createTables();
    }

    public static synchronized void initialize(String pluginFolderPath) {
        if (instance == null) {
            instance = new GeneralDatabase(pluginFolderPath);
        }
    }

    public static GeneralDatabase getInstance() {
        if (instance == null) {
            throw new IllegalStateException("GeneralDatabase not initialized! Call initialize() first.");
        }
        return instance;
    }

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private void createTables() {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS playerdata (
                username TEXT PRIMARY KEY NOT NULL,
                displayname TEXT,
                muted INTEGER
            );
            """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);

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

    private void validateColumn(String column) {
        if (!VALID_COLUMNS.contains(column)) {
            throw new IllegalArgumentException("Invalid column name: " + column);
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
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return mapper.map(rs);
                    }
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
        validateColumn(column);
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

    /**
     * @deprecated Use {@link #getNicknameAsync(String)} with proper async handling.
     * This method blocks and should NOT be called from region threads.
     */
    @Deprecated
    public String getNickname(String username) {
        return getNicknameAsync(username).join();
    }

    public CompletableFuture<String> getPlayerDataAsync(String username, String column) {
        validateColumn(column);
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

    public CompletableFuture<Void> updateShowJoinMsg(String username, boolean showJoinMsg) {
        return upsertPlayer(username, "showJoinMsg", showJoinMsg);
    }

    public CompletableFuture<Boolean> getPlayerShowJoinMsgAsync(String username) {
        return executeQueryAsync(
                "SELECT showJoinMsg FROM playerdata WHERE username = ?",
                rs -> rs.getInt("showJoinMsg") == 1,
                true,
                username
        );
    }

    /**
     * @deprecated Use {@link #getPlayerShowJoinMsgAsync(String)} with proper async handling.
     */
    @Deprecated
    public boolean getPlayerShowJoinMsg(String username) {
        return getPlayerShowJoinMsgAsync(username).join();
    }

    public CompletableFuture<Void> updateHidePrefix(String username, boolean hidePrefix) {
        return upsertPlayer(username, "hidePrefix", hidePrefix);
    }

    public CompletableFuture<Boolean> getPlayerHidePrefixAsync(String username) {
        return executeQueryAsync(
                "SELECT hidePrefix FROM playerdata WHERE username = ?",
                rs -> rs.getInt("hidePrefix") == 1,
                false,
                username
        );
    }

    /**
     * @deprecated Use {@link #getPlayerHidePrefixAsync(String)} with proper async handling.
     */
    @Deprecated
    public boolean getPlayerHidePrefix(String username) {
        return getPlayerHidePrefixAsync(username).join();
    }

    public CompletableFuture<Void> updateHideDeathMessages(String username, boolean hide) {
        return upsertPlayer(username, "hideDeathMessages", hide);
    }

    public CompletableFuture<Boolean> getPlayerHideDeathMessagesAsync(String username) {
        return executeQueryAsync(
                "SELECT hideDeathMessages FROM playerdata WHERE username = ?",
                rs -> rs.getInt("hideDeathMessages") == 1,
                false,
                username
        );
    }

    /**
     * @deprecated Use {@link #getPlayerHideDeathMessagesAsync(String)} with proper async handling.
     */
    @Deprecated
    public boolean getPlayerHideDeathMessages(String username) {
        return getPlayerHideDeathMessagesAsync(username).join();
    }

    public CompletableFuture<Void> updateHideAnnouncements(String username, boolean hide) {
        return upsertPlayer(username, "hideAnnouncements", hide);
    }

    public CompletableFuture<Boolean> getPlayerHideAnnouncementsAsync(String username) {
        return executeQueryAsync(
                "SELECT hideAnnouncements FROM playerdata WHERE username = ?",
                rs -> rs.getInt("hideAnnouncements") == 1,
                false,
                username
        );
    }

    /**
     * @deprecated Use {@link #getPlayerHideAnnouncementsAsync(String)} with proper async handling.
     */
    @Deprecated
    public boolean getPlayerHideAnnouncements(String username) {
        return getPlayerHideAnnouncementsAsync(username).join();
    }

    public CompletableFuture<Void> updateHideBadges(String username, boolean hide) {
        return upsertPlayer(username, "hideBadges", hide);
    }

    public CompletableFuture<Boolean> getPlayerHideBadgesAsync(String username) {
        return executeQueryAsync(
                "SELECT hideBadges FROM playerdata WHERE username = ?",
                rs -> rs.getInt("hideBadges") == 1,
                false,
                username
        );
    }

    /**
     * @deprecated Use {@link #getPlayerHideBadgesAsync(String)} with proper async handling.
     */
    @Deprecated
    public boolean getPlayerHideBadges(String username) {
        return getPlayerHideBadgesAsync(username).join();
    }

    public CompletableFuture<Boolean> isMutedAsync(String username) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT muted FROM playerdata WHERE username = ?")) {
                pstmt.setString(1, username);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        long mutedUntil = rs.getLong("muted");
                        if (rs.wasNull()) return false;
                        if (mutedUntil > Instant.now().getEpochSecond()) {
                            return true;
                        } else {
                            unmute(username);
                        }
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

    /**
     * @deprecated Use {@link #isMutedAsync(String)} with proper async handling.
     */
    @Deprecated
    public boolean isMuted(String username) {
        return isMutedAsync(username).join();
    }

    public CompletableFuture<Void> mute(String username, long timestamp) {
        return upsertPlayer(username, "muted", timestamp);
    }

    public CompletableFuture<Void> unmute(String username) {
        String sql = "UPDATE playerdata SET muted = NULL WHERE username = ?;";
        return executeUpdate(sql, username);
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

    public CompletableFuture<Void> updateHideCustomTab(String username, boolean hide) {
        return upsertPlayer(username, "hideCustomTab", hide);
    }

    public CompletableFuture<Boolean> getHideCustomTabAsync(String username) {
        return executeQueryAsync(
                "SELECT hideCustomTab FROM playerdata WHERE username = ?",
                rs -> rs.getBoolean("hideCustomTab"),
                false,
                username
        );
    }

    public CompletableFuture<Void> setVanillaLeaderboard(String username, boolean useVanilla) {
        return upsertPlayer(username, "useVanillaLeaderboard", useVanilla);
    }

    public CompletableFuture<Boolean> isVanillaLeaderboardAsync(String username) {
        return executeQueryAsync(
                "SELECT useVanillaLeaderboard FROM playerdata WHERE username = ?",
                rs -> rs.getBoolean("useVanillaLeaderboard"),
                false,
                username
        );
    }

    /**
     * @deprecated Use {@link #isVanillaLeaderboardAsync(String)} with proper async handling.
     */
    @Deprecated
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

    public CompletableFuture<Void> updatePreventPhantomSpawn(String username, boolean prevent) {
        return upsertPlayer(username, "preventPhantomSpawn", prevent);
    }

    public CompletableFuture<Boolean> getPreventPhantomSpawnAsync(String username) {
        return executeQueryAsync(
                "SELECT preventPhantomSpawn FROM playerdata WHERE username = ?",
                rs -> rs.getBoolean("preventPhantomSpawn"),
                true,
                username
        );
    }

    /**
     * @deprecated Use {@link #getPreventPhantomSpawnAsync(String)} with proper async handling.
     */
    @Deprecated
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
        if (databaseExecutor != null) {
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
        
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}