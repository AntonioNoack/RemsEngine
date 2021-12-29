package me.anno.io.text

import me.anno.io.ISaveable
import me.anno.io.InvalidFormatException
import me.anno.io.base.BaseReader
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.utils.files.LocalFile.toGlobalFile
import org.apache.logging.log4j.LogManager
import org.joml.*

/**
 * reads a JSON-similar format from a text file
 * */
abstract class TextReaderBase : BaseReader() {

    var tmpChar = -1
    private val tmpString = StringBuilder(32)

    private fun getUnusedPointer(): Int {
        return -1
    }

    abstract fun next(): Char

    abstract fun skipSpace(): Char

    override fun readObject(): ISaveable {
        assert(skipSpace(), '"')
        val firstProperty = readString()
        assert(firstProperty == "class", "Expected first property to be 'class', was $firstProperty")
        assert(skipSpace(), ':')
        assert(skipSpace(), '"')
        val clazz = readString()
        val nc0 = skipSpace()
        val obj = getNewClassInstance(clazz)
        if (nc0 == ',') {
            assert(skipSpace(), '"')
            val secondProperty = readString()
            assert(skipSpace(), ':')
            if (secondProperty == "i:*ptr") {
                val ptr = readInt()
                register(obj, ptr)
            } else {
                register(obj)
                readProperty(obj, secondProperty)
            }
            propertyLoop(obj)
        } else {
            assert(nc0, '}')
            register(obj)
        }
        return obj
    }

    override fun readAllInList() {
        assert(skipSpace(), '[')
        while (true) {
            when (val next = skipSpace()) {
                ',' -> Unit // nothing to do
                '{' -> readObject()
                ']' -> return
                else -> throw InvalidFormatException("Unexpected char $next, ${next.code}")
            }
        }
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

        val str = tmpString
        str.clear()

        while (true) {
            when (val char = next()) {
                '\\' -> str.append(
                    when (val char2 = next()) {
                        '\\' -> '\\'
                        'r' -> '\r'
                        'n' -> '\n'
                        't' -> '\t'
                        '"' -> '"'
                        '\'' -> '\''
                        'f' -> 12.toChar()
                        'b' -> '\b'
                        else -> throw InvalidFormatException("Unknown escape sequence \\$char2")
                    }
                )
                '"' -> return str.toString()
                else -> str.append(char)
            }
        }
    }

    /**
     * reads a number string; may return invalid results
     * if a string starts here, the string is read instead
     * */
    private fun readNumber(): String {
        val str = tmpString
        str.clear()
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

    private fun skipLine() {
        while (true) {
            if (next() == '\n') {
                return
            }
        }
    }

    private fun skipComment() {
        when (val next = next()) {
            '/' -> skipLine()
            '*' -> {
                var last = next
                while (true) {
                    val next2 = next()
                    if (next2 == '/' && last == '*') {
                        return
                    }
                    last = next2
                }
            }
            else -> throw InvalidFormatException("Expected a comment after '/', but got '$next'")
        }
    }

    private fun propertyLoop(obj: ISaveable) {
        while (true) {
            when (val next = skipSpace()) {
                ',' -> {// support for extra commas and comments after a comma
                    when (val next2 = skipSpace()) {
                        '"' -> {
                            tmpChar = '"'.code
                            readProperty(obj)
                        }
                        '}' -> return
                        '/' -> skipComment()
                        else -> throw InvalidFormatException("Expected property or end of object after comma, got '$next2'")
                    }
                }
                '}' -> return
                '/' -> skipComment()
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

    private fun readVector2d(allowCommaAtStart: Boolean = false): Vector2d {
        var c0 = skipSpace()
        if (c0 == ',' && allowCommaAtStart) c0 = skipSpace()
        assert(c0, '[', "Start of Vector")
        val rawX = readDouble()
        val sep0 = skipSpace()
        if (sep0 == ']') return Vector2d(rawX)
        assert(sep0, ',', "Separator of Vector")
        val rawY = readDouble()
        assert(skipSpace(), ']', "End of Vector")
        return Vector2d(rawX, rawY)
    }

    private fun readVector3d(allowCommaAtStart: Boolean = false): Vector3d {
        var c0 = skipSpace()
        if (c0 == ',' && allowCommaAtStart) c0 = skipSpace()
        assert(c0, '[', "Start of Vector")
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

    private fun readVector4d(allowCommaAtStart: Boolean = false): Vector4d {
        var c0 = skipSpace()
        if (c0 == ',' && allowCommaAtStart) c0 = skipSpace()
        assert(c0, '[', "Start of Vector")
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

    fun readProperty(obj: ISaveable) {
        assert(skipSpace(), '"')
        val typeName = readString()
        assert(skipSpace(), ':')
        readProperty(obj, typeName)
    }

    private fun readByte() = readLong().toByte()

    private fun readShort() = readLong().toShort()

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

    private fun readInt(): Int = readLong().toInt()

    private fun readLong(): Long {
        // 3x as fast as readNumber().toLong()
        var isFirst = true
        var isNegative = false
        var number = 0L
        loop@ while (true) {
            when (val next = if (isFirst) skipSpace() else next()) {
                '-' -> if (isFirst) {
                    isNegative = true
                } else throw InvalidFormatException("- inside number")
                in '0'..'9' -> number = 10 * number + (next.code - 48)
                '_' -> { // allowed for better readability of large numbers
                }
                '"' -> {
                    if (number == 0L) {
                        number = readLong()
                        assert(next(), '"')
                        break@loop
                    } else throw InvalidFormatException("Unexpected symbol \" inside number!")
                }
                else -> {
                    tmpChar = next.code
                    break@loop
                }
            }
            isFirst = false
        }
        return if (isNegative) -number else +number
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
            "u64", "l", "i64" -> obj.readLong(name, readLong())
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
            "m4x3" -> readWithBrackets(type) {
                obj.readMatrix4x3f(
                    name, Matrix4x3f(
                        readVector3f(),
                        readVector3f(true),
                        readVector3f(true),
                        readVector3f(true)
                    )
                )
            }
            "m4x4" -> readWithBrackets(type) {
                obj.readMatrix4x4f(name, Matrix4f(readVector4f(), readVector4f(), readVector4f(), readVector4f()))
            }
            "m3x3d" -> readWithBrackets(type) {
                obj.readMatrix3x3d(name, Matrix3d(readVector3d(), readVector3d(), readVector3d()))
            }
            "m4x3d" -> readWithBrackets(type) {
                obj.readMatrix4x3d(
                    name, Matrix4x3d(
                        readDouble(), readDouble(), readDouble(),
                        readDouble(), readDouble(), readDouble(),
                        readDouble(), readDouble(), readDouble(),
                        readDouble(), readDouble(), readDouble()
                    )
                )
            }
            "m4x4d" -> readWithBrackets(type) {
                obj.readMatrix4x4d(name, Matrix4d(readVector4d(), readVector4d(), readVector4d(), readVector4d()))
            }
            "AABBf" -> readWithBrackets(type) {
                obj.readAABBf(
                    name, AABBf()
                        .setMin(readVector3f())
                        .setMax(readVector3f(true))
                )
            }
            "AABBd" -> readWithBrackets(type) {
                obj.readAABBd(
                    name, AABBd()
                        .setMin(readVector3d())
                        .setMax(readVector3d(true))
                )
            }
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
                                val child = getByPointer(ptr)
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
        return getByPointer(readInt())
    }

    private fun readNull(): Nothing? {
        assert(next(), 'u', "Reading null")
        assert(next(), 'l', "Reading null")
        assert(next(), 'l', "Reading null")
        return null
    }

    fun register(value: ISaveable) = register(value, getUnusedPointer())

    private fun readObjectAndRegister(type: String): ISaveable {
        val instance = getNewClassInstance(type)
        val firstChar = skipSpace()
        if (firstChar == '}') {
            // nothing to do
            register(instance)
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
                register(instance)
            } else {
                assert(nextChar, ':')
                if (property0 == "*ptr" || property0 == "i:*ptr") {
                    val ptr = readNumber().toIntOrNull() ?: throw InvalidFormatException("Invalid pointer")
                    register(instance, ptr)
                } else {
                    register(instance)
                    readProperty(instance, property0)
                }
                var n = skipSpace()
                if (n != '}') {
                    if (n == ',') n = skipSpace()
                    if (n != '}') {
                        assert(n, '"')
                        tmpChar = '"'.code
                        readProperty(instance)
                        propertyLoop(instance)
                    }
                } // else nothing to do
            }
        }
        return instance
    }

    inline fun readWithBrackets(name: String, run: () -> Unit) {
        assert(skipSpace(), '[', name)
        run()
        assert(skipSpace(), ']', name)
    }

    private fun splitTypeName(typeName: String): Pair<String, String> {
        val index = typeName.indexOf(':')
        if (index < 0) error("Invalid Type:Name '$typeName'")
        val type = typeName.substring(0, index)
        val name = typeName.substring(index + 1)
        return type to name
    }

    companion object {
        private val LOGGER = LogManager.getLogger(TextReaderBase::class)
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