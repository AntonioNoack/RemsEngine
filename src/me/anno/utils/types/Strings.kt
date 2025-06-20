package me.anno.utils.types

import me.anno.config.DefaultConfig
import me.anno.fonts.Font
import me.anno.gpu.drawing.DrawTexts.getTextSizeX
import me.anno.io.files.FileReference
import me.anno.io.json.saveable.JsonWriterBase
import me.anno.maths.Maths
import me.anno.maths.Maths.fract
import me.anno.ui.base.text.TextPanel
import me.anno.utils.files.Files.formatFileSize
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.structures.lists.LazyList
import me.anno.utils.types.Floats.f1
import me.anno.utils.types.Ints.toIntOrDefault
import java.io.Writer
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow

@Suppress("unused")
object Strings {

    @JvmStatic
    fun Int.joinChars0(): CharArray {
        return Character.toChars(this)
    }

    @JvmStatic
    fun Int.joinChars(): CharSequence {
        return joinChars0().concatToString()
    }

    @JvmStatic
    fun List<Int>.joinChars(startIndex: Int = 0, endIndex: Int = size): CharSequence {
        val builder = StringBuilder(endIndex - startIndex)
        for (i in startIndex until endIndex) {
            builder.append(get(i).joinChars0())
        }
        return builder
    }

    @JvmStatic
    fun IntArray.joinChars(startIndex: Int = 0, endIndex: Int = size): CharSequence {
        val builder = StringBuilder(endIndex - startIndex)
        for (i in startIndex until endIndex) {
            builder.append(get(i).joinChars0())
        }
        return builder
    }

    @JvmStatic
    fun IntArrayList.joinChars(startIndex: Int = 0, endIndex: Int = size): CharSequence {
        return values.joinChars(startIndex, endIndex)
    }

    @JvmStatic
    fun IntArray.joinChars(startIndex: Int = 0, endIndex: Int = size, filter: (Int) -> Boolean): CharSequence {
        val builder = StringBuilder(endIndex - startIndex)
        for (i in startIndex until endIndex) {
            val char = get(i)
            if (filter(char)) {
                builder.append(char.joinChars0())
            }
        }
        return builder
    }

    @JvmStatic
    fun getLineWidth(line: List<Int>, endIndex: Int, tp: TextPanel) = getLineWidth(line, endIndex, tp.font)

    @JvmStatic
    fun getLineWidth(line: List<Int>, endIndex: Int, font: Font): Float {
        return if (endIndex == 0) 0f
        else {
            val stringValue = line.joinChars(0, min(endIndex, line.size))
            getTextSizeX(font, stringValue).toFloat()
        }
    }

    @JvmStatic
    fun getIndexFromText(characters: IntArrayList, localX: Float, tp: TextPanel): Int =
        getIndexFromText(characters, localX, tp.font)

    @JvmStatic
    fun getIndexFromText(characters: IntArrayList, localX: Float, font: Font): Int {
        val chars = characters.toList()
        val list = LazyList(characters.size + 1) {
            getLineWidth(chars, it, font)
        }
        var index = list.binarySearch { it.compareTo(localX) }
        if (index < 0) index = -1 - index
        // find the closer neighbor
        if (index > 0 && index <= characters.size && abs(list[index - 1] - localX) < abs(list[index] - localX)) {
            index--
        }
        return index
    }

    @JvmStatic
    fun getIndexFromText(characters: List<Int>, localX: Float, tp: TextPanel): Int =
        getIndexFromText(characters, localX, tp.font)

    @JvmStatic
    fun getIndexFromText(characters: List<Int>, localX: Float, font: Font): Int {
        val list = LazyList(characters.size + 1) {
            getLineWidth(characters, it, font)
        }
        var index = list.binarySearch { it.compareTo(localX) }
        if (index < 0) index = -1 - index
        // find the closer neighbor
        if (index > 0 && index <= characters.size && abs(list[index - 1] - localX) < abs(list[index] - localX)) {
            index--
        }
        return index
    }

    private const val DEFAULT_IMPORT_TYPE = "Text"

    @JvmStatic
    fun getImportTypeByExtension(fileExtension: String): String? {
        return DefaultConfig["import.mapping.$fileExtension"]?.toString()
            ?: DefaultConfig["import.mapping.*"]?.toString()
    }

    @JvmStatic
    fun findImportTypeExtensions(importType: String): List<String> {
        val prefix = "import.mapping."
        return DefaultConfig.entries
            .filter { it.key.startsWith(prefix) && it.value == importType }
            .map { it.key.substring(prefix.length) }
    }

    // 00:57:28.87 -> 57 * 60 + 28.87
    @JvmStatic
    fun String.parseTime(): Double {
        val parts = split(":").reversed()
        var seconds = parts[0].toDoubleOrNull() ?: 0.0
        if (parts.size > 1) seconds += 60.0 * (parts[1].toIntOrDefault(0))
        if (parts.size > 2) seconds += 3600.0 * (parts[2].toIntOrDefault(0))
        if (parts.size > 3) seconds += 24.0 * 3600.0 * (parts[3].toIntOrDefault(0))
        return seconds
    }

    // 00:57:28.87 -> 57 * 60 + 28.87
    @JvmStatic
    fun String.parseTimeOrNull(): Double? {

        val parts = split(":")
        val l = parts.size
        if (l > 4) return null

        var seconds = parts[l - 1].toDoubleOrNull() ?: return null
        if (parts.size < 2) return seconds

        val minutes = parts[l - 2].toIntOrDefault(-1)
        if (minutes < 0) return null
        seconds += 60.0 * minutes
        if (parts.size < 3) return seconds

        val hours = parts[l - 3].toIntOrDefault(-1)
        if (hours < 0) return null
        seconds += 3600.0 * hours
        if (parts.size < 4) return seconds

        val days = parts[0].toIntOrDefault(-1)
        if (days < 0) return null
        seconds += 24.0 * 3600.0 * days

        return seconds
    }

    @JvmStatic
    fun Double.formatTime(fractions: Int = 0): String {
        val fractionString = if (fractions > 0) {
            NumberFormatter.formatFloat(fract(this), fractions, false).substring(1)
        } else ""
        val seconds = toLong()
        if (seconds < 60) return "${seconds}${fractionString}s"
        if (seconds < 3600) return "${seconds / 60}m ${seconds % 60}${fractionString}s"
        return "${seconds / 3600}h ${(seconds / 60) % 60}m ${seconds % 60}${fractionString}s"
    }

    @JvmStatic
    fun Double?.formatTime2(fractions: Int): String {
        if (this == null || this.isNaN()) return "Unknown"
        if (fractions > 0) {
            val fractionString = NumberFormatter.formatFloat(fract(this), fractions, false)
            return formatTime2(0) + fractionString.substring(1)
        }
        val seconds = toLong()
        return "${format2(seconds / 3600)}:${format2((seconds / 60) % 60)}:${format2(seconds % 60)}"
    }

    @JvmStatic
    fun format2(i: Long) = if (i < 10) "0$i" else i.toString()

    @JvmStatic
    fun format2(i: Int) = if (i < 10) "0$i" else i.toString()

    @JvmStatic
    fun String.withLength(length: Int, atTheStart: Boolean = true): String {
        val spaces = length - this.length
        if (spaces <= 0) return this
        val builder = StringBuilder(1 + spaces)
        if (!atTheStart) builder.append(this)
        for (i in 0 until spaces) builder.append(' ')
        if (atTheStart) builder.append(this)
        return builder.toString()
    }

    @JvmStatic
    fun formatDownload(fileName: String, dt: Long, dl: Long, length1: Long, contentLength: Long): String {
        val speed = dl * 1e9 / dt
        val remaining = contentLength - length1
        val remainingTime = remaining / speed
        return "Downloading " +
                "$fileName " +
                "(${length1.formatFileSize()}/${contentLength.formatFileSize()}, ${(length1 * 100f / contentLength).f1()}%) " +
                "with ${speed.toLong().formatFileSize()}/s, " +
                "${remainingTime.formatTime(0)} remaining"
    }

    @JvmStatic
    fun formatDownloadEnd(fileName: String, dst: FileReference) = "Downloaded $fileName ($dst)"

    @JvmStatic
    fun incrementTab(x0: Float, tabSize: Float, relativeTabSize: Float): Float {
        var x = x0
        val r = x / tabSize
        x = (floor(r) + 1f) * tabSize
        if ((1f - fract(r)) * relativeTabSize < 1f) {// smaller than a space? -> add a tab
            x += tabSize
        }
        return x
    }

    @JvmStatic
    fun addPrefix(prefix: String?, suffix: String): String {
        return if (prefix == null) suffix
        else "$prefix$suffix"
    }

    @JvmStatic
    fun addSuffix(prefix: String, suffix: String?): String {
        return if (suffix == null) prefix
        else "$prefix$suffix"
    }

    @JvmStatic
    fun addPrefix(prefix: String?, mid: String, suffix: String): String {
        return if (prefix == null) suffix
        else "$prefix$mid$suffix"
    }

    @JvmStatic
    fun addSuffix(prefix: String, mid: String, suffix: String?): String {
        return if (suffix == null) prefix
        else "$prefix$mid$suffix"
    }

    @JvmStatic
    fun filterAlphaNumeric(str: String): String {
        return str.filter { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' }
    }

    // made external, so it avoids useless allocations
    @JvmStatic
    private fun append(i: Int, lastI: Int, value: String, data: StringBuilder, str: String): Int {
        if (i > lastI) data.append(value, lastI, i)
        data.append(str)
        return i + 1
    }

    @JvmStatic
    fun writeEscaped(value: String, data: StringBuilder, enclosing: Char) {
        var i = 0
        var lastI = 0
        while (i < value.length) {
            val str = when (value[i]) {
                '\\' -> "\\\\"
                '\t' -> "\\t"
                '\r' -> "\\r"
                '\n' -> "\\n"
                '"' -> "\\\""
                '\b' -> "\\b"
                12.toChar() -> "\\f"
                else -> null // nothing
            }
            if (str != null) {
                lastI = append(i, lastI, value, data, str)
            }
            i++
        }
        if (i > lastI) data.append(value, lastI, i)
    }

    const val DONT_ESCAPE = 0.toChar()

    @JvmStatic
    fun getEscapeChar(char: Char, quotes: Char): Char {
        return when (char) {
            '\\' -> char
            quotes -> char
            '"', '\'' -> DONT_ESCAPE
            '\t' -> 't'
            '\r' -> 'r'
            '\n' -> 'n'
            '\b' -> 'b'
            12.toChar() -> 'f'
            else -> DONT_ESCAPE
        }
    }

    @JvmStatic
    fun writeEscaped(value: String, data: Writer, quotes: Char) {
        for (index in value.indices) {
            val char = value[index]
            val esc = getEscapeChar(char, quotes)
            if (esc == DONT_ESCAPE) {
                data.append(char)
            } else {
                data.append('\\')
                data.append(esc)
            }
        }
    }

    @JvmStatic
    fun writeEscaped(value: String, data: JsonWriterBase, quotes: Char) {
        for (index in value.indices) {
            val char = value[index]
            val esc = getEscapeChar(char, quotes)
            if (esc == DONT_ESCAPE) {
                data.append(char)
            } else {
                data.append('\\')
                data.append(esc)
            }
        }
    }

    /**
     * allocation free isBlank()
     * */
    @JvmStatic
    fun CharSequence?.isBlank2(): Boolean {
        if (this == null) return true
        for (index in 0 until length + 0) {
            if (!this[index].isBlank()) {
                return false
            }
        }
        return true
    }

    @JvmStatic
    fun Char.isBlank(): Boolean {
        return when (this) {
            '\u0009', in '\u000a'..'\u000d',
            '\u0020', '\u0085', '\u00a0',
            '\u1680', '\u180e',
            in '\u2000'..'\u200D',
            '\u2028', '\u2029', '\u202f',
            '\u205f', '\u2060', '\u3000',
            '\ufeff' -> true
            else -> false
        }
    }

    @JvmStatic
    fun CharSequence?.isNotBlank2(): Boolean {
        return !isBlank2()
    }

    /**
     * allocation free ifBlank()
     * */
    @JvmStatic
    fun <V : CharSequence> V?.ifBlank2(other: V): V {
        return if (this == null || isBlank2()) other else this
    }

    @JvmStatic
    fun isNumber(s: String): Boolean = s.toDoubleOrNull() != null

    @JvmStatic
    fun isName(s: String): Boolean {
        if (s.isEmpty()) return false
        val s0 = s[0]
        return if (s0 in 'A'..'Z' || s0 in 'a'..'z') {
            for (i in 1 until s.length) {
                when (s[i]) {
                    in 'A'..'Z', in 'a'..'z', in '0'..'9', '_' -> {
                    }
                    else -> return false
                }
            }
            true
        } else false
    }

    @JvmStatic
    fun isArray(s: String): Boolean {
        // to do only names and such are allowed, only commas, and only valid numbers...
        // very complex -> take a guess
        return s.startsWith('[') && s.endsWith(']')
    }

    @JvmStatic
    fun countLines(str: String): Int {
        var ctr = 1
        for (i in str.indices) {
            if (str[i] == '\n') ctr++
        }
        return ctr
    }

    @JvmStatic
    fun CharSequence.toInt(): Int = toLong().toInt()

    @JvmStatic
    fun CharSequence.toInt(base: Int): Int = toLong(base).toInt()

    @JvmStatic
    fun CharSequence.toInt(i0: Int, i1: Int): Int = toLong(i0, i1).toInt()

    @JvmStatic
    fun CharSequence.toLong(): Long {
        return toLong(10, 0, length)
    }

    @JvmStatic
    fun CharSequence.toLong(base: Int): Long {
        return toLong(base, 0, length)
    }

    @JvmStatic
    fun CharSequence.toLong(i0: Int, i1: Int): Long {
        return toLong(10, i0, i1)
    }

    @JvmStatic
    fun CharSequence.toLong(base: Int, i0: Int, i1: Int): Long {
        if (i0 >= i1) return 0L
        val c0 = this[i0]
        val ix = if (c0 == '+' || c0 == '-') i0 + 1 else i0
        var number = 0L
        for (i in ix until i1) {
            val digit = Character.digit(this[i], base)
            number = base * number - digit
        }
        return if (c0 == '-') number else -number
    }

    @JvmStatic
    fun CharSequence.toFloat(defaultValue: Float = 0f): Float =
        toDouble(defaultValue.toDouble()).toFloat()

    @JvmStatic
    fun CharSequence.toFloat(i0: Int, i1: Int, defaultValue: Float = 0f): Float =
        toDouble(i0, i1, defaultValue.toDouble()).toFloat()

    @JvmStatic
    fun CharSequence.toDouble(defaultValue: Double = 0.0): Double =
        toDouble(0, length, defaultValue)

    @JvmStatic
    fun CharSequence.toDouble(i0: Int, i1: Int, defaultValue: Double = 0.0): Double {
        // support NaN, +/-Infinity
        if (startsWith("NaN", i0)) return Double.NaN
        if (startsWith("Inf", i0) || startsWith("+Inf", i0)) return Double.POSITIVE_INFINITY
        if (startsWith("-Inf", i0)) return Double.NEGATIVE_INFINITY
        var sign = 1.0
        var number = 0.0
        when (val char = this[i0]) {
            '-' -> sign = -1.0
            in '0'..'9' -> number = char.code - 48.0
            '.' -> return readAfterDot(i0 + 1, i1, sign, 0.0)
            else -> return defaultValue // parse error
        }
        for (i in i0 + 1 until i1) {
            when (val char = this[i]) {
                in '0'..'9' -> number = number * 10 + char.code - 48
                '.' -> return readAfterDot(i + 1, i1, sign, number)
                'e', 'E' -> return sign * number * 10.0.pow(toInt(i + 1, length))
                else -> return defaultValue // parse error
            }
        }
        return sign * number
    }

    @JvmStatic
    private fun CharSequence.readAfterDot(i0: Int, i1: Int, sign: Double, number: Double): Double {
        var exponent = 0.1
        var fraction = 0.0
        for (i in i0 until i1) {
            when (val char2 = this[i]) {
                in '0'..'9' -> {
                    fraction += exponent * (char2.code - 48)
                    exponent *= 0.1
                }
                'e', 'E' -> return sign * (number + fraction) * 10.0.pow(toInt(i + 1, length))
                else -> break // :/
            }
        }
        return sign * (number + fraction)
    }

    @JvmStatic
    fun String.iff(condition: Boolean): String {
        return if (condition) this else ""
    }


    @JvmStatic
    fun CharSequence.titlecase(): CharSequence {
        return if (isNotEmpty() && first().isLowerCase()) {
            first().uppercase() + substring(1)
        } else this
    }

    @JvmStatic
    fun CharSequence.indexOf2(query: Char, index: Int = 0): Int {
        val i = indexOf(query, index)
        return if (i < 0) length else i
    }

    @JvmStatic
    @Suppress("unused")
    fun CharSequence.indexOf2(query: String, index: Int = 0): Int {
        val i = indexOf(query, index)
        return if (i < 0) length else i
    }

    // S looks off -> probably incorrect (check out Courier New in Rem's Studio)
    //  -> their unicodes aren't continous, and they have varying meanings -> it's the best we can do
    //     without using an actual font
    @Suppress("SpellCheckingInspection")
    private const val SMALL_CAPS_ALPHABET = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡxʏᴢ"

    @JvmStatic
    @Suppress("unused")
    fun Char.smallCaps(): Char {
        return if (this in 'a'..'z') SMALL_CAPS_ALPHABET[this.code - 'a'.code] else this
    }

    @JvmStatic
    fun Char.normalCaps(): Char {
        val idx = SMALL_CAPS_ALPHABET.indexOf(this)
        return if (idx >= 0) ('a' + idx) else this
    }

    @JvmStatic
    fun CharSequence.smallCaps(): String {
        val result = StringBuilder(length)
        for (c in this) result.append(c.smallCaps())
        return result.toString()
    }

    /**
     * removes/undo-s smallCaps()
     * */
    @JvmStatic
    @Suppress("unused")
    fun CharSequence.normalCaps(): String {
        val result = StringBuilder(length)
        for (c in this) result.append(c.normalCaps())
        return result.toString()
    }

    private fun String.isTitleCase(): Boolean {
        if (isEmpty()) return true
        if (this[0] !in 'A'..'Z') return false
        for (i in 1 until length) {
            when (this[i]) {
                in 'A'..'Z', in 'a'..'z', in '0'..'9' -> {}
                ' ' -> if (i + 1 >= length || this[i + 1] !in 'A'..'Z') return false
            }
        }
        return true
    }

    @JvmStatic
    // by polyGeneLubricants, https://stackoverflow.com/a/2560017/4979303
    fun String.splitCamelCase(titlecase: Boolean = false): String {
        if (titlecase && isTitleCase()) return this
        return replace('_', ' ') // snake case replacements
            .splitCamelCaseI(titlecase)
            .replace("    ", " ")
            .replace("  ", " ")
            .replace("  ", " ")
    }

    @JvmStatic
    private fun String.splitCamelCaseI(titlecase: Boolean): String {
        if (isEmpty()) return this
        val builder = StringBuilder(length + 4)
        builder.append(if (titlecase) this[0].uppercase() else this[0])
        for (i in 1 until length) {
            val c = this[i]
            if (c in 'A'..'Z') {
                if (this[i - 1] in 'a'..'z') {
                    // MeshComponent -> Mesh_Component
                    builder.append(' ')
                } else if (i + 1 < length && this[i - 1] in 'A'..'Z' && this[i + 1] in 'a'..'z') {
                    // SDFMesh -> SDF Mesh
                    builder.append(' ')
                }
            }
            builder.append(c)
        }
        return builder.toString()
    }

    @JvmStatic
    fun String.camelCaseToTitle() =
        splitCamelCase(true)

    /**
     * converts enum value names into proper titlecase strings
     * */
    @JvmStatic
    @Suppress("unused")
    fun String.upperSnakeCaseToTitle() =
        lowercase().split('_').joinToString(" ") { it.titlecase() }

    @JvmStatic
    fun setNumber(pos: Int, num: Int, dst: CharArray) {
        if (num in 0..99) {
            dst[pos] = (num / 10 + 48).toChar()
            dst[pos + 1] = (num % 10 + 48).toChar()
        } else {
            dst[pos] = 'x'
            dst[pos + 1] = 'x'
        }
    }

    @JvmStatic
    fun CharSequence.shorten(maxLength: Int, cutLines: Boolean = true): CharSequence {
        val str = if (length > maxLength) substring(0, maxLength - 3) + "..." else this
        if (cutLines && '\n' in this) return str.toString().replace("\n", "\\n")
        return str
    }

    @JvmStatic
    fun String.shorten2Way(maxLength: Int, cutLines: Boolean = true): String {
        val halfLength = maxLength / 2
        var str = if (length > maxLength) substring(0, halfLength - 2) + "..." + substring(1 + length - halfLength)
        else this
        if (cutLines && '\n' in this) str = str.replace("\n", "\\n")
        return str
    }

    @JvmStatic
    @Suppress("unused")
    fun CharSequence.levenshtein(other: CharSequence, ignoreCase: Boolean) =
        distance(other, ignoreCase)

    /**
     * Levenshtein distance / edit distance,
     * O(|this| * |other|), so quite expensive for long strings
     * returns the number of changes, which would need to happen to change one string to the other
     * operations: change character, add character, remove character
     * distance >= abs(|this|-|other|)
     *
     * if you heavily rely on this method, write me, and I'll cache its dynamic allocations
     * */
    @JvmStatic
    fun CharSequence.distance(other: CharSequence, ignoreCase: Boolean = false): Int {
        if (this == other) return 0
        val sx = this.length + 1
        val sy = other.length + 1
        if (sx <= 1 || sy <= 1) return abs(sx - sy)
        if (sx <= 2 && sy <= 2) return 1
        // switching both sides may be valuable
        if (sx > sy + 5) return other.distance(this, ignoreCase)
        // create cache
        val dist = IntArray(sx * Maths.max(sy, 3))
        for (x in 1 until sx) dist[x] = x
        for (y in 1 until sy) {
            var i2 = (y % 3) * sx
            dist[i2++] = y
            var i1 = ((y + 2) % 3) * sx
            var i0 = ((y + 1) % 3) * sx - 1
            val prev1 = other[y - 1]
            for (i in 1 until sx) {
                val prev0 = this[i - 1]
                dist[i2] = when {
                    prev0.equals(prev1, ignoreCase) -> dist[i1]
                    i > 1 && y > 1 &&
                            prev0.equals(other[y - 2], ignoreCase) &&
                            prev1.equals(this[i - 2], ignoreCase) ->
                        Maths.min(dist[i0], dist[i2 - 1], dist[i1 + 1]) + 1
                    else -> Maths.min(dist[i1], dist[i2 - 1], dist[i1 + 1]) + 1
                }
                i0++
                i1++
                i2++
            }
        }
        val yi = (((sy + 2) % 3) + 1)
        return dist[sx * yi - 1]
    }

    // by polyGeneLubricants, https://stackoverflow.com/a/2560017/4979303
    // private val splitCamelCaseRegex = Regex("(?<=[A-Z])(?=[A-Z][a-z])|(?<=[^A-Z])(?=[A-Z])|(?<=[A-Za-z])(?=[^A-Za-z])")

    @JvmStatic
    fun spaces(ctr: Int): String = " ".repeat(ctr)

    @JvmStatic
    fun tabs(ctr: Int): String = "\t".repeat(ctr)

    @JvmStatic
    fun StringBuilder.removeRange2(i0: Int, i1: Int): StringBuilder {
        val len = i1 - i0
        if (len <= 0) return this
        for (i in i1 until length) {
            this[i - len] = this[i]
        }
        setLength(length - len)
        return this
    }

    @JvmStatic
    fun append(a: String?, separator: String, b: String?): String? {
        return when {
            a.isNotBlank2() && b.isNotBlank2() -> "$a$separator$b"
            a.isNotBlank2() -> a
            else -> b
        }
    }
}