package me.anno.installer

import me.anno.utils.*
import me.anno.video.FFMPEG
import me.anno.video.FFMPEG.ffmpegPath
import me.anno.video.FFMPEG.ffprobePath
import org.apache.logging.log4j.LogManager
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.SSLHandshakeException
import kotlin.concurrent.thread

object Installer {

    private val LOGGER = LogManager.getLogger(Installer::class)

    // on startup check if ffmpeg can be found
    // if not, download it - from our website?

    // check all dependencies

    // where should we put ffmpeg?
    // typically that would be (xyz on Linux, idk, maybe anywhere?, ~/.remsStudio?)
    // put it into ~/.AntonioNoack/RemsStudio?

    // all files need to be checked every time
    fun checkInstall(){
        if(!FFMPEG.isInstalled && OS.isWindows){
            downloadMaybe("ffmpeg.exe", ffmpegPath)
            downloadMaybe("ffprobe.exe", ffprobePath)
        }
    }

    fun downloadMaybe(src: String, dst: File){
        if(!dst.exists()) download(src, dst)
        else LOGGER.info("$src already is downloaded :)")
    }

    fun download(fileName: String, dstFile: File){
        // change files to files.phychi.com?
        // create a temporary file, and rename, so we know that we finished the download :)
        val tmp = File(dstFile.parentFile, dstFile.name+".tmp")
        thread {
            val totalURL = "https://api.phychi.com/remsstudio/download/$fileName"
            try {
                val con = URL(totalURL).openConnection() as HttpURLConnection
                val input = con.inputStream.buffered()
                dstFile.parentFile.mkdirs()
                val output = tmp.outputStream().buffered()
                val totalLength = con.contentLength.toLong()
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var time0 = System.nanoTime()
                var length0 = 0L
                var downloadedLength = 0L
                while(true){
                    val length = input.read(buffer)
                    if(length < 0) break
                    length0 += length
                    downloadedLength += length
                    output.write(buffer, 0, length)
                    val time1 = System.nanoTime()
                    val dt = time1-time0
                    if(dt > 1_000_000_000){
                        LOGGER.info(formatDownload(fileName, dt, length0, downloadedLength, totalLength))
                        time0 = time1
                        length0 = 0
                    }
                }
                output.close()
                tmp.renameTo(dstFile)
                LOGGER.info(formatDownloadEnd(fileName, dstFile))
            } catch (e: SSLHandshakeException){
                LOGGER.error("Something went wrong with HTTPS :/. Please update Java, or download $totalURL to $dstFile :)")
                e.printStackTrace()
            } catch (e: IOException){
                LOGGER.error("Tried to download $fileName from $totalURL to $dstFile, but failed! You can try to do it yourself.")
                e.printStackTrace()
            }
        }
    }

    fun uninstall(){

        // todo show a window
        // todo ask if the config should be deleted
        // todo ask if all (known, latest) projects should be erased

        // todo ask if ffmpeg shall be deleted, if it's not in the default install directory
        // todo put config into that default install directory???
        Uninstaller.uninstall()

    }

}