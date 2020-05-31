package me.anno.io.xml

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

    fun InputStream.readString(): String {
        val str = StringBuilder(20)
        while(true){
            when(val char = read()){
                '\\'.toInt() -> {
                    when(val second = read()){
                        else -> throw RuntimeException("Special character \\${second.toChar()} not yet implemented")
                    }
                }
                '"'.toInt() -> return str.toString()
                -1 -> throw EOFException()
                else -> str.append(char.toChar())
            }
        }
    }

    fun parse(input: InputStream) = parse(null, input)
    fun parse(firstChar: Int?, input: InputStream): Any? {

        when(val first = firstChar ?: input.skipSpaces()){
            '<'.toInt() -> {
                val (name, end) = input.readTypeUntilSpaceOrEnd()
                if(name.startsWith("/")) return endElement
                val xmlElement = XMLElement(name)
                // / is the end of an element
                var end2 = end
                if(end == ' '.toInt()){
                    var next = -1
                    // read the properties
                    propertySearch@ while(true){
                        // name="value"
                        val (propName, propEnd) = input.readTypeUntilSpaceOrEnd()
                        assert(propEnd, '=')
                        assert(input.read(), '"')
                        val value = input.readString()
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

    fun assert(a: Int, b: Int){
        if(a != b) throw RuntimeException("Expected ${b.toChar()}, but got ${a.toChar()}")
    }

    val endElement = Any()

}