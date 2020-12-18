package me.anno.io.xml

import me.anno.utils.LOGGER
import java.io.EOFException
import java.io.InputStream
import java.lang.RuntimeException
import java.lang.StringBuilder

object XMLReader {

    fun InputStream.skipSpaces(): Int {
        while(true){
            when(val char = read()){
                ' '.toInt(),
                '\t'.toInt(),
                '\r'.toInt(),
                '\n'.toInt() -> {}
                -1 -> throw EOFException()
                else -> return char
             }
        }
    }

    fun InputStream.readTypeUntilSpaceOrEnd(): Pair<String, Int> {
        var name = ""
        while(true){
            when(val char = read()){
                ' '.toInt(), '\t'.toInt(), '\r'.toInt(), '\n'.toInt() -> return name to ' '.toInt()
                '>'.toInt(), '='.toInt() -> return name to char
                -1 -> throw EOFException()
                else -> name += char.toChar()
            }
        }
    }

    fun InputStream.readString(startSymbol: Int): String {
        val str = StringBuilder(20)
        while(true){
            when(val char = read()){
                '\\'.toInt() -> {
                    str.append(when(val second = read()){
                        '\\'.toInt() -> "\\"
                        /*'U'.toInt(), 'u'.toInt() -> {
                            val str2 = "${read().toChar()}${read().toChar()}${read().toChar()}${read().toChar()}"
                            val value = str2.toIntOrNull(16) ?: {
                                LOGGER.warn("JSON String \\$second$str2 could not be parsed")
                                32
                            }()
                            Character.toChars(value).joinToString("")
                        }*/
                        else -> {
                            str.append('\\')
                            second.toChar()
                            // throw RuntimeException("Special character \\${second.toChar()} not yet implemented")
                        }
                    })
                }
                startSymbol -> return str.toString()
                -1 -> throw EOFException()
                else -> str.append(char.toChar())
            }
        }
    }

    fun InputStream.readUntil(end: String){
        val size = end.length
        val reversed = end.reversed()
        val buffer = CharArray(size)
        search@ while(true){
            for(i in 1 until size){
                buffer[i] = buffer[i-1]
            }
            val here = read()
            if(here == -1) throw EOFException()
            buffer[0] = here.toChar()
            for((i, target) in reversed.withIndex()){
                if(buffer[i] != target) continue@search
            }
            break
        }
    }

    fun parse(input: InputStream) = parse(null, input)
    fun parse(firstChar: Int?, input: InputStream): Any? {

        when(val first = firstChar ?: input.skipSpaces()){
            '<'.toInt() -> {
                val (name, end) = input.readTypeUntilSpaceOrEnd()
                // (name)
                when {
                    name.startsWith("?", true) -> {
                        // <?xml version="1.0" encoding="utf-8"?>
                        // I don't really care about it
                        // read until ?>
                        // or <?xpacket end="w"?>
                        input.readUntil("?>")
                        return parse(null, input)
                    }
                    name.startsWith("!--") -> {
                        // search until -->
                        input.readUntil("-->")
                        return parse(null, input)
                    }
                    name.startsWith("!doctype", true) -> {
                        var ctr = 1
                        while(ctr > 0){
                            when(input.read()){
                                '<'.toInt() -> ctr++
                                '>'.toInt() -> ctr--
                            }
                        }
                        return parse(null, input)
                    }
                    name.startsWith("![CDATA[", true) -> {
                        input.readUntil("]]>")
                        return parse(null, input)
                    }
                    name.startsWith('/') -> return endElement
                }
                val xmlElement = XMLElement(name)
                // / is the end of an element
                var end2 = end
                if(end == ' '.toInt()){
                    var next = -1
                    // read the properties
                    propertySearch@ while(true){
                        // name="value"
                        if(next < 0) next = input.skipSpaces()
                        val (propName, propEnd) = input.readTypeUntilSpaceOrEnd()
                        // ("  '${if(next < 0) "" else next.toChar().toString()}$propName' '${propEnd.toChar()}'")
                        assert(propEnd, '=')
                        val start = input.skipSpaces()
                        assert(start, '"', '\'')
                        val value = input.readString(start)
                        xmlElement[if(next < 0) propName else "${next.toChar()}$propName"] = value
                        next = input.skipSpaces()
                        when(next){
                            '/'.toInt(), '>'.toInt() -> {
                                end2 = next
                                break@propertySearch
                            }
                        }
                    }
                }

                when(end2){
                    '/'.toInt() -> {
                        assert(input.read(), '>')
                        return xmlElement
                    }
                    '>'.toInt() -> {
                        // read the body (all children)
                        var next: Int? = null
                        children@ while(true){
                            val child = parse(next, input)
                            next = null
                            when(child){
                                endElement -> return xmlElement
                                is String -> {
                                    xmlElement.children.add(child)
                                    next = '<'.toInt()
                                }
                                null -> throw RuntimeException()
                                else -> xmlElement.children.add(child)
                            }
                        }
                    }
                    else -> throw RuntimeException("Unknown end symbol ${end2.toChar()}")
                }
            }
            else -> {
                val str = StringBuilder(20)
                str.append(first.toChar())
                while(true){
                    when(val char = input.read()){
                        '\\'.toInt() -> {
                            when(val second = input.read()){
                                else -> throw RuntimeException("Special character \\${second.toChar()} not yet implemented")
                            }
                        }
                        '<'.toInt() -> return str.toString()
                        -1 -> throw EOFException()
                        else -> str.append(char.toChar())
                    }
                }
            }
        }

    }

    fun assert(a: Int, b: Char){
        if(a.toChar() != b) throw RuntimeException("Expected $b, but got ${a.toChar()}")
    }

    fun assert(a: Int, b: Char, c: Char){
        val ac = a.toChar()
        if(ac != b && ac != c) throw RuntimeException("Expected $b, but got ${a.toChar()}")
    }

    fun assert(a: Int, b: Int){
        if(a != b) throw RuntimeException("Expected ${b.toChar()}, but got ${a.toChar()}")
    }

    val endElement = Any()

}