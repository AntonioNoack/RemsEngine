package me.anno.video

import me.anno.gpu.GFX
import me.anno.studio.rems.Rendering
import me.anno.utils.Maths
import me.anno.utils.types.Floats.f1
import me.anno.utils.types.Strings.formatTime2
import me.anno.utils.types.Strings.withLength
import org.apache.logging.log4j.Logger
import java.io.InputStream
import kotlin.math.round

object FFMPEGUtils {

    fun processOutput(
        logger: Logger,
        name: String,
        startTime: Long,
        totalFrameCount: Long,
        stream: InputStream
    ) {
        val out = stream.bufferedReader()
        while (true) {
            val line = out.readLine() ?: break
            if (line.contains("unable", true) ||
                line.contains("null", true)
            ) {
                logger.error(line)
                Rendering.isRendering = false
            }
            // parse the line
            if (line.indexOf('=') > 0) {
                // Video: frame=   65 fps=0.0 q=28.0 size=       0kB time=00:00:00.63 bitrate=   0.6kbits/s speed=1.26x
                // Both:  frame=  896 fps=254 q=-1.0 size=     768kB time=00:00:14.92 bitrate= 421.5kbits/s speed=4.23x
                // Audio: size=     346kB time=00:00:22.08 bitrate= 128.2kbits/s speed=4.54x
                var frameIndex = 0L
                var fps = 0f
                var quality = 0f
                var size = 0
                val elapsedTime = (GFX.gameTime - startTime) * 1e-9
                var bitrate = 0
                var speed = 0f
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
                        when (key.toLowerCase()) {
                            "speed" -> speed = value.substring(0, value.length - 1).toFloat() // 0.15x
                            "bitrate" -> {
                                // parse bitrate? or just display it?
                            }
                            // "time" -> elapsedTime = value.parseTime()
                            "size", "lsize" -> {
                            }
                            "q" -> {
                            } // quality?
                            "frame" -> frameIndex = value.toLong()
                            "fps" -> fps = value.toFloat()
                        }
                    } catch (e: Exception) {
                        logger.warn("${e.javaClass}: ${e.message}")
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
                    if (frameIndex == 0L) "Unknown"
                    else round(elapsedTime / relativeProgress * (1.0 - relativeProgress)).formatTime2(0)
                val progress =
                    if (frameIndex == totalFrameCount) " Done"
                    else "${formatPercent(relativeProgress)}%".withLength(5)
                logger.info(
                    "$name-Progress: $progress, " +
                            "fps: ${fps.toString().withLength(5)}, " +
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
    }

    fun formatPercent(progress: Double) = Maths.clamp(progress * 100, 0.0, 100.0).f1()

}