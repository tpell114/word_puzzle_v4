import java.rmi.*;

public interface RemoteBroadcastInterface extends Remote{
    void recieve(BroadcastHandler.Message message) throws RemoteException;
}
