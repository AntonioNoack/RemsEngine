package me.anno.io.json

import java.io.EOFException
import java.io.InputStream
import java.lang.StringBuilder

// to avoid the import of fasterxml.json (17MB) we create our own solution
class JsonReader(val data: InputStream) {

    var index = 0
    var tmpChar = 0.toChar()

    fun next(): Char {
        if(tmpChar != 0.toChar()){
            val v = tmpChar
            tmpChar = 0.toChar()
            return v
        }
        val next = data.read()
        if(next < 0) throw EOFException()
        return next.toChar()
    }

    fun skipSpace(): Char {
        return when(val next = next()){
            '\r', '\n', '\t', ' ' -> skipSpace()
            else -> next
        }
    }

    fun putBack(char: Char){
        tmpChar = char
    }

    fun readString(): String {
        val str = StringBuilder()
        while(true){
            when(val next0 = next()){
                '\\' -> {
                    when(val next1 = next()){
                        '\\' -> str.append('\\')
                        'r' -> str.append('\r')
                        'n' -> str.append('\n')
                        't' -> str.append('\t')
                        '"' -> str.append('"')
                        '\'' -> str.append('\'')
                        'f' -> str.append(12.toChar())
                        'b' -> str.append('\b')
                        'u' -> str.append("${next()}${next()}${next()}${next()}".toInt(16).toChar())
                        else -> throw RuntimeException("Unknown escape sequence \\$next1")
                    }
                }
                '"' -> return str.toString()
                else -> {
                    str.append(next0)
                }
            }
        }
    }

    fun readNumber(): String {
        var str = ""
        var isFirst = true
        while(true){
            when(val next = if(isFirst) skipSpace() else next()){
                in '0' .. '9', '+', '-', '.', 'e', 'E' -> {
                    str += next
                }
                '_' -> {}
                '"' -> {
                    if(str.isEmpty()) return readString()
                    else throw RuntimeException("Unexpected symbol \" inside number!")
                }
                else -> {
                    tmpChar = next
                    return str
                }
            }
            isFirst = false
        }
    }

    fun readObject(): JsonObject {
        assert(skipSpace(), '{')
        var next = skipSpace()
        val obj = JsonObject()
        while(true){
            when(next){
                '}' -> return obj
                '"' -> {
                    val name = readString()
                    assert(skipSpace(), ':')
                    obj[name] = readSomething(skipSpace())
                    next = skipSpace()
                }
                ',' -> {
                    next = skipSpace()
                }
                else -> assert(next, '}', '"')
            }
        }
    }

    fun readSomething(next: Char): Any {
        when(next){
            in '0' .. '9', '.', '+', '-' -> {
                putBack(next)
                return readNumber()
            }
            '"' -> {
                return readString()
            }
            '[' -> {
                putBack(next)
                return readArray()
            }
            '{' -> {
                putBack(next)
                return readObject()
            }
            't', 'T' -> {
                assert(next(), 'r', 'R')
                assert(next(), 'u', 'U')
                assert(next(), 'e', 'E')
                return true
            }
            'f', 'F' -> {
                assert(next(), 'a', 'A')
                assert(next(), 'l', 'L')
                assert(next(), 's', 'S')
                assert(next(), 'e', 'E')
                return false
            }
            else -> throw RuntimeException("Expected value, got $next")
        }
    }

    fun readArray(): JsonArray {
        assert(skipSpace(), '[')
        var next = skipSpace()
        val obj = JsonArray()
        while(true){
            when(next){
                ']' -> return obj
                ',' -> {}
                else -> obj.add(readSomething(next))
            }
            next = skipSpace()
        }
    }

    // Java/Kotlin's defaults assert only works with arguments
    // we want ours to always work
    // we can't really put it elsewhere without prefix, because Kotlin will use the wrong import...
    fun assert(i: Char, c1: Char, c2: Char){
        if(i != c1 && i != c2) throw RuntimeException("Expected $c1 or $c2, but got $i")
    }

    fun assert(i: Char, c: Char){
        if(i != c) throw RuntimeException("Expected $c, but got $i")
    }

    fun assert(i: Char, c: Char, msg: String){
        if(i != c) throw RuntimeException(msg)
    }



}