package me.anno.ui.editor.code.tokenizer

/**
 * language-coloring engine inspired by CodeMirror (a JavaScript library);
 * based on a state machine, and states are given different colors
 * */
interface LanguageTokenizer {

    fun getStartState(): State
    fun getToken(stream: Stream, state: State): TokenType
    fun getIndentation(state: State, indentUnit: Int, textAfter: CharSequence): Int

    val lineComment: String
    val blockCommentStart: String
    val blockCommentEnd: String

    companion object {
        @JvmStatic
        fun fullMatch(pattern: String): Collection<String> {
            return listOf(pattern)
        }

        @JvmStatic
        fun fullMatch(list: List<String>): Collection<String> {
            return if (list.size < 16) list else list.toHashSet()
        }

        @JvmStatic
        fun partialMatch(pattern: String): (CharSequence) -> Boolean {
            return { tested -> pattern.startsWith(tested) }
        }

        @JvmStatic
        fun partialMatch(patterns: List<String>): (CharSequence) -> Boolean {
            return { tested -> patterns.any { pattern -> pattern.startsWith(tested) } }
        }
    }
}