package client;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

/*
 * Request sender that sends requests to and receives response from server.
 */
public class RequestSender implements Runnable {

    private int time; // seconds
    private InetAddress address;
    private int port;
    private List<String> files;
    private String servername;
    private Statistics stats;

    public RequestSender(int time, InetAddress address, int port, List<String> files, String servername, Statistics stats) {
        this.time = time;
        this.address = address;
        this.port = port;
        this.files = files;
        this.servername = servername;
        this.stats = stats;
    }

    @Override
    public void run() {
        long cur = System.currentTimeMillis();
        long fileCount = 0, byteCount = 0, waitTime = 0;
        while (System.currentTimeMillis() < cur + 1000 * time) {
            for (String file : files) {
                try {
                    Socket connSocket = new Socket(address, port);

                    long start = System.currentTimeMillis(); // time sending the request
                    DataOutputStream outToServer = new DataOutputStream(connSocket.getOutputStream());
                    outToServer.write(generate(file, servername));

                    BufferedReader inFromServer = new BufferedReader(new InputStreamReader(connSocket.getInputStream()));
                    String line = inFromServer.readLine();
                    byteCount += line.length() + 2;
                    long end = System.currentTimeMillis(); // time receiving the response
                    if (!line.startsWith("HTTP/1.0")) {
                        System.out.println("Invalid response...");
                    }
                    while ((line = inFromServer.readLine()) != null) {
                        byteCount += line.length() + 2;
                    }
                    fileCount++;
                    waitTime += (end - start);

                    connSocket.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        synchronized (this.stats) {
            stats.fileCount += fileCount;
            stats.byteCount += byteCount;
            stats.waitTime += waitTime;
        }
    }

    // Generate byte array of the request.
    private byte[] generate(String file, String servername) {
        StringBuilder sb = new StringBuilder();
        sb.append("GET " + file + " HTTP/1.0" + "\r\n");
        sb.append("HOST: " + servername + "\r\n");
        sb.append("\r\n");
        return sb.toString().getBytes(StandardCharsets.US_ASCII);
    }

}

