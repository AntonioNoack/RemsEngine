package me.anno.ui.editor.code.codemirror

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

}