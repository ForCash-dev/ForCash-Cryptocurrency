package nxt.http;

import nxt.Alias;
import nxt.Nxt;
import nxt.NxtException;
import nxt.Utils;

import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import java.awt.*;
import java.net.URI;
import java.util.Random;

class UpdateClient extends APIServlet.APIRequestHandler {

    static final UpdateClient instance = new UpdateClient();

    private UpdateClient() {
        super(new APITag[] {APITag.SETTINGS});
    }

    private static final String URL = "https://forcash.app";

    private static final String HTTP = "http://";
    private static final String DOWNLOADS = "/downloads/v";
    private static final String FORCASH_WALLET = "/forcash_wallet_v";

    private enum Arch {
        WIN32, WIN64, MAC
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest request) throws NxtException {
        final String[] peers = {
                "142.93.176.83",
                "68.183.177.7",
                "178.128.254.195",
                "209.97.177.48",
                "167.99.136.204"
        };

        switch (Utils.detectOS()) {
            case LINUX:
            case SERVER:
                openBrowser(URL);
                return null;
        }

        if (Alias.getAlias(Nxt.NRSVERSION) != null) {
            Random rand = new Random();
            String randomPeer = peers[rand.nextInt(peers.length)];

            String version = Alias.getAlias(Nxt.NRSVERSION).getAliasURI();;

            String architecture = null;

            switch (determineDevice(System.getProperty("os.name"))) {
                case WIN32:
                    architecture = "_x86.exe";
                    break;

                case WIN64:
                    architecture = "_x64.exe";
                    break;

                case MAC:
                    architecture = ".dmg";
                    break;
            }

            //  http://142.93.176.83/downloads/v1.0.0/forcash_wallet_v1.0.0_x64.exe
            String address = HTTP + randomPeer + DOWNLOADS + version + FORCASH_WALLET + version + architecture;

            openBrowser(address);
        }

        return null;
    }

    private void openBrowser(String address) {
        try {
            Desktop.getDesktop().browse(new URI(address));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Arch determineDevice(String OS) {
        if (!OS.contains("Windows")) {
            return Arch.MAC;
        }

        boolean is64bit = false;
        if (System.getProperty("os.name").contains("Windows")) {
            is64bit = (System.getenv("ProgramFiles(x86)") != null);
        } else {
            is64bit = (System.getProperty("os.arch").contains("64"));
        }

        return is64bit ? Arch.WIN64 : Arch.WIN32;
    }
}