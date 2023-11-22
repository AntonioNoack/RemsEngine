package me.anno.io.utils

import com.sun.jna.platform.FileUtils
import me.anno.io.files.FileReference
import org.apache.logging.log4j.LogManager
import java.io.IOException

/**
 * provides functionality to move files to the trash (revertible file deletion)
 * */
@Suppress("unused")
object TrashManager {

    private val LOGGER = LogManager.getLogger(TrashManager::class)

    fun moveToTrash(files: List<FileReference>): Boolean {
        val fileUtils: FileUtils = FileUtils.getInstance()
        return if (fileUtils.hasTrash()) {
            val fileArray = files.map { it.toFile() }.toTypedArray()
            try {
                fileUtils.moveToTrash(*fileArray)
                true
            } catch (e: IOException) {
                e.printStackTrace()
                false
            }
        } else {
            LOGGER.warn("Trash is not available")
            false
        }
    }
}