package server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

/*
 * HTTP server using thread pool with shared queue suspension.
 */
public class HTTPServerSharedQueueSuspension {

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

        // thread pool and queue
        Thread[] threads = new Thread[config.getThreadPoolSize()];
        Queue<Socket> queue = new LinkedList<>();
        for (int i = 0; i < config.getThreadPoolSize(); i++) {
            threads[i] = new ServiceThreadSharedQueueSuspension(config, fileCache, queue);
            threads[i].start();
        }

        while (true) {
            Socket connSocket = listenSocket.accept();
            synchronized (queue) {
                queue.add(connSocket);
                queue.notifyAll(); //
            }
        }
    }

}
