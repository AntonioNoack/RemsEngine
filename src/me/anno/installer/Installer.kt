package me.anno.installer

import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.maths.Maths.SECONDS_TO_NANOS
import me.anno.utils.OS
import me.anno.utils.types.Strings.formatDownload
import me.anno.utils.types.Strings.formatDownloadEnd
import me.anno.video.ffmpeg.FFMPEG
import me.anno.video.ffmpeg.FFMPEG.ffmpegPath
import me.anno.video.ffmpeg.FFMPEG.ffprobePath
import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.SSLHandshakeException
import kotlin.concurrent.thread

object Installer {

    @JvmStatic
    private val LOGGER = LogManager.getLogger(Installer::class)

    // on startup check if ffmpeg can be found
    // if not, download it - from our website?

    // check all dependencies

    // where should we put ffmpeg?
    // typically that would be (xyz on Linux, idk, maybe anywhere?, ~/.remsStudio?)
    // put it into ~/.AntonioNoack/RemsStudio?

    // all files need to be checked every time
    @JvmStatic
    fun checkInstall() {
        if (!FFMPEG.isInstalled && OS.isWindows) {
            downloadMaybe("ffmpeg/bin/ffmpeg.exe", ffmpegPath)
            downloadMaybe("ffmpeg/bin/ffprobe.exe", ffprobePath)
        }
    }

    @JvmStatic
    fun downloadMaybe(src: String, dst: FileReference) {
        if (!dst.exists) download(src, dst) {}
        else LOGGER.info("$src already is downloaded :)")
    }

    @JvmStatic
    fun download(fileName: String, dstFile: FileReference, callback: () -> Unit) =
        download(fileName, dstFile, true, callback)

    @JvmStatic
    fun download(fileName: String, dstFile: FileReference, withHttps: Boolean = true, callback: () -> Unit) {
        // change "files" to "files.phychi.com"?
        // create a temporary file, and rename, so we know that we finished the download :)
        val tmp = getReference(dstFile.getParent(), dstFile.name + ".tmp")
        thread(name = "Download $fileName") {
            val protocol = if (withHttps) "https" else "http"
            val name = fileName.replace(" ", "%20")
            val totalURL = "${protocol}://remsstudio.phychi.com/download/${name}"
            try {
                val con = URL(totalURL).openConnection() as HttpURLConnection
                val input = con.inputStream.buffered()
                dstFile.getParent()?.tryMkdirs()
                val output = tmp.outputStream().buffered()
                val totalLength = con.contentLength.toLong()
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var time0 = System.nanoTime()
                var length0 = 0L
                var downloadedLength = 0L
                while (true) {
                    val length = input.read(buffer)
                    if (length < 0) break
                    length0 += length
                    downloadedLength += length
                    output.write(buffer, 0, length)
                    val time1 = System.nanoTime()
                    val dt = time1 - time0
                    if (dt > SECONDS_TO_NANOS) {
                        LOGGER.info(formatDownload(fileName, dt, length0, downloadedLength, totalLength))
                        time0 = time1
                        length0 = 0
                    }
                }
                output.close()
                tmp.renameTo(dstFile)
                LOGGER.info(formatDownloadEnd(fileName, dstFile))
                callback()
            } catch (e: SSLHandshakeException) {
                if (withHttps) {
                    download(fileName, dstFile, false, callback)
                } else {
                    LOGGER.error("Something went wrong with HTTPS :/. Please update Java, or download $totalURL to $dstFile :)")
                    e.printStackTrace()
                }
            } catch (e: IOException) {
                LOGGER.error("Tried to download $fileName from $totalURL to $dstFile, but failed! You can try to do it yourself.")
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