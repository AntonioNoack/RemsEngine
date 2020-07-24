package me.anno.video

import me.anno.io.json.JsonArray
import me.anno.io.json.JsonObject
import me.anno.io.json.JsonReader
import me.anno.objects.cache.Cache
import me.anno.objects.cache.CacheData
import java.io.File

class FFMPEGMetadata(file: File): CacheData {

    val duration: Float

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

        val ffmpegProbe = FFMPEG.ffprobe
        val args = listOf(
            ffmpegProbe.absolutePath,
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

        val video = streams.firstOrNull { (it as JsonObject)["codec_type"]?.asText().equals("video", true) } as? JsonObject
        val audio = streams.firstOrNull { (it as JsonObject)["codec_type"]?.asText().equals("audio", true) } as? JsonObject

        duration = format["duration"].toString().toFloat()

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