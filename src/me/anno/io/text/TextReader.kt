package me.anno.io.text

import me.anno.io.ISaveable
import me.anno.io.InvalidFormatException
import me.anno.io.base.BaseReader
import me.anno.io.base.UnknownClassException
import org.apache.logging.log4j.LogManager
import org.joml.*
import java.io.EOFException

/**
 * reads a JSON-similar format from a text file
 * */
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
        val nc0 = skipSpace()
        var obj: ISaveable
        val ptr: Int
        if (nc0 == ',') {
            assert(skipSpace(), '"')
            val secondProperty = readString()
            assert(skipSpace(), ':')
            obj = getNewClassInstance(clazz)
            ptr = if (secondProperty == "i:*ptr") {
                readInt()
            } else {
                obj = readProperty(obj, secondProperty)
                getUnusedPointer() // not used
            }
            obj = propertyLoop(obj)
        } else {
            assert(nc0, '}')
            obj = getNewClassInstance(clazz)
            ptr = getUnusedPointer()
        }
        register(obj, ptr)
        return obj
    }

    override fun readAllInList() {
        assert(skipSpace(), '[')
        try {
            while (true) {
                when (val next = skipSpace()) {
                    ',' -> Unit // nothing to do
                    '{' -> readObject()
                    ']' -> return
                    else -> throw InvalidFormatException("Unexpected char $next")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
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

    private fun readStringValue(): String {
        assert(skipSpace(), '"', "Reading String")
        return readString()
    }

    private fun readString(): String {

        var startIndex = index
        val firstChar = next()
        if (firstChar == '"') return ""

        var builder0: StringBuilder? = null
        val dataLength = data.length

        fun appendSpecial(char: Char) {
            if (builder0 == null) builder0 = StringBuilder(8)
            val str2 = builder0!!
            str2.append(data.subSequence(startIndex, index - 2))
            str2.append(char)
            startIndex = index
        }

        fun append(char: Char): Boolean {
            return when (char) {
                '\\' -> {
                    if (index >= dataLength) throw InvalidFormatException("Expected \" at the end of string")
                    when (data[index++]) {
                        '\\' -> appendSpecial('\\')
                        'r' -> appendSpecial('\r')
                        'n' -> appendSpecial('\n')
                        't' -> appendSpecial('\t')
                        '"' -> appendSpecial('"')
                        '\'' -> appendSpecial('\'')
                        'f' -> appendSpecial(12.toChar())
                        'b' -> appendSpecial('\b')
                        else -> throw InvalidFormatException("Unknown escape sequence \\${data[index - 1]}")
                    }
                    false
                }
                '"' -> true
                else -> false
            }
        }

        append(firstChar)
        while (true) {
            if (index >= dataLength) throw InvalidFormatException("Expected \" at the end of string")
            if (append(data[index++])) {
                val builder = builder0
                return if (builder == null) {
                    data.substring(startIndex, index - 1)
                } else {
                    builder.append(data.subSequence(startIndex, index - 1))
                    builder.toString()
                }
            }
        }
    }

    /**
     * reads a number string; may return invalid results
     * if a string starts here, the string is read instead
     * */
    private fun readNumber(): String {
        val str = StringBuilder()
        var isFirst = true
        while (true) {
            when (val next = if (isFirst) skipSpace() else next()) {
                in '0'..'9', '+', '-', '.', 'e', 'E' -> {
                    str.append(next)
                }
                '_' -> {
                }
                '"' -> {
                    if (str.isEmpty()) return readString()
                    else throw InvalidFormatException("Unexpected symbol \" inside number!")
                }
                else -> {
                    tmpChar = next
                    return str.toString()
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
                else -> throw InvalidFormatException("Unexpected char $next in object of class ${obj.getClassName()}")
            }
        }
    }

    private fun <ArrayType, InstanceType> readTypedArray(
        typeName: String,
        createArray: (arraySize: Int) -> ArrayType,
        readValue: () -> InstanceType,
        putValue: (array: ArrayType, index: Int, value: InstanceType) -> Unit
    ): ArrayType {
        assert(skipSpace(), '[')
        val rawLength = readNumber()
        val length = rawLength.toIntOrNull() ?: error("Invalid $typeName[] length '$rawLength'")
        if (length < (data.length - index) / 2) {
            var i = 0
            val values = createArray(length)
            content@ while (true) {
                when (val next = skipSpace()) {
                    ',' -> {
                        val raw = readValue()
                        if (i < length) {
                            putValue(values, i++, raw)
                        }// else skip
                    }
                    ']' -> break@content
                    else -> error("unknown character $next in $typeName[]")
                }
            }
            if (i > length) LOGGER.warn("$typeName[] contained too many elements!")
            return values
        } else error("Broken file :/, $typeName[].length > data.length")
    }

    private fun readBool(): Boolean {
        return when (val firstChar = skipSpace()) {
            '0' -> false
            '1' -> true
            't', 'T' -> {
                assert(next(), 'r', "boolean:true")
                assert(next(), 'u', "boolean:true")
                assert(next(), 'e', "boolean:true")
                true
            }
            'f', 'F' -> {
                assert(next(), 'a', "boolean:false")
                assert(next(), 'l', "boolean:false")
                assert(next(), 's', "boolean:false")
                assert(next(), 'e', "boolean:false")
                false
            }
            else -> throw InvalidFormatException("Unknown boolean value starting with $firstChar")
        }
    }

    private fun readVector2f(): Vector2f {
        assert(skipSpace(), '[', "Start of Vector")
        val rawX = readFloat()
        val sep0 = skipSpace()
        if (sep0 == ']') return Vector2f(rawX)
        assert(sep0, ',', "Separator of Vector")
        val rawY = readFloat()
        assert(skipSpace(), ']', "End of Vector")
        return Vector2f(rawX, rawY)
    }

    private fun readVector3f(): Vector3f {
        assert(skipSpace(), '[', "Start of Vector")
        val rawX = readFloat()
        val sep0 = skipSpace()
        if (sep0 == ']') return Vector3f(rawX) // monotone / grayscale
        assert(sep0, ',', "Separator of Vector")
        val rawY = readFloat()
        assert(skipSpace(), ',', "Separator of Vector")
        val rawZ = readFloat()
        assert(skipSpace(), ']', "End of Vector")
        return Vector3f(rawX, rawY, rawZ)
    }

    private fun readVector4f(): Vector4f {
        assert(skipSpace(), '[', "Start of Vector")
        val rawX = readFloat()
        val sep0 = skipSpace()
        if (sep0 == ']') return Vector4f(rawX) // monotone
        assert(sep0, ',', "Separator of Vector")
        val rawY = readFloat()
        val sep1 = skipSpace()
        if (sep1 == ']') return Vector4f(rawX, rawX, rawX, rawY) // white with alpha
        assert(sep1, ',', "Separator of Vector")
        val rawZ = readFloat()
        val sep2 = skipSpace()
        if (sep2 == ']') return Vector4f(rawX, rawY, rawZ, 1f) // opaque color
        assert(sep2, ',', "Separator of Vector")
        val rawW = readFloat()
        assert(skipSpace(), ']', "End of Vector")
        return Vector4f(rawX, rawY, rawZ, rawW)
    }

    private fun readVector2d(): Vector2d {
        assert(skipSpace(), '[', "Start of Vector")
        val rawX = readDouble()
        val sep0 = skipSpace()
        if (sep0 == ']') return Vector2d(rawX)
        assert(sep0, ',', "Separator of Vector")
        val rawY = readDouble()
        assert(skipSpace(), ']', "End of Vector")
        return Vector2d(rawX, rawY)
    }

    private fun readVector3d(): Vector3d {
        assert(skipSpace(), '[', "Start of Vector")
        val rawX = readDouble()
        val sep0 = skipSpace()
        if (sep0 == ']') return Vector3d(rawX) // monotone / grayscale
        assert(sep0, ',', "Separator of Vector")
        val rawY = readDouble()
        assert(skipSpace(), ',', "Separator of Vector")
        val rawZ = readDouble()
        assert(skipSpace(), ']', "End of Vector")
        return Vector3d(rawX, rawY, rawZ)
    }

    private fun readVector4d(): Vector4d {
        assert(skipSpace(), '[', "Start of Vector")
        val rawX = readDouble()
        val sep0 = skipSpace()
        if (sep0 == ']') return Vector4d(rawX) // monotone
        assert(sep0, ',', "Separator of Vector")
        val rawY = readDouble()
        val sep1 = skipSpace()
        if (sep1 == ']') return Vector4d(rawX, rawX, rawX, rawY) // white with alpha
        assert(sep1, ',', "Separator of Vector")
        val rawZ = readDouble()
        val sep2 = skipSpace()
        if (sep2 == ']') return Vector4d(rawX, rawY, rawZ, 1.0) // opaque color
        assert(sep2, ',', "Separator of Vector")
        val rawW = readDouble()
        assert(skipSpace(), ']', "End of Vector")
        return Vector4d(rawX, rawY, rawZ, rawW)
    }

    fun readProperty(obj: ISaveable): ISaveable {
        assert(skipSpace(), '"')
        val typeName = readString()
        assert(skipSpace(), ':')
        return readProperty(obj, typeName)
    }

    private fun readByte() = readNumber().run {
        toIntOrNull()?.toByte() ?: error("Invalid byte", this)
    }

    private fun readShort() = readNumber().run {
        toIntOrNull()?.toShort() ?: error("Invalid short", this)
    }

    private fun readInt() = readNumber().run {
        toIntOrNull() ?: error("Invalid int", this)
    }

    private fun readLong() = readNumber().run {
        toLongOrNull() ?: error("Invalid long", this)
    }

    private fun readFloat() = readNumber().run {
        toFloatOrNull() ?: error("Invalid float", this)
    }

    private fun readDouble() = readNumber().run {
        toDoubleOrNull() ?: error("Invalid double", this)
    }

    private fun readFloatArray() = readTypedArray(
        "float",
        { FloatArray(it) }, { readFloat() },
        { array, index, value -> array[index] = value }
    )

    private fun readDoubleArray() = readTypedArray(
        "double",
        { DoubleArray(it) }, { readDouble() },
        { array, index, value -> array[index] = value }
    )

    private fun readProperty(obj: ISaveable, typeName: String): ISaveable {
        if (typeName == "class") {
            assert(skipSpace(), '"')
            val clazz = readString()
            // could be different in lists
            return if (clazz == obj.getClassName()) obj
            else getNewClassInstance(clazz)
        }
        var (type, name) = splitTypeName(typeName)
        when (type) {
            "i1", "b" -> obj.readBoolean(name, readBool())
            "i8", "B" -> obj.readByte(name, readByte())
            "i16", "s" -> obj.readShort(name, readShort())
            "i32", "i" -> obj.readInt(name, readInt())
            "u64", "l" -> obj.readLong(name, readLong())
            "f32", "f" -> obj.readFloat(name, readFloat())
            "f64", "d" -> obj.readDouble(name, readDouble())
            "i1[]", "b[]" -> obj.readBooleanArray(
                name,
                readTypedArray("boolean",
                    { BooleanArray(it) }, { readBool() },
                    { array, index, value -> array[index] = value })
            )
            "i8[]", "B[]" -> obj.readByteArray(
                name,
                readTypedArray("byte",
                    { ByteArray(it) }, { readByte() },
                    { array, index, value -> array[index] = value })
            )
            "i16[]", "s[]" -> obj.readShortArray(
                name,
                readTypedArray("short",
                    { ShortArray(it) }, { readShort() },
                    { array, index, value -> array[index] = value })
            )
            "i32[]", "i[]" -> obj.readIntArray(
                name,
                readTypedArray("int",
                    { IntArray(it) }, { readInt() },
                    { array, index, value -> array[index] = value })
            )
            "i64[]", "u64[]", "l[]" -> obj.readLongArray(
                name,
                readTypedArray("long",
                    { LongArray(it) }, { readLong() },
                    { array, index, value -> array[index] = value })
            )
            "f32[]", "f[]" -> obj.readFloatArray(name, readFloatArray())
            "f32[][]", "f[][]" -> {
                val fa0 = FloatArray(0)
                obj.readFloatArray2D(
                    name,
                    readTypedArray("float[]",
                        { Array(it) { fa0 } }, { readFloatArray() },
                        { array, index, value -> array[index] = value }
                    )
                )
            }
            "d[]" -> obj.readDoubleArray(name, readDoubleArray())
            "d[][]" -> {
                val fa0 = DoubleArray(0)
                obj.readDoubleArray2D(
                    name,
                    readTypedArray("double[]",
                        { Array(it) { fa0 } }, { readDoubleArray() },
                        { array, index, value -> array[index] = value }
                    )
                )
            }
            "v2" -> obj.readVector2f(name, readVector2f())
            "v3" -> obj.readVector3f(name, readVector3f())
            "v4" -> obj.readVector4f(name, readVector4f())
            "v2d" -> obj.readVector2d(name, readVector2d())
            "v3d" -> obj.readVector3d(name, readVector3d())
            "v4d" -> obj.readVector4d(name, readVector4d())
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
            "v2d[]" -> {
                val v0 = Vector2d()
                obj.readVector2dArray(
                    name,
                    readTypedArray("vector2f",
                        { Array(it) { v0 } },
                        { readVector2d() },
                        { array, index, value -> array[index] = value })
                )
            }
            "v3d[]" -> {
                val v0 = Vector3d()
                obj.readVector3dArray(
                    name,
                    readTypedArray("vector3f",
                        { Array(it) { v0 } },
                        { readVector3d() },
                        { array, index, value -> array[index] = value })
                )
            }
            "v4d[]" -> {
                val v0 = Vector4d()
                obj.readVector4dArray(
                    name,
                    readTypedArray("vector4f",
                        { Array(it) { v0 } },
                        { readVector4d() },
                        { array, index, value -> array[index] = value })
                )
            }
            "m3" -> obj.readMatrix3f(name, Matrix3f(readVector3f(), readVector3f(), readVector3f()))
            "m4x3" -> obj.readMatrix4x3f(name, Matrix4x3f(readVector3f(), readVector3f(), readVector3f(), readVector3f()))
            "m4" -> obj.readMatrix4f(name, Matrix4f(readVector4f(), readVector4f(), readVector4f(), readVector4f()))
            "S" -> obj.readString(name, readStringValue())
            "S[]" -> obj.readStringArray(
                name,
                readTypedArray("String",
                    { Array(it) { "" } }, { readStringValue() },
                    { array, index, value -> array[index] = value })
            )
            "*[]", "[]" -> {// array of mixed types
                val elements = readTypedArray("Any", { arrayOfNulls<ISaveable?>(it) }, {
                    when (val next = skipSpace()) {
                        'n' -> readNull()
                        '{' -> readObject()
                        in '0'..'9' -> readPtr(next)
                        else -> error("Missing { or ptr or null after starting object[], got '$next'")
                    }
                }, { array, index, value -> array[index] = value })
                obj.readObjectArray(name, elements)
            }
            else -> {
                if (type.endsWith("[]")) {// array, but all elements have the same type
                    type = type.substring(0, type.length - 2)
                    val elements = readTypedArray(type, { arrayOfNulls<ISaveable?>(it) }, {
                        when (val next = skipSpace()) {
                            'n' -> readNull()
                            '{' -> readObjectAndRegister(type)
                            in '0'..'9' -> readPtr(next)
                            else -> error("Missing { or ptr or null after starting object[], got '$next'")
                        }
                    }, { array, index, value -> array[index] = value })
                    obj.readObjectArray(name, elements)
                } else {
                    when (val next = skipSpace()) {
                        'n' -> obj.readObject(name, readNull())
                        '{' -> obj.readObject(name, readObjectAndRegister(type))
                        in '0'..'9' -> {
                            tmpChar = next
                            val rawPtr = readNumber()
                            val ptr = rawPtr.toIntOrNull() ?: error("Invalid pointer: $rawPtr")
                            if (ptr > 0) {
                                val child = content[ptr]
                                if (child == null) {
                                    addMissingReference(obj, name, ptr)
                                } else {
                                    obj.readObject(name, child)
                                }
                            }
                        }
                        else -> error("Missing { or ptr or null after starting object of class $type, got '$next'")
                    }
                }
            }
        }
        return obj
    }

    private fun readPtr(next: Char): ISaveable? {
        tmpChar = next
        val rawPtr = readNumber()
        val ptr = rawPtr.toIntOrNull() ?: error("Invalid pointer: $rawPtr")
        return if (ptr > 0) {
            content[ptr]
        } else null
    }

    private fun readNull(): Nothing? {
        assert(next(), 'u', "Reading null")
        assert(next(), 'l', "Reading null")
        assert(next(), 'l', "Reading null")
        return null
    }

    private fun readObjectAndRegister(type: String): ISaveable {
        val (value, ptr) = readObject(type)
        register(value, ptr)
        return value
    }

    private fun readObject(type: String): Pair<ISaveable, Int> {
        var child = try {
            getNewClassInstance(type)
        } catch (e: UnknownClassException) {
            println(data)
            throw e
        }
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
                assert(readString(), type)
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
                    readNumber().toIntOrNull() ?: throw InvalidFormatException("Invalid pointer")
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