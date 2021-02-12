package me.anno.io.trash

import com.sun.jna.platform.FileUtils
import org.apache.logging.log4j.LogManager
import java.io.File
import java.io.IOException


object TrashManager {

    private val LOGGER = LogManager.getLogger(TrashManager::class)

    fun moveToTrash(files: Array<File>): Boolean {
        val fileUtils: FileUtils = FileUtils.getInstance()
        return if (fileUtils.hasTrash()) {
            try {
                fileUtils.moveToTrash(*files)
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

    fun moveToTrash(file: File): Boolean {
        val fileUtils: FileUtils = FileUtils.getInstance()
        return if (fileUtils.hasTrash()) {
            try {
                fileUtils.moveToTrash(file)
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