package me.anno.input

import me.anno.io.files.FileReference
import me.anno.utils.InternalAPI

/**
 * Represents copy-paste capabilities (Ctrl+C, Ctrl+V)
 * */
object Clipboard {

    @InternalAPI
    var setClipboardContentImpl: ((String) -> Unit)? = null
    fun setClipboardContent(copied: String?) {
        copied ?: return
        setClipboardContentImpl?.invoke(copied)
    }

    @InternalAPI
    var getClipboardContentImpl: (() -> Any?)? = null

    /**
     * @return null, String or List<FileReference>
     * */
    fun getClipboardContent(): Any? {
        return getClipboardContentImpl?.invoke()
    }

    @InternalAPI
    var copyFilesImpl: ((List<FileReference>) -> Unit)? = null

    /**
     * is like calling "control-c" on those files
     * */
    fun copyFiles(files: List<FileReference>) {
        copyFilesImpl?.invoke(files)
    }
}