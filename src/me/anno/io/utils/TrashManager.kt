package me.anno.io.utils

import me.anno.io.files.FileReference
import org.apache.logging.log4j.LogManager

/**
 * provides functionality to move files to the trash (revertible file deletion)
 * */
@Suppress("unused")
object TrashManager {

    private val LOGGER = LogManager.getLogger(TrashManager::class)

    // returns true on success
    var moveToTrashImpl: ((List<FileReference>) -> Boolean)? = null
    fun moveToTrash(files: List<FileReference>): Boolean {
        return moveToTrashImpl?.invoke(files) ?: false
    }
}