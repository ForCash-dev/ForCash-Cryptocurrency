package nxt.http;

import nxt.NxtException;
import nxt.http.APIServlet.APIRequestHandler;
import nxt.peer.Peer;
import nxt.peer.Peers;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.MISSING_PEER;
import static nxt.http.JSONResponses.UNKNOWN_PEER;

public class BlacklistPeer extends APIRequestHandler {

    static final BlacklistPeer instance = new BlacklistPeer();
    
    private BlacklistPeer() {
        super(new APITag[] {APITag.NETWORK}, "peer");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest request)
            throws NxtException {
        JSONObject response = new JSONObject();
        
        String peerAddress = request.getParameter("peer");
        if (peerAddress == null) {
            return MISSING_PEER;
        }
        Peer peer = Peers.getPeer(peerAddress);
        if (peer == null) {
            //maybe add as new peer?
            return UNKNOWN_PEER;
        } else {
            peer.blacklist("Manual blacklist");
            response.put("done", true);
        }
        
        return response;
    }

    @Override
    boolean requirePassword() {
        return true;
    }
}
