package me.anno.utils.files

import me.anno.io.files.FileReference
import me.anno.utils.InternalAPI

object OpenFileExternally {

    @InternalAPI
    var openInBrowserImpl: ((String) -> Boolean)? = null
    fun openInBrowser(link: String): Boolean {
        return openInBrowserImpl?.invoke(link) ?: false
    }

    @InternalAPI
    var openInStandardProgramImpl: ((FileReference) -> Unit)? = null
    fun openInStandardProgram(file: FileReference) {
        openInStandardProgramImpl?.invoke(file)
    }

    @InternalAPI
    var editInStandardProgramImpl: ((FileReference) -> Unit)? = null
    fun editInStandardProgram(file: FileReference) {
        editInStandardProgramImpl?.invoke(file)
    }

    @InternalAPI
    var openInExplorerImpl: ((FileReference) -> Unit)? = null
    fun openInExplorer(file: FileReference) {
        openInExplorerImpl?.invoke(file)
    }

    fun editInStandardProgram(files: List<FileReference>) {
        for (file in files) {
            editInStandardProgram(file)
        }
    }

    fun openInStandardProgram(files: List<FileReference>) {
        for (file in files) {
            openInStandardProgram(file)
        }
    }

    fun openInExplorer(files: List<FileReference>) {
        for (file in files) {
            openInExplorer(file)
        }
    }
}