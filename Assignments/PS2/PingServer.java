import java.io.*;
import java.net.*;
import java.util.*;

/* 
 * Ping server.
 * Author: Fan Feng
 */        
public class PingServer {

  private static double LOSS_RATE = 0.3;
  private static int AVERAGE_DELAY = 100; // milliseconds

  public static void main(String[] args) throws Exception {
    // Get command line arguments.
    if (args.length < 2) {
      System.out.println("Usage: PingServer <port> <passwd> [-delay delay] [-loss loss]");
      return ;
    }

    int port = Integer.parseInt(args[0]);
    String passwd = args[1];
    // Get optional parameters -delay and -loss. Ignore command line arguments afterwards.
    int index = 2;
    if (index + 1 < args.length && args[index].equals("-delay")) {
      AVERAGE_DELAY = Integer.parseInt(args[index + 1]);
    }
    index += 2;
    if (index + 1 < args.length && args[index].equals("-loss")) {
      LOSS_RATE = Double.parseDouble(args[index + 1]);
    }

    // Create random number generator for use in simulating packet
    // loss and network delay.
    Random random = new Random();

    // Create a datagram socket for receiving and sending UDP packets
    // through the port specified on the command line.
    DatagramSocket socket = new DatagramSocket(port);

    // Processing loop.
    while (true) {
      // Create a datagram packet to hold incomming UDP packet.
      DatagramPacket request = new DatagramPacket(new byte[1024], 1024);
      // Block until receiving a UDP packet.
      socket.receive(request);
      // Print the received data, for debugging.
      // printData(request);

      // Decide whether to reply, or simulate packet loss.
      if (random.nextDouble() < LOSS_RATE) {
        System.out.println("  Reply not sent.");
        continue;
      }

      // Simulate prorogation delay.
      Thread.sleep((int) (random.nextDouble() * 2 * AVERAGE_DELAY));

      // Send reply.
      InetAddress clientHost = request.getAddress();
      int clientPort = request.getPort();
      byte[] data = request.getData(); // message data of the request

      PingMessage message = new PingMessage(); // default signature is PING
      try {
        // Parse message data and set class attributes.
        message.parse(data);
        if (!passwd.equals(message.getPasswd())) {
          throw new IllegalArgumentException("Invalid password...");
        }
        message.convertToResponse(); // convert PING to PINGECHO
        data = message.generate();
        System.out.println(message);
      }
      catch (Exception e) {
        e.printStackTrace();
        continue;
      }

      DatagramPacket response = new DatagramPacket(data, data.length, clientHost, clientPort);
      socket.send(response);
      System.out.println("  Reply sent.");
    }
  }

  /* 
   * Print ping data to the standard output stream.
   */
  private static void printData(DatagramPacket packet) throws Exception {
    // Obtain references to the packet's array of bytes.
    byte[] data = packet.getData();
    // Wrap the bytes in a byte array input stream so that you can read the
    // data as a stream of bytes.
    ByteArrayInputStream bais = new ByteArrayInputStream(data);
    // Wrap the byte array output stream in an input stream reader, so you can
    // read the data as a stream of **characters**: reader/writer handles characters.
    InputStreamReader isr = new InputStreamReader(bais);
    // Wrap the input stream reader in a bufferred reader, so you can read the
    // character data a line at a time. (A line is a sequence of chars terminated
    // by any combination of \r and \n.)
    BufferedReader br = new BufferedReader(isr);
    // The message data is contained in a single line, so read this line.
    String line = br.readLine();
    // Print host address and data received from it.
    System.out.println(
      "Received from " + packet.getAddress().getHostAddress() + ": " + line);
  }

}


