package me.anno.utils.strings

import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import kotlin.math.abs

object StringHelper {

    @JvmStatic
    fun CharSequence.titlecase(): CharSequence {
        if (isEmpty()) return this
        return if (first().isLowerCase()) {
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

    // todo S looks off -> probably incorrect (check out Courier New in Rem's Studio)
    private const val smallCapsMagic = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡxʏᴢ"

    @JvmStatic
    private val smallCapsMagicMin = smallCapsMagic.minOrNull()!!

    @JvmStatic
    private val smallCapsMagicMax = smallCapsMagic.maxOrNull()!!

    @JvmStatic
    @Suppress("unused")
    fun Char.smallCaps(): Char {
        return if (this in 'a'..'z') smallCapsMagic[this.code - 'a'.code] else this
    }

    @JvmStatic
    fun Char.normalCaps(): Char {
        return if (this in smallCapsMagicMin..smallCapsMagicMax) {
            val idx = smallCapsMagic.indexOf(this)
            if (idx >= 0) ('a' + idx) else this
        } else this
    }

    @JvmStatic
    fun CharSequence.smallCaps(): String {
        val result = StringBuffer(length)
        for (c in this) result.append(c.smallCaps())
        return result.toString()
    }

    /**
     * removes/undo-s smallCaps()
     * */
    @JvmStatic
    @Suppress("unused")
    fun CharSequence.normalCaps(): String {
        val result = StringBuffer(length)
        for (c in this) result.append(c.normalCaps())
        return result.toString()
    }

    @JvmStatic
    // by polyGeneLubricants, https://stackoverflow.com/a/2560017/4979303
    fun String.splitCamelCase(titlecase: Boolean = false): String {
        return replace('_', ' ') // snake case replacements
            .splitCamelCaseI(titlecase)
            // .replace(splitCamelCaseRegex, " ") // camelCase -> camel Case
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
            if (this[i - 1] in 'a'..'z' && c in 'A'..'Z') {
                builder.append(' ')
            }
            builder.append(c)
        }
        return builder.toString()
    }

    @JvmStatic
    fun String.camelCaseToTitle() =
        splitCamelCase(true)

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
        val dist = IntArray(sx * max(sy, 3))
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
                        min(dist[i0], dist[i2 - 1], dist[i1 + 1]) + 1
                    else -> min(dist[i1], dist[i2 - 1], dist[i1 + 1]) + 1
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

}