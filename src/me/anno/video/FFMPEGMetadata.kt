package me.anno.video

import me.anno.io.json.JsonArray
import me.anno.io.json.JsonObject
import me.anno.io.json.JsonReader
import me.anno.objects.cache.Cache
import me.anno.objects.cache.CacheData
import java.io.File

class FFMPEGMetadata(file: File): CacheData {

    val duration: Double

    val hasAudio: Boolean
    val hasVideo: Boolean

    val audioStartTime: Double
    val audioSampleRate: Int
    val audioDuration: Double
    val audioSampleCount: Long // 24h * 3600s/h * 48k = 4B -> Long

    val videoStartTime: Double
    val videoFPS: Double
    val videoDuration: Double
    val videoWidth: Int
    val videoHeight: Int
    val videoFrameCount: Int

    init {

        val args = listOf(
            FFMPEG.ffprobePathString,
            "-v", "quiet",
            "-print_format", "json",
            "-show_format",
            "-show_streams",
            "-print_format", "json",
            file.absolutePath
        )

        val process = ProcessBuilder(args).start()

        // get and parse the data :)
        val data = JsonReader(process.inputStream.buffered()).readObject()
        val streams = data["streams"] as JsonArray
        val format = data["format"] as JsonObject

        println(data)

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

        val video = streams.firstOrNull { (it as JsonObject)["codec_type"]?.asText().equals("video", true) } as? JsonObject
        val audio = streams.firstOrNull { (it as JsonObject)["codec_type"]?.asText().equals("audio", true) } as? JsonObject

        duration = format["duration"]?.toString()?.toDouble() ?: getDurationIfMissing(file)

        hasAudio = audio != null
        audioStartTime = audio?.get("start_time")?.asText()?.toDouble() ?: 0.0
        audioDuration = audio?.get("duration")?.asText()?.toDouble() ?: 0.0
        audioSampleRate = audio?.get("sample_rate")?.asText()?.toInt() ?: 20
        audioSampleCount = audio?.get("duration_ts")?.asText()?.toLong() ?: (audioSampleRate * audioDuration).toLong()

        hasVideo = video != null
        videoStartTime = video?.get("start_time")?.asText()?.toDouble() ?: 0.0
        videoDuration = video?.get("duration")?.asText()?.toDouble() ?: 0.0
        videoFrameCount = video?.get("nb_frames")?.asText()?.toInt() ?: 0
        videoWidth = video?.get("width")?.asText()?.toInt() ?: 0
        videoHeight = video?.get("height")?.asText()?.toInt() ?: 0
        videoFPS = video?.get("r_frame_rate")?.asText()?.parseFraction() ?: 30.0

    }

    // not working for the problematic file 001.gif
    fun getDurationIfMissing(file: File): Double {

        val args = listOf(
            // ffprobe -v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 input.mp4
            FFMPEG.ffmpegPathString, "-i", file.absolutePath, "-f", "null", "-"
        )

        val process = ProcessBuilder(args).start()

        // get and parse the data :)
        val data = String(process.inputStream.readBytes())
        println(data)
        val time = data.split("time=")[1].split(" ")[0]
        // frame=206723 fps=1390 q=-0.0 Lsize=N/A time=00:57:28.87 bitrate=N/A speed=23.2x
        return time.parseTime()

    }

    // 00:57:28.87 -> 57 * 60 + 28.87
    fun String.parseTime(): Double {
        val parts = split(":").reversed()
        var seconds = parts[0].toDouble()
        if(parts.size > 1) seconds += 60 * parts[1].toInt()
        if(parts.size > 2) seconds += 3600 * parts[2].toInt()
        if(parts.size > 3) seconds += 24 * 3600 * parts[3].toInt()
        return seconds
    }

    fun String.parseFraction(): Double {
        val i = indexOf('/')
        if(i < 0) return toDouble()
        val a = substring(0, i).trim().toDouble()
        val b = substring(i+1).trim().toDouble()
        return a/b
    }

    override fun destroy() {}

    companion object {
        fun getMeta(file: File, async: Boolean): FFMPEGMetadata? {
            if(file.isDirectory || !file.exists()) return null
            return Cache.getEntry("metadata" to file, 10_000, async){
                FFMPEGMetadata(file)
            } as? FFMPEGMetadata
        }
    }

}