package nxt;

import nxt.util.Logger;

import javax.imageio.ImageIO;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import java.awt.*;
import java.io.*;

class SystemTrayIcon {
    private final static String LABEL_SERVER_RUNNING = "Server is running...";
    private final static String LABEL_SERVER_QUITING = "Shutting down...";

    private final static String LABEL_BUTTON_OPEN = "Open Application";
    private final static String LABEL_BUTTON_QUIT = "Quit Server";

    private final static String OPEN_FORCASH_WALLET = "/Applications/ForCash Wallet.app";

    private final static String FORCASH_WALLET_TRAY_LABEL = "ForCash Wallet Server";

    private final static int POPUP_APP_INFO = 0;
    private final static int POPUP_SERVER_STATUS = 2;

    void createTrayIcon() {
        if (!SystemTray.isSupported()) {
            Logger.logDebugMessage("TRAY", "SystemTray is not supported");
            return;
        }

        final PopupMenu popup = new PopupMenu();

        Image img;
        try {
            File pathToFile = new File(getTrayIconImg());
            img = ImageIO.read(pathToFile);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        if (img == null) {
            return;
        }

        final TrayIcon trayIcon =  Utils.detectOS() == Utils.OSType.WINDOWS ?
            new TrayIcon(img, FORCASH_WALLET_TRAY_LABEL) :
            new TrayIcon(img);

        final SystemTray tray = SystemTray.getSystemTray();

        MenuItem open = new MenuItem(LABEL_BUTTON_OPEN);
        MenuItem quit = new MenuItem(LABEL_BUTTON_QUIT);

        if (Utils.detectOS() != Utils.OSType.WINDOWS) {
            popup.add(FORCASH_WALLET_TRAY_LABEL);
            popup.addSeparator();

            popup.add(LABEL_SERVER_RUNNING);
            popup.addSeparator();

            popup.getItem(POPUP_APP_INFO).setEnabled(false);
            popup.getItem(POPUP_SERVER_STATUS).setEnabled(false);
        } else {
            trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        openClient();
                    }
                }
            });
        }

        popup.add(open);
        popup.add(quit);

        trayIcon.setPopupMenu(popup);

        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            Logger.logDebugMessage("TRAY", "TrayIcon could not be added");
            return;
        }

        open.addActionListener(e -> {
            openClient();
        });

        quit.addActionListener(e -> {
            if (Utils.detectOS() != Utils.OSType.WINDOWS) {
                trayIcon.getPopupMenu().getItem(POPUP_SERVER_STATUS).setLabel(LABEL_SERVER_QUITING);
            }

            quit.setEnabled(false);
            open.setEnabled(false);

            try {
                BufferedReader in = getClientProcesses();

                Consumer<String> consumer;
                if (Utils.detectOS() == Utils.OSType.WINDOWS) {
                    consumer = SystemTrayIcon::killProcessWindows;
                } else {
                    consumer = SystemTrayIcon::killProcessMac;
                }

                in.lines().forEach(consumer);
            } catch (IOException exception) {
                Logger.logDebugMessage("TRAY", "Quit button" + exception.toString());
            }

            System.exit(0);
            tray.remove(trayIcon);
        });
    }

    private static void openClient() {
        try {
            if (Utils.detectOS() == Utils.OSType.WINDOWS && !isClientOpen()) {
                new ProcessBuilder(System.getProperty("user.dir") + "\\ForCash Wallet.exe").start();
            } else if (Utils.detectOS() != Utils.OSType.WINDOWS && Desktop.getDesktop() != null) {
                Desktop.getDesktop().open(new File(OPEN_FORCASH_WALLET));
            }
        } catch (IOException exception) {
            Logger.logDebugMessage("TRAY", "Opening ForCash Wallet client IO Exception (ProcessBuilder)");
        }
    }

    private static BufferedReader getClientProcesses() throws IOException {
        ProcessBuilder processBuilder = null;

        if (Utils.detectOS() == Utils.OSType.WINDOWS) {
            processBuilder = new ProcessBuilder("cmd", "/c", "for /F \"tokens=1,2\" %i in ('tasklist -V ^| findstr \"ForCash Wallet\" ^| findstr \"javaw.exe\"') do @echo %j");
        } else {
            processBuilder = new ProcessBuilder("/bin/sh", "-c", "ps -e | grep \"ForCash Wallet.app\" | grep \"JavaAppLauncher\" | awk '{print $1}'");
        }

        processBuilder.directory(new File(System.getProperty("user.dir")));

        Process process = processBuilder.start();

        return new BufferedReader(new InputStreamReader(process.getInputStream()));
    }

    private static boolean isClientOpen() {
        try {
            return getClientProcesses().lines().count() > 0;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String getTrayIconImg() {
        switch (Utils.detectOS()) {
            case WINDOWS:
                return System.getProperty("user.dir") + "/img/tray.png";

            case MAC:
                if (Utils.isMacMenuBarDarkMode()) {
                    return System.getProperty("user.dir") + "/tray_dark.png";
                } else {
                    return System.getProperty("user.dir") + "/tray_light.png";
                }

            case LINUX:
                return Nxt.FLATPAK_PREFIX + "/tray.png";
        }

        return null;
    }

    private static void killProcessMac(String s) {
        try {
            new ProcessBuilder("/bin/sh", "-c", "kill -9 " + s).start();
        } catch (IOException e) {
            Logger.logDebugMessage("TRAY: " + "Mac: " + "Cannot kill process - " + s);
        }
    }

    private static void killProcessWindows(String s) {
        try {
            new ProcessBuilder("taskkill.exe", "/F", "/PID", s).start();
        } catch (IOException e) {
            Logger.logDebugMessage("TRAY: " + "Windows: " + "Cannot kill process - " + s);
        }
    }
}
