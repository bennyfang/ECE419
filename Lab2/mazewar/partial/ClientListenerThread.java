import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientListenerThread implements Runnable {

    private MSocket mSocket  =  null;
    private Hashtable<String, Client> clientTable = null;
    private BlockingQueue eventQueue = null;
    private int clientSequenceNumber;

    public ClientListenerThread( MSocket mSocket,
                                Hashtable<String, Client> clientTable){
        this.mSocket = mSocket;
        this.clientTable = clientTable;
        this.eventQueue = new LinkedBlockingQueue<MPacket>();
        this.clientSequenceNumber = 0;
        if(Debug.debug) System.out.println("Instatiating ClientListenerThread");
    }

    public void run() {
        MPacket received = null;
        Client client = null;
        if(Debug.debug) System.out.println("Starting ClientListenerThread");
        while(true){
            try{
                received = (MPacket) mSocket.readObject();
                System.out.println("ClientListenerThread: Received " + received + "\n" +
     				   "clientSequenceNumber: " + clientSequenceNumber);
                if (received.sequenceNumber != clientSequenceNumber)
                {
                	try {
						eventQueue.put(received);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                }
                else 
                {
                	do {
	                    client = clientTable.get(received.name);
	                    if(received.event == MPacket.UP){
	                        client.forward();
	                    }else if(received.event == MPacket.DOWN){
	                        client.backup();
	                    }else if(received.event == MPacket.LEFT){
	                        client.turnLeft();
	                    }else if(received.event == MPacket.RIGHT){
	                        client.turnRight();
	                    }else if(received.event == MPacket.FIRE){
	                        client.fire();
	                    }else{
	                        throw new UnsupportedOperationException();
	                    }
	                    
	                    clientSequenceNumber++;
                	} while ( ( received = searchQueue()) != null );
                }
            }catch(IOException e){
                e.printStackTrace();
            }catch(ClassNotFoundException e){
                e.printStackTrace();
            }            
        }
    }
    
    private MPacket searchQueue() 
    {
    	if ( eventQueue.size()==0 ) return null;
    	
    	MPacket tempQueue[] = Arrays.copyOf(eventQueue.toArray(), eventQueue.toArray().length, MPacket[].class);
    	
    	for (int i=0 ; i < eventQueue.size() ; i++)
    	{
    		if (tempQueue[i].sequenceNumber == clientSequenceNumber)
    		{
    			eventQueue.remove(tempQueue[i]);
    			return tempQueue[i];
    		}
    	}
    	
    	return null;
    }
}
