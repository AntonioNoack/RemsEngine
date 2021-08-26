package me.anno.io.text

import me.anno.io.BufferedIO.useBuffered
import me.anno.io.ISaveable
import me.anno.io.InvalidFormatException
import me.anno.io.base.BaseReader
import me.anno.io.base.UnknownClassException
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.utils.files.LocalFile.toGlobalFile
import me.anno.utils.structures.arrays.IntArrayList
import org.apache.logging.log4j.LogManager
import org.joml.*
import java.io.EOFException
import java.io.InputStream
import java.io.InputStreamReader

/**
 * reads a JSON-similar format from a text file
 * */
class TextReader(val data: CharSequence) : BaseReader() {

    val length = data.length
    var index = 0
    var tmpChar = 0

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
                    else -> throw InvalidFormatException("Unexpected char $next, ${next.code}")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun next(): Char {
        if (tmpChar != 0) {
            val v = tmpChar
            tmpChar = 0
            return v.toChar()
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

    private fun readStringValueOrNull(): String? {
        return when (val c = skipSpace()) {
            '"' -> readString()
            'n' -> {
                assert(next(), 'u')
                assert(next(), 'l')
                assert(next(), 'l')
                null
            }
            else -> throw InvalidFormatException("Expected '\"' or 'n' but got $c for readStringValueOrNull")
        }
    }

    private fun readFile(): FileReference {
        return readStringValueOrNull()?.toGlobalFile() ?: InvalidRef
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
            if (index >= dataLength) throw InvalidFormatException("Expected \" at the end of string, $data")
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
                    tmpChar = next.code
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
                else -> throw InvalidFormatException("Unexpected char $next in object of class ${obj.className}")
            }
        }
    }

    fun <ArrayType, InstanceType> readTypedArray(
        typeName: String,
        createArray: (arraySize: Int) -> ArrayType,
        readValue: () -> InstanceType,
        putValue: (array: ArrayType, index: Int, value: InstanceType) -> Unit
    ): ArrayType {
        assert(skipSpace(), '[')
        val rawLength = readNumber()
        val length = rawLength.toIntOrNull() ?: error("Invalid $typeName[] length '$rawLength'")
        // mistakes of the past: ofc, there may be arrays with just zeros...
        /*if (length < (data.length - index) / 2) {
        } else error("Broken file :/, $typeName[].length > data.length ($rawLength)")*/
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

    private fun readVector2f(allowCommaAtStart: Boolean = false): Vector2f {
        var c0 = skipSpace()
        if (c0 == ',' && allowCommaAtStart) c0 = skipSpace()
        assert(c0, '[', "Start of Vector")
        val rawX = readFloat()
        val sep0 = skipSpace()
        if (sep0 == ']') return Vector2f(rawX)
        assert(sep0, ',', "Separator of Vector")
        val rawY = readFloat()
        assert(skipSpace(), ']', "End of Vector")
        return Vector2f(rawX, rawY)
    }

    private fun readVector3f(allowCommaAtStart: Boolean = false): Vector3f {
        var c0 = skipSpace()
        if (c0 == ',' && allowCommaAtStart) c0 = skipSpace()
        assert(c0, '[', "Start of Vector")
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

    private fun readVector4f(allowCommaAtStart: Boolean = false): Vector4f {
        var c0 = skipSpace()
        if (c0 == ',' && allowCommaAtStart) c0 = skipSpace()
        assert(c0, '[', "Start of Vector")
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

    private fun readQuaternionf(): Quaternionf {
        assert(skipSpace(), '[', "Start of Vector")
        val rawX = readFloat()
        val sep0 = skipSpace()
        assert(sep0, ',', "Separator of Vector")
        val rawY = readFloat()
        val sep1 = skipSpace()
        assert(sep1, ',', "Separator of Vector")
        val rawZ = readFloat()
        val sep2 = skipSpace()
        assert(sep2, ',', "Separator of Vector")
        val rawW = readFloat()
        assert(skipSpace(), ']', "End of Vector")
        return Quaternionf(rawX, rawY, rawZ, rawW)
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

    private fun readQuaterniond(): Quaterniond {
        assert(skipSpace(), '[', "Start of Vector")
        val rawX = readDouble()
        val sep0 = skipSpace()
        assert(sep0, ',', "Separator of Vector")
        val rawY = readDouble()
        val sep1 = skipSpace()
        assert(sep1, ',', "Separator of Vector")
        val rawZ = readDouble()
        val sep2 = skipSpace()
        assert(sep2, ',', "Separator of Vector")
        val rawW = readDouble()
        assert(skipSpace(), ']', "End of Vector")
        return Quaterniond(rawX, rawY, rawZ, rawW)
    }

    fun readProperty(obj: ISaveable): ISaveable {
        assert(skipSpace(), '"')
        val typeName = readString()
        assert(skipSpace(), ':')
        return readProperty(obj, typeName)
    }

    private fun readByte() = readNumber().run {
        toInt().toByte()// ?: error("Invalid byte", this)
    }

    private fun readShort() = readNumber().run {
        toInt().toShort()// ?: error("Invalid short", this)
    }

    private fun readChar(): Char {
        return when (val first = skipSpace()) {
            '\'' -> {
                val value = next()
                assert(next(), '\'')
                value
            }
            else -> {
                tmpChar = first.code
                readInt().toChar()
            }
        }
    }

    private fun readInt() = readNumber().run {
        toInt()// ?: error("Invalid int", this)
    }

    private fun readLong() = readNumber().run {
        toLong()// ?: error("Invalid long", this)
    }

    private fun readFloat() = readNumber().run {
        toFloat()// ?: error("Invalid float", this)
    }

    private fun readDouble() = readNumber().run {
        toDouble()// ?: error("Invalid double", this)
    }

    fun readFloatArray() = readTypedArray(
        "float",
        { FloatArray(it) }, { readFloat() },
        { array, index, value -> array[index] = value }
    )

    private fun readDoubleArray() = readTypedArray(
        "double",
        { DoubleArray(it) }, { readDouble() },
        { array, index, value -> array[index] = value }
    )

    private fun readStringArray() = readTypedArray("String",
        { Array(it) { "" } }, { readStringValue() },
        { array, index, value -> array[index] = value }
    )

    private fun readProperty(obj: ISaveable, typeName: String): ISaveable {
        if (typeName == "class") {
            assert(skipSpace(), '"')
            val clazz = readString()
            // could be different in lists
            return if (clazz == obj.className) obj
            else getNewClassInstance(clazz)
        }
        var (type, name) = splitTypeName(typeName)
        when (type) {
            "i1", "b" -> obj.readBoolean(name, readBool())
            "c" -> obj.readChar(name, readChar())
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
            "q4" -> obj.readQuaternionf(name, readQuaternionf())
            "v2d" -> obj.readVector2d(name, readVector2d())
            "v3d" -> obj.readVector3d(name, readVector3d())
            "v4d" -> obj.readVector4d(name, readVector4d())
            "q4d" -> obj.readQuaterniond(name, readQuaterniond())
            "v2[]" -> {
                val v0 = Vector2f()
                obj.readVector2fArray(
                    name,
                    readTypedArray("vector2f",
                        { Array(it) { v0 } }, { readVector2f() },
                        { array, index, value -> array[index] = value })
                )
            }
            "v3[]" -> {
                val v0 = Vector3f()
                obj.readVector3fArray(
                    name,
                    readTypedArray("vector3f",
                        { Array(it) { v0 } }, { readVector3f() },
                        { array, index, value -> array[index] = value })
                )
            }
            "v4[]" -> {
                val v0 = Vector4f()
                obj.readVector4fArray(
                    name,
                    readTypedArray("vector4f",
                        { Array(it) { v0 } }, { readVector4f() },
                        { array, index, value -> array[index] = value })
                )
            }
            "v2d[]" -> {
                val v0 = Vector2d()
                obj.readVector2dArray(
                    name,
                    readTypedArray("vector2d",
                        { Array(it) { v0 } }, { readVector2d() },
                        { array, index, value -> array[index] = value })
                )
            }
            "v3d[]" -> {
                val v0 = Vector3d()
                obj.readVector3dArray(
                    name,
                    readTypedArray("vector3d",
                        { Array(it) { v0 } }, { readVector3d() },
                        { array, index, value -> array[index] = value })
                )
            }
            "v4d[]" -> {
                val v0 = Vector4d()
                obj.readVector4dArray(
                    name,
                    readTypedArray("vector4d",
                        { Array(it) { v0 } }, { readVector4d() },
                        { array, index, value -> array[index] = value })
                )
            }
            "m3x3" -> {
                assert(skipSpace(), '[', "Start of m3x3")
                obj.readMatrix3x3f(
                    name, Matrix3f(
                        readVector3f(),
                        readVector3f(true),
                        readVector3f(true)
                    )
                )
                assert(skipSpace(), ']', "End of m3x3")
            }
            "m4x3" -> {
                assert(skipSpace(), '[', "Start of m4x3")
                obj.readMatrix4x3f(
                    name, Matrix4x3f(
                        readVector3f(),
                        readVector3f(true),
                        readVector3f(true),
                        readVector3f(true)
                    )
                )
                assert(skipSpace(), ']', "End of m4x3")
            }
            "m4x4" -> obj.readMatrix4x4f(name, Matrix4f(readVector4f(), readVector4f(), readVector4f(), readVector4f()))
            "m3x3d" -> obj.readMatrix3x3d(name, Matrix3d(readVector3d(), readVector3d(), readVector3d()))
            "m4x3d" -> obj.readMatrix4x3d(
                name, Matrix4x3d(
                    readDouble(), readDouble(), readDouble(),
                    readDouble(), readDouble(), readDouble(),
                    readDouble(), readDouble(), readDouble(),
                    readDouble(), readDouble(), readDouble()
                )
            )
            "m4x4d" -> obj.readMatrix4x4d(
                name,
                Matrix4d(readVector4d(), readVector4d(), readVector4d(), readVector4d())
            )
            "S" -> obj.readString(name, readStringValue())
            "S[]" -> obj.readStringArray(name, readStringArray())
            "S[][]" -> {
                val a0 = Array(0) { "" }
                obj.readStringArray2D(name, readTypedArray("String[]",
                    { Array(it) { a0 } }, { readStringArray() },
                    { array, index, value -> array[index] = value })
                )
            }
            "R" -> obj.readFile(name, readFile())
            "R[]" -> obj.readFileArray(
                name, readTypedArray("FileRef",
                    { Array(it) { InvalidRef } }, { readFile() },
                    { array, index, value -> array[index] = value })
            )
            "R[][]" -> {
                val a0 = Array<FileReference>(0) { InvalidRef }
                obj.readFileArray2D(
                    name, readTypedArray("FileRef",
                        { Array(it) { a0 } }, {
                            readTypedArray("FileRef[]",
                                { Array<FileReference>(it) { InvalidRef } }, { readFile() },
                                { array, index, value -> array[index] = value })
                        },
                        { array, index, value -> array[index] = value })
                )
            }
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
                            tmpChar = next.code
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
        tmpChar = next.code
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
            LOGGER.info(data)
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
                        tmpChar = '"'.code
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

        fun read(data: CharSequence): List<ISaveable> {
            val reader = TextReader(data)
            reader.readAllInList()
            // sorting is very important
            return reader.sortedContent
        }

        fun read(input: FileReference): List<ISaveable> {
            // buffered is very important and delivers an improvement of 5x
            return input.inputStream().useBuffered().use { read(it, input.length().toInt()) }
        }

        fun read(input: InputStream, length: Int = -1): List<ISaveable> {
            return read(InputStreamCharSequence(input.useBuffered(), length))
        }

        fun clone(element: ISaveable): ISaveable? {
            return read(TextWriter.toText(element, false)).getOrNull(0)
        }

    }

    class InputStreamCharSequence(val input: InputStream, length: Int) : CharSequence {

        // probably we could only memorize the last n bytes, since more won't be needed
        // or the TextReader could give the signal, that we're allowed to forget

        // to support non-ascii charsets
        private val reader = InputStreamReader(input)

        private val memory = IntArrayList(512)
        override var length: Int = if (length <= 0) Int.MAX_VALUE else length

        private fun ensureIndex(index: Int) {
            while (memory.size <= kotlin.math.min(index, length)) {
                val read = reader.read()
                if (read < 0) {
                    this.length = kotlin.math.min(
                        this.length,
                        memory.size
                    )
                }
                memory.add(read)
            }
            while (memory.size <= index) {
                memory.add(0)
            }
        }

        override fun get(index: Int): Char {
            ensureIndex(index)
            return memory[index].toChar()
        }

        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
            ensureIndex(endIndex - 1)
            return String(CharArray(endIndex - startIndex) {
                memory[startIndex + it].toChar()
            })
        }

        override fun toString(): String {
            val builder = StringBuilder(kotlin.math.min(1024, length))
            var index = -1
            while (++index < length) builder.append(get(index))
            return builder.toString()
        }

    }

}

/*fun main() { // a test, because I had a bug
    val readTest = OS.desktop.getChild("fbx.yaml")
    val fakeString = TextReader.InputStreamCharSequence(readTest.inputStream(), readTest.length().toInt())
    var i = 0
    while (i < fakeString.length) {
        val char = fakeString[i++]
        print(char)
    }
    logger.info()
    logger.info("characters: $i")
}*/