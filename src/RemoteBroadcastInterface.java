import java.rmi.*;

public interface RemoteBroadcastInterface extends Remote{
    void receive(BroadcastHandler.Message message) throws RemoteException;
}
