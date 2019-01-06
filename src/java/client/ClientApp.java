package client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.io.InputStream;
import java.net.Socket;
import java.util.Properties;

import client.Utils;

public class ClientApp extends Application {
    private final static String FORCASH_LABEL = "ForCash Wallet";
    private final static String FORCASH_ICON_NAME = "./img/forcash.png";

    public static final String FLATPAK_PREFIX = "/app/bin";

    private static boolean isTestnet = false;
    private static int productionPort, testnetPort = 6776;

    private static final Properties defaultProperties = new Properties();

    public static Boolean getBooleanProperty(String name) {
        String value = defaultProperties.getProperty(name);

        if (Boolean.TRUE.toString().equals(value)) {
            return true;
        } else if (Boolean.FALSE.toString().equals(value)) {
            return false;
        }

        return false;
    }

    private static int getIntProperty(String name) {
        String value = defaultProperties.getProperty(name);

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 10000;
        }
    }

    static {
        System.setProperty("fch-default.properties", "/conf/fch-default.properties");
        String prefix = isLinux() ? FLATPAK_PREFIX : System.getProperty("user.dir");

        System.out.println("Initializing FCH client");
        try (InputStream is = ClassLoader.getSystemResourceAsStream("fch-default.properties")) {
            if (is != null) {
                ClientApp.defaultProperties.load(is);
            } else {
                try (InputStream fis = new FileInputStream(prefix + "/conf/fch-default.properties")) {
                    ClientApp.defaultProperties.load(fis);
                } catch (IOException e) {
                    throw new RuntimeException("Error loading fch-default.properties from FileInputStream");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error loading fch-default.properties", e);
        }

        isTestnet = getBooleanProperty("fch.isTestnet");
        productionPort = getIntProperty("fch.apiServerPort");
    }

    private static boolean isLinux() {
        return Utils.detectOS() == Utils.OSType.LINUX;
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle(FORCASH_LABEL);

        if (isWindows(System.getProperty("os.name"))) {
            Image image = new Image("file:" + FORCASH_ICON_NAME);
            if (!image.isError()) {
                stage.getIcons().add(image);
            }
        }

        final WebView browser = new WebView();
        final WebEngine webEngine = browser.getEngine();

        browser.setContextMenuEnabled(false);

        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int width = gd.getDisplayMode().getWidth();
        int height = gd.getDisplayMode().getHeight();

        double minWidth = ((double) width) * 0.9;
        double minHeight = ((double) height) * 0.8;

        stage.setWidth(minWidth);
        stage.setMinWidth(minWidth);

        stage.setHeight(minHeight);
        stage.setMinHeight(minHeight);

        StackPane root = new StackPane();
        root.getChildren().add(browser);

        Scene scene = new Scene(root);
        webEngine.load("http://localhost:" + (isTestnet ? String.valueOf(testnetPort) : String.valueOf(productionPort)));

        scene.setRoot(root);

        stage.setScene(scene);
        stage.show();
    }

    private static boolean isWindows(String OS) {
        return OS.startsWith("Windows");
    }

    public static void main(String[] args) {
        try {
            boolean isReady = false, isStarted = false;

            do {
                try {
                    new Socket("localhost", isTestnet ? testnetPort : productionPort);
                    isReady = true;
                } catch (IOException ex) {
                    if (args.length == 0 && !isStarted) {
                        ProcessBuilder pb_server, pb_client;
                        
                        if (isWindows(System.getProperty("os.name"))) {
                            pb_client = new ProcessBuilder(System.getProperty("user.dir") + "\\ForCash Wallet.exe", "-server_flag");
                            pb_client.directory(new File(System.getProperty("user.dir")));
                            pb_client.start();

                            pb_server = new ProcessBuilder(System.getProperty("user.dir") + "\\ForCash Server.exe");
                            pb_server.directory(new File(System.getProperty("user.dir")));
                            pb_server.start();

                            System.exit(0);
                        } else {
                            pb_server = new ProcessBuilder(System.getProperty("user.dir") + "/PlugIns/jdk/Contents/Home/bin/java", "-jar", System.getProperty("user.dir") + "/Java/forcash_server.jar");
                            pb_server.directory(new File(System.getProperty("user.dir")));
                            pb_server.start();
                        }

                        isStarted = true;
                    }

                    Thread.sleep(1000);
                }
            } while (!isReady);
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
        }

        launch(args);
    }
}
