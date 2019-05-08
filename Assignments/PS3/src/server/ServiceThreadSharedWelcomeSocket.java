package server;

import java.net.ServerSocket;
import java.net.Socket;

/*
 * Service thread competing on welcome thread.
 */
public class ServiceThreadSharedWelcomeSocket extends Thread {

    private Configuration config;
    private ServerSocket listenSocket;
    private FileCache fileCache;

    public ServiceThreadSharedWelcomeSocket(Configuration config, ServerSocket listenSocket, FileCache fileCache) {
        this.config = config;
        this.listenSocket = listenSocket;
        this.fileCache = fileCache;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Socket connSocket;
                // take a ready connection from the accepted queue
                synchronized (listenSocket) {
                    connSocket = listenSocket.accept();
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
