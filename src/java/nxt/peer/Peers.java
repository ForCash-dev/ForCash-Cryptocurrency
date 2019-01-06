package nxt.peer;

import nxt.Account;
import nxt.Block;
import nxt.Constants;
import nxt.Db;
import nxt.Nxt;
import nxt.Transaction;
import nxt.util.Filter;
import nxt.util.JSON;
import nxt.util.Listener;
import nxt.util.Listeners;
import nxt.util.Logger;
import nxt.util.ThreadPool;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.DoSFilter;
import org.eclipse.jetty.servlets.GzipFilter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class Peers {

    public static enum Event {
        BLACKLIST, UNBLACKLIST, DEACTIVATE, REMOVE,
        DOWNLOADED_VOLUME, UPLOADED_VOLUME, WEIGHT,
        ADDED_ACTIVE_PEER, CHANGED_ACTIVE_PEER,
        NEW_PEER
    }

    static final int LOGGING_MASK_EXCEPTIONS = 1;
    static final int LOGGING_MASK_NON200_RESPONSES = 2;
    static final int LOGGING_MASK_200_RESPONSES = 4;
    static final int communicationLoggingMask;

    static final Set<String> wellKnownPeers;
    static final Set<String> knownBlacklistedPeers;

    static final int connectTimeout;
    static final int readTimeout;
    static final int blacklistingPeriod;
    static final boolean getMorePeers;
    static final int MAX_REQUEST_SIZE = 1024 * 1024;
    static final int MAX_RESPONSE_SIZE = 1024 * 1024;

    private static final int DEFAULT_PEER_PORT = 7884;
    private static final int TESTNET_PEER_PORT = 7874;
    private static final String myPlatform;
    private static final String myAddress;
    private static final int myPeerServerPort;
    private static final String myHallmark;
    private static final boolean shareMyAddress;
    private static final int maxNumberOfConnectedPublicPeers;
    private static final int maxNumberOfKnownPeers;
    private static final int minNumberOfKnownPeers;
    private static final boolean enableHallmarkProtection;
    private static final int pushThreshold;
    private static final int pullThreshold;
    private static final int sendToPeersLimit;
    private static final boolean usePeersDb;
    private static final boolean savePeers;
    private static final String dumpPeersVersion;


    static final JSONStreamAware myPeerInfoRequest;
    static final JSONStreamAware myPeerInfoResponse;

    private static final Listeners<Peer,Event> listeners = new Listeners<>();

    private static final ConcurrentMap<String, PeerImpl> peers = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, String> announcedAddresses = new ConcurrentHashMap<>();

    static final Collection<PeerImpl> allPeers = Collections.unmodifiableCollection(peers.values());

    static final ExecutorService peersService = Executors.newCachedThreadPool();
    private static final ExecutorService sendingService = Executors.newFixedThreadPool(10);

    static {

        myPlatform = Nxt.getStringProperty("fch.myPlatform");
        myAddress = Nxt.getStringProperty("fch.myAddress");
        if (myAddress != null && myAddress.endsWith(":" + TESTNET_PEER_PORT) && !Constants.isTestnet) {
            throw new RuntimeException("Port " + TESTNET_PEER_PORT + " should only be used for testnet!!!");
        }
        myPeerServerPort = Nxt.getIntProperty("fch.peerServerPort");
        if (myPeerServerPort == TESTNET_PEER_PORT && !Constants.isTestnet) {
            throw new RuntimeException("Port " + TESTNET_PEER_PORT + " should only be used for testnet!!!");
        }
        shareMyAddress = Nxt.getBooleanProperty("fch.shareMyAddress") && ! Constants.isOffline;
        myHallmark = Nxt.getStringProperty("fch.myHallmark");
        if (Peers.myHallmark != null && Peers.myHallmark.length() > 0) {
            try {
                Hallmark hallmark = Hallmark.parseHallmark(Peers.myHallmark);
                if (!hallmark.isValid() || myAddress == null) {
                    throw new RuntimeException();
                }
                URI uri = new URI("http://" + myAddress.trim());
                String host = uri.getHost();
                if (!hallmark.getHost().equals(host)) {
                    throw new RuntimeException();
                }
            } catch (RuntimeException | URISyntaxException e) {
                Logger.logMessage("Your hallmark is invalid: " + Peers.myHallmark + " for your address: " + myAddress);
                throw new RuntimeException(e.toString(), e);
            }
        }

        JSONObject json = new JSONObject();
        if (myAddress != null && myAddress.length() > 0) {
            try {
                URI uri = new URI("http://" + myAddress.trim());
                String host = uri.getHost();
                int port = uri.getPort();
                if (!Constants.isTestnet) {
                    if (port >= 0)
                        json.put("announcedAddress", myAddress);
                    else
                        json.put("announcedAddress", host + (myPeerServerPort != DEFAULT_PEER_PORT ? ":" + myPeerServerPort : ""));
                } else {
                    json.put("announcedAddress", host);
                }
            } catch (URISyntaxException e) {
                Logger.logMessage("Your announce address is invalid: " + myAddress);
                throw new RuntimeException(e.toString(), e);
            }
        }
        if (Peers.myHallmark != null && Peers.myHallmark.length() > 0) {
            json.put("hallmark", Peers.myHallmark);
        }
        json.put("application", Nxt.APPLICATION);
        json.put("version", Nxt.VERSION);
        json.put("platform", Peers.myPlatform);
        json.put("shareAddress", Peers.shareMyAddress);
        Logger.logDebugMessage("My peer info:\n" + json.toJSONString());
        myPeerInfoResponse = JSON.prepare(json);
        json.put("requestType", "getInfo");
        myPeerInfoRequest = JSON.prepareRequest(json);

        List<String> wellKnownPeersList = Constants.isTestnet ? Nxt.getStringListProperty("fch.testnetPeers")
                : Nxt.getStringListProperty("fch.wellKnownPeers");
        if (wellKnownPeersList.isEmpty() || Constants.isOffline) {
            wellKnownPeers = Collections.emptySet();
        } else {
            wellKnownPeers = Collections.unmodifiableSet(new HashSet<>(wellKnownPeersList));
        }

        List<String> knownBlacklistedPeersList = Nxt.getStringListProperty("fch.knownBlacklistedPeers");
        if (knownBlacklistedPeersList.isEmpty()) {
            knownBlacklistedPeers = Collections.emptySet();
        } else {
            knownBlacklistedPeers = Collections.unmodifiableSet(new HashSet<>(knownBlacklistedPeersList));
        }

        maxNumberOfConnectedPublicPeers = Nxt.getIntProperty("fch.maxNumberOfConnectedPublicPeers");
        maxNumberOfKnownPeers = Nxt.getIntProperty("fch.maxNumberOfKnownPeers");
        minNumberOfKnownPeers = Nxt.getIntProperty("fch.minNumberOfKnownPeers");
        connectTimeout = Nxt.getIntProperty("fch.connectTimeout");
        readTimeout = Nxt.getIntProperty("fch.readTimeout");
        enableHallmarkProtection = Nxt.getBooleanProperty("fch.enableHallmarkProtection");
        pushThreshold = Nxt.getIntProperty("fch.pushThreshold");
        pullThreshold = Nxt.getIntProperty("fch.pullThreshold");

        blacklistingPeriod = Nxt.getIntProperty("fch.blacklistingPeriod");
        communicationLoggingMask = Nxt.getIntProperty("fch.communicationLoggingMask");
        sendToPeersLimit = Nxt.getIntProperty("fch.sendToPeersLimit");
        usePeersDb = Nxt.getBooleanProperty("fch.usePeersDb") && ! Constants.isOffline;
        savePeers = usePeersDb && Nxt.getBooleanProperty("fch.savePeers");
        getMorePeers = Nxt.getBooleanProperty("fch.getMorePeers");
        dumpPeersVersion = Nxt.getStringProperty("fch.dumpPeersVersion");

        final List<Future<String>> unresolvedPeers = Collections.synchronizedList(new ArrayList<Future<String>>());

        ThreadPool.runBeforeStart(new Runnable() {

            private void loadPeers(Collection<String> addresses) {
                for (final String address : addresses) {
                    Future<String> unresolvedAddress = peersService.submit(new Callable<String>() {
                        @Override
                        public String call() {
                            Peer peer = Peers.addPeer(address);
                            return peer == null ? address : null;
                        }
                    });
                    unresolvedPeers.add(unresolvedAddress);
                }
            }

            @Override
            public void run() {
                if (! wellKnownPeers.isEmpty()) {
                    loadPeers(wellKnownPeers);
                }
                if (usePeersDb) {
                    Logger.logDebugMessage("Loading known peers from the database...");
                    loadPeers(PeerDb.loadPeers());
                }
            }
        }, false);

        ThreadPool.runAfterStart(new Runnable() {
            @Override
            public void run() {
                for (Future<String> unresolvedPeer : unresolvedPeers) {
                    try {
                        String badAddress = unresolvedPeer.get(5, TimeUnit.SECONDS);
                        if (badAddress != null) {
                            Logger.logDebugMessage("Failed to resolve peer address: " + badAddress);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException e) {
                        Logger.logDebugMessage("Failed to add peer", e);
                    } catch (TimeoutException e) {
                    }
                }
                Logger.logDebugMessage("Known peers: " + peers.size());
            }
        });

    }

    private static class Init {

        private final static Server peerServer;

        static {
            if (Peers.shareMyAddress) {
                peerServer = new Server();
                ServerConnector connector = new ServerConnector(peerServer);
                final int port = Constants.isTestnet ? TESTNET_PEER_PORT : Peers.myPeerServerPort;
                connector.setPort(port);
                final String host = Nxt.getStringProperty("fch.peerServerHost");
                connector.setHost(host);
                connector.setIdleTimeout(Nxt.getIntProperty("fch.peerServerIdleTimeout"));
                connector.setReuseAddress(true);
                peerServer.addConnector(connector);

                ServletHolder peerServletHolder = new ServletHolder(new PeerServlet());
                boolean isGzipEnabled = Nxt.getBooleanProperty("fch.enablePeerServerGZIPFilter");
                peerServletHolder.setInitParameter("isGzipEnabled", Boolean.toString(isGzipEnabled));
                ServletHandler peerHandler = new ServletHandler();
                peerHandler.addServletWithMapping(peerServletHolder, "/*");
                if (Nxt.getBooleanProperty("fch.enablePeerServerDoSFilter")) {
                    FilterHolder dosFilterHolder = peerHandler.addFilterWithMapping(DoSFilter.class, "/*", FilterMapping.DEFAULT);
                    dosFilterHolder.setInitParameter("maxRequestsPerSec", Nxt.getStringProperty("fch.peerServerDoSFilter.maxRequestsPerSec"));
                    dosFilterHolder.setInitParameter("delayMs", Nxt.getStringProperty("fch.peerServerDoSFilter.delayMs"));
                    dosFilterHolder.setInitParameter("maxRequestMs", Nxt.getStringProperty("fch.peerServerDoSFilter.maxRequestMs"));
                    dosFilterHolder.setInitParameter("trackSessions", "false");
                    dosFilterHolder.setAsyncSupported(true);
                }
                if (isGzipEnabled) {
                    FilterHolder gzipFilterHolder = peerHandler.addFilterWithMapping(GzipFilter.class, "/*", FilterMapping.DEFAULT);
                    gzipFilterHolder.setInitParameter("methods", "GET,POST");
                    gzipFilterHolder.setAsyncSupported(true);
                }

                peerServer.setHandler(peerHandler);
                peerServer.setStopAtShutdown(true);
                ThreadPool.runBeforeStart(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            peerServer.start();
                            Logger.logMessage("Started peer networking server at " + host + ":" + port);
                        } catch (Exception e) {
                            Logger.logErrorMessage("Failed to start peer networking server", e);
                            throw new RuntimeException(e.toString(), e);
                        }
                    }
                }, true);
            } else {
                peerServer = null;
                Logger.logMessage("shareMyAddress is disabled, will not start peer networking server");
            }
        }

        private static void init() {}

        private Init() {}

    }

    private static final Runnable peerUnBlacklistingThread = new Runnable() {

        @Override
        public void run() {

            try {
                try {

                    long curTime = System.currentTimeMillis();
                    for (PeerImpl peer : peers.values()) {
                        peer.updateBlacklistedStatus(curTime);
                    }

                } catch (Exception e) {
                    Logger.logDebugMessage("Error un-blacklisting peer", e);
                }
            } catch (Throwable t) {
                Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
                t.printStackTrace();
                System.exit(1);
            }

        }

    };

    private static final Runnable peerConnectingThread = new Runnable() {

        @Override
        public void run() {

            try {
                try {

                    final int now = Nxt.getEpochTime();
                    if (!hasEnoughConnectedPublicPeers(Peers.maxNumberOfConnectedPublicPeers)) {
                        List<Future> futures = new ArrayList<>();
                        for (int i = 0; i < 10; i++) {
                            futures.add(peersService.submit(new Runnable() {
                                @Override
                                public void run() {
                                    PeerImpl peer = (PeerImpl) getAnyPeer(ThreadLocalRandom.current().nextInt(2) == 0 ? Peer.State.NON_CONNECTED : Peer.State.DISCONNECTED, false);
                                    if (peer != null && now - peer.getLastConnectAttempt() > 600) {
                                        peer.connect();
                                    }
                                }
                            }));
                        }
                        for (Future future : futures) {
                            future.get();
                        }
                    }

                    for (PeerImpl peer : peers.values()) {
                        if (peer.getState() == Peer.State.CONNECTED && now - peer.getLastUpdated() > 3600) {
                            peer.connect();
                        }
                    }

                    if (hasTooManyKnownPeers() && hasEnoughConnectedPublicPeers(Peers.maxNumberOfConnectedPublicPeers)) {
                        int initialSize = peers.size();
                        for (PeerImpl peer : peers.values()) {
                            if (now - peer.getLastUpdated() > 24 * 3600) {
                                peer.remove();
                            }
                            if (hasTooFewKnownPeers()) {
                                break;
                            }
                        }
                        Logger.logDebugMessage("Reduced peer pool size from " + initialSize + " to " + peers.size());
                    }

                } catch (Exception e) {
                    Logger.logDebugMessage("Error connecting to peer", e);
                }
            } catch (Throwable t) {
                Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
                t.printStackTrace();
                System.exit(1);
            }

        }

    };

    private static final Runnable getMorePeersThread = new Runnable() {

        private final JSONStreamAware getPeersRequest;
        {
            JSONObject request = new JSONObject();
            request.put("requestType", "getPeers");
            getPeersRequest = JSON.prepareRequest(request);
        }

        private volatile boolean addedNewPeer;
        {
            Peers.addListener(new Listener<Peer>() {
                @Override
                public void notify(Peer peer) {
                    addedNewPeer = true;
                }
            }, Event.NEW_PEER);
        }

        @Override
        public void run() {

            try {
                try {
                    if (hasTooManyKnownPeers()) {
                        return;
                    }
                    Peer peer = getAnyPeer(Peer.State.CONNECTED, true);
                    if (peer == null) {
                        return;
                    }
                    JSONObject response = peer.send(getPeersRequest, 10 * 1024 * 1024);
                    if (response == null) {
                        return;
                    }
                    JSONArray peers = (JSONArray)response.get("peers");
                    Set<String> addedAddresses = new HashSet<>();
                    if (peers != null) {
                        for (Object announcedAddress : peers) {
                            if (addPeer((String) announcedAddress) != null) {
                                addedAddresses.add((String) announcedAddress);
                            }
                        }
                        if (savePeers && addedNewPeer) {
                            updateSavedPeers();
                            addedNewPeer = false;
                        }
                    }

                    JSONArray myPeers = new JSONArray();
                    for (Peer myPeer : Peers.getAllPeers()) {
                        if (! myPeer.isBlacklisted() && myPeer.getAnnouncedAddress() != null
                                && myPeer.getState() == Peer.State.CONNECTED && myPeer.shareAddress()
                                && ! addedAddresses.contains(myPeer.getAnnouncedAddress())
                                && ! myPeer.getAnnouncedAddress().equals(peer.getAnnouncedAddress())) {
                            myPeers.add(myPeer.getAnnouncedAddress());
                        }
                    }
                    if (myPeers.size() > 0) {
                        JSONObject request = new JSONObject();
                        request.put("requestType", "addPeers");
                        request.put("peers", myPeers);
                        peer.send(JSON.prepareRequest(request), 0);
                    }

                } catch (Exception e) {
                    Logger.logDebugMessage("Error requesting peers from a peer", e);
                }
            } catch (Throwable t) {
                Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
                t.printStackTrace();
                System.exit(1);
            }

        }

        private void updateSavedPeers() {
            Set<String> oldPeers = new HashSet<>(PeerDb.loadPeers());
            Set<String> currentPeers = new HashSet<>();
            for (Peer peer : Peers.peers.values()) {
                if (peer.getAnnouncedAddress() != null && ! peer.isBlacklisted()) {
                    currentPeers.add(peer.getAnnouncedAddress());
                }
            }
            Set<String> toDelete = new HashSet<>(oldPeers);
            toDelete.removeAll(currentPeers);
            try {
                Db.db.beginTransaction();
                PeerDb.deletePeers(toDelete);
	            //Logger.logDebugMessage("Deleted " + toDelete.size() + " peers from the peers database");
                currentPeers.removeAll(oldPeers);
                PeerDb.addPeers(currentPeers);
	            //Logger.logDebugMessage("Added " + currentPeers.size() + " peers to the peers database");
                Db.db.commitTransaction();
            } catch (Exception e) {
                Db.db.rollbackTransaction();
                throw e;
            } finally {
                Db.db.endTransaction();
            }
        }

    };

    static {
        Account.addListener(new Listener<Account>() {
            @Override
            public void notify(Account account) {
                for (PeerImpl peer : Peers.peers.values()) {
                    if (peer.getHallmark() != null && peer.getHallmark().getAccountId() == account.getId()) {
                        Peers.listeners.notify(peer, Peers.Event.WEIGHT);
                    }
                }
            }
        }, Account.Event.BALANCE);
    }

    static {
        if (! Constants.isOffline) {
            ThreadPool.scheduleThread("PeerConnecting", Peers.peerConnectingThread, 5);
            ThreadPool.scheduleThread("PeerUnBlacklisting", Peers.peerUnBlacklistingThread, 1);
            if (Peers.getMorePeers) {
                ThreadPool.scheduleThread("GetMorePeers", Peers.getMorePeersThread, 5);
            }
        }
    }

    public static void init() {
        Init.init();
    }

    public static void shutdown() {
        if (Init.peerServer != null) {
            try {
                Init.peerServer.stop();
            } catch (Exception e) {
                Logger.logShutdownMessage("Failed to stop peer server", e);
            }
        }
        if (dumpPeersVersion != null) {
            StringBuilder buf = new StringBuilder();
            for (Map.Entry<String,String> entry : announcedAddresses.entrySet()) {
                Peer peer = peers.get(entry.getValue());
                if (peer != null && peer.getState() == Peer.State.CONNECTED && peer.shareAddress() && !peer.isBlacklisted()
                        && peer.getVersion() != null && peer.getVersion().startsWith(dumpPeersVersion)) {
                    buf.append("('").append(entry.getKey()).append("'), ");
                }
            }
            Logger.logShutdownMessage(buf.toString());
        }
        ThreadPool.shutdownExecutor(sendingService);
        ThreadPool.shutdownExecutor(peersService);

    }

    public static boolean addListener(Listener<Peer> listener, Event eventType) {
        return Peers.listeners.addListener(listener, eventType);
    }

    public static boolean removeListener(Listener<Peer> listener, Event eventType) {
        return Peers.listeners.removeListener(listener, eventType);
    }

    static void notifyListeners(Peer peer, Event eventType) {
        Peers.listeners.notify(peer, eventType);
    }

    public static int getDefaultPeerPort() {
        return Constants.isTestnet ? TESTNET_PEER_PORT : DEFAULT_PEER_PORT;
    }

    public static Collection<? extends Peer> getAllPeers() {
        return allPeers;
    }

    public static List<Peer> getActivePeers() {
        return getPeers(new Filter<Peer>() {
            @Override
            public boolean ok(Peer peer) {
                return peer.getState() != Peer.State.NON_CONNECTED;
            }
        });
    }

    public static List<Peer> getPeers(final Peer.State state) {
        return getPeers(new Filter<Peer>() {
            @Override
            public boolean ok(Peer peer) {
                return peer.getState() == state;
            }
        });
    }

    public static List<Peer> getPeers(Filter<Peer> filter) {
        return getPeers(filter, Integer.MAX_VALUE);
    }

    public static List<Peer> getPeers(Filter<Peer> filter, int limit) {
        List<Peer> peerList = new ArrayList<>();
        for (PeerImpl peer : peers.values()) {
            if (filter.ok(peer)) {
                peerList.add(peer);
                if (peerList.size() >= limit) {
                    return peerList;
                }
            }
        }
        return peerList;
    }

    public static Peer getPeer(String peerAddress) {
        Peer peer;
        if ((peer = peers.get(peerAddress)) != null) {
            return peer;
        }
        String address;
        if ((address = announcedAddresses.get(peerAddress)) != null) {
            peer = peers.get(address);
        }
        return peer;
    }

    public static Peer addPeer(String announcedAddress) {
        if (announcedAddress == null) {
            return null;
        }
        announcedAddress = announcedAddress.trim();
        Peer peer;
        if ((peer = peers.get(announcedAddress)) != null) {
            return peer;
        }
        String address;
        if ((address = announcedAddresses.get(announcedAddress)) != null && (peer = peers.get(address)) != null) {
            return peer;
        }
        try {
            URI uri = new URI("http://" + announcedAddress);
            String host = uri.getHost();
            if (host == null) {
                return null;
            }
            int port = uri.getPort();
            if ((peer = peers.get(addressWithPort(host, port))) != null) {
                return peer;
            }
            InetAddress inetAddress = InetAddress.getByName(host);
            return addPeer(inetAddress.getHostAddress(), port, announcedAddress);
        } catch (URISyntaxException | UnknownHostException e) {
            //Logger.logDebugMessage("Invalid peer address: " + announcedAddress + ", " + e.toString());
            return null;
        }
    }

    static PeerImpl addPeer(final String address, int port, final String announcedAddress) {

        //re-add the [] to ipv6 addresses lost in getHostAddress() above
        String cleanAddress = address;
        if (cleanAddress.split(":").length > 2) {
            cleanAddress = "[" + cleanAddress + "]";
        }

        cleanAddress = addressWithPort(cleanAddress, port);

        PeerImpl peer;
        if ((peer = peers.get(cleanAddress)) != null) {
            return peer;
        }
        String peerAddress = normalizeHostAndPort(cleanAddress);
        if (peerAddress == null) {
            return null;
        }
        if ((peer = peers.get(peerAddress)) != null) {
            return peer;
        }

        String announcedPeerAddress = address.equals(announcedAddress) ? peerAddress : normalizeHostAndPort(announcedAddress);

        if (Peers.myAddress != null && Peers.myAddress.length() > 0 && Peers.myAddress.equalsIgnoreCase(announcedPeerAddress)) {
            return null;
        }

        peer = new PeerImpl(peerAddress, announcedPeerAddress);
        if (Constants.isTestnet && peer.getPort() > 0 && peer.getPort() != TESTNET_PEER_PORT) {
            Logger.logDebugMessage("Peer " + peerAddress + " on testnet is not using port " + TESTNET_PEER_PORT + ", ignoring");
            return null;
        }
        if (!Constants.isTestnet && peer.getPort() > 0 && peer.getPort() == TESTNET_PEER_PORT) {
            Logger.logDebugMessage("Peer " + peerAddress + " is using testnet port " + peer.getPort() + ", ignoring");
            return null;
        }
        if (!hasTooManyKnownPeers()) {
            peers.put(peerAddress, peer);
            if (announcedAddress != null) {
                updateAddress(peer);
            }
            listeners.notify(peer, Event.NEW_PEER);
        }
        return peer;
    }

    static PeerImpl removePeer(PeerImpl peer) {
        if (peer.getAnnouncedAddress() != null) {
            announcedAddresses.remove(peer.getAnnouncedAddress());
        }
        return peers.remove(peer.getPeerAddress());
    }

    static void updateAddress(PeerImpl peer) {
        String oldAddress = announcedAddresses.put(peer.getAnnouncedAddress(), peer.getPeerAddress());
        if (oldAddress != null && !peer.getPeerAddress().equals(oldAddress)) {
            //Logger.logDebugMessage("Peer " + peer.getAnnouncedAddress() + " has changed address from " + oldAddress
            //        + " to " + peer.getPeerAddress());
            Peer oldPeer = peers.remove(oldAddress);
            if (oldPeer != null) {
                Peers.notifyListeners(oldPeer, Peers.Event.REMOVE);
            }
        }
        peers.put(peer.getPeerAddress(), peer);
    }

    public static void connectPeer(Peer peer) {
        peer.unBlacklist();
        ((PeerImpl)peer).connect();
    }
    
    public static void sendToSomePeers(Block block) {
        JSONObject request = block.getJSONObject();
        request.put("requestType", "processBlock");
        sendToSomePeers(request);
    }

    public static void sendToSomePeers(List<? extends Transaction> transactions) {
        JSONObject request = new JSONObject();
        JSONArray transactionsData = new JSONArray();
        for (Transaction transaction : transactions) {
            transactionsData.add(transaction.getJSONObject());
        }
        request.put("requestType", "processTransactions");
        request.put("transactions", transactionsData);
        sendToSomePeers(request);
    }

    private static void sendToSomePeers(final JSONObject request) {
        sendingService.submit(new Runnable() {
            @Override
            public void run() {
                final JSONStreamAware jsonRequest = JSON.prepareRequest(request);

                int successful = 0;
                List<Future<JSONObject>> expectedResponses = new ArrayList<>();
                for (final Peer peer : peers.values()) {

                    if (Peers.enableHallmarkProtection && peer.getWeight() < Peers.pushThreshold) {
                        continue;
                    }

                    if (!peer.isBlacklisted() && peer.getState() == Peer.State.CONNECTED && peer.getAnnouncedAddress() != null) {
                        Future<JSONObject> futureResponse = peersService.submit(new Callable<JSONObject>() {
                            @Override
                            public JSONObject call() {
                                return peer.send(jsonRequest);
                            }
                        });
                        expectedResponses.add(futureResponse);
                    }
                    if (expectedResponses.size() >= Peers.sendToPeersLimit - successful) {
                        for (Future<JSONObject> future : expectedResponses) {
                            try {
                                JSONObject response = future.get();
                                if (response != null && response.get("error") == null) {
                                    successful += 1;
                                }
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            } catch (ExecutionException e) {
                                Logger.logDebugMessage("Error in sendToSomePeers", e);
                            }

                        }
                        expectedResponses.clear();
                    }
                    if (successful >= Peers.sendToPeersLimit) {
                        return;
                    }
                }
            }
        });
    }

    public static Peer getAnyPeer(final Peer.State state, final boolean applyPullThreshold) {
        return getWeightedPeer(getPublicPeers(state, applyPullThreshold));
    }

    public static List<Peer> getPublicPeers(final Peer.State state, final boolean applyPullThreshold) {
        return getPeers(new Filter<Peer>() {
            @Override
            public boolean ok(Peer peer) {
                return !peer.isBlacklisted() && peer.getState() == state && peer.getAnnouncedAddress() != null
                        && (!applyPullThreshold || !Peers.enableHallmarkProtection || peer.getWeight() >= Peers.pullThreshold);
            }
        });
    }

    public static Peer getWeightedPeer(List<Peer> selectedPeers) {
        if (selectedPeers.isEmpty()) {
            return null;
        }
        if (! Peers.enableHallmarkProtection || ThreadLocalRandom.current().nextInt(3) == 0) {
            return selectedPeers.get(ThreadLocalRandom.current().nextInt(selectedPeers.size()));
        }
        long totalWeight = 0;
        for (Peer peer : selectedPeers) {
            long weight = peer.getWeight();
            if (weight == 0) {
                weight = 1;
            }
            totalWeight += weight;
        }
        long hit = ThreadLocalRandom.current().nextLong(totalWeight);
        for (Peer peer : selectedPeers) {
            long weight = peer.getWeight();
            if (weight == 0) {
                weight = 1;
            }
            if ((hit -= weight) < 0) {
                return peer;
            }
        }
        return null;
    }

    static String addressWithPort(String address) {
        try {
            URI uri = new URI("http://" + address.trim());
            return addressWithPort(uri.getHost(), uri.getPort());
        } catch (URISyntaxException e) {
            return null;
        }
    }

    static String addressWithPort(String host, int port) {
        return port > 0 && port != Peers.getDefaultPeerPort() ? host + ":" + port : host;
    }

    static String normalizeHostAndPort(String address) {
        try {
            if (address == null) {
                return null;
            }
            URI uri = new URI("http://" + address.trim());
            String host = uri.getHost();
            if (host == null || host.equals("") || host.equals("localhost") ||
                                host.equals("127.0.0.1") || host.equals("[0:0:0:0:0:0:0:1]")) {
                return null;
            }
            InetAddress inetAddress = InetAddress.getByName(host);
            if (inetAddress.isAnyLocalAddress() || inetAddress.isLoopbackAddress() ||
                                                   inetAddress.isLinkLocalAddress()) {
                return null;
            }
            return addressWithPort(host, uri.getPort());
        } catch (URISyntaxException |UnknownHostException e) {
            return null;
        }
    }

    static boolean hasTooFewKnownPeers() {
        return peers.size() < Peers.minNumberOfKnownPeers;
    }

    static boolean hasTooManyKnownPeers() {
        return peers.size() > Peers.maxNumberOfKnownPeers;
    }

    private static boolean hasEnoughConnectedPublicPeers(int limit) {
        return getPeers(new Filter<Peer>() {
            @Override
            public boolean ok(Peer peer) {
                return !peer.isBlacklisted() && peer.getState() == Peer.State.CONNECTED && peer.getAnnouncedAddress() != null
                        && (! Peers.enableHallmarkProtection || peer.getWeight() > 0);
            }
        }, limit).size() >= limit;
    }

    private Peers() {} // never

}
