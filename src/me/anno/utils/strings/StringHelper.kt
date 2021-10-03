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
    fun String.splitCamelCase(): String {
        return replace(
            splitCamelCaseRegex,
            " "
        )
    }

    fun String.camelCaseToTitle(): String {
        return splitCamelCase().titlecase()
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
        var str = if (length > maxLength) substring(0, maxLength - 3) + "..." else this
        if (cutLines && '\n' in this) str = str.replace("\n", "\\n")
        return str
    }

    fun String.shorten2Way(maxLength: Int, cutLines: Boolean = true): String {
        val halfLength = maxLength / 2
        var str = if (length > maxLength) substring(0, halfLength - 2) + "..." + substring(1 + length - halfLength)
        else this
        if (cutLines && '\n' in this) str = str.replace("\n", "\\n")
        return str
    }

    // by polyGeneLubricants, https://stackoverflow.com/a/2560017/4979303
    private val splitCamelCaseRegex = Regex("(?<=[A-Z])(?=[A-Z][a-z])|(?<=[^A-Z])(?=[A-Z])|(?<=[A-Za-z])(?=[^A-Za-z])")

}