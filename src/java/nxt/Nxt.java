package nxt;

import nxt.http.API;
import nxt.peer.Peers;
import nxt.user.Users;
import nxt.util.Logger;
import nxt.util.ThreadPool;
import nxt.util.Time;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.logging.Level;

public class Nxt {
    public static final Boolean SERVER = true;

    public static final String VERSION = "1.0.1";
    public static final String APPLICATION = "NRS";

    public static final String NRSVERSION = "version";

    public static final String FLATPAK_PREFIX = "/app/bin";
    public static final String FLATPAK_PERSISTENT_PREFIX = "/var";

    private static volatile Time time = new Time.EpochTime();

    private static final Properties defaultProperties = new Properties();

    static {
        String prefix = isLinux() ? FLATPAK_PERSISTENT_PREFIX + "/config" : System.getProperty("user.dir");

        if (isLinux()) {
            try {
                new ProcessBuilder(FLATPAK_PREFIX + "/persistent_conf.sh").start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.setProperty("fch-default.properties", "/conf/fch-default.properties");
        try (InputStream is = ClassLoader.getSystemResourceAsStream("fch-default.properties")) {
            if (is != null) {
                Nxt.defaultProperties.load(is);
            } else {
                try (InputStream fis = new FileInputStream(prefix + "/conf/fch-default.properties")) {
                    Nxt.defaultProperties.load(fis);
                } catch (IOException e) {
                    throw new RuntimeException("Error loading fch-default.properties from FileInputStream:" + prefix + "/conf/fch-default.properties");
                }
            }
            if (!VERSION.equals(Nxt.defaultProperties.getProperty("fch.version"))) {
                throw new RuntimeException("Using an fch-default.properties file from a version other than " + VERSION + " is not supported!!!");
            }
        } catch (IOException e) {
            throw new RuntimeException("Error loading fch-default.properties", e);
        }
    }

    private static boolean isLinux() {
        return Utils.detectOS() == Utils.OSType.LINUX;
    }

    private static final Properties properties = new Properties(defaultProperties);

    static {
        try (InputStream is = ClassLoader.getSystemResourceAsStream("fch.properties")) {
            if (is != null) {
                Nxt.properties.load(is);
            } // ignore if missing
        } catch (IOException e) {
            throw new RuntimeException("Error loading nxt.properties", e);
        }
    }

    public static int getIntProperty(String name) {
        try {
            int result = Integer.parseInt(properties.getProperty(name));
            Logger.logMessage(name + " = \"" + result + "\"");
            return result;
        } catch (NumberFormatException e) {
            Logger.logMessage(name + " not defined, assuming 0");
            return 0;
        }
    }

    public static String getStringProperty(String name) {
        return getStringProperty(name, null, false);
    }

    public static String getStringProperty(String name, String defaultValue) {
        return getStringProperty(name, defaultValue, false);
    }

    public static String getStringProperty(String name, String defaultValue, boolean doNotLog) {
        String value = properties.getProperty(name);
        if (value != null && !"".equals(value)) {
            Logger.logMessage(name + " = \"" + (doNotLog ? "{not logged}" : value) + "\"");
            return value;
        } else {
            Logger.logMessage(name + " not defined");
            return defaultValue;
        }
    }

    public static List<String> getStringListProperty(String name) {
        String value = getStringProperty(name);
        if (value == null || value.length() == 0) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String s : value.split(";")) {
            s = s.trim();
            if (s.length() > 0) {
                result.add(s);
            }
        }
        return result;
    }

    public static Boolean getBooleanProperty(String name) {
        String value = properties.getProperty(name);
        if (Boolean.TRUE.toString().equals(value)) {
            Logger.logMessage(name + " = \"true\"");
            return true;
        } else if (Boolean.FALSE.toString().equals(value)) {
            Logger.logMessage(name + " = \"false\"");
            return false;
        }
        Logger.logMessage(name + " not defined, assuming false");
        return false;
    }

    public static Blockchain getBlockchain() {
        return BlockchainImpl.getInstance();
    }

    public static BlockchainProcessor getBlockchainProcessor() {
        return BlockchainProcessorImpl.getInstance();
    }

    public static TransactionProcessor getTransactionProcessor() {
        return TransactionProcessorImpl.getInstance();
    }

    public static Transaction.Builder newTransactionBuilder(byte[] senderPublicKey, long amountNQT, long feeNQT, short deadline, Attachment attachment) {
        return new TransactionImpl.BuilderImpl((byte) 1, senderPublicKey, amountNQT, feeNQT, deadline, (Attachment.AbstractAttachment) attachment);
    }

    public static int getEpochTime() {
        return time.getTime();
    }

    static void setTime(Time time) {
        Nxt.time = time;
    }

    public static void main(String[] args) {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    Nxt.shutdown();
                }
            }));
            run();
        } catch (Throwable t) {
            System.out.println("Fatal error: " + t.toString());
        }
    }

    public static void init(Properties customProperties) {
        properties.putAll(customProperties);
        run();
    }

    public static void run() {
        Init.run();
    }

    public static void shutdown() {
        Logger.logShutdownMessage("Shutting down...");
        API.shutdown();
        Users.shutdown();
        Peers.shutdown();
        ThreadPool.shutdown();
        Db.shutdown();
        Logger.logShutdownMessage("FCH server " + VERSION + " stopped.");
        Logger.shutdown();
    }

    public static class Init {
        static {
            try {
                long startTime = System.currentTimeMillis();
                Logger.init();
                logSystemProperties();
                Db.init();
                TransactionProcessorImpl.getInstance();
                BlockchainProcessorImpl.getInstance();
                Account.init();
                Alias.init();
                Asset.init();
                DigitalGoodsStore.init();
                Hub.init();
                Order.init();
                Poll.init();
                Trade.init();
                AssetTransfer.init();
                Vote.init();
                Currency.init();
                CurrencyBuyOffer.init();
                CurrencySellOffer.init();
                CurrencyFounder.init();
                CurrencyMint.init();
                CurrencyTransfer.init();
                Exchange.init();
                Peers.init();
                Generator.init();
                API.init();
                Users.init();
                DebugTrace.init();
                int timeMultiplier = (Constants.isTestnet && Constants.isOffline) ? Math.max(Nxt.getIntProperty("fch.timeMultiplier"), 1) : 1;
                ThreadPool.start(timeMultiplier);
                if (timeMultiplier > 1) {
                    setTime(new Time.FasterTime(Math.max(getEpochTime(), Nxt.getBlockchain().getLastBlock().getTimestamp()), timeMultiplier));
                    Logger.logMessage("TIME WILL FLOW " + timeMultiplier + " TIMES FASTER!");
                }

                long currentTime = System.currentTimeMillis();
                Logger.logMessage("Initialization took " + (currentTime - startTime) / 1000 + " seconds");
                Logger.logMessage("FCH server " + VERSION + " started successfully.");
                if (Constants.isTestnet) {
                    Logger.logMessage("RUNNING ON TESTNET - DO NOT USE REAL ACCOUNTS!");
                }
            } catch (Exception e) {
                Logger.logErrorMessage(e.getMessage(), e);
                System.exit(1);
            }

            System.setProperty("apple.awt.UIElement", "true");

            SystemTrayIcon systemTrayIcon = new SystemTrayIcon();
            systemTrayIcon.createTrayIcon();
        }

        private static void run() {
        }

        public Init() {
        } // never

    }

    private static void logSystemProperties() {
        String[] loggedProperties = new String[]{
                "java.version",
                "java.vm.version",
                "java.vm.name",
                "java.vendor",
                "java.vm.vendor",
                "java.home",
                "java.library.path",
                "java.class.path",
                "os.arch",
                "sun.arch.data.model",
                "os.name",
                "file.encoding"
        };
        for (String property : loggedProperties) {
            Logger.logDebugMessage(String.format("%s = %s", property, System.getProperty(property)));
        }
        Logger.logDebugMessage(String.format("availableProcessors = %s", Runtime.getRuntime().availableProcessors()));
        Logger.logDebugMessage(String.format("maxMemory = %s", Runtime.getRuntime().maxMemory()));
    }

    public Nxt() {
    } // never

}
