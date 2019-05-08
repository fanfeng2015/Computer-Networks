package server;

import java.net.ServerSocket;
import java.net.Socket;

/*
 * Sequential HTTP server.
 */
public class HTTPServerSequential {

    public static void main(String[] args) throws Exception { // assume that args follows the required format
        String configFile = args[1];
        Configuration config = new Configuration(configFile);
        if (!config.isValid()) {
            System.out.println("Invalid configuration...");
            return;
        }

        // create server socket
        ServerSocket listenSocket = new ServerSocket(config.getPort());
        FileCache fileCache = new FileCache(config.getCacheSize());
        while (true) {
            // take a ready connection from the accepted queue
            Socket connSocket = listenSocket.accept();
            RequestHandler rh = new RequestHandler(config, connSocket, fileCache);
            rh.processRequest();
        }
    }

}
