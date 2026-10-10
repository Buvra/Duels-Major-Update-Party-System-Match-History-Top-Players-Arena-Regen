package me.realized.duels.data;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import me.realized.duels.DuelsPlugin;

public class SqliteUserRepository implements UserRepository {

    private static final String CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS duels_users (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "name TEXT NOT NULL, " +
                    "wins INTEGER NOT NULL, " +
                    "losses INTEGER NOT NULL, " +
                    "requests INTEGER NOT NULL, " +
                    "matches TEXT NOT NULL DEFAULT '', " +
                    "ratings TEXT NOT NULL DEFAULT ''" +
                    ")";

    private static final String CREATE_NAME_INDEX =
            "CREATE INDEX IF NOT EXISTS idx_duels_users_name ON duels_users(name)";

    private static final String UPDATE_USER =
            "UPDATE duels_users SET " +
                    "name = ?, " +
                    "wins = ?, " +
                    "losses = ?, " +
                    "requests = ?, " +
                    "matches = ?, " +
                    "ratings = ? " +
                    "WHERE uuid = ?";

    private static final String INSERT_USER =
            "INSERT INTO duels_users (uuid, name, wins, losses, requests, matches, ratings) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private final DuelsPlugin plugin;
    private final String url;

    public SqliteUserRepository(DuelsPlugin plugin) {
        this.plugin = plugin;

        File folder = plugin.getDataFolder();
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IllegalStateException("Could not create plugin data folder.");
        }

        File dbFile = new File(folder, "duels.db");
        this.url = "jdbc:sqlite:" + dbFile.getAbsolutePath();

        try {
            this.initialize();
        } catch (SQLException ex) {
            throw new IllegalStateException("Could not initialize SQLite database.", ex);
        }
    }

    private Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(this.url);

        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL");
            st.execute("PRAGMA synchronous=NORMAL");
            st.execute("PRAGMA busy_timeout=5000");
        }

        return connection;
    }

    private void initialize() throws SQLException {
        try (Connection con = getConnection();
             Statement st = con.createStatement()) {

            st.executeUpdate(CREATE_TABLE);
            st.executeUpdate(CREATE_NAME_INDEX);

            ensureColumnExists(con, "duels_users", "matches", "TEXT NOT NULL DEFAULT ''");
            ensureColumnExists(con, "duels_users", "ratings", "TEXT NOT NULL DEFAULT ''");
        }
    }

    private void ensureColumnExists(Connection con, String table, String column, String definition) throws SQLException {
        boolean exists = false;

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(" + table + ")")) {

            while (rs.next()) {
                String current = rs.getString("name");
                if (column.equalsIgnoreCase(current)) {
                    exists = true;
                    break;
                }
            }
        }

        if (!exists) {
            try (Statement st = con.createStatement()) {
                st.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            }
        }
    }

    @Override
    public UserData findByUuid(UUID uuid) throws Exception {
        String sql = "SELECT * FROM duels_users WHERE uuid = ?";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, uuid.toString());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                return map(rs);
            }
        }
    }

    @Override
    public UserData findByName(String name) throws Exception {
        String sql = "SELECT * FROM duels_users WHERE name = ?";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name.toLowerCase());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                return map(rs);
            }
        }
    }

    @Override
    public void save(UserData user) throws Exception {
        try (Connection con = getConnection()) {
            con.setAutoCommit(false);

            try {
                this.save(con, user);
                con.commit();
            } catch (Exception ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    @Override
    public void saveAll(Collection<UserData> users) throws Exception {
        if (users == null || users.isEmpty()) {
            return;
        }

        try (Connection con = getConnection()) {
            con.setAutoCommit(false);

            try {
                for (UserData user : users) {
                    if (user == null) {
                        continue;
                    }

                    this.save(con, user);
                }

                con.commit();
            } catch (Exception ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    @Override
    public void delete(UUID uuid) throws Exception {
        String sql = "DELETE FROM duels_users WHERE uuid = ?";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        }
    }

    private UserData map(ResultSet rs) throws SQLException {
        UUID uuid = UUID.fromString(rs.getString("uuid"));
        String name = rs.getString("name");

        int defaultRating = plugin.getConfiguration().getDefaultRating();
        int matchesToDisplay = plugin.getConfiguration().getMatchesToDisplay();

        UserData user = new UserData(uuid, name, defaultRating, matchesToDisplay);
        user.setWins(rs.getInt("wins"));
        user.setLosses(rs.getInt("losses"));
        user.setRequests(rs.getInt("requests") == 1);

        String matchesData = "";
        try {
            matchesData = rs.getString("matches");
        } catch (SQLException ignored) {
        }

        String ratingsData = "";
        try {
            ratingsData = rs.getString("ratings");
        } catch (SQLException ignored) {
        }

        user.setMatches(deserializeMatches(matchesData));
        user.setRatings(deserializeRatings(ratingsData));
        user.applyRuntimeSettings(defaultRating, matchesToDisplay);
        user.markPersisted();

        return user;
    }

    private void save(Connection con, UserData user) throws Exception {
        String uuid = user.getUuid().toString();
        String name = user.getName() == null ? "" : user.getName().toLowerCase();
        int wins = user.getWins();
        int losses = user.getLosses();
        int requests = user.canRequest() ? 1 : 0;
        String matches = serializeMatches(user.getMatchData());
        String ratings = serializeRatings(user.getRatings());

        int updated;

        try (PreparedStatement ps = con.prepareStatement(UPDATE_USER)) {
            ps.setString(1, name);
            ps.setInt(2, wins);
            ps.setInt(3, losses);
            ps.setInt(4, requests);
            ps.setString(5, matches);
            ps.setString(6, ratings);
            ps.setString(7, uuid);
            updated = ps.executeUpdate();
        }

        if (updated == 0) {
            try (PreparedStatement ps = con.prepareStatement(INSERT_USER)) {
                ps.setString(1, uuid);
                ps.setString(2, name);
                ps.setInt(3, wins);
                ps.setInt(4, losses);
                ps.setInt(5, requests);
                ps.setString(6, matches);
                ps.setString(7, ratings);
                ps.executeUpdate();
            }
        }
    }

    private String serializeMatches(List<MatchData> matches) {
        if (matches == null || matches.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        for (MatchData match : matches) {
            if (match == null) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append("\n");
            }

            builder.append(nullToEmpty(match.getWinner())).append("|");
            builder.append(nullToEmpty(match.getLoser())).append("|");
            builder.append(nullToEmpty(match.getKit())).append("|");
            builder.append(match.getTime()).append("|");
            builder.append(match.getDuration()).append("|");
            builder.append(match.getHealth()).append("|");
            builder.append(match.getBetAmount());
        }

        return builder.toString();
    }

    private List<MatchData> deserializeMatches(String data) {
        List<MatchData> matches = new ArrayList<MatchData>();

        if (data == null || data.trim().isEmpty() || data.trim().equals("[]")) {
            return matches;
        }

        String[] lines = data.split("\\n");

        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }

            String[] parts = line.split("\\|", -1);
            if (parts.length < 6) {
                continue;
            }

            try {
                String winner = emptyToNull(parts[0]);
                String loser = emptyToNull(parts[1]);
                String kit = emptyToNull(parts[2]);
                long time = parseLong(parts[3]);
                long duration = parseLong(parts[4]);
                double health = parseDouble(parts[5]);
                double betAmount = parts.length >= 7 ? parseDouble(parts[6]) : 0D;

                matches.add(new MatchData(
                        winner,
                        loser,
                        kit,
                        time,
                        duration,
                        health,
                        betAmount
                ));
            } catch (Exception ignored) {
            }
        }

        return matches;
    }

    private String serializeRatings(ConcurrentHashMap<String, Integer> ratings) {
        if (ratings == null || ratings.isEmpty()) {
            return "";
        }

        List<String> keys = new ArrayList<String>(ratings.keySet());
        Collections.sort(keys, String.CASE_INSENSITIVE_ORDER);

        StringBuilder builder = new StringBuilder();

        for (String key : keys) {
            if (key == null) {
                continue;
            }

            Integer value = ratings.get(key);
            if (value == null) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append("\n");
            }

            builder.append(this.nullToEmpty(key)).append("|").append(value.intValue());
        }

        return builder.toString();
    }

    private ConcurrentHashMap<String, Integer> deserializeRatings(String data) {
        ConcurrentHashMap<String, Integer> ratings = new ConcurrentHashMap<String, Integer>();

        if (data == null || data.trim().isEmpty()) {
            return ratings;
        }

        String[] lines = data.split("\\n");
        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }

            String[] parts = line.split("\\|", 2);
            if (parts.length != 2) {
                continue;
            }

            try {
                ratings.put(parts[0], Integer.parseInt(parts[1]));
            } catch (NumberFormatException ignored) {
            }
        }

        return ratings;
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception ex) {
            return 0L;
        }
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception ex) {
            return 0D;
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value.replace("\n", " ").replace("|", "/");
    }

    private String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
