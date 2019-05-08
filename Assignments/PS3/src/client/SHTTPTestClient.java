package client;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/*
 * Main program of the test client.
 */
public class SHTTPTestClient {

    public static void main(String[] args) throws IOException { // assume that args follows the required format
        String server = args[1], servername = args[3];
        int port = Integer.parseInt(args[5]), parallel = Integer.parseInt(args[7]);
        String filename = args[9];
        int time = Integer.parseInt(args[11]);

        List<String> files = parse(filename);
        InetAddress address = InetAddress.getByName(server); // server IP address

        // create a list of threads
        Statistics stats = new Statistics();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < parallel; i++) {
            RequestSender rd = new RequestSender(time, address, port, files, servername, stats);
            threads.add(new Thread(rd));
        }

        // start all threads
        long start = System.currentTimeMillis();
        for (Thread thread : threads) {
            thread.start();
        }

        try { // sleep main thread to let all other threads finish
            Thread.sleep(1000 * time + 300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long end = System.currentTimeMillis();
        System.out.println("Transaction throughput (per second) = " + 1000 * stats.fileCount / (end - start - 300));
        System.out.println("Data rate throughput (per second) = " + 1000 * stats.byteCount / (end - start - 300));
        System.out.println("Average wait time (in milliseconds) = " + stats.waitTime / stats.fileCount);
    }

    // Parse the input file and return list of file names contained in the file.
    private static List<String> parse(String filename) throws FileNotFoundException {
        List<String> files = new ArrayList<>();
        Scanner scanner = new Scanner(new FileReader(filename));
        while (scanner.hasNext()) {
            files.add(scanner.next());
        }
        scanner.close();
        return files;
    }

}
