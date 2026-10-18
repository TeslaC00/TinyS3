package dev.teslac00;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Storage {

    private static final String DIRECTORY = "storage";

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
