package me.anno.ui.editor.code.codemirror

// this is a language-coloring engine inspired by CodeMirror (a JavaScript library)
// it is based on a state machine, and states are given different colors
interface Language {

    fun getStartState(): State

    fun getToken(stream: Stream, state: State): TokenType

    fun getIndentation(state: State, indentUnit: Int, textAfter: CharSequence): Int

    val electricInput: Regex
    val lineComment: String
    val blockCommentStart: String
    val blockCommentEnd: String

}