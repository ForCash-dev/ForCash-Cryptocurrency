package nxt.peer;

import nxt.Nxt;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class GetInfo extends PeerServlet.PeerRequestHandler {

    static final GetInfo instance = new GetInfo();

    private GetInfo() {}


    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {
        PeerImpl peerImpl = (PeerImpl)peer;
        String announcedAddress = (String)request.get("announcedAddress");
        if (announcedAddress != null && (announcedAddress = announcedAddress.trim()).length() > 0) {
            announcedAddress = Peers.addressWithPort(announcedAddress);
            if (peerImpl.getAnnouncedAddress() != null && ! announcedAddress.equals(peerImpl.getAnnouncedAddress())) {
                // force verification of changed announced address
                Logger.logDebugMessage("Peer " + peer.getPeerAddress() + " changed announced address from " + peer.getAnnouncedAddress() + " to " + announcedAddress);
                peerImpl.setState(Peer.State.NON_CONNECTED);
            }
            peerImpl.setAnnouncedAddress(announcedAddress);
        }
        String application = (String)request.get("application");
        if (application == null) {
            application = "?";
        }
        peerImpl.setApplication(application.trim());

        String version = (String)request.get("version");
        if (version == null) {
            version = "?";
        }
        peerImpl.setVersion(version.trim());

        String platform = (String)request.get("platform");
        if (platform == null) {
            platform = "?";
        }
        peerImpl.setPlatform(platform.trim());

        peerImpl.setShareAddress(Boolean.TRUE.equals(request.get("shareAddress")));
        peerImpl.analyzeHallmark(peer.getPeerAddress(), (String)request.get("hallmark"));
        peerImpl.setLastUpdated(Nxt.getEpochTime());

        Peers.notifyListeners(peerImpl, Peers.Event.ADDED_ACTIVE_PEER);

        return Peers.myPeerInfoResponse;

    }

}
