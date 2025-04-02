import java.rmi.*;
import java.rmi.server.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class Client extends UnicastRemoteObject implements RemoteBroadcastInterface {

    WordRepositoryInterface wordRepo;
    private BroadcastHandler broadcastHandler;
    private CrissCrossPuzzleInterface server;
    private String username;
    private Integer gameID = -1;
    private volatile Boolean gameOverFlag = false;
    private volatile Boolean gameStarted = false;
    private char[][] currentPuzzle;
    private PuzzleObject puzzle;

    public Client(String username) throws RemoteException {
        super();
        try {
            this.username = username;
            this.broadcastHandler = new BroadcastHandler(username);
            wordRepo = (WordRepositoryInterface) Naming.lookup("rmi://localhost/WordRepository");
            Naming.rebind("rmi://localhost/" + username + "_Client", this);
            new Thread(this::processMessages).start();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

    }

    public static void main(String[] args) throws RemoteException {

        try {
            System.out.println("Enter your username: ");
            String username = new BufferedReader(new InputStreamReader(System.in)).readLine();
            Client client = new Client(username);
            client.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void run() {
        try {
            server = (CrissCrossPuzzleInterface) Naming.lookup("rmi://localhost:1099/Server");
            showMainMenu();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void receive(BroadcastHandler.Message message) throws RemoteException {
        broadcastHandler.receive(message);
    }

    private void showMainMenu() {
        while (!gameOverFlag) {
            System.out.println(Constants.MAIN_MENU_MESSAGE);
            try {
                int choice = Integer.parseInt(System.console().readLine());
                handleMenuChoice(choice);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input!");
            }

        }
    }

    private void handleMenuChoice(int choice) {
        switch (choice) {
            case 1:
                startNewGame();
                break;
            case 2:
                joinExistingGame();
                break;
            case 3:
                exitGame();
                break;
            default:
                System.out.println("Invalid choice!");

        }
    }

    private void startNewGame() {

        try {
            System.out.println("Enter desired number of words");
            int numWords = Integer.parseInt(System.console().readLine());

            System.out.println("Enter desired number of players");
            int numOfPlayers = Integer.parseInt(System.console().readLine());

            server.startGame(username, numWords, numOfPlayers); // fix
            broadcastHandler.broadcast("JOIN", gameID);
            waitForGameStart();
        } catch (Exception e) {
            System.out.println("Could not start a game");

        }

    }

    private void joinExistingGame() {
        System.out.println("Enter game ID: ");
        int targetGameID = Integer.parseInt(System.console().readLine());
        try {
            if (!server.isGameReady(targetGameID)) { // Fix condition
                if (server.joinGame(targetGameID, username)) {
                    gameID = targetGameID;


                    //Need help here
                    char[][] puzzleSlave = server.getInitialPuzzle(gameID);
                    this.puzzle = new PuzzleObject(gameID);
                    this.puzzle.initPuzzleSlave(puzzleSlave);



                    broadcastHandler.broadcast("JOIN", gameID);
                    waitForGameStart();
                }
            } else {
                System.out.println("Game is already full or running.");
            }
        } catch (Exception e) {
            System.out.println("Error joining game: " + e.getMessage());
        }
    }

    private void waitForGameStart() throws InterruptedException {
        synchronized (this) {
            while (!gameOverFlag && !gameStarted) {

                wait();
            }
            try{
                if(gameStarted){
            startGameSession();
                }
            }
            catch(Exception e){
                System.out.println("Could not start game");
            }
        }
    }

    private void startGameSession() {
        try {
            new Thread(() -> {
                try {
                    handleGameInput();
                } catch (Exception e) {
                    System.err.println("Error in game input thread: " + e.getMessage());
                }
            }).start();       
        } catch (Exception e) {
            System.out.println("Error starting game session: " + e.getMessage());
        }
    }

    private void handleGameInput(){
        System.out.println(Constants.GUESS_MESSAGE);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (!gameOverFlag) {
                if (reader.ready()) {
                    String input = reader.readLine();
                    handleGameCommand(input);
                }
                Thread.sleep(100); //so they dont spam
            }
    }
    catch(Exception e){
        System.out.println("error handling input");
    }
}

    private void handleGameCommand(String input) throws RemoteException{
        if(input.equals("~")){
            exitGame();
        }
        else if(input.startsWith("?")){
            checkWord(input.substring(1));
        }
        else if(isValidGuess(input))
        broadcastHandler.broadcast("GUESS", input);
        else
        System.out.println("");
    }

 

    private void processMessages()  {
        
        while (!gameOverFlag) {
            try{
            BroadcastHandler.Message msg = broadcastHandler.getNextMessage();
            if (msg != null){
                 processMessage(msg);
            }
        }
        catch (RemoteException e) {
            System.err.println("Remote connection error: " + e.getMessage());
            e.printStackTrace();
           // gameOverFlag = true; 
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
   
    }

    private void processMessage(BroadcastHandler.Message msg) throws RemoteException{
        switch (msg.type) {
            case "GUESS": processGuess(msg); break;
            case "STATE": updateState(msg); break;
            case "JOIN": handlePlayerJoin(msg); break;
            case "GAMEOVER": System.out.println(msg.contents); break;
            case "GAMESTART":
            synchronized (this) {
                gameStarted = true;
                notifyAll();
            }
            break;
            
        }

    }

    private void processGuess(BroadcastHandler.Message msg) throws RemoteException {
        String guess = (String) msg.contents;
        System.out.println("Processing guess from " + msg.senderID + ": " + guess);
        boolean solved;
        if(guess.length() == 1){
            solved = puzzle.guessChar(msg.senderID, guess.charAt(0));
        }
        else{
            solved = puzzle.guessWord(msg.senderID, guess);
        }
        currentPuzzle = puzzle.getPuzzleSlaveCopy();
        renderPuzzle();
        if(solved || puzzle.getGuessCounter() <= 0){
            gameOverFlag = true;
            broadcastHandler.broadcast("GAMEOVER", "Game Over! Solved: " + solved);
        }
    }

    private void updateState(BroadcastHandler.Message msg) {
       // this.currentPuzzle = (char[][]) msg.contents;
       System.out.println(msg.contents);
        //renderPuzzle();
    }

    private void handlePlayerJoin(BroadcastHandler.Message msg) {
        System.out.println("Player: " + msg.senderID + " has joined the game");
    }

        /**
     * Prints the given puzzle to the console.
     *
     * @param puzzle the 2D array to print
     */
    private void renderPuzzle() {

        for (int i = 0; i < currentPuzzle.length; i++) {
            for (int j = 0; j < currentPuzzle[i].length; j++) {
                System.out.print(currentPuzzle[i][j]);
            }
            System.out.println();
        }
    }

    private void exitGame() {
        try {
            broadcastHandler.broadcast("LEAVE", gameID);
            server.playerQuit(gameID, username);
            
            UnicastRemoteObject.unexportObject(this, true);
        System.exit(1); 
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        e.printStackTrace();
        }
    }




 
    /**
     * Prompts the user for a letter guess and ensures it is valid. Valid guesses
     * are strings of letters, the string "?", or the string "~". If the user
     * enters an invalid guess, this method will repeatedly prompt the user until
     * a valid guess is entered.
     * 
     * @return the user's valid guess
     */
    private boolean isValidGuess(String guess){
        boolean isValid = false;

        System.out.println(Constants.GUESS_MESSAGE);
        if(!guess.matches("^[a-zA-Z?~\\\\]*$") || guess.equals("")){
            isValid = false;
        }
        else
        isValid = true;

        return isValid;
    }

    public Boolean checkWord(String word) throws RemoteException {
        return wordRepo.checkWord(word);
    }

    
}