package nxt;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Utils {
    private static String OS = System.getProperty("os.name").toLowerCase();

    public enum OSType {
        WINDOWS,
        MAC,
        LINUX,
        SERVER
    }

    public static int versionCompare(String str1, String str2) {
        String[] vals1 = str1.split("\\.");
        String[] vals2 = str2.split("\\.");

        int i = 0;

        while (i < vals1.length && i < vals2.length && vals1[i].equals(vals2[i])) {
            i++;
        }

        if (i < vals1.length && i < vals2.length) {
            int diff = Integer.valueOf(vals1[i]).compareTo(Integer.valueOf(vals2[i]));
            return Integer.signum(diff);
        }

        return Integer.signum(vals1.length - vals2.length);
    }

    public static OSType detectOS() {
        if (OS.contains("win")) {
            return OSType.WINDOWS;
        } else if (OS.contains("mac")) {
            return OSType.MAC;
        } else if (!Nxt.SERVER) {
            return OSType.LINUX;
        } else {
            return OSType.SERVER;
        }
    }

    static boolean isMacMenuBarDarkMode() {
        try {
            final Process proc = Runtime.getRuntime().exec(new String[] {"defaults", "read", "-g", "AppleInterfaceStyle"});
            proc.waitFor(100, TimeUnit.MILLISECONDS);
            return proc.exitValue() == 0;
        } catch (IOException | InterruptedException | IllegalThreadStateException ex) {
            return false;
        }
    }
}
