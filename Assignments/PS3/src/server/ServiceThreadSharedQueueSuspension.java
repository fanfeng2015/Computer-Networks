package server;

import java.net.Socket;
import java.util.Queue;

/*
 * Service thread using shared queue suspension.
 */
public class ServiceThreadSharedQueueSuspension extends Thread {

    private Configuration config;
    private FileCache fileCache;
    private Queue<Socket> queue;

    public ServiceThreadSharedQueueSuspension(Configuration config, FileCache fileCache, Queue<Socket> queue) {
        this.config = config;
        this.fileCache = fileCache;
        this.queue = queue;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Socket connSocket;
                synchronized (queue) {
                    while (queue.isEmpty()) {
                        queue.wait();
                    }
                    connSocket = queue.remove();
                }
                if (connSocket != null) {
                    RequestHandler rh = new RequestHandler(config, connSocket, fileCache);
                    rh.processRequest();
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
