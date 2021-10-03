package me.anno.video

import me.anno.cache.CacheSection
import me.anno.cache.data.ICacheData
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.json.JsonArray
import me.anno.io.json.JsonObject
import me.anno.io.json.JsonReader
import me.anno.utils.process.BetterProcessBuilder
import me.anno.utils.types.Strings.parseTime
import org.apache.logging.log4j.LogManager
import kotlin.math.ceil
import kotlin.math.roundToInt

class FFMPEGMetadata(val file: FileReference) : ICacheData {

    val duration: Double

    val hasAudio: Boolean
    val hasVideo: Boolean

    val audioStartTime: Double
    val audioSampleRate: Int
    val audioDuration: Double
    val audioSampleCount: Long // 24h * 3600s/h * 48k = 4B -> Long

    val videoStartTime: Double
    var videoFPS: Double
    var videoDuration: Double
    val videoWidth: Int
    val videoHeight: Int
    var videoFrameCount: Int

    override fun toString(): String {
        return "FFMPEGMetadata(file: $file, audio: $hasAudio, video: $hasVideo)"
    }

    init {

        val args = listOf(
            "-v", "quiet",
            "-print_format", "json",
            "-show_format",
            "-show_streams",
            "-print_format", "json",
            file.absolutePath
        )

        val builder = BetterProcessBuilder(FFMPEG.ffprobePathString, args.size, true)
        builder += args

        val process = builder.start()

        // get and parse the data :)
        val data = JsonReader(process.inputStream.buffered()).readObject()
        val streams = data["streams"] as? JsonArray ?: JsonArray()
        val format = data["format"] as? JsonObject ?: JsonObject()

        // critical, not working 001.gif file data from ffprobe:
        // works in Windows, just not Linux
        // todo transfer ffmpeg to Java? maybe ffmpeg is an older version on Linux?
        // {streams=[
        //      {
        //          pix_fmt=bgra,
        //          time_base=1/100,
        //          coded_height=270,
        //          level=-99,
        //          r_frame_rate=12000/1001,
        //          index=0,
        //          codec_name=gif,
        //          sample_aspect_ratio=0:1,
        //          disposition=
        //              {dub=0, karaoke=0, default=0, original=0, visual_impaired=0, forced=0, attached_pic=0, timed_thumbnails=0, comment=0, hearing_impaired=0, lyrics=0, clean_effects=0 },
        //          codec_tag=0x0000,
        //          has_b_frames=0,
        //          refs=1,
        //          codec_time_base=1/12,
        //          width=480,
        //          display_aspect_ratio=0:1,
        //          codec_tag_string=[0][0][0][0],
        //          coded_width=480,
        //          avg_frame_rate=12/1,
        //          codec_type=video,
        //          codec_long_name=GIF (Graphics Interchange Format),
        //          height=270
        //      }
        //  ],
        //  format={
        //      filename=/home/antonio/Pictures/Anime/001.gif,
        //      size=3414192,
        //      probe_score=100,
        //      nb_programs=0,
        //      format_long_name=CompuServe Graphics Interchange Format (GIF),
        //      nb_streams=1,
        //      format_name=gif
        //  }}

        duration = format["duration"]?.toString()?.toDouble() ?: getDurationIfMissing(file)

        val audio = streams.firstOrNull {
            (it as JsonObject)["codec_type"]?.asText().equals("audio", true)
        } as? JsonObject

        hasAudio = audio != null
        audioStartTime = audio?.get("start_time")?.asText()?.toDouble() ?: 0.0
        audioDuration = audio?.get("duration")?.asText()?.toDouble() ?: duration
        audioSampleRate = audio?.get("sample_rate")?.asText()?.toInt() ?: 20
        audioSampleCount = audio?.get("duration_ts")?.asText()?.toLong() ?: (audioSampleRate * audioDuration).toLong()

        val video = streams.firstOrNull {
            (it as JsonObject)["codec_type"]?.asText().equals("video", true)
        } as? JsonObject

        hasVideo = video != null
        videoStartTime = video?.get("start_time")?.asText()?.toDouble() ?: 0.0
        videoDuration = video?.get("duration")?.asText()?.toDouble() ?: duration
        videoFrameCount = video?.get("nb_frames")?.asText()?.toInt() ?: 0
        videoWidth = video?.get("width")?.asText()?.toInt() ?: 0
        videoHeight = video?.get("height")?.asText()?.toInt() ?: 0
        videoFPS = video?.get("r_frame_rate")?.asText()?.parseFraction() ?: 30.0

        if (videoFrameCount == 0) {
            if (videoDuration > 0.0) {
                videoFrameCount = ceil(videoDuration * videoFPS).toInt()
                LOGGER.info("Frame count was 0, corrected it to $videoFrameCount = $videoDuration * $videoFPS")
            }
        } else {
            val expectedFrameCount = (videoDuration * videoFPS).roundToInt()
            if (expectedFrameCount * 10 !in videoFrameCount * 9..videoFrameCount * 11) {
                // something is wrong
                // nb_frames is probably correct
                LOGGER.warn("$file: Frame Count / Frame Rate / Video Duration incorrect! $videoDuration s * $videoFPS fps is not $videoFrameCount frames")
                videoDuration = duration // could be incorrect
                videoFPS = videoFrameCount / videoDuration
                LOGGER.warn("$file: Corrected by setting duration to $duration s and fps to $videoFPS")
            }
        }
        LOGGER.info("Loaded info about $file: $duration * $videoFPS = $videoFrameCount frames / $audioDuration * $audioSampleRate = $audioSampleCount samples")

    }

    // not working for the problematic file 001.gif
    fun getDurationIfMissing(file: FileReference): Double {

        LOGGER.warn("Duration is missing for $file")

        val args = listOf(
            // ffprobe -v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 input.mp4
            "-i", file.absolutePath, "-f", "null", "-"
        )

        val builder = BetterProcessBuilder(FFMPEG.ffmpegPathString, args.size, true)
        builder += args

        val process = builder.start()

        // get and parse the data :)
        val data = String(process.inputStream.readBytes())
        if (data.isEmpty()) return 0.0
        else LOGGER.info("Duration, because missing: $data")
        val time = data.split("time=")[1].split(" ")[0]
        // frame=206723 fps=1390 q=-0.0 Lsize=N/A time=00:57:28.87 bitrate=N/A speed=23.2x
        return time.parseTime()

    }

    fun String.parseFraction(): Double {
        val i = indexOf('/')
        if (i < 0) return toDouble()
        val a = substring(0, i).trim().toDouble()
        val b = substring(i + 1).trim().toDouble()
        return a / b
    }

    override fun destroy() {}

    companion object {

        private val LOGGER = LogManager.getLogger(FFMPEGMetadata::class)
        private val metadataCache = CacheSection("Metadata")

        private fun createMetadata(file: FileReference, i: Long) = FFMPEGMetadata(file)

        fun getMeta(path: String, async: Boolean): FFMPEGMetadata? {
            return getMeta(getReference(path), async)
        }

        fun getMeta(file: FileReference, async: Boolean): FFMPEGMetadata? {
            if (file.isDirectory || !file.exists) return null
            // if (!async) GFX.checkIsNotGFXThread()
            return metadataCache.getEntry(file, file.lastModified, 300_000, async, ::createMetadata) as? FFMPEGMetadata
        }
    }

}