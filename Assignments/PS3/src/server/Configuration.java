package server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/*
 * Server configuration.
 */
public class Configuration {

    private int port, cacheSize, threadPoolSize;
    private String monitorName; // optional heartbeat monitor
    private Map<String, String> map = new HashMap<>(); // { serverName: documentRoot }

    public Configuration(String configFile) throws IOException {
        parse(configFile);
    }

    // Returns true if the configuration is valid, false otherwise.
    public boolean isValid() {
        return port != 0 && cacheSize != 0 && threadPoolSize != 0 && !map.isEmpty();
    }

    // Parses the configuration file.
    private void parse(String configFile) throws IOException {
        File file = new File(configFile);
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line, documentRoot = null, serverName;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.contains("VirtualHost")) {
                continue;
            }
            String[] tokens = line.split("\\s+");
            String key = tokens[0], value = tokens[1];
            switch (key) {
                case "Listen":
                    port = Integer.valueOf(value);
                    break;
                case "CacheSize":
                    cacheSize = 1000 * Integer.valueOf(value); // config parameter is in KB
                    break;
                case "ThreadPoolSize":
                    threadPoolSize = Integer.valueOf(value);
                    break;
                case "Monitor":
                    monitorName = value;
                case "DocumentRoot":
                    documentRoot = value;
                    break;
                case "ServerName":
                    serverName = value;
                    map.put(serverName, documentRoot);
                    break;
                default:
                    System.out.println("Unknown configuration parameter...");
                    break;
            }
        }
    }

    public int getPort() {
        return port;
    }

    public int getCacheSize() {
        return cacheSize;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public String getMonitorName() {
        return monitorName;
    }

    public String getDocumentRoot(String serverName) {
        return map.get(serverName);
    }

}
