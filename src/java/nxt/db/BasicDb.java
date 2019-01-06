package nxt.db;

import nxt.util.Logger;
import org.h2.jdbcx.JdbcConnectionPool;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class BasicDb {

    public static final class DbProperties {

        private long maxCacheSize;
        private String dbUrl;
        private int maxConnections;
        private int loginTimeout;
        private int defaultLockTimeout;

        public DbProperties maxCacheSize(int maxCacheSize) {
            this.maxCacheSize = maxCacheSize;
            return this;
        }

        public DbProperties dbUrl(String dbUrl) {
            this.dbUrl = dbUrl;
            return this;
        }

        public DbProperties maxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
            return this;
        }

        public DbProperties loginTimeout(int loginTimeout) {
            this.loginTimeout = loginTimeout;
            return this;
        }

        public DbProperties defaultLockTimeout(int defaultLockTimeout) {
            this.defaultLockTimeout = defaultLockTimeout;
            return this;
        }

    }

    private JdbcConnectionPool cp;
    private volatile int maxActiveConnections;
    private final String dbUrl;
    private final int maxConnections;
    private final int loginTimeout;
    private final int defaultLockTimeout;

    public BasicDb(DbProperties dbProperties) {
        long maxCacheSize = dbProperties.maxCacheSize;
        if (maxCacheSize == 0) {
            maxCacheSize = Math.min(256, Math.max(16, (Runtime.getRuntime().maxMemory() / (1024 * 1024) - 128)/2)) * 1024;
        }
        String dbUrl = dbProperties.dbUrl;
        if (!dbUrl.contains("CACHE_SIZE=")) {
            dbUrl += ";CACHE_SIZE=" + maxCacheSize;
        }
        this.dbUrl = dbUrl;
        this.maxConnections = dbProperties.maxConnections;
        this.loginTimeout = dbProperties.loginTimeout;
        this.defaultLockTimeout = dbProperties.defaultLockTimeout;
    }

    public void init(String username, String password, DbVersion dbVersion) {
        Logger.logDebugMessage("Database jdbc url set to: " + dbUrl);
        cp = JdbcConnectionPool.create(dbUrl, username, password);
        cp.setMaxConnections(maxConnections);
        cp.setLoginTimeout(loginTimeout);
        try (Connection con = cp.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.executeUpdate("SET DEFAULT_LOCK_TIMEOUT " + defaultLockTimeout);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        dbVersion.init(this);
    }

    public void shutdown() {
        try {
            Connection con = cp.getConnection();
            Statement stmt = con.createStatement();
            stmt.execute("SHUTDOWN COMPACT");
            Logger.logShutdownMessage("Database shutdown completed");
        } catch (SQLException e) {
            Logger.logShutdownMessage(e.toString(), e);
        }
    }

    public void analyzeTables() {
        try (Connection con = cp.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.execute("ANALYZE SAMPLE_SIZE 0");
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public Connection getConnection() throws SQLException {
        Connection con = getPooledConnection();
        con.setAutoCommit(true);
        return con;
    }

    protected Connection getPooledConnection() throws SQLException {
        Connection con = cp.getConnection();
        int activeConnections = cp.getActiveConnections();
        if (activeConnections > maxActiveConnections) {
            maxActiveConnections = activeConnections;
            Logger.logDebugMessage("Database connection pool current size: " + activeConnections);
        }
        return con;
    }

}
