import java.rmi.*;

public interface CrissCrossPuzzleInterface extends Remote {

    public void startGame(String player, Integer numWords, Integer numberOfPlayers) throws RemoteException;
    public Boolean joinGame(Integer gameID, String player) throws RemoteException;
    public char[][] getInitialPuzzle(Integer gameID) throws RemoteException;
    Boolean isGameReady(Integer gameID) throws RemoteException;
    Integer getPlayerCount(Integer gameID) throws RemoteException;




    public void playerQuit(Integer gameID, String username) throws RemoteException;
  //  public Boolean checkWord(String word) throws RemoteException;
 


}
