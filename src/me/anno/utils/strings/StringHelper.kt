package me.anno.utils.strings

import me.anno.maths.Maths.min
import kotlin.math.abs

object StringHelper {

    @JvmStatic
    fun String.titlecase(): String {
        if (isEmpty()) return this
        return if (first().isLowerCase()) {
            first().uppercase() + substring(1)
        } else this
    }

    @JvmStatic
    fun String.indexOf2(query: Char, index: Int = 0): Int {
        val i = indexOf(query, index)
        return if (i < 0) length else i
    }

    @JvmStatic
    fun String.indexOf2(query: String, index: Int = 0): Int {
        val i = indexOf(query, index)
        return if (i < 0) length else i
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
    fun String.levenshtein(other: String, ignoreCase: Boolean) =
        distance(other, ignoreCase)

    /**
     * Levenshtein distance / edit distance,
     * O(|this| * |other|), so quite expensive for long strings
     * returns the number of changes, which would need to happen to change one string to the other
     * operations: change character, add character, remove character
     * distance >= abs(|this|-|other|)
     * */
    @JvmStatic
    fun String.distance(other: String, ignoreCase: Boolean = false): Int {
        if (this == other) return 0
        val m = this.length
        val n = other.length
        if (m == 0 || n == 0) return abs(m - n)
        if (m <= 1 && n <= 1) return 1
        val stride = m + 1
        val d = IntArray(stride * (n + 1))
        for (i in 1..m) d[i] = i
        for (j in 1..n) d[j * stride] = j
        for (j in 1..n) {
            var k = j * stride + 1
            val cj = other[j - 1]
            for (i in 1..m) {
                val ci = this[i - 1]
                d[k] = when {
                    ci.equals(cj, ignoreCase) -> d[k - 1 - stride]
                    i > 1 && j > 1 &&
                            ci.equals(other[j - 2], ignoreCase) &&
                            cj.equals(this[i - 2], ignoreCase) ->
                        min(d[k - 2 - 2 * stride], d[k - 1], d[k - stride]) + 1
                    else ->
                        min(d[k - stride - 1], min(d[k - 1], d[k - stride])) + 1
                }
                k++
            }
        }
        return d.last()
    }

    // by polyGeneLubricants, https://stackoverflow.com/a/2560017/4979303
    // private val splitCamelCaseRegex = Regex("(?<=[A-Z])(?=[A-Z][a-z])|(?<=[^A-Z])(?=[A-Z])|(?<=[A-Za-z])(?=[^A-Za-z])")

}