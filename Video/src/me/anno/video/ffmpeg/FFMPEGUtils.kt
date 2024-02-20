package me.anno.video.ffmpeg

import me.anno.Time
import me.anno.utils.types.Floats.formatPercent
import me.anno.utils.types.Strings.formatTime2
import me.anno.utils.types.Strings.withLength
import org.apache.logging.log4j.Logger
import java.io.InputStream
import java.util.ArrayList
import kotlin.math.max
import kotlin.math.round

object FFMPEGUtils {

    fun processOutput(
        logger: Logger,
        name: String,
        startTime: Long,
        targetFPS: Double,
        totalFrameCount: Int,
        stream: InputStream,
        onFailure: () -> Unit
    ) {
        val out = stream.bufferedReader()
        var lastTime = Time.nanoTime
        var progressGuess = 0.0
        var hasFailed = false
        val lines = ArrayList<String>()
        while (true) {

            val line = out.readLine() ?: break
            lines += line

            if (!hasFailed) {
                if (line.contains("unable", true) ||
                    line.contains("null", true) ||
                    line.contains("error", true) ||
                    line.contains("failed", true)
                ) {
                    onFailure()
                    hasFailed = true
                }
            }

            // parse the line
            if (line.indexOf('=') > 0) {
                // Video: frame=   65 fps=0.0 q=28.0 size=       0kB time=00:00:00.63 bitrate=   0.6kbits/s speed=1.26x
                // Both:  frame=  896 fps=254 q=-1.0 size=     768kB time=00:00:14.92 bitrate= 421.5kbits/s speed=4.23x
                // Audio: size=     346kB time=00:00:22.08 bitrate= 128.2kbits/s speed=4.54x
                var frameIndex = 0
                var fps = 0f
                // var quality = 0f
                // var size = 0
                val thisTime = Time.nanoTime
                val deltaTime = thisTime - lastTime
                lastTime = thisTime
                val elapsedTime = (thisTime - startTime) * 1e-9
                // var bitrate = 0
                var hasFrameInformation = false
                var speedStr = "?"
                var remaining = line
                while (remaining.isNotEmpty()) {
                    val firstIndex = remaining.indexOf('=')
                    if (firstIndex < 0) break
                    val key = remaining.substring(0, firstIndex).trim()
                    remaining = remaining.substring(firstIndex + 1).trim()
                    var secondIndex = remaining.indexOf(' ')
                    if (secondIndex < 0) secondIndex = remaining.length
                    val value = remaining.substring(0, secondIndex)
                    try {
                        when (key.lowercase()) {
                            "speed" -> {
                                // if frame is not delivered (rendering audio only), update frame
                                speedStr = value
                                val speed = value.substring(0, value.length - 1).toDouble()
                                if (!hasFrameInformation) {
                                    // time since last: guess fps
                                    progressGuess += speed * deltaTime * targetFPS / 1e9
                                    frameIndex = progressGuess.toInt()
                                    fps = (speed * targetFPS).toFloat()
                                }
                            } // 0.15x
                            "bitrate" -> {
                                // parse bitrate? or just display it?
                            }
                            // "time" -> elapsedTime = value.parseTime()
                            "size", "lsize" -> {
                            }
                            "q" -> {
                            } // quality?
                            "frame" -> {
                                frameIndex = value.toInt()
                                hasFrameInformation = true
                            }
                            "fps" -> fps = value.toFloat()
                        }
                    } catch (e: Exception) {
                        logger.warn("${e::class}: ${e.message}")
                    }
                    // LOGGER.info("$key: $value")
                    if (secondIndex == remaining.length) break
                    remaining = remaining.substring(secondIndex)
                }
                // update progress bar after this
                // + log other statistics
                val relativeProgress = frameIndex.toDouble() / totalFrameCount
                // estimate remaining time
                // round the value to not confuse artists (and to "give" 0.5s "extra" ;))
                val remainingTime =
                    if (frameIndex == 0) "Unknown"
                    else max(0.0, round(elapsedTime / relativeProgress * (1.0 - relativeProgress)))
                        .formatTime2(0)
                val progress =
                    if (frameIndex == totalFrameCount) " Done"
                    else "${relativeProgress.formatPercent()}%".withLength(5)
                val fpsString = if (name == "Audio") "" else "fps: ${fps.toString().withLength(5)}, "
                logger.info(
                    "$name-Progress: $progress, " +
                            fpsString +
                            "speed: ${speedStr.withLength(5)}, " +
                            "elapsed: ${round(elapsedTime).formatTime2(0)}, " +
                            "remaining: $remainingTime"
                )
            }// else {
            // the rest logged is only x264 statistics
            // LOGGER.debug(line)
            // }
            // frame=  151 fps= 11 q=12.0 size=     256kB time=00:00:04.40 bitrate= 476.7kbits/s speed=0.314x
            // or [libx264 @ 000001c678804000] frame I:1     Avg QP:19.00  size:  2335
        }
        if (hasFailed) {
            for (line in lines)
                logger.error(line)
        }
    }

}