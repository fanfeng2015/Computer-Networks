package server;

import java.net.ServerSocket;

/*
 * HTTP server using thread pool with service threads competing on welcome socket.
 */
public class HTTPServerSharedWelcomeSocket {

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

        // thread pool
        Thread[] threads = new Thread[config.getThreadPoolSize()];
        for (int i = 0; i < config.getThreadPoolSize(); i++) {
            threads[i] = new ServiceThreadSharedWelcomeSocket(config, listenSocket, fileCache);
            threads[i].start();
        }
    }

}
