import java.net.*;
import java.util.*;

/*
 * Ping client.
 * Author: Fan Feng
 */
public class PingClient {

	private static int count = 10; // number of ping requests
	private static int timeout = 1000;

	public static void main(String[] args) throws Exception {
	    // Get command line argument.
	    if (args.length < 3) {
			System.out.println("Usage: PingClient <host> <port> <passwd>");
	    	return ;
	    }
	    String host = args[0];
	    int port = Integer.parseInt(args[1]);
	    String passwd = args[2];

	    // Output metrics
	    int lossCount = 0;
	    long minDelay = Long.MAX_VALUE, maxDelay = Long.MIN_VALUE, totalDelay = 0L;

	    DatagramSocket socket = new DatagramSocket();
	    InetAddress address = InetAddress.getByName(host); // IP address
	    socket.setSoTimeout(timeout);
	    for (int i = 0; i < count; i++) {
	    	PingMessage requestMessage = new PingMessage();
	    	requestMessage.setSequenceNumber((short) i);
	    	requestMessage.setPasswd(passwd);
	    	byte[] data = requestMessage.generate();
        	System.out.println(requestMessage);
	    	// Send packet
	    	DatagramPacket requestPacket = new DatagramPacket(data, data.length, address, port);	    	
	    	socket.send(requestPacket);
	    	// Receive packet
	    	DatagramPacket responsePacket = new DatagramPacket(new byte[1024], 1024);
	    	try {
	    		socket.receive(responsePacket);
	    	}
	    	catch (SocketTimeoutException e) { // packet loss
	    		lossCount++;
	    		continue;
	    	}
	    	// Packet received
	    	PingMessage responseMessage = new PingMessage();
	    	try {
	    		responseMessage.parse(responsePacket.getData());
        		System.out.println(responseMessage);
	    		if (!responseMessage.isResponse()) {
	    			throw new IllegalArgumentException("Invalid message: Not a response...");
	    		}
	    	}
	    	catch (Exception e) {
	    		e.printStackTrace();
	    		continue;
	    	}
	    	// Update output metrics
	    	long curDelay = System.currentTimeMillis() - responseMessage.getClientSendTime(); // PingServer sleeps for 2 * AVERAGE_DELAY
	    	minDelay = Math.min(minDelay, curDelay);
	    	maxDelay = Math.max(maxDelay, curDelay);
	    	totalDelay += curDelay;
	    }
	    socket.close();
	    double lossRate = (double) lossCount / count;
	    double avgDelay = (double) totalDelay / (count - lossCount);
	    System.out.println(
	    	"MIN RTT = " + minDelay + ", MAX RTT = " + maxDelay + ", AVG RTT = " + avgDelay);
	    System.out.println("LOSS RATE = " + lossRate);
	}

}


