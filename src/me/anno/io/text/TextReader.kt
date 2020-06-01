package me.anno.io.text

import me.anno.io.ISaveable
import me.anno.io.base.BaseReader
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import java.io.EOFException
import java.lang.StringBuilder

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
        val str = StringBuilder()
        while(true){
            when(next()){
                '\\' -> {
                    when(next()){
                        '\\' -> {
                            str.append(data.substring(startIndex, index-1))
                            startIndex = index
                        }
                        'r' -> {
                            str.append(data.substring(startIndex, index-2))
                            str.append('\r')
                            startIndex = index
                        }
                        'n' -> {
                            str.append(data.substring(startIndex, index-2))
                            str.append('\n')
                            startIndex = index
                        }
                        't' -> {
                            str.append(data.substring(startIndex, index-2))
                            str.append('\t')
                            startIndex = index
                        }
                        '"' -> {
                            str.append(data.substring(startIndex, index-2))
                            str.append('"')
                            startIndex = index
                        }
                        '\'' -> {
                            str.append(data.substring(startIndex, index-2))
                            str.append('\'')
                            startIndex = index
                        }
                        'f' -> {
                            str.append(data.substring(startIndex, index-2))
                            str.append(12.toChar())
                            startIndex = index
                        }
                        'b' -> {
                            str.append(data.substring(startIndex, index-2))
                            str.append('\b')
                            startIndex = index
                        }
                        else -> throw RuntimeException("Unknown escape sequence \\${data[index-1]}")
                    }
                }
                '"' -> {
                    str.append(data.substring(startIndex, index-1))
                    return str.toString()
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

    fun readObject(): ISaveable {
        assert(skipSpace(), '"')
        val firstProperty = readString()
        assert(firstProperty == "class", "Expected first property to be 'class', was $firstProperty")
        assert(skipSpace(), ':')
        assert(skipSpace(), '"')
        val clazz = readString()
        assert(skipSpace(), ',')
        assert(skipSpace(), '"')
        val secondProperty = readString()
        assert(secondProperty == "i:*ptr", "Expected second property to be '*ptr', was $secondProperty")
        assert(skipSpace(), ':')
        val ptr = readNumber().toIntOrNull() ?: throw RuntimeException("Expected second property to be ptr")
        var obj = getNewClassInstance(clazz)
        obj = propertyLoop(obj)
        register(obj, ptr)
        return obj
    }

    fun readList(): List<ISaveable> {
        TODO("read the list")
    }

    fun readAllInList(){
        assert(skipSpace(), '[')
        while(true){
            when(val next = skipSpace()){
                ',' -> {} // nothing to do#
                '{' -> readObject()
                ']' -> return
                else -> throw RuntimeException("Unexpected char $next")
            }
        }
    }

    fun propertyLoop(obj0: ISaveable): ISaveable {
        var obj = obj0
        while(true){
            when(val next = skipSpace()){
                ',' -> obj = readProperty(obj)
                '}' -> return obj
                else -> throw RuntimeException("Unexpected char $next in object of class ${obj.getClassName()}")
            }
        }
    }

    fun readProperty(obj: ISaveable): ISaveable {
        assert(skipSpace(), '"')
        val typeName = readString()
        assert(skipSpace(), ':')
        if(typeName == "class"){
            assert(skipSpace(), '"')
            val clazz = readString()
            // could be different in lists
            return if(clazz == obj.getClassName()) obj
            else getNewClassInstance(clazz)
        }
        val (type, name) = splitTypeName(typeName)
        when(type){
            "b" -> {
                val value = when(val c0 = skipSpace()){
                    '0' -> false
                    '1' -> true
                    't', 'T' -> {
                        assert(next(), 'r')
                        assert(next(), 'u')
                        assert(next(), 'e')
                        true
                    }
                    'f', 'F' -> {
                        assert(next(), 'a')
                        assert(next(), 'l')
                        assert(next(), 's')
                        assert(next(), 'e')
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
                assert(skipSpace(), '[')
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
                assert(skipSpace(), '[')
                val rawX = readNumber()
                assert(skipSpace(), ',')
                val rawY = readNumber()
                assert(skipSpace(), ']')
                obj.readVector2(name, Vector2f(
                    rawX.toFloatOrNull() ?: error("invalid number $rawX"),
                    rawY.toFloatOrNull() ?: error("invalid number $rawY")
                ))
            }
            "v3" -> {
                assert(skipSpace(), '[')
                val rawX = readNumber()
                assert(skipSpace(), ',')
                val rawY = readNumber()
                assert(skipSpace(), ',')
                val rawZ = readNumber()
                assert(skipSpace(), ']')
                obj.readVector3(name, Vector3f(
                    rawX.toFloatOrNull() ?: error("invalid number $rawX"),
                    rawY.toFloatOrNull() ?: error("invalid number $rawY"),
                    rawZ.toFloatOrNull() ?: error("invalid number $rawZ")
                ))
            }
            "v4" -> {
                assert(skipSpace(), '[')
                val rawX = readNumber()
                assert(skipSpace(), ',')
                val rawY = readNumber()
                assert(skipSpace(), ',')
                val rawZ = readNumber()
                assert(skipSpace(), ',')
                val rawW = readNumber()
                assert(skipSpace(), ']')
                obj.readVector4(name, Vector4f(
                    rawX.toFloatOrNull() ?: error("invalid number $rawX"),
                    rawY.toFloatOrNull() ?: error("invalid number $rawY"),
                    rawZ.toFloatOrNull() ?: error("invalid number $rawZ"),
                    rawW.toFloatOrNull() ?: error("invalid number $rawW")
                ))
            }
            "S" -> {
                assert(skipSpace(), '"')
                obj.readString(name, readString())
            }
            else -> {
                when(val next = skipSpace()){
                    'n' -> {
                        assert(next(), 'u')
                        assert(next(), 'l')
                        assert(next(), 'l')
                        obj.readObject(name, null)
                    }
                    '{' -> {
                        var child = getNewClassInstance(type)
                        assert(skipSpace(), '"')
                        var property0 = readString()
                        if(property0 == "class"){
                            assert(skipSpace(), ':')
                            assert(skipSpace(), '"')
                            assert(readString() == type)
                            assert(skipSpace(), ',')
                            assert(skipSpace(), '"')
                            property0 = readString()
                        }
                        assert(property0 == "*ptr")
                        assert(skipSpace(), ':')
                        val ptr = readNumber().toIntOrNull() ?: throw RuntimeException("Invalid pointer")
                        var n = skipSpace()
                        if(n != '}'){
                            if(n == ',') n = skipSpace()
                            if(n != '}'){
                                assert(n, '"')
                                tmpChar = '"'
                                child = readProperty(child)
                                child = propertyLoop(child)
                            }
                        } // else nothing to do
                        register(child, ptr)
                        obj.readObject(name, child)
                    }
                    in '0' .. '9' -> {
                        tmpChar = next
                        val rawPtr = readNumber()
                        val ptr = rawPtr.toIntOrNull() ?: error("invalid pointer: $rawPtr")
                        if(ptr > 0){
                            val child = content[ptr]
                            if(child == null){
                                addMissingReference(obj, name, ptr)
                            } else {
                                obj.readObject(name, child)
                            }
                        }
                    }
                    else -> error("Missing { or ptr or null after starting object of class $type")
                }
            }
        }
        return obj
    }

    fun splitTypeName(typeName: String): Pair<String, String> {
        val index = typeName.indexOf(':')
        if(index < 0) error("broken type:name : $typeName")
        val type = typeName.substring(0, index)
        val name = typeName.substring(index+1)
        return type to name
    }

    companion object {
        fun fromText(data: String): List<ISaveable> {
            val reader = TextReader(data)
            reader.readAllInList()
            // sorting is very important
            return reader.content.entries.sortedBy { it.key }.map { it.value }.toList()
        }
    }

}