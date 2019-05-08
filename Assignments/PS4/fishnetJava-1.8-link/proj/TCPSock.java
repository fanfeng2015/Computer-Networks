import java.util.LinkedList;
import java.util.Queue;

/**
 * <p>Title: CPSC 433/533 Programming Assignment</p>
 *
 * <p>Description: Fishnet socket implementation</p>
 *
 * <p>Copyright: Copyright (c) 2006</p>
 *
 * <p>Company: Yale University</p>
 *
 * @author Hao Wang
 * @version 1.0
 */
public class TCPSock {

    enum State { // TCP socket states
        CLOSED,
        INITIALIZED,
        BIND,
        LISTEN,
        SYN_SENT,
        ESTABLISHED,
        SHUTDOWN // close requested, FIN not sent (due to unsent data in queue)
    }

    private State state = State.CLOSED; // default socket state

    private TCPManager tcpMan;

    private int localAddr = -1;
    private int localPort = -1;
    private int remoteAddr = -1;
    private int remotePort = -1;

    private int backlog = 0; // remaining space of the backlog queue
    private Queue<TCPSock> backlogQueue = null; // hold pending connections

    private RingBuffer sendBuffer, recvBuffer; // send / receive buffer

    // sliding window
    private int sendBase;
    private int recvBase;

    // reliable data transfer

    // flow control and congestion control

    public TCPSock(TCPManager tcpMan) {
        this.tcpMan = tcpMan;
    }

    public void initialize() {
        this.state = State.INITIALIZED;
        this.localPort = this.remoteAddr = this.remotePort = -1;
        this.localAddr = tcpMan.getAddr();
        this.sendBuffer = new RingBuffer(4096); // TODO
        this.secvBuffer = new RingBuffer(4096);
    }

    public void cleanup() {
        this.state = State.CLOSED;
        this.localAddr = this.localPort = this.remoteAddr = this.remotePort = -1;
        if (backlogQueue != null) {
            for (TCPSock sock : backlogQueue) {
                sock.cleanup();
            }
        }
        backlogQueue = null;
        sendBuffer = recvBuffer = null;
    }

    /*
     * The following are the socket APIs of TCP transport service.
     * All APIs are NON-BLOCKING.
     */

    /**
     * Bind a socket to a local port
     *
     * @param localPort int local port number to bind the socket to
     * @return int 0 on success, -1 otherwise
     */
    public int bind(int localPort) { // INITIALIZED -> BIND
        // TODO: Check port range?
        if (this.state == State.INITIALIZED && this.tcpMan.findSocket(this.localAddr, this.localPort) == null) {
            this.state = State.BIND;
            this.localPort = localPort;
            return 0;
        }
        return -1;
    }

    /**
     * Listen for connections on a socket
     * @param backlog int Maximum number of pending connections
     * @return int 0 on success, -1 otherwise
     */
    public int listen(int backlog) { // BIND -> LISTEN
        if (this.state == State.BIND) {
            this.state = State.LISTEN;
            this.backlog = backlog;
            this.backlogQueue = new LinkedList<>();
            return 0;
        }
        return -1;
    }

    /**
     * Initiate connection to a remote socket
     *
     * @param destAddr int Destination node address
     * @param destPort int Destination port
     * @return int 0 on success, -1 otherwise
     */
    public int connect(int destAddr, int destPort) { // BIND -> SYN_SENT
        if (this.state == State.BIND) {
            this.state = State.SYN_SENT;
            this.remoteAddr = destAddr;
            this.remotePort = destPort;
            this.sendBase = rand.nextInt(1024);
            sendSYN();
            return 0;
        }
        return -1;
    }

    /**
     * Accept a connection on a socket
     *
     * @return TCPSock The first established connection on the request queue
     */
    public TCPSock accept() {
        if (this.state == State.LISTEN && !this.backlogQueue.isEmpty()) {
            TCPSock sock = this.backlogQueue.poll();
            return sock;
        }
        return null;
    }

    /**
     * Initiate closure of a connection (graceful shutdown)
     */
    public void close() {
        if (this.state == State.LISTEN ||
            this.state == State.INITIALIZED ||
            this.state == State.BIND) {
            this.state = State.CLOSED;
            this.cleanup();
        } else if (this.state == State.SYN_SENT) {
            this.state = State.CLOSED;
            this.sendFIN(this.sendBase);
            this.cleanup();
        }
        else if (this.state == State.ESTABLISHED) {
            this.state = State.SHUTDOWN;
            sendData();
        }
    }

    /**
     * Release a connection immediately (abortive shutdown)
     */
    public void release() {
        if (this.state == State.ESTABLISHED || this.state == State.SHUTDOWN) {
            this.sendFIN(this.sendBase);
        }
        this.state = State.CLOSED;
        this.cleanup();
    }

    /**
     * Write to the socket up to len bytes from the buffer buf starting at
     * position pos.
     *
     * @param buf byte[] the buffer to write from
     * @param pos int starting position in buffer
     * @param len int number of bytes to write
     * @return int on success, the number of bytes written, which may be smaller
     *             than len; on failure, -1
     */
    public int write(byte[] buf, int pos, int len) { // write to the send buffer
        if (this.state == State.ESTABLISHED) {
            len = Math.min(len, this.sendBuffer.remaining());
            byte[] temp = new byte[len];
            System.arraycopy(buf, pos, temp, 0, len);
            int count = this.sendBuffer.put(temp); // bytes written to send buffer
            this.sendData();
            return count;
        }
        return -1;
    }

    /**
     * Read from the socket up to len bytes into the buffer buf starting at
     * position pos.
     *
     * @param buf byte[] the buffer
     * @param pos int starting position in buffer
     * @param len int number of bytes to read
     * @return int on success, the number of bytes read, which may be smaller
     *             than len; on failure, -1
     */
    public int read(byte[] buf, int pos, int len) {
        if (this.state == State.ESTABLISHED || this.state == State.SHUTDOWN) {
            len = Math.min(len, this.recvBuffer.size());
            byte[] temp = this.recvBuffer.get(0, len);
            len = temp.length;
            System.arraycopy(temp, 0, buf, pos, len);
            this.recvBuffer.move(len);
            // If SHUTDOWN and no data in send buffer and recv buffer, release the socket.
            if (this.state == State.SHUTDOWN && this.recvBuffer.size() == 0 && this.sendBuffer.size() == 0) {
                this.release();
            }
            return len;
        }
        return -1;
    }

    /**
     * Called by TCPmanager if a packet arrives at node and match this sock
     **/
    public void onReceive(int srcAddr, int srcPort, Transport segment) {
        switch (segment.getType()){
            case Transport.SYN:
                receiveSYN(srcAddr, srcPort, segment);
                break;
            case Transport.ACK:
                receiveACK(srcAddr, srcPort, segment);
                break;
            case Transport.DATA:
                receiveDATA(srcAddr, srcPort, segment);
                break;
            case Transport.FIN:
                receiveFIN(srcAddr, srcPort, segment);
                break;
            default:
                System.out.println("Unknow TCP package type" + segment.getType());
                break;
        }
    }

    public boolean isConnectionPending() {
        return (this.state == State.SYN_SENT);
    }

    public boolean isClosed() {
        return (this.state == State.CLOSED);
    }

    public boolean isConnected() {
        return (this.state == State.ESTABLISHED);
    }

    public boolean isClosurePending() {
        return (this.state == State.SHUTDOWN);
    }

    /*
     * End of socket API
     */

    private void sendSYN() { // sender -> receiver
        if (this.state == State.SYN_SENT) {
            System.out.print("S");
            Transport segment = new Transport(this.localPort, this.remotePort, Transport.SYN, this.sendBase, this.sendBase, new byte[0]);
            this.tcpMan.send(this, segment);
            try { // handle loss of SYN
                Method method = Callback.getMethod("sendSYN", this, null);
                Callback cb = new Callback(method, (Object) this, null);
                this.tcpMan.addTimer(1000, cb);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void recvSYN(int srcAddr, int srcPort, Transport segment) { // at receiver
        System.out.print("S");
        int seqNum = segment.getSeqNum();
        int ackNum = seqNum + 1;
        if (this.state == State.LISTEN) { // welcome socket, create a connection socket
            if (this.backlogQueue.size() < this.backlog) { // room in the backlog queue
                TCPSock sock = this.tcpMan.socket();
                if (sock == null) {
                    this.sendFIN(ackNum);
                }
                sock.initialize();
                sock.setState(State.ESTABLISHED);
                sock.setLocalAddr(this.localAddr);
                sock.setLocalPort(this.localPort);
                sock.setRemoteAddr(srcAddr);
                sock.setRemotePort(srcPort);
                sock.setRecvBase(ackNum); // connection established
                this.backlogQueue.add(sock);
                sock.sendACK();
            } else { // no enough room in the backlog queue (too many pending connections)
                this.sendFIN(srcAddr, srcPort, ackNum);
            }
        } else if (this.state == State.ESTABLISHED && seqNum == this.recvBase - 1) { // connection socket, previously lost SYN
            this.sendACK();
        }
    }

    private void sendACK() { // receiver -> sender
        Transport segment = new Transport(this.localPort, this.remotePort, Transport.ACK, this.recvBuffer.remaining(), this.recvBase, new bytes[0]);
        this.tcpMan.send(this, segment);
    }





    // TODO: recvACK(...) -> sendData(...)
    private void recvACK(int srcAddr, int srcPort, Transport segment) { // at sender
        if (this.state == State.SYN_SENT) {

        }
        else if (this.state == State.ESTABLISHED || this.state == State.SHUTDOWN) {

        }
    }

    // TODO
    private void sendData() {

    }

    // TODO:
    private void recvDATA(int srcAddr, int srcPort, Transport segment) {

    }





    private void sendFIN(int ackNum) {
        this.sendFIN(this.remoteAddr, this.remotePort, ackNum);
    }

    private void sendFIN(int remoteAddr, int remotePort, int ackNum) {
        System.out.println("F");
        Transport segment = new Transport(this.localPort, this.remotePort, Transport.FIN, this.recvBuffer.remaining(), ackNum, new byte[0]);
        this.tcpMan.send(this.localAddr, remoteAddr, segment);
    }

    private void recvFIN(int srcAddr, int srcPort, Transport segment) {
        System.out.print("F");
        if (this.state == State.ESTABLISHED) {
            this.close();
        }
    }

    // ------------------------------------------------------------------------
    // Getters and setters
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public int getLocalAddr() {
        return localAddr;
    }

    public void setLocalAddr(int localAddr) {
        this.localAddr = localAddr;
    }

    public int getLocalPort() {
        return localPort;
    }

    public void setLocalPort(int localPort) {
        this.localPort = localPort;
    }

    public int getRemoteAddr() {
        return remoteAddr;
    }

    public void setRemoteAddr(int remoteAddr) {
        this.remoteAddr = remoteAddr;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public void setRemotePort(int remotePort) {
        this.remotePort = remotePort;
    }

}
