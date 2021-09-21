package me.anno.utils.strings

object StringHelper {

    fun String.titlecase(): String {
        if (isEmpty()) return this
        return if (first().isLowerCase()) {
            first().uppercase() + substring(1)
        } else this
    }

    fun String.indexOf2(query: Char, index: Int): Int {
        val i = indexOf(query, index)
        return if (i < 0) length else i
    }

    fun String.indexOf2(query: String, index: Int): Int {
        val i = indexOf(query, index)
        return if (i < 0) length else i
    }

    // by polyGeneLubricants, https://stackoverflow.com/a/2560017/4979303
    fun splitCamelCase(s: String): String {
        return s.replace(
            String.format(
                "%s|%s|%s",
                "(?<=[A-Z])(?=[A-Z][a-z])",
                "(?<=[^A-Z])(?=[A-Z])",
                "(?<=[A-Za-z])(?=[^A-Za-z])"
            ).toRegex(),
            " "
        )
    }

    fun setNumber(pos: Int, num: Int, dst: CharArray) {
        if (num in 0..99) {
            dst[pos] = (num / 10 + 48).toChar()
            dst[pos + 1] = (num % 10 + 48).toChar()
        } else {
            dst[pos] = 'x'
            dst[pos + 1] = 'x'
        }
    }

    fun String.shorten(maxLength: Int, cutLines: Boolean = true): String {
        return if (cutLines && '\n' in this) {
            (if (length > maxLength) substring(0, maxLength - 3) + "..."
            else this).replace("\n", "\\n")
        } else {
            if (length > maxLength) substring(0, maxLength - 3) + "..."
            else this
        }
    }

}