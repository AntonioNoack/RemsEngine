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

}