package me.anno.io.text

import me.anno.io.ISaveable
import me.anno.io.base.BaseReader
import me.anno.io.base.InvalidFormatException
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.utils.files.LocalFile.toGlobalFile
import org.apache.logging.log4j.LogManager
import org.joml.*

/**
 * reads a JSON-similar format from a text file
 * */
abstract class TextReaderBase(val workspace: FileReference) : BaseReader() {

    var tmpChar = -1
    private var lineNumber = 1
    private var lineIndex = 0 // index within line

    fun readNext(char: Char) {
        readNext(char.code)
    }

    fun readNext(char: Int) {
        if (char == '\n'.code) {
            lineNumber++
            lineIndex = 0
        } else lineIndex++
    }

    private val tmpString = StringBuilder(32)

    private fun getUnusedPointer(): Int {
        return -1
    }

    abstract fun next(): Char

    abstract fun skipSpace(): Char

    override fun readObject(): ISaveable {
        assertEquals(skipSpace(), '"')
        val firstProperty = readString()
        assertEquals(firstProperty, "class", "Expected first property to be 'class'")
        assertEquals(skipSpace(), ':')
        assertEquals(skipSpace(), '"')
        val clazz = readString()
        val nc0 = skipSpace()
        val obj = getNewClassInstance(clazz)
        allInstances.add(obj)
        if (nc0 == ',') {
            assertEquals(skipSpace(), '"')
            val secondProperty = readString()
            assertEquals(skipSpace(), ':')
            if (secondProperty == "i:*ptr") {
                val ptr = readInt()
                register(obj, ptr)
            } else {
                register(obj)
                readProperty(obj, secondProperty)
            }
            propertyLoop(obj)
        } else {
            assertEquals(nc0, '}')
            register(obj)
        }
        return obj
    }

    override fun readAllInList() {
        assertEquals(skipSpace(), '[')
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
                assertEquals(next(), 'u')
                assertEquals(next(), 'l')
                assertEquals(next(), 'l')
                null
            }
            else -> throw InvalidFormatException("Expected '\"' or 'n' but got $c for readStringValueOrNull")
        }
    }

    private fun readFile(): FileReference {
        return readStringValueOrNull()?.toGlobalFile(workspace) ?: InvalidRef
    }

    private fun readStringValue(): String {
        assertEquals(skipSpace(), '"', "Reading String")
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

    private fun <ArrayType, InstanceType> readArray(
        typeName: String,
        createArray: (arraySize: Int) -> ArrayType,
        readValue: () -> InstanceType,
        putValue: (array: ArrayType, index: Int, value: InstanceType) -> Unit
    ): ArrayType {
        assertEquals(skipSpace(), '[')
        val rawLength = readLong()
        if (rawLength > Int.MAX_VALUE || rawLength < 0) error("Invalid $typeName[] length '$rawLength'")
        val length = rawLength.toInt()
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
                else -> error("unknown character $next in $typeName[] in $lineNumber:$lineIndex")
            }
        }
        if (i > length) LOGGER.warn("$typeName[] contained too many elements!")
        return values
    }

    private inline fun <reified Type> readArray(
        typeName: String, a0: Type,
        crossinline readValue: () -> Type,
    ): Array<Type> {
        return readArray(typeName,
            { Array(it) { a0 } }, { readValue() },
            { array, index, value -> array[index] = value }
        )
    }

    private inline fun <reified Type2> readArray2(
        typeName: String, sampleArray: Array<Type2>,
        crossinline readValue: () -> Type2,
    ): Array<Array<Type2>> {
        val sampleInstance = sampleArray[0]
        return readArray(typeName,
            { Array(it) { sampleArray } },
            { readArray(typeName, sampleInstance, readValue) },
            { array, index, value -> array[index] = value }
        )
    }

    private fun readBool(): Boolean {
        return when (val firstChar = skipSpace()) {
            '0' -> false
            '1' -> true
            't', 'T' -> {
                assertEquals(next(), 'r', "boolean:true")
                assertEquals(next(), 'u', "boolean:true")
                assertEquals(next(), 'e', "boolean:true")
                true
            }
            'f', 'F' -> {
                assertEquals(next(), 'a', "boolean:false")
                assertEquals(next(), 'l', "boolean:false")
                assertEquals(next(), 's', "boolean:false")
                assertEquals(next(), 'e', "boolean:false")
                false
            }
            else -> throw InvalidFormatException("Unknown boolean value starting with $firstChar")
        }
    }

    private fun readVector2f(allowCommaAtStart: Boolean = false): Vector2f {
        var c0 = skipSpace()
        if (c0 == ',' && allowCommaAtStart) c0 = skipSpace()
        assertEquals(c0, '[', "Start of Vector")
        val rawX = readFloat()
        val sep0 = skipSpace()
        if (sep0 == ']') return Vector2f(rawX)
        assertEquals(sep0, ',', "Separator of Vector")
        val rawY = readFloat()
        assertEquals(skipSpace(), ']', "End of Vector")
        return Vector2f(rawX, rawY)
    }

    private fun readVector3f(allowCommaAtStart: Boolean = false): Vector3f {
        var c0 = skipSpace()
        if (c0 == ',' && allowCommaAtStart) c0 = skipSpace()
        assertEquals(c0, '[', "Start of Vector")
        val rawX = readFloat()
        val sep0 = skipSpace()
        if (sep0 == ']') return Vector3f(rawX) // monotone / grayscale
        assertEquals(sep0, ',', "Separator of Vector")
        val rawY = readFloat()
        assertEquals(skipSpace(), ',', "Separator of Vector")
        val rawZ = readFloat()
        assertEquals(skipSpace(), ']', "End of Vector")
        return Vector3f(rawX, rawY, rawZ)
    }

    private fun readVector4f(allowCommaAtStart: Boolean = false): Vector4f {
        var c0 = skipSpace()
        if (c0 == ',' && allowCommaAtStart) c0 = skipSpace()
        assertEquals(c0, '[', "Start of Vector")
        val rawX = readFloat()
        val sep0 = skipSpace()
        if (sep0 == ']') return Vector4f(rawX) // monotone
        assertEquals(sep0, ',', "Separator of Vector")
        val rawY = readFloat()
        val sep1 = skipSpace()
        if (sep1 == ']') return Vector4f(rawX, rawX, rawX, rawY) // white with alpha
        assertEquals(sep1, ',', "Separator of Vector")
        val rawZ = readFloat()
        val sep2 = skipSpace()
        if (sep2 == ']') return Vector4f(rawX, rawY, rawZ, 1f) // opaque color
        assertEquals(sep2, ',', "Separator of Vector")
        val rawW = readFloat()
        assertEquals(skipSpace(), ']', "End of Vector")
        return Vector4f(rawX, rawY, rawZ, rawW)
    }

    private fun readQuaternionf(): Quaternionf {
        assertEquals(skipSpace(), '[', "Start of Vector")
        val rawX = readFloat()
        val sep0 = skipSpace()
        assertEquals(sep0, ',', "Separator of Vector")
        val rawY = readFloat()
        val sep1 = skipSpace()
        assertEquals(sep1, ',', "Separator of Vector")
        val rawZ = readFloat()
        val sep2 = skipSpace()
        assertEquals(sep2, ',', "Separator of Vector")
        val rawW = readFloat()
        assertEquals(skipSpace(), ']', "End of Vector")
        return Quaternionf(rawX, rawY, rawZ, rawW)
    }

    private fun readVector2d(allowCommaAtStart: Boolean = false): Vector2d {
        var c0 = skipSpace()
        if (c0 == ',' && allowCommaAtStart) c0 = skipSpace()
        assertEquals(c0, '[', "Start of Vector")
        val rawX = readDouble()
        val sep0 = skipSpace()
        if (sep0 == ']') return Vector2d(rawX)
        assertEquals(sep0, ',', "Separator of Vector")
        val rawY = readDouble()
        assertEquals(skipSpace(), ']', "End of Vector")
        return Vector2d(rawX, rawY)
    }

    private fun readVector3d(allowCommaAtStart: Boolean = false): Vector3d {
        var c0 = skipSpace()
        if (c0 == ',' && allowCommaAtStart) c0 = skipSpace()
        assertEquals(c0, '[', "Start of Vector")
        val rawX = readDouble()
        val sep0 = skipSpace()
        if (sep0 == ']') return Vector3d(rawX) // monotone / grayscale
        assertEquals(sep0, ',', "Separator of Vector")
        val rawY = readDouble()
        assertEquals(skipSpace(), ',', "Separator of Vector")
        val rawZ = readDouble()
        assertEquals(skipSpace(), ']', "End of Vector")
        return Vector3d(rawX, rawY, rawZ)
    }

    private fun readVector4d(allowCommaAtStart: Boolean = false): Vector4d {
        var c0 = skipSpace()
        if (c0 == ',' && allowCommaAtStart) c0 = skipSpace()
        assertEquals(c0, '[', "Start of Vector")
        val rawX = readDouble()
        val sep0 = skipSpace()
        if (sep0 == ']') return Vector4d(rawX) // monotone
        assertEquals(sep0, ',', "Separator of Vector")
        val rawY = readDouble()
        val sep1 = skipSpace()
        if (sep1 == ']') return Vector4d(rawX, rawX, rawX, rawY) // white with alpha
        assertEquals(sep1, ',', "Separator of Vector")
        val rawZ = readDouble()
        val sep2 = skipSpace()
        if (sep2 == ']') return Vector4d(rawX, rawY, rawZ, 1.0) // opaque color
        assertEquals(sep2, ',', "Separator of Vector")
        val rawW = readDouble()
        assertEquals(skipSpace(), ']', "End of Vector")
        return Vector4d(rawX, rawY, rawZ, rawW)
    }

    private fun readPlanef(allowCommaAtStart: Boolean = false): Planef {
        var c0 = skipSpace()
        if (c0 == ',' && allowCommaAtStart) c0 = skipSpace()
        assertEquals(c0, '[', "Start of Vector")
        val rawX = readFloat()
        val sep0 = skipSpace()
        assertEquals(sep0, ',', "Separator of Vector")
        val rawY = readFloat()
        val sep1 = skipSpace()
        assertEquals(sep1, ',', "Separator of Vector")
        val rawZ = readFloat()
        val sep2 = skipSpace()
        assertEquals(sep2, ',', "Separator of Vector")
        val rawW = readFloat()
        assertEquals(skipSpace(), ']', "End of Vector")
        return Planef(rawX, rawY, rawZ, rawW)
    }

    private fun readPlaned(allowCommaAtStart: Boolean = false): Planed {
        var c0 = skipSpace()
        if (c0 == ',' && allowCommaAtStart) c0 = skipSpace()
        assertEquals(c0, '[', "Start of Vector")
        val rawX = readDouble()
        val sep0 = skipSpace()
        assertEquals(sep0, ',', "Separator of Vector")
        val rawY = readDouble()
        val sep1 = skipSpace()
        assertEquals(sep1, ',', "Separator of Vector")
        val rawZ = readDouble()
        val sep2 = skipSpace()
        assertEquals(sep2, ',', "Separator of Vector")
        val rawW = readDouble()
        assertEquals(skipSpace(), ']', "End of Vector")
        return Planed(rawX, rawY, rawZ, rawW)
    }

    private fun readQuaterniond(): Quaterniond {
        assertEquals(skipSpace(), '[', "Start of Vector")
        val rawX = readDouble()
        val sep0 = skipSpace()
        assertEquals(sep0, ',', "Separator of Vector")
        val rawY = readDouble()
        val sep1 = skipSpace()
        assertEquals(sep1, ',', "Separator of Vector")
        val rawZ = readDouble()
        val sep2 = skipSpace()
        assertEquals(sep2, ',', "Separator of Vector")
        val rawW = readDouble()
        assertEquals(skipSpace(), ']', "End of Vector")
        return Quaterniond(rawX, rawY, rawZ, rawW)
    }

    private fun readVector2i(allowCommaAtStart: Boolean = false): Vector2i {
        var c0 = skipSpace()
        if (c0 == ',' && allowCommaAtStart) c0 = skipSpace()
        assertEquals(c0, '[', "Start of Vector")
        val rawX = readInt()
        val sep0 = skipSpace()
        if (sep0 == ']') return Vector2i(rawX)
        assertEquals(sep0, ',', "Separator of Vector")
        val rawY = readInt()
        assertEquals(skipSpace(), ']', "End of Vector")
        return Vector2i(rawX, rawY)
    }

    private fun readVector3i(allowCommaAtStart: Boolean = false): Vector3i {
        var c0 = skipSpace()
        if (c0 == ',' && allowCommaAtStart) c0 = skipSpace()
        assertEquals(c0, '[', "Start of Vector")
        val rawX = readInt()
        val sep0 = skipSpace()
        if (sep0 == ']') return Vector3i(rawX) // monotone / grayscale
        assertEquals(sep0, ',', "Separator of Vector")
        val rawY = readInt()
        assertEquals(skipSpace(), ',', "Separator of Vector")
        val rawZ = readInt()
        assertEquals(skipSpace(), ']', "End of Vector")
        return Vector3i(rawX, rawY, rawZ)
    }

    private fun readVector4i(allowCommaAtStart: Boolean = false): Vector4i {
        var c0 = skipSpace()
        if (c0 == ',' && allowCommaAtStart) c0 = skipSpace()
        assertEquals(c0, '[', "Start of Vector")
        val rawX = readInt()
        val sep0 = skipSpace()
        if (sep0 == ']') return Vector4i(rawX) // monotone
        assertEquals(sep0, ',', "Separator of Vector")
        val rawY = readInt()
        val sep1 = skipSpace()
        if (sep1 == ']') return Vector4i(rawX, rawX, rawX, rawY) // white with alpha
        assertEquals(sep1, ',', "Separator of Vector")
        val rawZ = readInt()
        val sep2 = skipSpace()
        if (sep2 == ']') return Vector4i(rawX, rawY, rawZ, 255) // opaque color
        assertEquals(sep2, ',', "Separator of Vector")
        val rawW = readInt()
        assertEquals(skipSpace(), ']', "End of Vector")
        return Vector4i(rawX, rawY, rawZ, rawW)
    }

    private fun readMatrix2x2(): Matrix2f {
        assertEquals(skipSpace(), '[', "Start of m2x2")
        val m = Matrix2f(
            readVector2f(),
            readVector2f(true),
        )
        assertEquals(skipSpace(), ']', "End of m2x2")
        return m
    }

    private fun readMatrix2x2d(): Matrix2d {
        assertEquals(skipSpace(), '[', "Start of m2x2d")
        val m = Matrix2d(
            readVector2d(),
            readVector2d(true),
        )
        assertEquals(skipSpace(), ']', "End of m2x2d")
        return m
    }

    private fun readMatrix3x2(): Matrix3x2f {
        assertEquals(skipSpace(), '[', "Start of m3x2")
        val a = readVector2f()
        val b = readVector2f(true)
        val c = readVector2f(true)
        val m = Matrix3x2f(
            a.x, a.y,
            b.x, b.y,
            c.x, c.y
        )
        assertEquals(skipSpace(), ']', "End of m3x2")
        return m
    }

    private fun readMatrix3x2d(): Matrix3x2d {
        assertEquals(skipSpace(), '[', "Start of m3x2d")
        val a = readVector2d()
        val b = readVector2d(true)
        val c = readVector2d(true)
        val m = Matrix3x2d(
            a.x, a.y,
            b.x, b.y,
            c.x, c.y
        )
        assertEquals(skipSpace(), ']', "End of m3x2d")
        return m
    }

    private fun readMatrix3x3(): Matrix3f {
        assertEquals(skipSpace(), '[', "Start of m3x3")
        val m = Matrix3f(
            readVector3f(),
            readVector3f(true),
            readVector3f(true)
        )
        assertEquals(skipSpace(), ']', "End of m3x3")
        return m
    }

    private fun readMatrix3x3d(): Matrix3d {
        assertEquals(skipSpace(), '[', "Start of m3x3d")
        val m = Matrix3d(
            readVector3d(),
            readVector3d(true),
            readVector3d(true)
        )
        assertEquals(skipSpace(), ']', "End of m3x3d")
        return m
    }

    private fun readMatrix4x3(): Matrix4x3f {
        assertEquals(skipSpace(), '[', "Start of m4x3")
        val m = Matrix4x3f(
            readVector3f(),
            readVector3f(true),
            readVector3f(true),
            readVector3f(true)
        )
        assertEquals(skipSpace(), ']', "End of m4x3")
        return m
    }

    private fun readMatrix4x3d(): Matrix4x3d {
        assertEquals(skipSpace(), '[', "Start of m4x3d")
        val m = Matrix4x3d() // constructor is missing somehow...
        m.set(
            readVector3d(),
            readVector3d(true),
            readVector3d(true),
            readVector3d(true)
        )
        assertEquals(skipSpace(), ']', "End of m4x3d")
        return m
    }

    private fun readMatrix4x4(): Matrix4f {
        assertEquals(skipSpace(), '[', "Start of m4x4")
        val m = Matrix4f(
            readVector4f(),
            readVector4f(true),
            readVector4f(true),
            readVector4f(true)
        )
        assertEquals(skipSpace(), ']', "End of m4x4")
        return m
    }

    private fun readMatrix4x4d(): Matrix4d {
        assertEquals(skipSpace(), '[', "Start of m4x4d")
        val m = Matrix4d(
            readVector4d(),
            readVector4d(true),
            readVector4d(true),
            readVector4d(true)
        )
        assertEquals(skipSpace(), ']', "End of m4x4d")
        return m
    }

    fun readProperty(obj: ISaveable) {
        assertEquals(skipSpace(), '"')
        val typeName = readString()
        assertEquals(skipSpace(), ':')
        readProperty(obj, typeName)
    }

    private fun readByte() = readLong().toByte()

    private fun readShort() = readLong().toShort()

    private fun readChar(): Char {
        return when (val first = skipSpace()) {
            '\'', '"' -> {
                val value = next()
                assertEquals(next(), first)
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
        // originally 3x as fast as readNumber().toLong()
        var isFirst = true
        var isNegative = false
        var isColor = false
        var number = 0L
        var numDigits = 0
        var base = 10
        loop@ while (true) {
            when (val next = if (isFirst) skipSpace() else next()) {
                '0' -> {
                    if (isFirst) base = 8 // oct mode activated; continue with x for hex
                    number *= base
                    numDigits++
                }
                // 156
                in '1'..'9' -> {
                    number = base * number + (next.code - 48)
                    numDigits++
                }
                // 0b011101
                'b', 'B' -> {
                    if (base == 8 && numDigits == 1) base = 2
                    else {
                        number = base * number + 11
                        numDigits++
                    }
                }
                // 0x1dffa
                in 'A'..'F' -> if (isColor || base > 10) {
                    number = base * number + (next.code - 55)
                    numDigits++
                } else {
                    tmpChar = next.code
                    break@loop
                }
                in 'a'..'f' -> if (isColor || base > 10) {
                    number = base * number + (next.code - 87)
                    numDigits++
                } else {
                    tmpChar = next.code
                    break@loop
                }
                '-' -> if (isFirst) {
                    isNegative = true
                } else {
                    tmpChar = next.code
                    break@loop
                }
                '#' -> if (isFirst) {
                    isColor = true
                    base = 16
                } else {
                    tmpChar = next.code
                    break@loop
                }
                'x', 'X' -> {
                    base = 16
                }
                '_' -> { // allowed for better readability of large numbers
                }
                '"', '\'' -> {
                    if (isFirst) {
                        number = readLong()
                        assertEquals(next(), next)
                        break@loop
                    } else {
                        tmpChar = next.code
                        break@loop
                    }
                }
                else -> {
                    tmpChar = next.code
                    break@loop
                }
            }
            isFirst = false
        }
        return when {
            isColor -> when (numDigits) {
                0 -> 0
                1 -> number * 0x111111 + black
                3 -> number.and(0xf) * 0x11 +
                        number.and(0xf0) * 0x110 +
                        number.and(0xf00) * 0x1100 + black
                4 -> number.and(0xf) * 0x11 +
                        number.and(0xf0) * 0x110 +
                        number.and(0xf00) * 0x1100 +
                        number.and(0xf000) * 0x11000
                6 -> number or black
                else -> number
            }.toInt().toLong()
            isNegative -> -number
            else -> number
        }
    }

    private fun readFloat() = readNumber().run {
        toFloat()// ?: error("Invalid float", this)
    }

    private fun readDouble() = readNumber().run {
        toDouble()// ?: error("Invalid double", this)
    }

    private fun readBoolArray() = readArray("boolean",
        { BooleanArray(it) }, { readBool() },
        { array, index, value -> array[index] = value })

    private fun readCharArray() = readArray("char",
        { CharArray(it) }, { readChar() },
        { array, index, value -> array[index] = value })

    private fun readByteArray() = readArray("byte",
        { ByteArray(it) }, { readByte() },
        { array, index, value -> array[index] = value })

    private fun readShortArray() = readArray("short",
        { ShortArray(it) }, { readShort() },
        { array, index, value -> array[index] = value })

    private fun readIntArray() = readArray("int",
        { IntArray(it) }, { readInt() },
        { array, index, value -> array[index] = value })

    private fun readLongArray() = readArray("long",
        { LongArray(it) }, { readLong() },
        { array, index, value -> array[index] = value })

    private fun readFloatArray() = readArray(
        "float",
        { FloatArray(it) }, { readFloat() },
        { array, index, value -> array[index] = value }
    )

    private fun readDoubleArray() = readArray(
        "double",
        { DoubleArray(it) }, { readDouble() },
        { array, index, value -> array[index] = value }
    )

    private fun readProperty(obj: ISaveable, typeName: String): ISaveable {
        if (typeName == "class") {
            assertEquals(skipSpace(), '"')
            val clazz = readString()
            // could be different in lists
            return if (clazz == obj.className) obj
            else {
                val obj2 = getNewClassInstance(clazz)
                allInstances.add(obj2)
                obj2
            }
        }
        var (type, name) = splitTypeName(typeName)
        when (type) {
            "i1", "b" -> obj.readBoolean(name, readBool())
            "c" -> obj.readChar(name, readChar())
            "i8", "B" -> obj.readByte(name, readByte())
            "i16", "s" -> obj.readShort(name, readShort())
            "i32", "i", "col" -> obj.readInt(name, readInt())
            "u64", "l", "i64" -> obj.readLong(name, readLong())
            "f32", "f" -> obj.readFloat(name, readFloat())
            "f64", "d" -> obj.readDouble(name, readDouble())
            "i1[]", "b[]" -> obj.readBooleanArray(name, readBoolArray())
            "i1[][]", "b[][]" -> obj.readBooleanArray2D(name, readArray("bool[]", boolArray0) { readBoolArray() })
            "c[]" -> obj.readCharArray(name, readCharArray())
            "c[][]" -> obj.readCharArray2D(name, readArray("char[]", charArray0) { readCharArray() })
            "i8[]", "B[]" -> obj.readByteArray(name, readByteArray())
            "i8[][]", "B[][]" -> obj.readByteArray2D(name, readArray("byte[]", byteArray0) { readByteArray() })
            "i16[]", "s[]" -> obj.readShortArray(name, readShortArray())
            "i16[][]", "s[][]" -> obj.readShortArray2D(name, readArray("short[]", shortArray0) { readShortArray() })
            "i32[]", "i[]", "col[]" -> obj.readIntArray(name, readIntArray())
            "i32[][]", "i[][]", "col[][]" -> obj.readIntArray2D(name, readArray("int[]", intArray0) { readIntArray() })
            "i64[]", "u64[]", "l[]" -> obj.readLongArray(name, readLongArray())
            "i64[][]", "u64[][]", "l[][]" ->
                obj.readLongArray2D(name, readArray("long[]", longArray0) { readLongArray() })
            "f32[]", "f[]" -> obj.readFloatArray(name, readFloatArray())
            "f32[][]", "f[][]" -> obj.readFloatArray2D(name, readArray("float[]", floatArray0) { readFloatArray() })
            "d[]" -> obj.readDoubleArray(name, readDoubleArray())
            "d[][]" -> obj.readDoubleArray2D(name, readArray("double[]", doubleArray0) { readDoubleArray() })
            "v2" -> obj.readVector2f(name, readVector2f())
            "v3" -> obj.readVector3f(name, readVector3f())
            "v4" -> obj.readVector4f(name, readVector4f())
            "q4", "q4f" -> obj.readQuaternionf(name, readQuaternionf())
            "v2d" -> obj.readVector2d(name, readVector2d())
            "v3d" -> obj.readVector3d(name, readVector3d())
            "v4d" -> obj.readVector4d(name, readVector4d())
            "p4", "p4f" -> obj.readPlanef(name, readPlanef())
            "p4d" -> obj.readPlaned(name, readPlaned())
            "q4d" -> obj.readQuaterniond(name, readQuaterniond())
            "v2i" -> obj.readVector2i(name, readVector2i())
            "v3i" -> obj.readVector3i(name, readVector3i())
            "v4i" -> obj.readVector4i(name, readVector4i())
            "v2[]" -> obj.readVector2fArray(name, readArray("vector2f", vector2f0) { readVector2f() })
            "v3[]" -> obj.readVector3fArray(name, readArray("vector3f", vector3f0) { readVector3f() })
            "v4[]" -> obj.readVector4fArray(name, readArray("vector4f", vector4f0) { readVector4f() })
            "v2d[]" -> obj.readVector2dArray(name, readArray("vector2d", vector2d0) { readVector2d() })
            "v3d[]" -> obj.readVector3dArray(name, readArray("vector3d", vector3d0) { readVector3d() })
            "v4d[]" -> obj.readVector4dArray(name, readArray("vector4d", vector4d0) { readVector4d() })
            "v2i[]" -> obj.readVector2iArray(name, readArray("vector2i", vector2i0) { readVector2i() })
            "v3i[]" -> obj.readVector3iArray(name, readArray("vector3i", vector3i0) { readVector3i() })
            "v4i[]" -> obj.readVector4iArray(name, readArray("vector4i", vector4i0) { readVector4i() })
            "v2[][]" -> obj.readVector2fArray2D(name, readArray2("vector2f[]", vector2f0a) { readVector2f() })
            "v3[][]" -> obj.readVector3fArray2D(name, readArray2("vector3f[]", vector3f0a) { readVector3f() })
            "v4[][]" -> obj.readVector4fArray2D(name, readArray2("vector4f[]", vector4f0a) { readVector4f() })
            "v2d[][]" -> obj.readVector2dArray2D(name, readArray2("vector2d[]", vector2d0a) { readVector2d() })
            "v3d[][]" -> obj.readVector3dArray2D(name, readArray2("vector3d[]", vector3d0a) { readVector3d() })
            "v4d[][]" -> obj.readVector4dArray2D(name, readArray2("vector4d[]", vector4d0a) { readVector4d() })
            "v2i[][]" -> obj.readVector2iArray2D(name, readArray2("vector2i[]", vector2i0a) { readVector2i() })
            "v3i[][]" -> obj.readVector3iArray2D(name, readArray2("vector3i[]", vector3i0a) { readVector3i() })
            "v4i[][]" -> obj.readVector4iArray2D(name, readArray2("vector4i[]", vector4i0a) { readVector4i() })
            "m2x2" -> obj.readMatrix2x2f(name, readMatrix2x2())
            "m3x2" -> obj.readMatrix3x2f(name, readMatrix3x2())
            "m3x3" -> obj.readMatrix3x3f(name, readMatrix3x3())
            "m4x3" -> obj.readMatrix4x3f(name, readMatrix4x3())
            "m4x4" -> obj.readMatrix4x4f(name, readMatrix4x4())
            "m2x2[]" -> obj.readMatrix2x2fArray(name, readArray("m2x2", matrix2x2) { readMatrix2x2() })
            "m3x2[]" -> obj.readMatrix3x2fArray(name, readArray("m3x2", matrix3x2) { readMatrix3x2() })
            "m3x3[]" -> obj.readMatrix3x3fArray(name, readArray("m3x3", matrix3x3) { readMatrix3x3() })
            "m4x3[]" -> obj.readMatrix4x3fArray(name, readArray("m4x3", matrix4x3) { readMatrix4x3() })
            "m4x4[]" -> obj.readMatrix4x4fArray(name, readArray("m4x4", matrix4x4) { readMatrix4x4() })
            "m2x2[][]" -> obj.readMatrix2x2fArray2D(name, readArray2("m2x2", matrix2x2a) { readMatrix2x2() })
            "m3x2[][]" -> obj.readMatrix3x2fArray2D(name, readArray2("m3x2", matrix3x2a) { readMatrix3x2() })
            "m3x3[][]" -> obj.readMatrix3x3fArray2D(name, readArray2("m3x3", matrix3x3a) { readMatrix3x3() })
            "m4x3[][]" -> obj.readMatrix4x3fArray2D(name, readArray2("m4x3", matrix4x3a) { readMatrix4x3() })
            "m4x4[][]" -> obj.readMatrix4x4fArray2D(name, readArray2("m4x4", matrix4x4a) { readMatrix4x4() })
            "m2x2d" -> obj.readMatrix2x2d(name, readMatrix2x2d())
            "m3x2d" -> obj.readMatrix3x2d(name, readMatrix3x2d())
            "m3x3d" -> obj.readMatrix3x3d(name, readMatrix3x3d())
            "m4x3d" -> obj.readMatrix4x3d(name, readMatrix4x3d())
            "m4x4d" -> obj.readMatrix4x4d(name, readMatrix4x4d())
            "m2x2d[]" -> obj.readMatrix2x2dArray(name, readArray("m2x2d", matrix2x2d) { readMatrix2x2d() })
            "m3x2d[]" -> obj.readMatrix3x2dArray(name, readArray("m3x2d", matrix3x2d) { readMatrix3x2d() })
            "m3x3d[]" -> obj.readMatrix3x3dArray(name, readArray("m3x3d", matrix3x3d) { readMatrix3x3d() })
            "m4x3d[]" -> obj.readMatrix4x3dArray(name, readArray("m4x3d", matrix4x3d) { readMatrix4x3d() })
            "m4x4d[]" -> obj.readMatrix4x4dArray(name, readArray("m4x4d", matrix4x4d) { readMatrix4x4d() })
            "m2x2d[][]" -> obj.readMatrix2x2dArray2D(name, readArray2("m2x2d", matrix2x2da) { readMatrix2x2d() })
            "m3x2d[][]" -> obj.readMatrix3x2dArray2D(name, readArray2("m3x2d", matrix3x2da) { readMatrix3x2d() })
            "m3x3d[][]" -> obj.readMatrix3x3dArray2D(name, readArray2("m3x3d", matrix3x3da) { readMatrix3x3d() })
            "m4x3d[][]" -> obj.readMatrix4x3dArray2D(name, readArray2("m4x3d", matrix4x3da) { readMatrix4x3d() })
            "m4x4d[][]" -> obj.readMatrix4x4dArray2D(name, readArray2("m4x4d", matrix4x4da) { readMatrix4x4d() })
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
            "S[]" -> obj.readStringArray(name, readArray("String", "") { readStringValue() })
            "S[][]" -> obj.readStringArray2D(name, readArray2("String[]", emptyArray()) { readStringValue() })
            "R" -> obj.readFile(name, readFile())
            "R[]" -> obj.readFileArray(name, readArray("FileRef", InvalidRef) { readFile() })
            "R[][]" -> obj.readFileArray2D(name, readArray2("FileRef", file0a) { readFile() })
            "*[]", "[]" -> {// array of mixed types
                val elements = readArray("Any", { arrayOfNulls<ISaveable?>(it) }, {
                    when (val next = skipSpace()) {
                        'n' -> readNull()
                        '{' -> readObject()
                        in '0'..'9' -> readPtr(next)
                        else -> error("Missing { or ptr or null after starting object[], got '$next' in $lineNumber:$lineIndex")
                    }
                }, { array, index, value -> array[index] = value })
                obj.readObjectArray(name, elements)
            }
            else -> {
                if (type.endsWith("[]")) {// array, but all elements have the same type
                    type = type.substring(0, type.length - 2)
                    val elements = readArray(type, { arrayOfNulls<ISaveable?>(it) }, {
                        when (val next = skipSpace()) {
                            'n' -> readNull()
                            '{' -> readObjectAndRegister(type)
                            in '0'..'9' -> readPtr(next)
                            else -> error("Missing { or ptr or null after starting object[], got '$next' in $lineNumber:$lineIndex")
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
                            val ptr =
                                rawPtr.toIntOrNull() ?: error("Invalid pointer: $rawPtr in $lineNumber:$lineIndex")
                            if (ptr > 0) {
                                val child = getByPointer(ptr, false)
                                if (child == null) {
                                    addMissingReference(obj, name, ptr)
                                } else {
                                    obj.readObject(name, child)
                                }
                            }
                        }
                        else -> error("Missing { or ptr or null after starting object of class $type, got '$next' in $lineNumber:$lineIndex")
                    }
                }
            }
        }
        return obj
    }

    private fun readPtr(next: Char): ISaveable? {
        tmpChar = next.code
        return getByPointer(readInt(), warnIfMissing = true)
    }

    private fun readNull(): Nothing? {
        assertEquals(next(), 'u', "Reading null")
        assertEquals(next(), 'l', "Reading null")
        assertEquals(next(), 'l', "Reading null")
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
            assertEquals(firstChar, '"')
            var property0 = readString()
            if (property0 == "class") {
                assertEquals(skipSpace(), ':')
                assertEquals(skipSpace(), '"')
                assertEquals(readString(), type)
                assertEquals(skipSpace(), ',')
                assertEquals(skipSpace(), '"')
                property0 = readString()
            }
            val nextChar = skipSpace()
            if (nextChar == '}') {
                // nothing to do
                register(instance)
            } else {
                assertEquals(nextChar, ':')
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
                        assertEquals(n, '"')
                        tmpChar = '"'.code
                        readProperty(instance)
                        propertyLoop(instance)
                    }
                } // else nothing to do
            }
        }
        return instance
    }

    private inline fun readWithBrackets(name: String, run: () -> Unit) {
        assertEquals(skipSpace(), '[', name)
        run()
        assertEquals(skipSpace(), ']', name)
    }

    private fun splitTypeName(typeName: String): Pair<String, String> {
        val index = typeName.indexOf(':')
        if (index < 0) error("Invalid Type:Name '$typeName' in $lineNumber:$lineIndex")
        val type = typeName.substring(0, index)
        val name = typeName.substring(index + 1)
        return type to name
    }

    companion object {
        private val boolArray0 = BooleanArray(0)
        private val charArray0 = CharArray(0)
        private val shortArray0 = ShortArray(0)
        private val byteArray0 = ByteArray(0)
        private val intArray0 = IntArray(0)
        private val longArray0 = LongArray(0)
        private val floatArray0 = FloatArray(0)
        private val doubleArray0 = DoubleArray(0)
        private val vector2f0 = Vector2f()
        private val vector3f0 = Vector3f()
        private val vector4f0 = Vector4f()
        private val vector2d0 = Vector2d()
        private val vector3d0 = Vector3d()
        private val vector4d0 = Vector4d()
        private val vector2i0 = Vector2i()
        private val vector3i0 = Vector3i()
        private val vector4i0 = Vector4i()
        private val matrix2x2 = Matrix2f()
        private val matrix3x2 = Matrix3x2f()
        private val matrix3x3 = Matrix3f()
        private val matrix4x3 = Matrix4x3f()
        private val matrix4x4 = Matrix4f()
        private val matrix2x2d = Matrix2d()
        private val matrix3x2d = Matrix3x2d()
        private val matrix3x3d = Matrix3d()
        private val matrix4x3d = Matrix4x3d()
        private val matrix4x4d = Matrix4d()
        private val vector2f0a = arrayOf(Vector2f())
        private val vector3f0a = arrayOf(Vector3f())
        private val vector4f0a = arrayOf(Vector4f())
        private val vector2d0a = arrayOf(Vector2d())
        private val vector3d0a = arrayOf(Vector3d())
        private val vector4d0a = arrayOf(Vector4d())
        private val vector2i0a = arrayOf(Vector2i())
        private val vector3i0a = arrayOf(Vector3i())
        private val vector4i0a = arrayOf(Vector4i())
        private val matrix2x2a = arrayOf(Matrix2f())
        private val matrix3x2a = arrayOf(Matrix3x2f())
        private val matrix3x3a = arrayOf(Matrix3f())
        private val matrix4x3a = arrayOf(Matrix4x3f())
        private val matrix4x4a = arrayOf(Matrix4f())
        private val matrix2x2da = arrayOf(Matrix2d())
        private val matrix3x2da = arrayOf(Matrix3x2d())
        private val matrix3x3da = arrayOf(Matrix3d())
        private val matrix4x3da = arrayOf(Matrix4x3d())
        private val matrix4x4da = arrayOf(Matrix4d())
        private val file0a = arrayOf<FileReference>(InvalidRef)
        private const val black = 255.shl(24).toLong()
        private val LOGGER = LogManager.getLogger(TextReaderBase::class)
    }
}
