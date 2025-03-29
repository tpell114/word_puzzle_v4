import java.rmi.*;
import java.rmi.server.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class Client extends UnicastRemoteObject implements RemoteBroadcastInterface {

    private final BroadcastHandler broadcastHandler;
    private CrissCrossPuzzleInterface server;
    private String username;
    private Integer gameID = -1;
    private volatile Boolean gameOverFlag = false;
    private char[][] currentPuzzle;
    private int remainingGuesses;

    public Client(String username) throws RemoteException {
            super();
            try{
            this.username = username;
            this.broadcastHandler = new BroadcastHandler(username);
          Naming.rebind("rmi://localhost/" + username + "_Client", this);
            }
           catch (Exception e) {
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
                modifyWordRepository();
                break;
            case 4:
                exitGame();
                break;
            default:
                System.out.println("Invalid choice!");

        }
    }

    private void startNewGame() {
        System.out.println("Enter desired number of words");
        int numWords = Integer.parseInt(System.console().readLine());

        System.out.println("Enter desired number of players");
        int numOfPlayers = Integer.parseInt(System.console().readLine());

        gameID = server.startGame(username, null, numWords, numOfPlayers); // fix
        broadcastHandler.broadcast("JOIN", gameID);
        waitForGameStart();

    }

    private void joinExistingGame() {
        System.out.println("Enter game ID: ");
        int targetGameID = Integer.parseInt(System.console().readLine());

        if (server.joinGame(targetGameID, username)) {
            gameID = targetGameID;
            broadcastHandler.broadcast("JOIN_ACK", gameID);
            waitForGameStart();
        }
    }

    private void waitForGameStart() throws InterruptedException {
        synchronized (this) {
            while (!gameOverFlag) {
                wait();
            }
            startGameSession();
        }
    }

    private void startGameSession() {
        new Thread(this::handleGameInput).start();
        new Thread(this::processMessages).start();
    }

    private void handleGameInput(){
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (!gameOverFlag) {
                if (reader.ready()) {
                    String input = reader.readLine();
                    handleGameCommand(input);
                }
                Thread.sleep(100); //so they dont spam
            }
    }

    private void handleGameCommand(String input) throws RemoteException{
        if(input.equals("~")){
            exitGame();
        }
        else if(input.startsWith("?")){
            checkWord(input.substring(1));
        }
        else 
        broadcastHandler.broadcast("GUESS", input);
    }

    @Override
    public void receive(BroadcastHandler.Message message) throws RemoteException{
        broadcastHandler.receive(message);
    }

    private void processMessages() {
        while (!gameOverFlag) {
            BroadcastHandler.Message msg = broadcastHandler.getNextMessage();
            if (msg != null) processMessage(msg);
        }
    }

    private void processMessage(BroadcastHandler.Message msg){
        switch (msg.type) {
            case "GUESS": processGuess(msg); break;
            case "STATE": updateState(msg); break;
            case "JOIN": handlePlayerJoin(msg); break;
        }

    }

    private void processGuess(BroadcastHandler.Message msg) {
        String guess = (String) msg.contents;
        System.out.println("Processing guess from " + msg.senderID + ": " + guess);

        //to do

        broadcastHandler.broadcast("STATE_UPDATE", currentPuzzle);
    }

    private void updateState(BroadcastHandler.Message msg) {
        this.currentPuzzle = (char[][]) msg.contents;
        renderPuzzle();
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
            //go back to main menu or something
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        e.printStackTrace();
        }
    }






  


    /**
     * Allows the user to join an existing game by entering a valid game ID.
     * If the game ID is valid, the player is added to the game and waits for the game to start.
     * If the entered game ID is 0, the user is returned to the main menu.
     * Upon successful joining, the initial puzzle state is displayed and the player is notified
     * to wait for their turn.
     *
     * @throws Exception if an error occurs during joining, game startup, or communication with the server
     */
    private void joinGame() {

        try {
            System.out.println("\nEnter the ID of the game you would like to join. Or enter 0 to return to the main menu: ");
            this.gameID = Integer.valueOf(System.console().readLine());

            while(!server.joinGame(gameID, this.username, this)){

                if(this.gameID == 0){
                    return;
                }

                System.out.println("Invalid game ID.");
                System.out.println("\nEnter the ID of the game you would like to join. Or enter 0 to return to the main menu: ");
                this.gameID = Integer.valueOf(System.console().readLine());
            }

            System.out.println("You have joined game ID: " + gameID 
                            + "\nPlease wait for the game to start...");

            synchronized (this){wait();}

            printPuzzle(server.getInitialPuzzle(gameID));
            System.out.println("Counter: " + server.getGuessCounter(gameID) 
                            + "\nWord guessed: 0" 
                            + "\nPlease wait for your turn.");
            myTurn = false;
            playGame();

        } catch (Exception e) {
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
    private String getValidGuess(){

        System.out.println(Constants.GUESS_MESSAGE);
        String guess = System.console().readLine().toLowerCase().trim();

        while ((!guess.matches("^[a-zA-Z?~\\\\]*$") || guess.equals(""))){
            System.out.println("Invalid input."
                            + "\n" + Constants.GUESS_MESSAGE);
            guess = System.console().readLine().toLowerCase().trim();
        }

        return guess;
    }

    /**
     * Modifies the word repository by repeatedly prompting the user for a valid
     * command and executing it. The user is prompted for a command until they
     * enter "~" to quit. The commands are as follows:
     * 
     * +word: Adds the given word to the word repository
     * -word: Removes the given word from the word repository
     * ?word: Checks if the given word exists in the word repository
     * 
     * This method will continue to prompt the user for a command until they
     * enter "~" to quit.
     */
    private void modifyWordRepo(){

        try {
            System.out.println(Constants.WORD_REPO_MESSAGE);
            String input = System.console().readLine();

            while (!input.equals("~") && (input.isEmpty() || !input.matches("^[+-?][a-zA-Z]*$"))) {
                System.out.println("Invalid input."
                                + "\n" + Constants.WORD_REPO_MESSAGE);
                input = System.console().readLine();
            }

            while (!input.equals("~")) {

                currentSequence = server.getSequence(this.username, this.gameID);

                // 50% chance of repeating the same sequence number
                if (Math.random() < 0.5) {
                    System.out.println("[Test] Simulating duplicate request with sequence: " + currentSequence);
                } else {
                    currentSequence++;
                }

                if (input.charAt(0) == '+') {

                    Integer result = server.addWord(input.substring(1), username, currentSequence);

                    if (result == 1) {
                        System.out.println("\nSuccessfully added word '" + input.substring(1) + "' to the word repository.");
                    } else if (result == 0) {
                        System.out.println("\nFailed to add word '" + input.substring(1) + "' to the word repository, it may already exist.");
                    } else if (result == -1) {
                        System.out.println("[Test] Server ignored duplicate guess. Trying again...");
                        continue;
                    }

                } else if (input.charAt(0) == '-') {

                    Integer result = server.removeWord(input.substring(1), username, currentSequence);
                    
                    if (result == 1) {
                        System.out.println("\nSuccessfully removed word '" + input.substring(1) + "' from the word repository.");
                    } else if (result == 0) {
                        System.out.println("\nFailed to remove word '" + input.substring(1) + "' from the word repository, it may not exist.");
                    } else if (result == -1) {
                        System.out.println("[Test] Server ignored duplicate guess. Trying again...");
                        continue;
                    }
                    
                } else if (input.charAt(0) == '?') {

                    if (server.checkWord(input.substring(1))){
                        System.out.println("\nWord '" + input.substring(1) + "' exists in the word repository.");
                    } else {
                        System.out.println("\nWord '" + input.substring(1) + "' does not exist in the word repository.");
                    }
                }

                System.out.println(Constants.WORD_REPO_MESSAGE);
                input = System.console().readLine();

                while (!input.equals("~") && (input.isEmpty() || !input.matches("^[+-?][a-zA-Z]*$"))) {
                    System.out.println("Invalid input."
                                    + "\n" + Constants.WORD_REPO_MESSAGE);
                    input = System.console().readLine();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Handles the client's disconnection. If the user was in an active game, it
     * calls playerQuit() to notify the server that the user has quit. Finally,
     * it calls unexportObject() to break the RMI connection.
     * 
     * @throws Exception if an error occurs while disconnecting
     */
    private void handleExit(){

        try {
            if(gameID != -1){
                server.playerQuit(gameID, username);
            }

            UnicastRemoteObject.unexportObject(this, true);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    }

    
}
