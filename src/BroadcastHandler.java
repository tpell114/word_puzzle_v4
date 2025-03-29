import  java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;


public class BroadcastHandler extends UnicastRemoteObject {
    private int lamportClock = 0;
    private final PriorityQueue<Message> messageQueue = new PriorityQueue<>(); //keep messages ordered 
    private final Map<String, Integer> lastSequence = new ConcurrentHashMap<>();
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
        int sequence = lastSequence.getOrDefault(peerID, 0) + 1;
        Message msg = new Message(lamportClock, peerID, sequence, type, contents);

        messageQueue.add(msg);
        lastSequence.put(peerID, sequence);

        for(Map.Entry<String, RemoteBroadcastInterface> entry : peers.entrySet()){
                entry.getValue().receive(msg);
            }
    
    
        }

    public synchronized  void receive(Message message) throws RemoteException{
        lamportClock = Math.max(lamportClock, message.timeStamp) + 1;
        messageQueue.add(message);
        processQueue();
    }

    private void processQueue(){
        while(!messageQueue.isEmpty()){
            Message head = messageQueue.peek();
            if(canDeliver(head)){
                deliver(head);
                messageQueue.remove();
            }
            else{
                break;
            }
        }
    }

    private  boolean canDeliver(Message message){
        return lastSequence.getOrDefault(message.senderID, 0) + 1 == message.sequence;
    }

    private void deliver(Message message){
        switch(message.type){
            case "GUESS":
            handleGuess(message);
            break;
            case "STATE_UPDATE":
            updateState(message);
            break;
            case "JOIN":
            handleJoin(message);
            break;
        }
        lastSequence.put(message.senderID, message.sequence);
    }
    

    public static class Message implements  Comparable<Message>{
        public final int timeStamp;
        public final String senderID;
        public final int sequence;
        public final String type; //indicates what command to do
        public final Object contents;

        public Message(int t, String s, int seq, String ty, Object c){
            timeStamp = t; 
            senderID = s; 
            sequence = seq; 
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
