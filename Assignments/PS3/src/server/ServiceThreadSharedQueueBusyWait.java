package server;

import java.net.Socket;
import java.util.Queue;

/*
 * Service thread using shared queue busy wait.
 */
public class ServiceThreadSharedQueueBusyWait extends Thread {

    private Configuration config;
    private FileCache fileCache;
    private Queue<Socket> queue;

    public ServiceThreadSharedQueueBusyWait(Configuration config, FileCache fileCache, Queue<Socket> queue) {
        this.config = config;
        this.fileCache = fileCache;
        this.queue = queue;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Socket connSocket = null;
                while (connSocket == null) {
                    synchronized (queue) {
                        if (!queue.isEmpty()) {
                            connSocket = queue.remove();
                        }
                    }
                }
                RequestHandler rh = new RequestHandler(config, connSocket, fileCache);
                rh.processRequest();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
