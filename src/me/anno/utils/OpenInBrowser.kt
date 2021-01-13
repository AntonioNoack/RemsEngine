package me.anno.utils

import org.apache.logging.log4j.LogManager
import java.awt.Desktop
import java.net.URL

object OpenInBrowser {

    private fun URL.openInExplorer101(): Boolean {
        return if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(this.toURI())
            true
        } else false
    }

    fun URL.openInBrowser() {
        if(openInExplorer101()) return
        val url = toString()
        when {
            OS.isWindows -> {
                val rt = Runtime.getRuntime()
                rt.exec("rundll32 url.dll,FileProtocolHandler $url")
            }
            OS.isLinux -> {
                val rt = Runtime.getRuntime()
                val browsers = arrayOf(
                    "epiphany", "firefox", "mozilla", "konqueror",
                    "netscape", "opera", "links", "lynx"
                )
                val cmd = StringBuffer()
                for (i in browsers.indices) if (i == 0) cmd.append(
                    "${browsers[i]} \"$url\""
                ) else cmd.append(" || ${browsers[i]} \"$url\"")
                // If the first didn't work, try the next browser and so on
                rt.exec(arrayOf("sh", "-c", cmd.toString()))
            }
            OS.isMac -> {
                val rt = Runtime.getRuntime()
                rt.exec("open $url")
            }
            else -> LOGGER.warn("Opening a file in browser is not supported")
        }
    }

    private val LOGGER = LogManager.getLogger(OpenInBrowser::class)

}