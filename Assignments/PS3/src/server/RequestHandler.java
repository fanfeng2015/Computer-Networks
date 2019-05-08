package server;

import java.io.*;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

/*
 * Request handler that processes request and outputs response.
 */
public class RequestHandler implements Runnable {

    private Configuration config;
    private Socket connSocket;
    private BufferedReader inFromClient;
    private DataOutputStream outToClient;
    private FileCache fileCache;

    private String documentRoot;
    private String url;
    private String fileDir;
    private File fileContent;

    private Map<String, String> request = new HashMap<>(); // request body, e.g., { HOST: <ServerName>, ... }

    public RequestHandler(Configuration config, Socket connSocket, FileCache fileCache) throws Exception {
        this.config = config;
        this.connSocket = connSocket;
        this.inFromClient = new BufferedReader(new InputStreamReader(connSocket.getInputStream()));
        this.outToClient = new DataOutputStream(connSocket.getOutputStream());
        this.fileCache = fileCache;
    }

    public void processRequest() {
        try {
            mapURL2File();
            if (fileContent != null) { // file found
                // check If-Modified-Since header
                String sinceModified = request.get("If-Modified-Since");
                if (sinceModified != null && !isModifiedSince(fileContent, sinceModified)) {
                    outputError(400, "Not Modified Since" + sinceModified);
                }
                else if (fileContent.canExecute()) {
                    execute(); // response header and response body are returned inside execute()
                }
                else {
                    outputResponseHeader();
                    outputResponseBody();
                }
            }
            connSocket.close();
        }
        catch (Exception e) {
            outputError(400, "Server Error");
        }
    }

    private void mapURL2File() throws Exception {
        // read and parse request
        String line = inFromClient.readLine();
        String[] tokens = line.split("\\s");
        if (tokens.length < 2 || !tokens[0].equals("GET")) {
            outputError(500, "Bad Request");
            return;
        }
        while (!(line = inFromClient.readLine()).equals("")) { // (?) null check results in infinite loop...
            String[] temp = line.split(":");
            request.put(temp[0], temp[1].trim());
        }

        documentRoot = config.getDocumentRoot(request.get("HOST"));
        if (documentRoot.endsWith("/")) {
            documentRoot = documentRoot.substring(0, documentRoot.length() - 1);
        }

        // handle url
        url = tokens[1];
        if (url.contains("..")) { // security attack
            outputError(400, "Security Attack");
            return;
        }
        if (url.startsWith("/")) {
            url = url.substring(1);
            url = (url.isEmpty()) ? "/" : url;
        }
        if (url.endsWith("/")) { // file name is not specified
            String userAgent = request.get("User-Agent");
            if (userAgent != null && userAgent.contains("iPhone")) {
                fileDir = documentRoot + "/" + url + "index_m.html";
                findFile(fileDir);
                if (fileContent != null) { // index_m.html found
                    return;
                }
            }
            url += "index.html";
        }
        fileDir = documentRoot + "/" + url;
        if (fileDir.endsWith("load")) { // heartbeat monitoring
            if (config.getMonitorName().equals("HeartbeatMonitor") && HeartbeatMonitor.accept()) {
                outputResponseHeader();
                outputResponseBody("Heartbeat Monitor".getBytes());
            }
            else {
                outputError(503, "Server Overload");
            }
            return;
        }
        findFile(fileDir);
    }

    // Find file first in cache if exist, then in file system. Output error if not
    // found, otherwise insert into cache.
    private synchronized void findFile(String fileDir) {
        if (fileCache.containsKey(fileDir)) {
            fileContent = fileCache.get(fileDir);
            return;
        }
        fileContent = new File(fileDir);
        if (!fileContent.isFile()) {
            outputError(404,  "Not Found");
            fileContent = null;
            return;
        }
        fileCache.put(fileDir, fileContent);
    }

    // Execute the CGI file. Output response header and response body composed of environment variables.
    private void execute() throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(fileContent.getAbsolutePath());
        processBuilder.redirectErrorStream(true);
        Map<String, String> env = processBuilder.environment();
        env.put("QUERY_STRING", "...");
        env.put("REMOTE_ADDR", connSocket.getInetAddress().toString());
        env.put("REMOTE_HOST", connSocket.getInetAddress().getCanonicalHostName());
        env.put("REMOTE_IDENT", "...");
        env.put("REMOTE_USER", "...");
        env.put("REQUEST_METHOD", "GET");
        env.put("SERVER_NAME", request.get("HOST"));
        env.put("SERVER_PORT", connSocket.getLocalPort() + "");
        env.put("SERVER_PROTOCOL", "HTTP");
        env.put("SERVER_SOFTWARE", "...");

        Process process = processBuilder.start();
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        StringBuilder sb = new StringBuilder();
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        outputResponseHeader();
        outputResponseBody(sb.toString().getBytes());
        fileContent = null;
    }

    private void outputResponseHeader() throws Exception {
        outToClient.writeBytes("HTTP/1.0 200 OK \r\n");
        outToClient.writeBytes("Set-Cookie: CPSC433/533 ... ff242 \r\n");
        if (url.endsWith(".jpg")) {
            outToClient.writeBytes("Content-Type: image/jpeg \r\n");
        }
        else if (url.endsWith(".gif")) {
            outToClient.writeBytes("Content-Type: image/gif \r\n");
        }
        else if (url.endsWith(".html") || url.endsWith(".htm")) {
            outToClient.writeBytes("Content-Type: text/html \r\n");
        }
        else {
            outToClient.writeBytes("Content-Type: text/plain \r\n");
        }
        if (fileContent != null) { // output Last-Modified header
            String lastModified = convertToDate(fileContent.lastModified());
            outToClient.writeBytes("Last-Modified: " + lastModified + " \r\n");
        }
    }

    private void outputResponseBody() throws Exception {
        int numOfBytes = (int) fileContent.length();
        outToClient.writeBytes("Content-Length: " + numOfBytes + " \r\n");
        // send file content
        FileInputStream fileStream  = new FileInputStream(fileDir);
        byte[] fileInBytes = new byte[numOfBytes];
        fileStream.read(fileInBytes);
        outToClient.write(fileInBytes, 0, numOfBytes);
        outToClient.writeBytes("\r\n");
    }

    private void outputResponseBody(byte[] bytes) throws Exception {
        outToClient.writeBytes("Content-Length: " + bytes.length + " \r\n");
        outToClient.write(bytes, 0, bytes.length);
        outToClient.writeBytes("\r\n");
    }

    private void outputError(int errCode, String errMsg) {
        try {
            outToClient.writeBytes("HTTP/1.0 " + errCode + " " + errMsg + " \r\n");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Return whether the file has been modified since the date.
    private boolean isModifiedSince(File fileContent, String sinceModified) throws Exception {
        return fileContent.lastModified() > convertToMillis(sinceModified);
    }

    // Convert time in milliseconds to RFC 1123 date format.
    private String convertToDate(long milliseconds) {
        DateFormat converter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
        converter.setTimeZone(TimeZone.getTimeZone("GMT"));
        return converter.format(new Date(milliseconds));
    }

    // Convert string representation of RFC 1123 date format to milliseconds.
    private long convertToMillis(String str) throws Exception {
        DateFormat converter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = converter.parse(str);
        return date.getTime();
    }

    @Override
    public void run() {
        processRequest();
    }

}
