package me.anno.ui.editor.code.tokenizer

import me.anno.utils.structures.lists.Lists.any2
import me.anno.utils.types.Booleans.toInt

// todo do we have a style which highlights the colons and dashes?
class YamlTokenizer(var customKeys: Set<String> = emptySet()) : LanguageTokenizer {

    companion object {
        @JvmStatic
        private val keywords0 = "true,false,yes,no,on,off,null,~"

        @JvmStatic
        private val keywords = keywords0.split(',')

        @JvmStatic
        private val indentTokens = LanguageTokenizer.fullMatch(listOf("-", "?", ":"))

        @JvmStatic
        private val dedentTokens = LanguageTokenizer.fullMatch(emptyList<String>())

        @JvmStatic
        private val dedentPartial = LanguageTokenizer.partialMatch(emptyList<String>())
    }

    fun string(quote: Char): (Stream, State) -> TokenType {
        return { stream, state ->
            var escaped = false
            while (true) {
                val ch = stream.next()
                if (ch.code == 0) break
                if (ch == quote && !escaped) break
                escaped = !escaped && (quote == '"' && ch == '\\')
            }
            if (!escaped) state.next = ::normal
            TokenType.STRING
        }
    }

    fun normal(stream: Stream, state: State): TokenType {
        val ch = stream.next()

        // comment
        if (ch == '#') {
            stream.eatWhile { it != '\n' }
            return TokenType.COMMENT
        }

        // quoted string
        if (ch == '"' || ch == '\'') {
            state.next = string(ch)
            return state.next(stream, state)
        }

        // number
        if (ch in '0' .. '9' || (ch == '-' && stream.peek().firstOrNull()?.isDigit() == true)) {
            stream.eatWhile { it in '0' .. '9' || it in ".eE+-x" }
            return TokenType.NUMBER
        }

        // punctuation / operators
        return when (ch) {
            '-', ':', '?' -> TokenType.OPERATOR
            '{', '}', '[', ']' -> TokenType.BRACKET
            ',' -> TokenType.PUNCTUATION
            else -> {
                if (ch.isLetter() || ch == '_' || ch == '~') {
                    stream.eatWhile { it.isLetterOrDigit() || it in "-_." }
                    return TokenType.VARIABLE
                }
                TokenType.UNKNOWN
            }
        }
    }

    override fun getStartState(): State {
        return State(0, 0, ::normal)
    }

    override fun getToken(stream: Stream, state: State): TokenType {
        if (stream.eatSpace()) return TokenType.UNKNOWN
        var style = state.next(stream, state)
        val word = stream.current().toString()

        if (style == TokenType.VARIABLE) {
            when {
                keywords.any2 { keyword -> keyword.equals(word, true) } ->
                    style = TokenType.KEYWORD
                word in customKeys ->
                    style = TokenType.VARIABLE
            }
        }

        if (style != TokenType.COMMENT && style != TokenType.STRING) {
            if (indentTokens.contains(word)) state.indentDepth++
            if (dedentTokens.contains(word)) state.indentDepth--
        }

        return style
    }

    override fun getIndentation(state: State, indentUnit: Int, textAfter: CharSequence): Int {
        val isClosing = dedentPartial(textAfter)
        return state.indent0 + indentUnit * (state.indentDepth - isClosing.toInt())
    }

    override val lineComment: String = "#"
    override val blockCommentStart: String? = null
    override val blockCommentEnd: String? = null

    override val brackets: List<Pair<String, String>> = listOf(
        "{" to "}",
        "[" to "]"
    )
}