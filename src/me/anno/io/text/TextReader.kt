package me.anno.io.text

import me.anno.io.base.BaseReader
import me.anno.io.Saveable
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import java.io.EOFException

class TextReader(val data: String): BaseReader(){

    val length = data.length
    var index = 0
    var tmpChar = 0.toChar()

    fun next(): Char {
        if(tmpChar != 0.toChar()){
            val v = tmpChar
            tmpChar = 0.toChar()
            return v
        }
        return if(index < length){
            data[index++]
        } else throw EOFException()
    }

    fun skipSpace(): Char {
        return if(index < length){
            when(val next = next()){
                '\r', '\n', '\t', ' ' -> skipSpace()
                else -> next
            }
        } else throw EOFException()
    }

    fun readString(): String {
        var startIndex = index
        var str = ""
        while(true){
            when(next()){
                '\\' -> {
                    when(next()){
                        '\\' -> {
                            str += data.substring(startIndex, index-1)
                            startIndex = index
                        }
                        'r' -> {
                            str += data.substring(startIndex, index-2) + "\r"
                            startIndex = index
                        }
                        'n' -> {
                            str += data.substring(startIndex, index-2) + "\n"
                            startIndex = index
                        }
                        '"' -> {
                            str += data.substring(startIndex, index-2) + "\""
                            startIndex = index
                        }
                        '\'' -> {
                            str += data.substring(startIndex, index-2) + "'"
                            startIndex = index
                        }
                        'u' -> TODO("UTF-8 reader for Json")
                        else -> throw RuntimeException("Unknown escape sequence \\${data[index-1]}")
                    }
                }
                '"' -> return str + data.substring(startIndex, index-1)
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

    fun readObject(): Saveable {
        assertChar(skipSpace(), '"')
        val firstProperty = readString()
        assert(firstProperty == "class", "Expected first property to be 'class', was $firstProperty")
        assertChar(skipSpace(), ':')
        assertChar(skipSpace(), '"')
        val clazz = readString()
        val obj = getNewClassInstance(clazz)
        propertyLoop(obj)
        register(obj)
        return obj
    }

    fun readList(): List<Saveable> {
        TODO("read the list")
    }

    fun readAllInList(){
        assertChar(skipSpace(), '[')
        while(true){
            when(val next = skipSpace()){
                ',' -> {} // nothing to do#
                '{' -> readObject()
                ']' -> return
                else -> throw RuntimeException("Unexpected char $next")
            }
        }
    }

    fun propertyLoop(obj: Saveable){
        while(true){
            when(val next = skipSpace()){
                ',' -> readProperty(obj)
                '}' -> return
                else -> throw java.lang.RuntimeException("Unexpected char $next in object of class ${obj.getClassName()}")
            }
        }
    }

    fun readProperty(obj: Saveable){
        assertChar(skipSpace(), '"')
        val typeName = readString()
        assertChar(skipSpace(), ':')
        val (type, name) = splitTypeName(typeName)
        when(type){
            "b" -> {
                val value = when(val c0 = skipSpace()){
                    '0' -> false
                    '1' -> true
                    't', 'T' -> {
                        assertChar(next(), 'r')
                        assertChar(next(), 'u')
                        assertChar(next(), 'e')
                        true
                    }
                    'f', 'F' -> {
                        assertChar(next(), 'a')
                        assertChar(next(), 'l')
                        assertChar(next(), 's')
                        assertChar(next(), 'e')
                        false
                    }
                    else -> throw java.lang.RuntimeException("Unknown boolean value starting with $c0")
                }
                obj.readBool(name, value)
            }
            "B" -> {// int8
                val raw = readNumber()
                obj.readByte(name, raw.toIntOrNull()?.toByte() ?: error("invalid byte $raw"))
            }
            "s" -> {// int16
                val raw = readNumber()
                obj.readShort(name, raw.toIntOrNull()?.toShort() ?: error("invalid short $raw"))
            }
            "i" -> {// int32
                val raw = readNumber()
                obj.readInt(name, raw.toIntOrNull() ?: error("invalid int $raw"))
            }
            "u64", "l" -> {
                val raw = readNumber()
                obj.readLong(name, raw.toLongOrNull() ?: error("invalid long $raw"))
            }
            "f" -> {
                val raw = readNumber()
                obj.readFloat(name, raw.toFloatOrNull() ?: error("invalid float $raw"))
            }
            "d" -> {
                val raw = readNumber()
                obj.readDouble(name, raw.toDoubleOrNull() ?: error("invalid double $raw"))
            }
            "i[]" -> {
                assertChar(skipSpace(), '[')
                val rawLength = readNumber()
                val length = rawLength.toIntOrNull() ?: error("invalid i[] length $rawLength")
                if(length < (data.length - index)/2){
                    var i = 0
                    val values = IntArray(length)
                    content@while(true){
                        when(val next = skipSpace()){
                            ',' -> {
                                val raw = readNumber()
                                if(i < length){
                                    values[i++] = raw.toIntOrNull() ?: error("invalid int $raw at i[$i]")
                                }// else skip
                            }
                            ']' -> {
                                break@content
                            }
                            else -> error("unknown character $next in i[]")
                        }
                    }
                    if(i > length) println("i[] contained too many elements!")
                    obj.readIntArray(name, values)
                } else error("broken file :/, i[].length > data.length")
            }
            "v2" -> {
                assertChar(skipSpace(), '[')
                val rawX = readNumber()
                assertChar(skipSpace(), ',')
                val rawY = readNumber()
                assertChar(skipSpace(), ']')
                obj.readVector2(name, Vector2f(
                    rawX.toFloatOrNull() ?: error("invalid number $rawX"),
                    rawY.toFloatOrNull() ?: error("invalid number $rawY")
                ))
            }
            "v3" -> {
                assertChar(skipSpace(), '[')
                val rawX = readNumber()
                assertChar(skipSpace(), ',')
                val rawY = readNumber()
                assertChar(skipSpace(), ',')
                val rawZ = readNumber()
                assertChar(skipSpace(), ']')
                obj.readVector3(name, Vector3f(
                    rawX.toFloatOrNull() ?: error("invalid number $rawX"),
                    rawY.toFloatOrNull() ?: error("invalid number $rawY"),
                    rawZ.toFloatOrNull() ?: error("invalid number $rawZ")
                ))
            }
            "v4" -> {
                assertChar(skipSpace(), '[')
                val rawX = readNumber()
                assertChar(skipSpace(), ',')
                val rawY = readNumber()
                assertChar(skipSpace(), ',')
                val rawZ = readNumber()
                assertChar(skipSpace(), ',')
                val rawW = readNumber()
                assertChar(skipSpace(), ']')
                obj.readVector4(name, Vector4f(
                    rawX.toFloatOrNull() ?: error("invalid number $rawX"),
                    rawY.toFloatOrNull() ?: error("invalid number $rawY"),
                    rawZ.toFloatOrNull() ?: error("invalid number $rawZ"),
                    rawW.toFloatOrNull() ?: error("invalid number $rawW")
                ))
            }
            "S" -> {
                assertChar(skipSpace(), '"')
                obj.readString(name, readString())
            }
            else -> {
                when(val next = skipSpace()){
                    'n' -> {
                        assertChar(next(), 'u')
                        assertChar(next(), 'l')
                        assertChar(next(), 'l')
                        obj.readObject(name, null)
                    }
                    '{' -> {
                        val child = getNewClassInstance(type)
                        val n = skipSpace()
                        if(n != '}'){
                            assertChar(n, '"')
                            tmpChar = '"'
                            readProperty(child)
                            propertyLoop(child)
                        } // else nothing to do
                        obj.readObject(name, child)
                    }
                    in '0' .. '9' -> {
                        tmpChar = next
                        val rawPtr = readNumber()
                        val ptr = rawPtr.toLongOrNull() ?: error("invalid pointer: $rawPtr")
                        if(ptr > 0){
                            val child = content[ptr]
                            if(child == null){
                                addMissingReference(obj, name, ptr)
                            } else {
                                obj.readObject(name, child)
                            }
                        }
                    }
                    else -> error("missing { or ptr or null after starting object of class $type")
                }
            }
        }
    }

    fun splitTypeName(typeName: String): Pair<String, String> {
        val index = typeName.indexOf(':')
        if(index < 0) error("broken type:name : $typeName")
        val type = typeName.substring(0, index)
        val name = typeName.substring(index+1)
        return type to name
    }

    companion object {
        fun fromText(data: String): List<Saveable> {
            val reader = TextReader(data)
            reader.readAllInList()
            return reader.content.values.toList()
        }
    }

}