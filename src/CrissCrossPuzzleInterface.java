import java.rmi.*;

public interface CrissCrossPuzzleInterface extends Remote {

    public Integer startGame(String player, Integer numWords, Integer numberOfPlayers) throws RemoteException;
    public Boolean joinGame(Integer gameID, String player) throws RemoteException;
    public char[][] getInitialPuzzle(Integer gameID) throws RemoteException;



    public void playerQuit(Integer gameID, String username) throws RemoteException;



    public Integer addWord(String word, String username, Integer sequence) throws RemoteException;
    public Integer removeWord(String word, String username, Integer sequence) throws RemoteException;
    public Boolean checkWord(String word) throws RemoteException;
 


}
