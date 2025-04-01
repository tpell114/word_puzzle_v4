import  java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;


public class BroadcastHandler extends UnicastRemoteObject implements RemoteBroadcastInterface {
    private int lamportClock = 0;
    private final PriorityQueue<Message> messageQueue = new PriorityQueue<>(); //keep messages ordered 
    private final String peerID;
    private final ConcurrentHashMap<String, RemoteBroadcastInterface> peers = new ConcurrentHashMap<>();

    public BroadcastHandler(String peerID) throws  RemoteException{
        super();
        this.peerID = peerID;
    }

    public synchronized void addPeer(String peerID, RemoteBroadcastInterface peer) {
        peers.put(peerID, peer);
    }

    public synchronized void removePeer(String peerID) {
        peers.remove(peerID);
    }



    public synchronized void broadcast(String type, Object contents) throws  RemoteException{
        lamportClock ++;
        Message msg = new Message(lamportClock, peerID, type, contents);

        messageQueue.add(msg);

        for(RemoteBroadcastInterface peer : peers.values()){
            peer.receive(msg);
        }
    
    
        }


        @Override
    public synchronized  void receive(Message message) throws RemoteException{
        lamportClock = Math.max(lamportClock, message.timeStamp) + 1;
        messageQueue.add(message);
        processQueue();
    }

    private void processQueue(){
        while(!messageQueue.isEmpty()){
            Message head = messageQueue.poll();
                deliver(head);
            }
            
        }

        public synchronized Message getNextMessage() {
            return messageQueue.poll();
        }
    


    public  void deliver(Message head){
        System.out.println("Delivering:" + head);
    }




    public static class Message implements  Comparable<Message>{
        public final int timeStamp;
        public final String senderID;
        public final String type; //indicates what command to do
        public final Object contents;

        public Message(int t, String s, String ty, Object c){
            timeStamp = t; 
            senderID = s; 
            type = ty; 
            contents = c;
        }

        @Override
        public int compareTo(Message otherMsg){
            int timeCompare = Integer.compare(this.timeStamp, otherMsg.timeStamp);
            return timeCompare != 0 ? timeCompare : this.senderID.compareTo(otherMsg.senderID); //use senderID as tie breaker


        }

    }
    
}
