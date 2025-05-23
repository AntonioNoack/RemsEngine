package me.anno.jvm

import me.anno.io.files.FileReference
import me.anno.io.files.inner.InnerFile
import me.anno.jvm.utils.BetterProcessBuilder
import me.anno.utils.OS
import me.anno.utils.files.OpenFileExternally
import org.apache.logging.log4j.LogManager
import java.awt.Desktop
import java.io.File
import java.net.URL

object OpenFileExternallyImpl {

    private val LOGGER = LogManager.getLogger(OpenFileExternallyImpl::class)
    private fun FileReference.toFile() = File(absolutePath.replace("!!", "/"))

    fun register() {
        OpenFileExternally.openInBrowserImpl = OpenFileExternallyImpl::openInBrowser
        OpenFileExternally.openInStandardProgramImpl = OpenFileExternallyImpl::openInStandardProgram
        OpenFileExternally.editInStandardProgramImpl = OpenFileExternallyImpl::editInStandardProgram
        OpenFileExternally.openInExplorerImpl = OpenFileExternallyImpl::openInExplorer
    }

    private fun openInExplorer101(link: String): Boolean {
        return if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URL(link).toURI())
            true
        } else false
    }

    fun openInBrowser(link: String): Boolean {
        if (openInExplorer101(link)) return true
        when {
            OS.isWindows -> {
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler $link")
                return true
            }
            OS.isLinux -> {
                val browsers = listOf(
                    "epiphany", "firefox", "mozilla", "konqueror",
                    "netscape", "opera", "links", "lynx"
                )
                val cmd = StringBuilder()
                for (i in browsers.indices) if (i == 0) cmd.append(
                    "${browsers[i]} \"$link\""
                ) else cmd.append(" || ${browsers[i]} \"$link\"")
                // If the first didn't work, try the next browser and so on
                Runtime.getRuntime()
                    .exec(arrayOf("sh", "-c", cmd.toString()))
                return true
            }
            OS.isMacOS -> {
                Runtime.getRuntime().exec("open $link")
                return true
            }
            else -> {
                // todo implement this for Web: this is just a link with target=new
                LOGGER.warn("Opening a file in browser is not supported")
                return false
            }
        }
    }

    fun openInStandardProgram(file: FileReference) {
        val parent = file.getParent()
        if (parent is InnerFile) {
            return openInStandardProgram(parent)
        }
        try {
            Desktop.getDesktop().open(file.toFile())
        } catch (e: Exception) {
            LOGGER.warn(e)
        }
    }

    fun editInStandardProgram(file: FileReference) {
        val parent = file.getParent()
        if (parent is InnerFile) {
            return editInStandardProgram(parent)
        }
        try {
            Desktop.getDesktop().edit(file.toFile())
        } catch (e: Exception) {
            LOGGER.warn(e.message)
            OpenFileExternally.openInStandardProgram(file)
        }
    }

    fun openInExplorer(self: FileReference) {
        openInExplorer(self.toFile())
    }

    private fun openInExplorer(self: File) {
        if (!self.exists()) {
            val parent = self.parentFile
            if (parent != null) openInExplorer(parent)
            else LOGGER.warn("Cannot open file $this, as it does not exist!")
        } else {
            when {
                OS.isWindows -> {// https://stackoverflow.com/questions/2829501/implement-open-containing-folder-and-highlight-file
                    val builder = BetterProcessBuilder(null, 3, true)
                    builder.addAll(listOf("explorer.exe", "/select,", self.absolutePath))
                    builder.start()
                }
                Desktop.isDesktopSupported() -> {
                    val desktop = Desktop.getDesktop()
                    desktop.open(if (self.isDirectory) self else self.parentFile ?: self)
                }
                OS.isLinux -> {// https://askubuntu.com/questions/31069/how-to-open-a-file-manager-of-the-current-directory-in-the-terminal
                    val builder = BetterProcessBuilder(null, 2, true)
                    builder.addAll(listOf("xdg-open", self.absolutePath))
                    builder.start()
                }
                else -> LOGGER.warn("File.openInExplorer() is not implemented on that platform")
            }
        }
    }
}