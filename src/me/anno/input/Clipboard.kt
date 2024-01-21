package me.anno.input

import me.anno.io.files.FileReference

object Clipboard {

    var setClipboardContentImpl: ((String) -> Unit)? = null
    var getClipboardContentImpl: (() -> Any?)? = null
    var copyFilesImpl: ((List<FileReference>) -> Unit)? = null

    fun setClipboardContent(copied: String?) {
        copied ?: return
        setClipboardContentImpl?.invoke(copied)
    }

    /**
     * @return null, String or List<FileReference>
     * */
    fun getClipboardContent(): Any? {
        return getClipboardContentImpl?.invoke()
    }

    /**
     * is like calling "control-c" on those files
     * */
    fun copyFiles(files: List<FileReference>) {
        copyFilesImpl?.invoke(files)
    }
}