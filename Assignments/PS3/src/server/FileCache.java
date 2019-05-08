package server;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/*
 * File cache that maps file directory to file content.
 */
public class FileCache {

    private int remainingBytes;
    private Map<String, File> dir2File;

    public FileCache(int cacheSize) {
        this.remainingBytes = cacheSize;
        this.dir2File = new HashMap<>();
    }

    public boolean containsKey(String dir) {
        return dir2File.containsKey(dir);
    }

    public File get(String dir) {
        return dir2File.get(dir);
    }

    // Put to cache if there is enough space.
    public boolean put(String dir, File file) {
        int numOfBytes = (int) file.length();
        if (numOfBytes < remainingBytes) {
            remainingBytes -= numOfBytes;
            dir2File.put(dir, file); // might replace an existing entry
            return true;
        }
        return false;
    }

}
