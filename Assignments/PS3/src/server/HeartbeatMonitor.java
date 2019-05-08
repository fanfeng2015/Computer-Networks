package server;

import java.util.Random;

/*
 * Heartbeat monitor util class that randomly accepts/rejects a request
 * with an acceptance rate of THRESHOLD.
 */
public final class HeartbeatMonitor {

    private static final int THRESHOLD = 50;

    public static boolean accept() {
        Random rand = new Random();
        return rand.nextInt(100) < THRESHOLD;
    }

}
