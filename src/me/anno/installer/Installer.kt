package me.anno.installer

import me.anno.Time
import me.anno.gpu.GFX
import me.anno.io.BufferedIO.useBuffered
import me.anno.io.files.FileReference
import me.anno.io.files.Reference.getReference
import me.anno.io.yaml.generic.SimpleYAMLReader
import me.anno.maths.Maths.SECONDS_TO_NANOS
import me.anno.ui.base.progress.ProgressBar
import me.anno.utils.types.Strings.formatDownload
import me.anno.utils.types.Strings.formatDownloadEnd
import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.SSLHandshakeException
import kotlin.concurrent.thread

object Installer {

    @JvmStatic
    private val LOGGER = LogManager.getLogger(Installer::class)

    private val mirrors = SimpleYAMLReader.read(
        getReference("res://mirrors.yaml")
            .readLinesSync(1024),
        false
    )

    @JvmStatic
    fun checkFFMPEGInstall() {
        try {
            val clazz = Installer.javaClass.classLoader.loadClass("me.anno.video.VideoPlugin")
            val method = clazz.getMethod("checkFFMPEGInstall")
            method.invoke(null)
        } catch (e: Exception) {
            LOGGER.warn("VideoPlugin unavailable")
        }
    }

    @JvmStatic
    fun downloadMaybe(src: String, dst: FileReference) {
        if (!dst.exists) download(src, dst) {}
        else LOGGER.info("$src already is downloaded :)")
    }

    private fun generateURL(fileName: String, withHttps: Boolean): String {
        val protocol = if (withHttps) "https" else "http"
        val name = fileName.replace(" ", "%20")
        // create subdomain for downloads?
        return "${protocol}://remsstudio.phychi.com/download/${name}"
    }

    @JvmStatic
    fun download(fileName: String, dstFile: FileReference, callback: () -> Unit) {
        download(
            fileName, dstFile, listOfNotNull(
                mirrors[fileName],
                generateURL(fileName, true),
                generateURL(fileName, false),
            ), callback
        )
    }

    @JvmStatic
    fun download(fileName: String, dstFile: FileReference, urls: List<String>, callback: () -> Unit) {
        thread(name = "Download $fileName") {
            downloadSync(fileName, dstFile, urls, callback)
        }
    }

    @JvmStatic
    fun downloadSync(fileName: String, dstFile: FileReference, urls: List<String>, callback: () -> Unit) {
        // change "files" to "files.phychi.com"?
        // create a temporary file, and rename, so we know that we finished the download :)
        val tmp = dstFile.getSibling(dstFile.name + ".tmp")
        val progress = GFX.someWindow.addProgressBar(fileName, "Bytes", Double.NaN)
        for (i in urls.indices) {
            val url = urls[i]
            try {
                runDownload(URL(url), fileName, dstFile, tmp, progress)
                callback()
                return
            } catch (e: SSLHandshakeException) {
                if (url == urls.last()) {
                    progress.cancel(false)
                    LOGGER.error("Something went wrong with HTTPS :/. Please update Java, or download $url to $dstFile :)")
                    e.printStackTrace()
                }
            } catch (e: IOException) {
                if (url == urls.last()) {
                    progress.cancel(false)
                    LOGGER.error("Tried to download $fileName from $url to $dstFile, but failed! You can try to do it yourself.")
                    e.printStackTrace()
                }
            }
        }
    }

    @JvmStatic
    private fun runDownload(
        url: URL, fileName: String, dstFile: FileReference, tmp: FileReference,
        progress: ProgressBar?,
    ) {
        val con = url.openConnection() as HttpURLConnection
        val contentLength = con.contentLength
        if (contentLength > 0L) progress?.total = con.contentLength.toDouble()
        val input = con.inputStream.useBuffered()
        dstFile.getParent().tryMkdirs()
        val output = tmp.outputStream()
        val totalLength = con.contentLength.toLong()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var time0 = Time.nanoTime
        var length0 = 0L
        var downloadedLength = 0L
        while (true) {
            val length = input.read(buffer)
            if (length < 0) break
            length0 += length
            downloadedLength += length
            output.write(buffer, 0, length)
            val time1 = Time.nanoTime
            val dt = time1 - time0
            progress?.progress = downloadedLength.toDouble()
            if (dt > SECONDS_TO_NANOS) {
                LOGGER.info(formatDownload(fileName, dt, length0, downloadedLength, totalLength))
                time0 = time1
                length0 = 0
            }
        }
        output.close()
        tmp.renameTo(dstFile)
        progress?.finish(true)
        LOGGER.info(formatDownloadEnd(fileName, dstFile))
    }

    @JvmStatic
    fun download(
        fileName: String,
        srcFile: FileReference,
        dstFile: FileReference,
        callback: () -> Unit
    ) {
        // change "files" to "files.phychi.com"?
        // create a temporary file, and rename, so we know that we finished the download :)
        val tmp = dstFile.getSibling(dstFile.name + ".tmp")
        thread(name = "Download $fileName") {
            val progress = GFX.someWindow.addProgressBar(fileName, "Bytes", Double.NaN)
            try {
                runDownload(URL(srcFile.absolutePath), fileName, dstFile, tmp, progress)
                callback()
            } catch (e: IOException) {
                progress.cancel(false)
                LOGGER.error("Tried to download $fileName from $srcFile to $dstFile, but failed! You can try to do it yourself.")
                e.printStackTrace()
            }
        }
    }

    @JvmStatic
    fun uninstall() {

        // to do show a window
        // to do ask if the config should be deleted
        // to do ask if all (known, latest) projects should be erased

        // to do ask if ffmpeg shall be deleted, if it's not in the default install directory
        // to do put config into that default install directory???
        Uninstaller.uninstall()
    }
}