import java.rmi.*;
import java.rmi.server.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server extends UnicastRemoteObject implements CrissCrossPuzzleInterface {

    private WordRepositoryInterface wordRepo;
    private GameState currentGame = null;

    protected Server() throws RemoteException {
        super();
        try {
            wordRepo = (WordRepositoryInterface) Naming.lookup("rmi://localhost/WordRepository");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        try {
            Server server = new Server();
            System.out.println("The game server is running...");
            Naming.rebind("rmi://localhost:1099/Server", server);
            System.out.println("Server is registered with the RMI registry with URL: rmi://localhost:1099/Server");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public synchronized Integer startGame(String username, Integer numWords, Integer numberOfPlayers)
            throws RemoteException {
        if (currentGame != null) {
            throw new RemoteException("A game is already in progess");
        }
        currentGame = new GameState(numWords, numberOfPlayers, username);
        return currentGame.getGameID();

    }

    public synchronized Boolean joinGame(Integer gameID, String username) throws RemoteException {
        if (currentGame == null || currentGame.getGameID() != gameID) {
            return false;
        }
        return currentGame.addPlayer(username);
    }

    public synchronized void playerQuit(Integer gameID, String username) throws RemoteException {
        if (currentGame != null && currentGame.getGameID() == gameID) {
            currentGame.removePlayer(username);
            if (currentGame.getPlayerCount() == 0) {
                currentGame = null;
            }
        }
    }

    public synchronized String getGameState() throws RemoteException {
        if (currentGame == null) {
            return "NO GAME";
        } else if (currentGame.isReadyToStart()) {
            return "PENDING";
        } else {
            return "RUNNING";
        }
    }

    public synchronized List<String> getPlayerList() throws RemoteException {
        if (currentGame == null) {
            return Collections.emptyList();
        }
        return currentGame.getPlayerList();
    }

    public synchronized Map<String, RemoteBroadcastInterface> getPlayerReferences() throws RemoteException {
        if (currentGame == null) {
            return Collections.emptyMap();
        }

        Map<String, RemoteBroadcastInterface> references = new HashMap<>();
        for (String player : currentGame.getPlayerList()) {
            try {
                RemoteBroadcastInterface playerRef = (RemoteBroadcastInterface) Naming
                        .lookup("rmi://localhost/" + player + "_Client");
                references.put(player, playerRef);

            } catch (Exception e) {
                System.err.println("Could not locate player reference: " + player);
            }
        }
        return references;
    }

    private static class GameState {
        private final int gameID;
        private final int numWords;
        private final int requiredPlayers;
        private final Set<String> players = new HashSet<>();

        public GameState(int numWords, int requiredPlayers, String creator) {
            this.gameID = 1;
            this.numWords = numWords;
            this.requiredPlayers = requiredPlayers;
            this.players.add(creator);
        }
s
        public boolean addPlayer(String player) {
            if (player.size() < requiredPlayers) {
                return players.add(player);
            }
            return false;
        }

        public boolean removePlayer(String player) {
            return players.remove(player);
        }

        public boolean isReadyToStart() {
            return players.size() < requiredPlayers;
        }

        public List<String> getPlayerList() {
            return new ArrayList<>(players);
        }

        public int getPlayerCount() {
            return players.size();
        }

        public int getGameId() {
            return gameID;
        }

    }
}





