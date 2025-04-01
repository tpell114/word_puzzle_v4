import java.rmi.*;
import java.rmi.server.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server extends UnicastRemoteObject implements CrissCrossPuzzleInterface {

    ConcurrentHashMap<Integer, PuzzleObject> gamesMap = new ConcurrentHashMap<>();

   // private WordRepositoryInterface wordRepo;
    private GameState currentGame = null;
    private PuzzleObject initialPuzzle;
    private  BroadcastHandler broadcastHandler;
    private String username;

    protected Server() throws RemoteException {
        super();
        try {
            this.username = "SERVER";
            this.broadcastHandler = new BroadcastHandler(username);
            Naming.rebind("rmi://localhost/" + username + "_Client", this);
          //  wordRepo = (WordRepositoryInterface) Naming.lookup("rmi://localhost/WordRepository");
        }

         catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
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


    public synchronized void startGame(String username, Integer numWords, Integer numberOfPlayers)
            throws RemoteException {
        if (currentGame != null) {
            throw new RemoteException("A game is already in progess");
        }
        
        currentGame = new GameState(numWords, numberOfPlayers, username);
        gamesMap.put(1,new PuzzleObject(username, 1, numWords, 100));
        broadcastHandler.broadcast("STATE", getInitialPuzzle(1));

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

    public synchronized Boolean isGameReady(Integer gameID) throws RemoteException {
        return currentGame != null && currentGame.getGameID() == gameID && currentGame.isReadyToStart();
    }

    public synchronized Integer getPlayerCount(Integer gameID) throws RemoteException {
        return currentGame.getPlayerCount();
    }

     public char[][] getInitialPuzzle(Integer gameID) throws RemoteException {
        return gamesMap.get(gameID).getPuzzleSlaveCopy();
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

        public boolean addPlayer(String player) {
            if (players.size() < requiredPlayers) {
                return players.add(player);
            }
            return false;
        }

        public boolean removePlayer(String player) {
            return players.remove(player);
        }

        public boolean isReadyToStart() {
            return players.size() == requiredPlayers;
        }

        public List<String> getPlayerList() {
            return new ArrayList<>(players);
        }

        public int getPlayerCount() {
            return players.size();
        }


        public int getGameID() {
            return gameID;
        }

    }

   

}





