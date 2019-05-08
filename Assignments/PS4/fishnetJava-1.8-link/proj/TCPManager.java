import java.util.ArrayList;
import java.util.List;

/**
 * <p>Title: CPSC 433/533 Programming Assignment</p>
 *
 * <p>Description: Fishnet TCP manager</p>
 *
 * <p>Copyright: Copyright (c) 2006</p>
 *
 * <p>Company: Yale University</p>
 *
 * @author Hao Wang
 * @version 1.0
 */
public class TCPManager {

    private static final byte dummy[] = new byte[0];

    private Node node;
    private int addr;
    private Manager manager;
    private TCPSock[] socks = new TCPSock[]; // TODO

    public TCPManager(Node node, int addr, Manager manager) {
        this.node = node;
        this.addr = addr;
        this.manager = manager;
        for (int i = 0; i < socks.length; i++){
            socks[i] = new TCPSock(this);
        }
    }

    /**
     * Start this TCP manager
     */
    public void start() {

    }

    /*
     * Begin socket API
     */

    /**
     * Create a socket
     * @return TCPSock the newly created socket, which is not yet bound to a local port.
     */
    public TCPSock socket() {
        for (TCPSock sock : socks){
            if (sock.getState() == State.CLOSED) {
                sock.setState(State.INIT);
                sock.initialize();
                return sock;
            }
        }
        return null;
    }

    /*
     * End Socket API
     */

    public void addTimer(int deltaT, Callback cb) {
        this.manager.addTimer(this.addr, deltaT, cb);
    }

    // Find best matched socket and delegate the socket to handle segment.
    public void onReceive(int srcAddr, int destAddr, Transport segment) {
        int destPort = segment.getDestPort();
        int srcPort = segment.getSrcPort();
        // find best matched socket
        TCPSock sock = findBestMatchSock(srcAddr, srcPort, destAddr, destPort);
        if (sock != null) {
            sock.onReceive(srcAddr, srcPort, segment);
        }
        else {
            System.out.println("No socket matched with " +
                                "(" + srcAddr + ", " + srcPort + ", " + destAddr + ", " + destPort + ")");
        }
    }

    private TCPSock findBestMatchSock(int srcAddr, int srcPort, int destAddr, int destPort) {
        TCPSock sock = null;
        for (TCPSock candidate : socks) { // match a connection socket
            if (candidate.getLocalAddr() == destAddr &&
                candidate.getLocalPort() == destPort &&
                candidate.getRemoteAddr() == srcAddr &&
                candidate.getRemotePort() == srcPort)
            {
                sock = candidate;
                break;
            }
        }
        if (sock == null || sock.isClosed()) {
            for (TCPSock candidate : socks) { // match a welcome socket
                if (candidate.getLocalAddr() == destAddr &&
                    candidate.getLocalPort() == destPort &&
                    candidate.getState() == TCPSock.State.LISTEN)
                {
                    sock = candidate;
                    break;
                }
            }
        }
        return sock;
    }

    // Find the socket with local address and port being the input address and port.
    public TCPSock findSocket(int destAddr, int destPort) {
        for (TCPSock candidate : socks) {
            if (candidate.getLocalAddr() == destAddr && candidate.getLocalPort() == destPort) {
                return candidate;
            }
        }
        return null;
    }


    public void send(TCPSock sock, Transport segment){
        this.send(sock.getLocalAddr(), sock.getRemoteAddr(), segment);
    }

    // Send segment
    private void send(int localAddr, int remoteAddr, Transport segment){
        this.node.sendSegment(localAddr, remoteAddr, Protocol.TRANSPORT_PKT, segment.pack());
    }

    // ======================================================================================
    // Getters and setters
    public int getAddr() {
        return this.addr;
    }

}


