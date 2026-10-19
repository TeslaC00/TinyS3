package dev.teslac00;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Storage {

    private static final String DIRECTORY = "storage";

    public static List<String> getKeys() {
        File rootFolder = new File(DIRECTORY);
        if (!rootFolder.exists()) {
            if (!rootFolder.mkdir()) {
                Logger.warn("Cannot create directory: ", DIRECTORY);
                return List.of();
            }
        }

        List<String> keys = new ArrayList<>();

        File[] files = rootFolder.listFiles();
        if (files == null || files.length == 0) return List.of();

        for (File file : files) {
            if (file.isFile()) {
                keys.add(file.getName());
            }
        }

        return keys;
    }

    public static File getFile(String fileName) {
        File rootFolder = new File(DIRECTORY);
        if (!rootFolder.exists()) {
            if (!rootFolder.mkdir()) {
                Logger.warn("Cannot create directory: ", DIRECTORY);
                return null;
            }
        }

        File[] files = rootFolder.listFiles();
        if (files == null) return null;

        for (File file : files) {
            if (fileName.equals(file.getName())) {
                return file;
            }
        }

        return null;
    }

    public static void store(String fileName, byte[] data) throws IOException {
        File rootFolder = new File(DIRECTORY);
        if (!rootFolder.exists()) {
            if (!rootFolder.mkdir()) {
                Logger.warn("Cannot create directory: ", DIRECTORY);
                throw new IOException("Cannot create directory: " + DIRECTORY);
            }
        }

        File saveFile = new File(rootFolder, fileName);
        try (FileOutputStream fos = new FileOutputStream(saveFile)) {
            fos.write(data);
            fos.flush();
        }
    }

}
