package fergaral.datetophoto.utils

import java.io.File
import java.util.HashMap

/**
 * Created by fer on 7/06/16.
 */
object Folders {
    private val foldersMap = HashMap<String, File>()

    fun add(folderName: String, folderFile: File) {
        if (!foldersMap.containsKey(folderName))
            foldersMap.put(folderName, folderFile)
    }

    operator fun get(folderName: String): File? {
        return foldersMap[folderName]
    }
}
