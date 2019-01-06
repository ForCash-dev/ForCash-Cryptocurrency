package nxt.http;

import nxt.Alias;
import nxt.Generator;
import nxt.Nxt;
import nxt.Utils;
import nxt.peer.Peer;
import nxt.peer.Peers;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import java.util.List;

import static nxt.http.JSONResponses.*;


public final class StartForging extends APIServlet.APIRequestHandler {
    static final StartForging instance = new StartForging();

    private StartForging() {
        super(new APITag[] {APITag.FORGING}, "secretPhrase");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {
        JSONObject response;

        String secretPhrase = req.getParameter("secretPhrase");
        if (secretPhrase == null) {
            return MISSING_SECRET_PHRASE;
        }

        List<Peer> peers = Peers.getPeers(Peer.State.CONNECTED);
        if (peers == null || peers.size() <= 0) {
            response = new JSONObject();
            response.put("connectionError", true);
            return response;
        }

        if (Alias.getAlias(Nxt.NRSVERSION) != null) {
            String NRSAliasURI = Alias.getAlias(Nxt.NRSVERSION).getAliasURI();

            if (NRSAliasURI != null && Utils.versionCompare(Nxt.VERSION, NRSAliasURI) == -1) {
                Generator.stopForging(secretPhrase);

                response = new JSONObject();
                response.put("versionForging", false);
                response.put("userVersion", Nxt.VERSION);
                response.put("lastVersion", NRSAliasURI);
                return response;
            }
        }

        Generator generator = Generator.startForging(secretPhrase);
        if (generator == null) {
            return UNKNOWN_ACCOUNT;
        }

        response = new JSONObject();
        response.put("versionForging", true);
        response.put("deadline", generator.getDeadline());
        response.put("hitTime", generator.getHitTime());
        return response;
    }

    @Override
    boolean requirePost() {
        return true;
    }

}
