package me.anno.io.text

import me.anno.io.ISaveable
import me.anno.io.base.BaseReader
import org.apache.logging.log4j.LogManager
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import java.io.EOFException

class TextReader(val data: String) : BaseReader() {

    val length = data.length
    var index = 0
    var tmpChar = 0.toChar()

    private var unusedPointer = Int.MAX_VALUE
    private fun getUnusedPointer(): Int {
        return unusedPointer--
    }

    override fun readObject(): ISaveable {
        assert(skipSpace(), '"')
        val firstProperty = readString()
        assert(firstProperty == "class", "Expected first property to be 'class', was $firstProperty")
        assert(skipSpace(), ':')
        assert(skipSpace(), '"')
        val clazz = readString()
        assert(skipSpace(), ',')
        assert(skipSpace(), '"')
        val secondProperty = readString()
        assert(skipSpace(), ':')
        var obj = getNewClassInstance(clazz)
        val ptr = if (secondProperty == "i:*ptr") {
            // assert(secondProperty == "i:*ptr", "Expected second property to be '*ptr', was $secondProperty")
            readNumber().toIntOrNull() ?: throw RuntimeException("Expected second property to be ptr")
        } else {
            obj = readProperty(obj, secondProperty)
            getUnusedPointer() // not used
        }
        obj = propertyLoop(obj)
        register(obj, ptr)
        return obj
    }

    override fun readAllInList() {
        assert(skipSpace(), '[')
        while (true) {
            when (val next = skipSpace()) {
                ',' -> Unit // nothing to do
                '{' -> readObject()
                ']' -> return
                else -> throw RuntimeException("Unexpected char $next")
            }
        }
    }

    private fun next(): Char {
        if (tmpChar != 0.toChar()) {
            val v = tmpChar
            tmpChar = 0.toChar()
            return v
        }
        return if (index < length) {
            data[index++]
        } else throw EOFException()
    }

    private fun skipSpace(): Char {
        return if (index < length) {
            when (val next = next()) {
                '\r', '\n', '\t', ' ' -> skipSpace()
                else -> next
            }
        } else throw EOFException()
    }

    private fun readString(): String {
        var startIndex = index
        val str = StringBuilder()
        while (true) {
            when (next()) {
                '\\' -> {
                    when (next()) {
                        '\\' -> {
                            str.append(data.substring(startIndex, index - 1))
                            startIndex = index
                        }
                        'r' -> {
                            str.append(data.substring(startIndex, index - 2))
                            str.append('\r')
                            startIndex = index
                        }
                        'n' -> {
                            str.append(data.substring(startIndex, index - 2))
                            str.append('\n')
                            startIndex = index
                        }
                        't' -> {
                            str.append(data.substring(startIndex, index - 2))
                            str.append('\t')
                            startIndex = index
                        }
                        '"' -> {
                            str.append(data.substring(startIndex, index - 2))
                            str.append('"')
                            startIndex = index
                        }
                        '\'' -> {
                            str.append(data.substring(startIndex, index - 2))
                            str.append('\'')
                            startIndex = index
                        }
                        'f' -> {
                            str.append(data.substring(startIndex, index - 2))
                            str.append(12.toChar())
                            startIndex = index
                        }
                        'b' -> {
                            str.append(data.substring(startIndex, index - 2))
                            str.append('\b')
                            startIndex = index
                        }
                        else -> throw RuntimeException("Unknown escape sequence \\${data[index - 1]}")
                    }
                }
                '"' -> {
                    str.append(data.substring(startIndex, index - 1))
                    return str.toString()
                }
            }
        }
    }

    private fun readNumber(): String {
        var str = ""
        var isFirst = true
        while (true) {
            when (val next = if (isFirst) skipSpace() else next()) {
                in '0'..'9', '+', '-', '.', 'e', 'E' -> {
                    str += next
                }
                '_' -> {
                }
                '"' -> {
                    if (str.isEmpty()) return readString()
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

    private fun propertyLoop(obj0: ISaveable): ISaveable {
        var obj = obj0
        while (true) {
            when (val next = skipSpace()) {
                ',' -> obj = readProperty(obj)
                '}' -> return obj
                else -> throw RuntimeException("Unexpected char $next in object of class ${obj.getClassName()}")
            }
        }
    }

    private fun <ArrayType, InstanceType> readTypedArray(
        typeName: String,
        createArray: (arraySize: Int) -> ArrayType,
        readValue: () -> InstanceType?,
        putValue: (array: ArrayType, index: Int, value: InstanceType) -> Unit
    ): ArrayType {
        assert(skipSpace(), '[')
        val rawLength = readNumber()
        val length = rawLength.toIntOrNull() ?: error("invalid $typeName[] length $rawLength")
        if (length < (data.length - index) / 2) {
            var i = 0
            val values = createArray(length)
            content@ while (true) {
                when (val next = skipSpace()) {
                    ',' -> {
                        val raw = readValue()
                        if (i < length) {
                            putValue(values, i++, raw ?: error("invalid $typeName $raw at $typeName[$i]"))
                        }// else skip
                    }
                    ']' -> {
                        break@content
                    }
                    else -> error("unknown character $next in $typeName[]")
                }
            }
            if (i > length) LOGGER.warn("$typeName[] contained too many elements!")
            return values
        } else error("broken file :/, $typeName[].length > data.length")
    }

    private fun readBool(): Boolean {
        return when (val c0 = skipSpace()) {
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
    }

    private fun readVector2f(): Vector2f {
        assert(skipSpace(), '[', "Start of Vector")
        val rawX = readNumber()
        assert(skipSpace(), ',', "Separator of Vector")
        val rawY = readNumber()
        assert(skipSpace(), ']', "End of Vector")
        return Vector2f(
            rawX.toFloatOrNull() ?: error("Invalid x coordinate $rawX"),
            rawY.toFloatOrNull() ?: error("Invalid y coordinate $rawY")
        )
    }

    private fun readVector3f(): Vector3f {
        assert(skipSpace(), '[', "Start of Vector")
        val rawX = readNumber()
        assert(skipSpace(), ',', "Separator of Vector")
        val rawY = readNumber()
        assert(skipSpace(), ',', "Separator of Vector")
        val rawZ = readNumber()
        assert(skipSpace(), ']', "End of Vector")
        return Vector3f(
            rawX.toFloatOrNull() ?: error("Invalid x coordinate $rawX"),
            rawY.toFloatOrNull() ?: error("Invalid y coordinate $rawY"),
            rawZ.toFloatOrNull() ?: error("Invalid z coordinate $rawZ")
        )
    }

    private fun readVector4f(): Vector4f {
        assert(skipSpace(), '[', "Start of Vector")
        val rawX = readNumber()
        assert(skipSpace(), ',', "Separator of Vector")
        val rawY = readNumber()
        assert(skipSpace(), ',', "Separator of Vector")
        val rawZ = readNumber()
        assert(skipSpace(), ',', "Separator of Vector")
        val rawW = readNumber()
        assert(skipSpace(), ']', "End of Vector")
        return Vector4f(
            rawX.toFloatOrNull() ?: error("Invalid x coordinate $rawX"),
            rawY.toFloatOrNull() ?: error("Invalid y coordinate $rawY"),
            rawZ.toFloatOrNull() ?: error("Invalid z coordinate $rawZ"),
            rawW.toFloatOrNull() ?: error("Invalid w coordinate $rawW")
        )
    }

    private fun readProperty(obj: ISaveable): ISaveable {
        assert(skipSpace(), '"')
        val typeName = readString()
        assert(skipSpace(), ':')
        return readProperty(obj, typeName)
    }

    private fun readProperty(obj: ISaveable, typeName: String): ISaveable {
        if (typeName == "class") {
            assert(skipSpace(), '"')
            val clazz = readString()
            // could be different in lists
            return if (clazz == obj.getClassName()) obj
            else getNewClassInstance(clazz)
        }
        val (type, name) = splitTypeName(typeName)
        when (type) {
            "b" -> obj.readBoolean(name, readBool())
            "B" -> {// int8
                val raw = readNumber()
                obj.readByte(name, raw.toIntOrNull()?.toByte() ?: error("Invalid byte $raw"))
            }
            "s" -> {// int16
                val raw = readNumber()
                obj.readShort(name, raw.toIntOrNull()?.toShort() ?: error("Invalid short $raw"))
            }
            "i" -> {// int32
                val raw = readNumber()
                obj.readInt(name, raw.toIntOrNull() ?: error("Invalid int $raw"))
            }
            "u64", "l" -> {
                val raw = readNumber()
                obj.readLong(name, raw.toLongOrNull() ?: error("Invalid long $raw"))
            }
            "f" -> {
                val raw = readNumber()
                obj.readFloat(name, raw.toFloatOrNull() ?: error("Invalid float $raw"))
            }
            "d" -> {
                val raw = readNumber()
                obj.readDouble(name, raw.toDoubleOrNull() ?: error("Invalid double $raw"))
            }
            "b[]" -> obj.readBooleanArray(
                name,
                readTypedArray("boolean",
                    { BooleanArray(it) },
                    { readBool() },
                    { array, index, value -> array[index] = value })
            )
            "B[]" -> obj.readByteArray(
                name,
                readTypedArray("byte",
                    { ByteArray(it) },
                    { readNumber().toIntOrNull()?.toByte() ?: error("Invalid byte") },
                    { array, index, value -> array[index] = value })
            )
            "s[]" -> obj.readShortArray(
                name,
                readTypedArray("short",
                    { ShortArray(it) },
                    { readNumber().toIntOrNull()?.toShort() ?: error("Invalid short") },
                    { array, index, value -> array[index] = value })
            )
            "i[]" -> obj.readIntArray(
                name,
                readTypedArray("int",
                    { IntArray(it) },
                    { readNumber().toInt() },
                    { array, index, value -> array[index] = value })
            )
            "l[]" -> obj.readLongArray(
                name,
                readTypedArray("long",
                    { LongArray(it) },
                    { readNumber().toLong() },
                    { array, index, value -> array[index] = value })
            )
            "f[]" -> obj.readFloatArray(
                name,
                readTypedArray("float",
                    { FloatArray(it) },
                    { readNumber().toFloat() },
                    { array, index, value -> array[index] = value })
            )
            "f[][]" -> {
                val fa0 = FloatArray(0)
                obj.readFloatArray2D(
                    name,
                    readTypedArray("float[]",
                        { Array(it) { fa0 } }, {
                            readTypedArray("float",
                                { FloatArray(it) },
                                { readNumber().toFloat() },
                                { array, index, value -> array[index] = value })
                        },
                        { array, index, value -> array[index] = value }
                    )
                )
            }
            "d[]" -> obj.readDoubleArray(
                name,
                readTypedArray("double",
                    { DoubleArray(it) },
                    { readNumber().toDouble() },
                    { array, index, value -> array[index] = value })
            )
            "d[][]" -> {
                val fa0 = DoubleArray(0)
                obj.readDoubleArray2D(
                    name,
                    readTypedArray("double[]",
                        { Array(it) { fa0 } }, {
                            readTypedArray("double",
                                { DoubleArray(it) },
                                { readNumber().toDouble() },
                                { array, index, value -> array[index] = value })
                        },
                        { array, index, value -> array[index] = value }
                    )
                )
            }
            "v2" -> obj.readVector2f(name, readVector2f())
            "v3" -> obj.readVector3f(name, readVector3f())
            "v4" -> obj.readVector4f(name, readVector4f())
            "v2[]" -> {
                val v0 = Vector2f()
                obj.readVector2fArray(
                    name,
                    readTypedArray("vector2f",
                        { Array(it) { v0 } },
                        { readVector2f() },
                        { array, index, value -> array[index] = value })
                )
            }
            "v3[]" -> {
                val v0 = Vector3f()
                obj.readVector3fArray(
                    name,
                    readTypedArray("vector3f",
                        { Array(it) { v0 } },
                        { readVector3f() },
                        { array, index, value -> array[index] = value })
                )
            }
            "v4[]" -> {
                val v0 = Vector4f()
                obj.readVector4fArray(
                    name,
                    readTypedArray("vector4f",
                        { Array(it) { v0 } },
                        { readVector4f() },
                        { array, index, value -> array[index] = value })
                )
            }
            "S" -> {
                assert(skipSpace(), '"')
                obj.readString(name, readString())
            }
            else -> {
                when (val next = skipSpace()) {
                    'n' -> {
                        assert(next(), 'u')
                        assert(next(), 'l')
                        assert(next(), 'l')
                        obj.readObject(name, null)
                    }
                    '{' -> {
                        val (child, ptr) = readObject(name)
                        register(child, ptr)
                        obj.readObject(name, child)
                    }
                    in '0'..'9' -> {
                        tmpChar = next
                        val rawPtr = readNumber()
                        val ptr = rawPtr.toIntOrNull() ?: error("invalid pointer: $rawPtr")
                        if (ptr > 0) {
                            val child = content[ptr]
                            if (child == null) {
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

    private fun readObject(type: String): Pair<ISaveable, Int> {
        var child = getNewClassInstance(type)
        val firstChar = skipSpace()
        val ptr: Int
        if (firstChar == '}') {
            // nothing to do
            ptr = getUnusedPointer() // not used
        } else {
            assert(firstChar, '"')
            var property0 = readString()
            if (property0 == "class") {
                assert(skipSpace(), ':')
                assert(skipSpace(), '"')
                assert(readString() == type)
                assert(skipSpace(), ',')
                assert(skipSpace(), '"')
                property0 = readString()
            }
            val nextChar = skipSpace()
            if (nextChar == '}') {
                // nothing to do
                ptr = getUnusedPointer() // not used
            } else {
                assert(nextChar, ':')
                ptr = if (property0 == "*ptr" || property0 == "i:*ptr") {
                    readNumber().toIntOrNull() ?: throw RuntimeException("Invalid pointer")
                } else {
                    child = readProperty(child, property0)
                    getUnusedPointer() // not used
                }
                var n = skipSpace()
                if (n != '}') {
                    if (n == ',') n = skipSpace()
                    if (n != '}') {
                        assert(n, '"')
                        tmpChar = '"'
                        child = readProperty(child)
                        child = propertyLoop(child)
                    }
                } // else nothing to do
            }
        }
        return child to ptr
    }

    private fun splitTypeName(typeName: String): Pair<String, String> {
        val index = typeName.indexOf(':')
        if (index < 0) error("Invalid Type:Name '$typeName'")
        val type = typeName.substring(0, index)
        val name = typeName.substring(index + 1)
        return type to name
    }

    companion object {
        private val LOGGER = LogManager.getLogger(TextReader::class.java)
        fun fromText(data: String): List<ISaveable> {
            val reader = TextReader(data)
            reader.readAllInList()
            // sorting is very important
            return reader.sortedContent
        }
    }

}