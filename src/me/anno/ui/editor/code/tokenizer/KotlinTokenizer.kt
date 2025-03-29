package me.anno.ui.editor.code.tokenizer

import me.anno.utils.types.Booleans.toInt

object KotlinTokenizer : LanguageTokenizer {

    @JvmStatic
    private val keywords = LanguageTokenizer.fullMatch(
        ("public,protected,private,fun,class,var,val,return,if,else,try,catch,while,do,when,data,interface," +
                "true,false,;,this,import,package,object,companion").split(',')
    )

    @JvmStatic
    private val indentTokens = LanguageTokenizer.fullMatch("{")

    @JvmStatic
    private val dedentTokens = LanguageTokenizer.fullMatch("}")

    @JvmStatic
    private val dedentPartial = LanguageTokenizer.partialMatch("}") // ?

    @Suppress("SameReturnValue")
    fun readString(stream: Stream, state: State): TokenType {
        var escaped = false
        search@ while (true) {
            val ch = stream.next()
            if (ch.code == 0) break
            if (ch == '"' && !escaped) break
            if (ch == '$' && !escaped) {
                val next = stream.peek().firstOrNull() ?: ' '
                if (next == '{') {
                    // todo string expression
                } else if (next in 'A'..'Z' || next in 'a'..'z' || next in "_") {
                    // string interpolation
                    state.next = ::readVariableInString
                    return TokenType.STRING
                } // else ignore
            }
            escaped = !escaped && ch == '\\'
        }
        if (!escaped) state.next = ::normal
        return TokenType.STRING
    }

    fun readChar(stream: Stream, state: State): TokenType {
        var escaped = false
        search@ while (true) {
            val ch = stream.next()
            if (ch.code == 0) break
            if (ch == ' ' && !escaped) break
            escaped = !escaped && ch == '\\'
        }
        if (!escaped) state.next = ::normal
        return TokenType.STRING
    }

    fun readVariableInString(stream: Stream, state: State): TokenType {
        string@ while (true) {
            when (stream.peek().firstOrNull() ?: ' ') {
                in 'A'..'Z', in 'a'..'z', in '0'..'9', in "_" -> {
                    stream.next()
                } // continue
                else -> {
                    stream.next()
                    break@string
                }
            }
        }
        state.next = ::readString
        return TokenType.VARIABLE
    }

    fun readBlockComment(stream: Stream) {
        var ch = '*'
        while (true) {
            if (ch == '/' && stream.eat('*')) {
                readBlockComment(stream) // recursive block comments :3
            } else if (ch == '*' && stream.eat('/')) {
                break
            }
            ch = stream.next()
            if (ch.code == 0) break
        }
    }

    fun normal(stream: Stream, state: State): TokenType {
        val ch = stream.next()
        return when {
            ch == '/' && stream.eat('*') -> {
                // block comment
                readBlockComment(stream)
                TokenType.COMMENT
            }
            ch == '/' && stream.eat('/') -> {
                // line comment
                stream.eatWhile { it != '\n' }
                TokenType.COMMENT
            }
            else -> when (ch) {
                '"' -> readString(stream, state)
                '\'' -> readChar(stream, state)
                in '0'..'9' -> {
                    stream.eatWhile { it in '0'..'9' || it in "eE.+-'" }
                    TokenType.NUMBER
                }
                in 'A'..'Z', in 'a'..'z' -> {
                    stream.eatWhile { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it in "-_" }
                    TokenType.VARIABLE
                }
                '(', ')' -> TokenType.BRACKET
                '.', ',' -> TokenType.PUNCTUATION
                '=', '*', '+', '-', '/' -> TokenType.OPERATOR
                else -> TokenType.UNKNOWN
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
                keywords.contains(word) -> style = TokenType.KEYWORD
                // builtIns.contains(word) -> style = TokenType.BUILTIN
            }
        }
        if (style != TokenType.COMMENT && style != TokenType.STRING) {
            when {
                indentTokens.contains(word) -> state.indentDepth++
                dedentTokens.contains(word) -> state.indentDepth--
            }
        }
        return style
    }

    override fun getIndentation(state: State, indentUnit: Int, textAfter: CharSequence): Int {
        val isClosing = dedentPartial(textAfter)
        return state.indent0 + indentUnit * (state.indentDepth - isClosing.toInt())
    }

    override val lineComment: String = "//"
    override val blockCommentStart: String = "/*"
    override val blockCommentEnd: String = "*/"

    override val brackets: List<Pair<String, String>>
        get() = listOf(
            "(" to ")",
            "[" to "]",
            "{" to "}",
            "\"\"\"" to "\"\"\""
        )
}