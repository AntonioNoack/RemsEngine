package me.anno.ui.editor.code.tokenizer

import me.anno.utils.types.Booleans.toInt

class LuaTokenizer(var customVariables: Set<String> = emptySet()) : LanguageTokenizer {

    companion object {
        @JvmStatic
        private val builtIns = LanguageTokenizer.fullMatch(
            listOf(
                "_G", "_VERSION",
                "assert", "collectgarbage", "dofile",
                "error", "getfenv", "getmetatable", "ipairs",
                "load", "loadfile", "loadstring",
                "module", "next", "pairs", "pcall",
                "print", "rawequal", "rawget", "rawset",
                "require", "select", "setfenv", "setmetatable",
                "tonumber", "tostring", "type", "unpack", "xpcall",

                "coroutine.create", "coroutine.resume", "coroutine.running",
                "coroutine.status", "coroutine.wrap", "coroutine.yield",

                "debug.debug", "debug.getfenv", "debug.gethook", "debug.getinfo",
                "debug.getlocal", "debug.getmetatable", "debug.getregistry", "debug.getupvalue",
                "debug.setfenv", "debug.sethook", "debug.setlocal", "debug.setmetatable",
                "debug.setupvalue", "debug.traceback",

                "close", "flush", "lines", "read", "seek", "setvbuf", "write",

                "io.close", "io.flush", "io.input", "io.lines", "io.open",
                "io.output", "io.popen", "io.read", "io.stderr", "io.stdin",
                "io.stdout", "io.tmpfile", "io.type", "io.write",

                "math.abs", "math.acos", "math.asin", "math.atan",
                "math.atan2", "math.ceil", "math.cos", "math.cosh",
                "math.deg", "math.exp", "math.floor", "math.fmod",
                "math.frexp", "math.huge", "math.ldexp", "math.log",
                "math.log10", "math.max", "math.min", "math.modf", "math.pi",
                "math.pow", "math.rad", "math.random", "math.randomseed",
                "math.sin", "math.sinh", "math.sqrt", "math.tan", "math.tanh",

                "os.clock", "os.date", "os.difftime", "os.execute",
                "os.exit", "os.getenv", "os.remove", "os.rename",
                "os.setlocale", "os.time", "os.tmpname",

                "package.cpath", "package.loaded", "package.loaders", "package.loadlib",
                "package.path", "package.preload", "package.seeall",

                "string.byte", "string.char", "string.dump", "string.find",
                "string.format", "string.gmatch", "string.gsub", "string.len",
                "string.lower", "string.match", "string.rep", "string.reverse",
                "string.sub", "string.upper",

                "table.concat", "table.insert", "table.maxn",
                "table.remove", "table.sort"
            )
        )

        @JvmStatic
        private val keywords = LanguageTokenizer.fullMatch(
            listOf(
                "and", "break", "elseif", "false", "nil", "not", "or", "return",
                "true", "function", "end", "if", "then", "else", "do",
                "while", "repeat", "until", "for", "in", "local"
            )
        )

        @JvmStatic
        private val indentTokens = LanguageTokenizer.fullMatch(listOf("function", "if", "repeat", "do", "\\(", "{"))

        @JvmStatic
        private val dedentTokens = LanguageTokenizer.fullMatch(listOf("end", "until", "\\)", "}"))

        @JvmStatic
        private val dedentPartial = LanguageTokenizer.partialMatch(listOf("end", "until", "\\)", "}", "else", "elseif"))
    }

    private fun readBracket(stream: Stream): Int {
        var level = 0
        while (stream.eat('=')) ++level
        stream.eat('[')
        return level
    }

    private fun bracketed(level: Int, style: TokenType): (Stream, State) -> TokenType {
        return { stream, state ->
            var curLevel = -1
            loop@ while (true) {
                val ch = stream.next()
                if (ch.code == 0) break@loop
                when {
                    curLevel == -1 -> {
                        if (ch == ']') curLevel = 0
                    }
                    ch == '=' -> ++curLevel
                    ch == ']' && curLevel == level -> {
                        state.next = ::normal
                        break@loop
                    }
                    else -> curLevel = -1
                }
            }
            style
        }
    }

    fun string(type: Char): (Stream, State) -> TokenType {
        return { stream, state ->
            var escaped = false
            while (true) {
                val ch = stream.next()
                if (ch.code == 0) break
                if (ch == type && !escaped) break
                escaped = !escaped && ch == '\\'
            }
            if (!escaped) state.next = ::normal
            TokenType.STRING
        }
    }

    fun normal(stream: Stream, state: State): TokenType {
        val ch = stream.next()
        if (ch == '-' && stream.eat('-')) {
            // block comment
            if (stream.eat('[') && stream.eat('[')) {
                state.next = bracketed(readBracket(stream), TokenType.COMMENT)
                return state.next(stream, state)
            }
            // line comment
            stream.eatWhile { it != '\n' }
            return TokenType.COMMENT
        }
        if (ch == '"' || ch == '\'') {
            state.next = string(ch)
            return state.next(stream, state)
        }
        val peek = stream.peek()
        val peek1 = peek.firstOrNull()
        if (ch == '[' && peek.length == 1 && (peek1 == '[' || peek1 == '=')) {
            state.next = bracketed(readBracket(stream), TokenType.STRING)
            return state.next(stream, state)
        }
        when (ch) {
            in '0'..'9' -> {
                stream.eatWhile { it in '0'..'9' || it in "eE.+-" }
                return TokenType.NUMBER
            }
            in 'A'..'Z', in 'a'..'z' -> {
                stream.eatWhile { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it in "-_" }
                return TokenType.VARIABLE
            }
            '(', ')' -> return TokenType.BRACKET
            '.', ',' -> return TokenType.PUNCTUATION
            '=', '*', '+', '-', '/' -> return TokenType.OPERATOR
        }
        return TokenType.UNKNOWN
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
                builtIns.contains(word) -> style = TokenType.BUILTIN
                customVariables.contains(word) -> style = TokenType.VARIABLE2
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

    // override val electricInput: Regex = Regex("/^\\s*(?:end|until|else|\\)|\\})\$/") // patterns, that change the line indentation (?)
    override val lineComment: String = "--"
    override val blockCommentStart: String = "--[["
    override val blockCommentEnd: String = "]]"
}