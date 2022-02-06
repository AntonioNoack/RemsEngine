package me.anno.video.ffmpeg

import me.anno.io.utils.StringMap
import me.anno.utils.Warning.warn
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager
import java.lang.Exception
import java.lang.RuntimeException
import java.util.logging.Level.parse

class FFMPEGMetaParser(): StringMap(){

    var debug = false

    companion object {
        private val LOGGER = LogManager.getLogger(FFMPEGMetaParser::class)
    }

    /**
     * video (mp4):
    Output #0, rawvideo, to 'pipe:':
        Metadata:
            major_brand     : mp42
            minor_version   : 0
            compatible_brands: mp41isom
            title           : HITMAN 2
            artist          : Microsoft Game DVR
            encoder         : Lavf58.29.100
            Stream #0:0(und): Video: rawvideo (I420 / 0x30323449), yuv420p, 1728x1080 [SAR 1:1 DAR 8:5], q=2-31, 537477 kb/s, 24 fps, 24 tbn, 24 tbc (default)
            Metadata:
                creation_time   : 2020-03-19T13:25:46.000000Z
                handler_name    : VideoHandler
                encoder         : Lavc58.54.100 rawvideo

     * image(webp, not argb):
    Output #0, rawvideo, to 'pipe:':
        Metadata:
            encoder         : Lavf58.29.100
            Stream #0:0: Video: rawvideo (I420 / 0x30323449), yuv420p, 530x735, q=2-31, 112190 kb/s, 24 fps, 24 tbn, 24 tbc
            Metadata:
                encoder         : Lavc58.54.100 rawvideo
     *
     * */

    constructor(data: String): this(){
        parse(data)
    }

    fun getDepth(line: String): Int {
        for(i in line.indices){
            if(line[i] != ' ') return i/2
        }
        return line.length/2
    }

    fun String.specialSplit(): ArrayList<String> {
        val list = ArrayList<String>()
        var i0 = 0
        var i = 0
        fun put(){
            if(i > i0){
                list += substring(i0, i)
            }
            i0 = i+1
        }
        while(i < length){
            when(this[i]){
                ',', '(', ')', '[', ']', ':' -> {
                    put()
                    list += this[i].toString()
                }
                ' ' -> {
                    put()
                }
            }
            i++
        }
        put()
        return list
    }

    var level0Type = ""
    var level1Type = ""

    fun removeBrackets(list: MutableList<String>){
        var depth = 0
        list.removeAll {
            when(it){
                "(", "[" -> {
                    depth++
                    true
                }
                ")", "]" -> {
                    depth--
                    true
                }
                else -> depth>0
            }
        }
    }

    fun parseLine(line: String, stream: FFMPEGStream){
        if(line.isBlank2()) return
        // if(debug) LOGGER.debug(line)
        val depth = getDepth(line)
        val data = line.trim().specialSplit()
        if(debug) LOGGER.debug("$depth $data")
        when(depth){
            0 -> {
                level0Type = data[0]
            }
            1 -> {
                // to do parse dar for correct ratio? ... can be corrected manually...
                level1Type = data[0]
                when(level1Type){
                    "Duration" -> {
                        if(level0Type == "Input"){
                            // [Duration, :, 00, :, 00, :, 31.95, ,, start, :, 0.000000, ,, bitrate, :, 10296, kb/s]
                            val durParts = data.subList(2, data.indexOf(","))
                            try {
                                if(!durParts.withIndex().all { (index, value) -> ((index % 2) == 0) || value == ":" }){
                                    throw RuntimeException("Invalid ffmpeg-duration? $data")
                                }
                                val duration = when(durParts.size){
                                    1 -> durParts[0].toDoubleOrNull() ?: 0.01
                                    3 -> durParts[0].toDouble() * 60 + durParts[2].toDouble()
                                    5 -> durParts[0].toDouble() * 3600 + durParts[2].toDouble() * 60 + durParts[4].toDouble()
                                    7 -> durParts[0].toDouble() * 3600 * 24 + durParts[2].toDouble() * 3600 + durParts[4].toDouble() * 60 + durParts[6].toDouble()
                                    else -> throw RuntimeException("Invalid ffmpeg-duration? $data")
                                }
                                stream.sourceLength = duration
                                // ("duration: $duration")
                            } catch (e: Exception){
                                e.message?.apply { warn(this) }
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
            2 -> {
                if(level0Type == "Output" && data[0] == "Stream"){
                    val videoTypeIndex = data.indexOf("rawvideo")
                    if(debug) LOGGER.debug(data.toString())
                    if(videoTypeIndex > -1 && videoTypeIndex+2 < data.size && data[videoTypeIndex+1] == "("){
                        stream.codec = data[videoTypeIndex+2]
                    }
                    removeBrackets(data)
                    val wh = data.mapNotNull {
                        try {
                            val widthHeight = it.split('x').map { dim -> dim.toIntOrNull() }
                            val width = widthHeight[0] as Int
                            val height = widthHeight[1] as Int
                            width to height
                        } catch (e: Exception){
                            null
                        }
                    }.firstOrNull()
                    if(wh != null){
                        // we got our info <3
                        stream.w = wh.first
                        stream.h = wh.second
                    }
                }
                if(level0Type == "Input" && data[0] == "Stream"){
                    try {
                        val fpsIndex = data.indexOf("fps")-1
                        if(fpsIndex > -1){
                            stream.sourceFPS = data[fpsIndex].toDouble()
                        }
                    } catch (e: Exception){
                        e.printStackTrace()
                    }
                    removeBrackets(data)
                    val wh = data.mapNotNull {
                        try {
                            val widthHeight = it.split('x').map { dim -> dim.toIntOrNull() }
                            val width = widthHeight[0] as Int
                            val height = widthHeight[1] as Int
                            width to height
                        } catch (e: Exception){
                            null
                        }
                    }.firstOrNull()
                    if(wh != null){
                        // we got our info <3
                        stream.srcW = wh.first
                        stream.srcH = wh.second
                    }
                }
            }
        }
    }



}