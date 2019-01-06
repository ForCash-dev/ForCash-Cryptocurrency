package nxt;

import nxt.db.BasicDb;
import nxt.db.TransactionalDb;

public final class Db {
    private static String getDbUrl() {
        String prefix = Nxt.getStringProperty("fch.dbPrefix");
        String suffix = Nxt.getStringProperty("fch.dbSuffix");

        String linuxPrefix = Utils.detectOS() == Utils.OSType.LINUX ? Nxt.FLATPAK_PERSISTENT_PREFIX + "/data/" : "";

        String dbUrl = Constants.isTestnet ?
                Nxt.getStringProperty("fch.testDbUrl") :
                Nxt.getStringProperty("fch.dbUrl");

        return prefix + linuxPrefix + dbUrl + suffix;
    }

    public static final TransactionalDb db = new TransactionalDb(new BasicDb.DbProperties()
            .maxCacheSize(Nxt.getIntProperty("fch.dbCacheKB"))
            .dbUrl(getDbUrl())
            .maxConnections(Nxt.getIntProperty("fch.maxDbConnections"))
            .loginTimeout(Nxt.getIntProperty("fch.dbLoginTimeout"))
            .defaultLockTimeout(Nxt.getIntProperty("fch.dbDefaultLockTimeout") * 1000)
    );

    /*
    public static final BasicDb userDb = new BasicDb(new BasicDb.DbProperties()
            .maxCacheSize(Nxt.getIntProperty("fch.userDbCacheKB"))
            .dbUrl(Constants.isTestnet ? Nxt.getStringProperty("fch.testUserDbUrl") : Nxt.getStringProperty("fch.userDbUrl"))
            .maxConnections(Nxt.getIntProperty("fch.maxUserDbConnections"))
            .loginTimeout(Nxt.getIntProperty("fch.userDbLoginTimeout"))
            .defaultLockTimeout(Nxt.getIntProperty("fch.userDbDefaultLockTimeout") * 1000)
    );
    */

    static void init() {
        db.init("sa", "sa", new NxtDbVersion());
        //userDb.init("sa", "databaseencryptionpassword sa", new UserDbVersion());
    }

    static void shutdown() {
        //userDb.shutdown();
        db.shutdown();
    }

    private Db() {} // never

}
