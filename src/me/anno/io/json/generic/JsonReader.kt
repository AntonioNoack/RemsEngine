package me.anno.io.json.generic

import me.anno.io.xml.ComparableStringBuilder
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFail
import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.Strings.joinChars
import java.io.ByteArrayInputStream
import java.io.EOFException
import java.io.InputStream
import java.io.Reader

/**
 * to avoid the import of FasterXML (17 MB) or similar, we create our own light-weight solution to reading JSON files;
 * this has no reflection support, so it is safe to use (except for OutOfMemoryError), but you have to implement the mapping yourself
 * */
open class JsonReader(val data: Reader) {

    companion object {
        fun isHex(c: Char): Boolean {
            return c in '0'..'9' || c in 'A'..'F' || c in 'a'..'f'
        }

        fun toHex(c: Char): Int {
            return when (c) {
                in '0'..'9' -> c.code - 48
                in 'a'..'f' -> c.code - 'a'.code + 10
                else -> c.code - 'A'.code + 10
            }
        }

        fun toHex(c: Char, d: Char): Int {
            return toHex(c).shl(4) or toHex(d)
        }

        fun toHex(a: Char, b: Char, c: Char, d: Char): Int {
            return toHex(a, b).shl(8) or toHex(c, d)
        }
    }

    constructor(stream: InputStream): this(stream.reader())
    constructor(data: ByteArray) : this(ByteArrayInputStream(data))
    constructor(data: String) : this(data.reader())

    var index = 0
    var tmpChar = 0.toChar()

    fun close() {
        data.close()
    }

    fun next(): Char {
        if (tmpChar != 0.toChar()) {
            val v = tmpChar
            tmpChar = 0.toChar()
            return v
        }
        val next = data.read()
        if (next < 0) throw EOFException()
        return next.toChar()
    }

    fun skipSpace(): Char {
        return when (val next = next()) {
            '\r', '\n', '\t', ' ' -> skipSpace()
            else -> next
        }
    }

    fun putBack(char: Char) {
        tmpChar = char
    }

    val type0 = ComparableStringBuilder()
    fun readString(readOpeningQuotes: Boolean = true): String {
        return readString(type0, readOpeningQuotes).toString()
    }

    fun readString(builder: ComparableStringBuilder, readOpeningQuotes: Boolean = true): ComparableStringBuilder {
        if (readOpeningQuotes) assertEquals(skipSpace(), '"')
        builder.clear()
        while (true) {
            when (val next0 = next()) {
                '\\' -> {
                    when (val next1 = next()) {
                        '\\' -> builder.append('\\')
                        'r' -> builder.append('\r')
                        'n' -> builder.append('\n')
                        't' -> builder.append('\t')
                        '"' -> builder.append('"')
                        '\'' -> builder.append('\'')
                        'f' -> builder.append(12.toChar())
                        'b' -> builder.append('\b')
                        'u' -> {
                            val a = next()
                            val b = next()
                            val c = next()
                            val d = next()
                            if (!isHex(a) || !isHex(b) || !isHex(c) || !isHex(d)) {
                                throw JsonFormatException("Expected 4 hex characters after \\u")
                            }
                            builder.append(toHex(a, b, c, d).joinChars())
                        }
                        else -> throw RuntimeException("Unknown escape sequence \\$next1")
                    }
                }
                '"' -> return builder
                else -> builder.append(next0)
            }
        }
    }

    fun skipString() {
        while (true) {
            when (next()) {
                '\\' -> {
                    when (val next1 = next()) {
                        '\\', 'r', 'n', 't', '"', '\'', 'f', 'b' -> {}
                        'u' -> {
                            assertTrue(isHex(next()), "expected hex for \\u[0]")
                            assertTrue(isHex(next()), "expected hex for \\u[1]")
                            assertTrue(isHex(next()), "expected hex for \\u[2]")
                            assertTrue(isHex(next()), "expected hex for \\u[3]")
                        }
                        else -> throw RuntimeException("Unknown escape sequence \\$next1")
                    }
                }
                '"' -> return
                else -> {}
            }
        }
    }

    fun readNumber(builder: ComparableStringBuilder): CharSequence {
        builder.clear()
        var isFirst = true
        while (true) {
            when (val next = if (isFirst) skipSpace() else next()) {
                in '0'..'9', '+', '-', '.', 'e', 'E' -> {
                    builder.append(next)
                }
                '_' -> {}
                '"' -> {
                    if (builder.isEmpty()) return readString(false)
                    else throw RuntimeException("Unexpected symbol \" inside number!")
                }
                else -> {
                    tmpChar = next
                    return builder
                }
            }
            isFirst = false
        }
    }

    fun readNumber(): String {
        return readNumber(type0).toString()
    }

    fun skipNumber(readOpeningChar: Boolean = true) {
        var isEmpty = readOpeningChar
        var isFirst = readOpeningChar
        while (true) {
            when (val next = if (isFirst) skipSpace() else next()) {
                in '0'..'9', '+', '-', '.', 'e', 'E' -> isEmpty = false
                '_' -> {}
                '"' -> {
                    if (isEmpty) {
                        skipString()
                        return
                    } else throw RuntimeException("Unexpected symbol \" inside number!")
                }
                else -> {
                    tmpChar = next
                    return
                }
            }
            isFirst = false
        }
    }

    fun readObject(readOpeningBracket: Boolean = true, filter: ((String) -> Boolean)? = null): HashMap<String, Any?> {
        if (readOpeningBracket) assertEquals(skipSpace(), '{')
        var next = skipSpace()
        val obj = LinkedHashMap<String, Any?>()
        while (true) {
            when (next) {
                '}' -> return obj
                '"' -> {
                    val name = readString(false)
                    assertEquals(skipSpace(), ':')
                    if (filter == null || filter(name)) {
                        obj[name] = readValue(skipSpace(), filter)
                    } else skipValue()
                    next = skipSpace()
                }
                ',' -> next = skipSpace()
                else -> assert(next, '}', '"')
            }
        }
    }

    fun skipObject(readOpeningBracket: Boolean = true) {
        if (readOpeningBracket) assertEquals(skipSpace(), '{')
        var next = skipSpace()
        while (true) {
            when (next) {
                '}' -> return
                '"' -> {
                    skipString() // name
                    assertEquals(skipSpace(), ':')
                    skipValue()
                    next = skipSpace()
                }
                ',' -> next = skipSpace()
                else -> assert(next, '}', '"')
            }
        }
    }

    fun readValue(next: Char, filter: ((String) -> Boolean)? = null): Any? {
        return when (next) {
            in '0'..'9', '.', '+', '-' -> {
                putBack(next)
                readNumber()
            }
            '"' -> readString(false)
            '[' -> readArray(false)
            '{' -> readObject(false, filter)
            't', 'T' -> {
                assert(next(), 'r', 'R')
                assert(next(), 'u', 'U')
                assert(next(), 'e', 'E')
                true
            }
            'f', 'F' -> {
                assert(next(), 'a', 'A')
                assert(next(), 'l', 'L')
                assert(next(), 's', 'S')
                assert(next(), 'e', 'E')
                false
            }
            'n', 'N' -> {
                assert(next(), 'u', 'U')
                assert(next(), 'l', 'L')
                assert(next(), 'l', 'L')
                null
            }
            else -> assertFail("Expected value, got $next")
        }
    }

    fun skipValue(next: Char = skipSpace()) {
        when (next) {
            in '0'..'9', '.', '+', '-' -> skipNumber(false)
            '"' -> skipString()
            '[' -> skipArray(false)
            '{' -> skipObject(false)
            // should we throw errors while skipping?
            't', 'T' -> {
                assert(next(), 'r', 'R')
                assert(next(), 'u', 'U')
                assert(next(), 'e', 'E')
            }
            'f', 'F' -> {
                assert(next(), 'a', 'A')
                assert(next(), 'l', 'L')
                assert(next(), 's', 'S')
                assert(next(), 'e', 'E')
            }
            'n', 'N' -> {
                assert(next(), 'u', 'U')
                assert(next(), 'l', 'L')
                assert(next(), 'l', 'L')
            }
            else -> assertFail("Expected value, got $next")
        }
    }

    fun readArray(readOpeningBracket: Boolean = true): ArrayList<Any?> {
        if (readOpeningBracket) assertEquals(skipSpace(), '[')
        var next = skipSpace()
        val result = ArrayList<Any?>()
        while (true) {
            when (next) {
                ']' -> return result
                ',' -> {}
                else -> result.add(readValue(next))
            }
            next = skipSpace()
        }
    }

    fun skipArray(readOpeningBracket: Boolean = true) {
        if (readOpeningBracket) assertEquals(skipSpace(), '[')
        var next = skipSpace()
        while (true) {
            when (next) {
                ']' -> return
                ',' -> {}
                else -> skipValue(next)
            }
            next = skipSpace()
        }
    }

    // Java/Kotlin's defaults assert only works with arguments
    // we want ours to always work
    // we can't really put it elsewhere without prefix, because Kotlin will use the wrong import...
    protected fun assert(i: Char, c1: Char, c2: Char) {
        assertTrue(i == c1 || i == c2) {
            "Expected $c1 or $c2, but got $i"
        }
    }
}