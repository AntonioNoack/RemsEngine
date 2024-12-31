package me.anno.video

import me.anno.io.MediaMetadata
import me.anno.io.Streams.readText
import me.anno.io.files.FileReference
import me.anno.io.json.generic.JsonReader
import me.anno.jvm.utils.BetterProcessBuilder
import me.anno.utils.types.AnyToDouble
import me.anno.utils.types.AnyToInt
import me.anno.utils.types.Floats.roundToIntOr
import me.anno.utils.types.Strings.parseTime
import me.anno.video.ffmpeg.FFMPEG
import me.anno.video.ffmpeg.FFMPEGStream
import org.apache.logging.log4j.LogManager
import kotlin.math.ceil
import kotlin.math.max

object FFMPEGMetadata {

    private val LOGGER = LogManager.getLogger(VideoPlugin::class)

    fun MediaMetadata.loadFFMPEG() {
        val args = listOf(
            "-v", "quiet",
            "-print_format", "json",
            "-show_format",
            "-show_streams",
            "-print_format", "json",
            file.absolutePath
        )

        val builder = BetterProcessBuilder(FFMPEG.ffprobe, args.size, true)
        builder += args

        val process = builder.start()

        // get and parse the data :)
        FFMPEGStream.logOutput(null, file.absolutePath, process.errorStream, true)
        val data = JsonReader(process.inputStream.bufferedReader()).readObject()

        val streams = data["streams"] as? ArrayList<*> ?: ArrayList<Any?>()
        val format = data["format"] as? HashMap<*, *> ?: HashMap<String, Any?>()

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
            (it as HashMap<*, *>)["codec_type"].toString().equals("audio", true)
        } as? HashMap<*, *>

        if (audio != null) {
            hasAudio = true
            audioStartTime = AnyToDouble.getDouble(audio["start_time"], 0.0)
            audioDuration = AnyToDouble.getDouble(audio["duration"], duration)
            audioSampleRate = AnyToInt.getInt(audio["sample_rate"], 20)
            // duration_ts cannot be trusted
            audioSampleCount =
                (audioSampleRate * audioDuration).toLong() // getLong(audio["duration_ts"], (audioSampleRate * audioDuration).toLong())
            audioChannels = AnyToInt.getInt(audio["channels"], 1)
        }

        val video = streams.firstOrNull {
            (it as HashMap<*, *>)["codec_type"].toString().equals("video", true)
        } as? HashMap<*, *>

        if (video != null) {

            hasVideo = true
            videoStartTime = AnyToDouble.getDouble(video["start_time"], 0.0)
            videoDuration = AnyToDouble.getDouble(video["duration"], duration)
            videoFrameCount = max(0, AnyToInt.getInt(video["nb_frames"], 0))
            videoWidth = AnyToInt.getInt(video["width"], 0)
            videoHeight = AnyToInt.getInt(video["height"], 0)
            videoFPS = video["r_frame_rate"]?.toString()?.parseFraction() ?: 30.0

            if (videoFrameCount == 0) {
                if (videoDuration > 0.0 && videoFPS != 90000.0) {
                    videoFrameCount = max(ceil(videoDuration * videoFPS).toInt(), 1)
                    LOGGER.info("Frame count was 0, corrected it to $videoFrameCount = $videoDuration * $videoFPS")
                } else {
                    videoFrameCount = 1
                    videoDuration = 1.0
                    videoFPS = 1.0 / duration
                }
            } else {
                val expectedFrameCount = (videoDuration * videoFPS).roundToIntOr()
                if (expectedFrameCount * 10 !in videoFrameCount * 9..videoFrameCount * 11) {
                    // something is wrong
                    val frameCount = max(expectedFrameCount, videoFrameCount)
                    LOGGER.warn("$file: Frame Count / Frame Rate / Video Duration incorrect! $videoDuration s * $videoFPS fps is not $videoFrameCount frames")
                    videoDuration = frameCount / videoFPS // could be incorrect
                    // videoFPS = videoFrameCount / videoDuration
                    videoFrameCount = frameCount
                    LOGGER.warn("$file: Corrected by setting duration to $videoDuration s and frameCount to $frameCount")
                }
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

        val builder = BetterProcessBuilder(FFMPEG.ffmpeg, args.size, true)
        builder += args

        val process = builder.start()

        // get and parse the data :)
        FFMPEGStream.devNull(file.absolutePath, process.errorStream)
        val data = process.inputStream.readText()
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
}