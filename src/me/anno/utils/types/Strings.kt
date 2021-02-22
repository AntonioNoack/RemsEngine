package me.anno.utils.types

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX.loadTexturesSync
import me.anno.gpu.GFXx2D.getTextSize
import me.anno.ui.base.Font
import me.anno.ui.base.text.TextPanel
import me.anno.utils.files.Files.formatFileSize
import me.anno.utils.Maths.fract
import me.anno.utils.structures.lists.ExpensiveList
import me.anno.utils.types.Floats.f1
import java.io.File
import java.lang.StringBuilder
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min

object Strings {

    fun List<Int>.joinChars() = joinToString(""){ String(Character.toChars(it)) }

    fun getLineWidth(line: List<Int>, endIndex: Int, tp: TextPanel) =
        getLineWidth(line, endIndex, tp.font)

    fun getLineWidth(line: List<Int>, endIndex: Int, font: Font): Float {
        return if(endIndex == 0) 0f
        else {
            loadTexturesSync.push(true)
            val stringValue = line.subList(0, min(endIndex, line.size)).joinChars()
            val measures = getTextSize(font, stringValue, -1)
            loadTexturesSync.pop()
            measures.first.toFloat()
        }
    }

    fun getIndexFromText(characters: List<Int>, localX: Float, tp: TextPanel) =
        getIndexFromText(characters, localX, tp.font)

    fun getIndexFromText(characters: List<Int>, localX: Float, font: Font): Int {
        val list = ExpensiveList(characters.size + 1) {
            getLineWidth(characters, it, font)
        }
        var index = list.binarySearch { it.compareTo(localX) }
        if(index < 0) index = -1 - index
        // find the closer neighbor
        if(index > 0 && index < characters.size && abs(list[index-1]-localX) < abs(list[index]-localX)){
            index--
        }
        return index
    }

    const val defaultImportType = "Text"

    fun String.getImportType(): String =
        DefaultConfig["import.mapping.$this"]?.toString() ?:
        DefaultConfig["import.mapping.${toLowerCase()}"]?.toString() ?:
        DefaultConfig["import.mapping.*"]?.toString() ?: defaultImportType

    // 00:57:28.87 -> 57 * 60 + 28.87
    fun String.parseTime(): Double {
        val parts = split(":").reversed()
        var seconds = parts[0].toDouble()
        if(parts.size > 1) seconds += 60 * parts[1].toInt()
        if(parts.size > 2) seconds += 3600 * parts[2].toInt()
        if(parts.size > 3) seconds += 24 * 3600 * parts[3].toInt()
        return seconds
    }

    fun Double.formatTime(): String {
        val seconds = toLong()
        if(seconds < 60) return "${seconds}s"
        if(seconds < 3600) return "${seconds/60}m ${seconds%60}s"
        return "${seconds/3600}h ${(seconds/60)%60}m ${seconds%60}s"
    }

    fun formatDownload(fileName: String, dt: Long, dl: Long, length1: Long, contentLength: Long): String {
        val speed = dl*1e9/dt
        val remaining = contentLength-length1
        val remainingTime = remaining / speed
        return "Downloading " +
                "$fileName " +
                "(${length1.formatFileSize()}/${contentLength.formatFileSize()}, ${(length1*100f/contentLength).f1()}%) " +
                "with ${speed.toLong().formatFileSize()}/s, " +
                "${remainingTime.formatTime()} remaining"
    }

    fun formatDownloadEnd(fileName: String, dst: File) = "Downloaded $fileName ($dst)"

    fun incrementTab(x0: Float, tabSize: Float, relativeTabSize: Float): Float {
        var x = x0
        val r = x / tabSize
        x = (floor(r) + 1f) * tabSize
        if((1f - fract(r)) * relativeTabSize < 1f){// smaller than a space? -> add a tab
            x += tabSize
        }
        return x
    }

    fun addPrefix(prefix: String?, suffix: String): String {
        return if (prefix == null) suffix
        else "$prefix$suffix"
    }

    fun addSuffix(prefix: String, suffix: String?): String {
        return if (suffix == null) prefix
        else "$prefix$suffix"
    }

    fun filterAlphaNumeric(str: String): String {
        return str.filter { it in 'A' .. 'Z' || it in 'a' .. 'z' || it in '0' .. '9' }
    }

    fun writeEscaped(value: String, data: StringBuilder){
        var i = 0
        var lastI = 0
        fun put() {
            if (i > lastI) {
                data.append(value.substring(lastI, i))
            }
            lastI = i + 1
        }
        while (i < value.length) {
            fun append(str: String){
                put()
                data.append(str)
            }
            when (value[i]) {
                '\\' -> append("\\\\")
                '\t' -> append("\\t")
                '\r' -> append("\\r")
                '\n' -> append("\\n")
                '"' -> append("\\\"")
                '\b' -> append("\\b")
                12.toChar() -> append("\\f")
                else -> {
                } // nothing
            }
            i++
        }
        put()
    }

}