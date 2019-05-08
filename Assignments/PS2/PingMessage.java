import java.lang.IllegalArgumentException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/* 
 * Containler class of the ping message transmitted between client and server.
 * Author: Fan Feng
 */
public class PingMessage {

	private static final String REQUEST = "PING";
	private static final String RESPONSE = "PINGECHO";

	public String signature;
	// 2 bytes, starts from 0 and increases by 1 for each successive ping message
	public short sequenceNumber;
	// 8 bytes, local timestamp
	public long clientSendTime;
	public String passwd;

	// Constructor
	public PingMessage() { 
		signature = REQUEST;
		sequenceNumber = 0;
		clientSendTime = System.currentTimeMillis();
		passwd = "";
	}

	// Parse the message data and set class attributes accordingly.
	public void parse(byte[] data) {
		ByteBuffer buffer = ByteBuffer.wrap(data);
    	buffer.order(ByteOrder.BIG_ENDIAN);
		// Parse signature
		byte[] array = new byte[8];
		buffer.get(array);
		signature = new String(array, StandardCharsets.US_ASCII);
		if (!signature.equals(RESPONSE)) {
			array = new byte[4];
			buffer.position(0);
			buffer.get(array);
			signature = new String(array, StandardCharsets.US_ASCII);
			if (!signature.equals(REQUEST)) {
				throw new IllegalArgumentException("Invalid signature...");
			}
		}
		// Parse sequence number
		buffer.get(); // ignore space
		sequenceNumber = buffer.getShort();
		// Parse client send time
		buffer.get(); // ignore space
		clientSendTime = buffer.getLong();
		// Parse password
		buffer.get(); // ignore space
		array = new byte[buffer.remaining()];
		buffer.get(array);
		String str = new String(array, StandardCharsets.US_ASCII);
		int end = str.indexOf(" \r\n");
		if (end != -1) {
			passwd = str.substring(0, end);
		} else {
			throw new IllegalArgumentException("Invalid message data: Not ending with CRLF...");
		}
	}

	// Generate message data as a byte array according to the class attributes.
	public byte[] generate() {
		ByteBuffer buffer = ByteBuffer.allocate(1024);
    	buffer.order(ByteOrder.BIG_ENDIAN);
		buffer.put(signature.getBytes(StandardCharsets.US_ASCII));
		buffer.put(" ".getBytes(StandardCharsets.US_ASCII)); // add sapce
		buffer.putShort(sequenceNumber);
		buffer.put(" ".getBytes(StandardCharsets.US_ASCII)); // add space
		buffer.putLong(clientSendTime);
		buffer.put(" ".getBytes(StandardCharsets.US_ASCII)); // add space
		buffer.put(passwd.getBytes(StandardCharsets.US_ASCII));
		buffer.put(" \r\n".getBytes(StandardCharsets.US_ASCII));
		return buffer.array();
	}

	@Override
	public String toString() {
		return signature + " " + sequenceNumber + " " + clientSendTime + " " + passwd;
	}

	// Convert the signature of ping message to PINGECHO.
	public void convertToResponse() {
		this.signature = RESPONSE;
	}

	public boolean isResponse() {
		return signature.equals(RESPONSE);
	}

	// Getters and setters.
	public String getSignature() {
		return signature;
	}

	public short getSequenceNumber() {
		return sequenceNumber;
	}

	public void setSequenceNumber(short sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	public long getClientSendTime() {
		return clientSendTime;
	}

	public String getPasswd() {
		return passwd;
	}

	public void setPasswd(String passwd) {
		this.passwd = passwd;
	}

}


