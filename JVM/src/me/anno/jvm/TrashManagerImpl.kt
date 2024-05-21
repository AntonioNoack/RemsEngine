package me.anno.jvm

import com.sun.jna.platform.FileUtils
import me.anno.io.files.FileFileRef
import me.anno.io.files.FileReference
import org.apache.logging.log4j.LogManager
import java.io.IOException

object TrashManagerImpl {
    private val LOGGER = LogManager.getLogger(TrashManagerImpl::class)
    fun moveToTrash(files: List<FileReference>): Boolean {
        val fileUtils: FileUtils = FileUtils.getInstance()
        return if (fileUtils.hasTrash()) {
            val fileArray = files
                .filterIsInstance<FileFileRef>()
                .map { it.file }.toTypedArray()
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