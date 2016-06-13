package fergaral.datetophoto.utils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by fer on 7/06/16.
 */
public final class Folders {
    private static final Map<String, File> foldersMap = new HashMap<>();

    public static void add(String folderName, File folderFile) {
        if(!foldersMap.containsKey(folderName))
            foldersMap.put(folderName, folderFile);
    }

    public static File get(String folderName) {
        return foldersMap.get(folderName);
    }
}
