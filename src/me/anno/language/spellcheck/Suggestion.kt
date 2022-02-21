package me.anno.language.spellcheck

data class Suggestion(
    val start: Int,
    val end: Int,
    val message: String,
    val shortMessage: String,
    val improvements: List<String>
) {

    val clearMessage = message
        .replace("<suggestion>", "")
        .replace("</suggestion>", "")

    operator fun contains(index: Int) = index in start until end
    fun withOffset(offset: Int) = Suggestion(start + offset, end + offset, message, shortMessage, improvements)

}