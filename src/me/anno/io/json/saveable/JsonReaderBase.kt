package me.anno.io.json.saveable

import me.anno.io.base.BaseReader
import me.anno.io.base.InvalidFormatException
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.saveable.Saveable
import me.anno.utils.files.LocalFile.toGlobalFile
import me.anno.utils.structures.lists.Lists.createArrayList
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.AABBf
import org.joml.Matrix2d
import org.joml.Matrix2f
import org.joml.Matrix3d
import org.joml.Matrix3f
import org.joml.Matrix3x2d
import org.joml.Matrix3x2f
import org.joml.Matrix4d
import org.joml.Matrix4f
import org.joml.Matrix4x3d
import org.joml.Matrix4x3f
import org.joml.Planed
import org.joml.Planef
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector3i
import org.joml.Vector4d
import org.joml.Vector4f
import org.joml.Vector4i

/**
 * reads a JSON-similar format from a text file
 * */
abstract class JsonReaderBase(val workspace: FileReference) : BaseReader() {

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

    override fun readObject(): Saveable {
        assertEquals(skipSpace(), '"')
        val firstProperty = readString()
        assertEquals(firstProperty, "class", "Expected first property to be 'class'")
        assertEquals(skipSpace(), ':')
        assertEquals(skipSpace(), '"')
        val clazz = readString()
        val nc0 = skipSpace()
        val obj = getNewClassInstance(clazz)
        allInstances.add(obj)
        obj.onReadingStarted()
        if (nc0 == ',') {
            assertEquals(skipSpace(), '"')
            val secondProperty = readString()
            assertEquals(skipSpace(), ':')
            if (isPtrProperty(secondProperty)) {
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
        obj.onReadingEnded()
        return obj
    }

    override fun readAllInList() {
        assertEquals(skipSpace(), '[', "Expected JSON to start with an array")
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
     * reads a number string; may return invalid results;
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

    private fun propertyLoop(obj: Saveable) {
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

    private fun <ArrayType> readArray(
        typeName: String,
        createArray: (arraySize: Int) -> ArrayType,
        putValue: (array: ArrayType, index: Int) -> Unit
    ): ArrayType {
        val c0 = skipSpace()
        if (c0 == '"') {
            val array = readArray(typeName, createArray, putValue)
            assertEquals(skipSpace(), '"', "end-of-str[]")
            return array
        }
        assertEquals(c0, '[')
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
                ',' -> putValue(values, i++)
                ']' -> break@content
                else -> error("unknown character $next in $typeName[] in $lineNumber:$lineIndex")
            }
        }
        if (i > length) LOGGER.warn("$typeName[] contained too many elements!")
        return values
    }

    private fun <Type> readArray(
        typeName: SimpleType, a0: Type,
        readValue: () -> Type,
    ): ArrayList<Type> {
        return readArray(typeName.array, a0, readValue)
    }

    private fun <Type> readArray(
        typeName: String, a0: Type,
        readValue: () -> Type,
    ): ArrayList<Type> {
        return readArray(typeName,
            { createArrayList(it, a0) },
            { array, index -> array[index] = readValue() }
        )
    }

    private fun <Type> readArray2D(
        typeName: SimpleType, sampleInstance: Type,
        readValue: () -> Type
    ): ArrayList<List<Type>> {
        return readArray2D(typeName.array2d, sampleInstance, readValue)
    }

    private fun <Type> readArray2D(
        typeName: String, sampleInstance: Type,
        readValue: () -> Type,
    ): ArrayList<List<Type>> {
        val sampleArray = emptyList<Type>()
        return readArray(typeName,
            { createArrayList(it, sampleArray) },
            { array, index -> array[index] = readArray(typeName, sampleInstance, readValue) }
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
            '"' -> {
                val value = readBool()
                assertEquals(next(), '"', "boolean:quotes")
                value
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

    private fun readAABBf(): AABBf {
        return readWithBrackets("AABBf") {
            AABBf()
                .setMin(readVector3f())
                .setMax(readVector3f(true))
        }
    }

    private fun readAABBd(): AABBd {
        return readWithBrackets("AABBd") {
            AABBd()
                .setMin(readVector3d())
                .setMax(readVector3d(true))
        }
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

    fun readProperty(obj: Saveable) {
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
                1 -> number * 0x111111 + BLACK
                3 -> number.and(0xf) * 0x11 +
                        number.and(0xf0) * 0x110 +
                        number.and(0xf00) * 0x1100 + BLACK
                4 -> number.and(0xf) * 0x11 +
                        number.and(0xf0) * 0x110 +
                        number.and(0xf00) * 0x1100 +
                        number.and(0xf000) * 0x11000
                6 -> number or BLACK
                else -> number
            }.toInt().toLong()
            isNegative -> -number
            else -> number
        }
    }

    private fun readFloat() = readNumber().toFloatOrNull() ?: 0f
    private fun readDouble() = readNumber().toDoubleOrNull() ?: 0.0

    private fun readBoolArray() = readArray("boolean",
        { BooleanArray(it) }, { array, index -> array[index] = readBool() })

    private fun readCharArray() = readArray("char",
        { CharArray(it) }, { array, index -> array[index] = readChar() })

    private fun readByteArray() = readArray("byte",
        { ByteArray(it) }, { array, index -> array[index] = readByte() })

    private fun readShortArray() = readArray("short",
        { ShortArray(it) }, { array, index -> array[index] = readShort() })

    private fun readIntArray() = readArray("int",
        { IntArray(it) }, { array, index -> array[index] = readInt() })

    private fun readLongArray() = readArray("long",
        { LongArray(it) }, { array, index -> array[index] = readLong() })

    private fun readFloatArray() = readArray("float",
        { FloatArray(it) }, { array, index -> array[index] = readFloat() }
    )

    private fun readDoubleArray() = readArray("double",
        { DoubleArray(it) }, { array, index -> array[index] = readDouble() }
    )

    private fun readProperty(obj: Saveable, typeName: String) {
        var (type, name) = splitTypeName(typeName)
        when (type) {
            "*[]", "[]" -> {// array of mixed types
                val elements = readArray("Any", {
                    createArrayList<Saveable?>(it, null)
                }, { array, index ->
                    array[index] = when (val next = skipSpace()) {
                        'n' -> readNull()
                        '{' -> readObject()
                        in '0'..'9' -> readPtr(next)
                        else -> error("Missing { or ptr or null after starting object[], got '$next' in $lineNumber:$lineIndex")
                    }
                })
                obj.setProperty(name, elements)
            }
            else -> {
                val reader = readers[type]
                if (reader != null) {
                    reader(this, obj, name)
                } else if (type.endsWith("[]")) {// array, but all elements have the same type
                    type = type.substring(0, type.length - 2)
                    val elements = readArray(type, {
                        createArrayList<Saveable?>(it, null)
                    }, { array, index ->
                        array[index] = when (val next = skipSpace()) {
                            'n' -> readNull()
                            '{' -> readObjectAndRegister(type)
                            in '0'..'9' -> readPtr(next)
                            else -> error("Missing { or ptr or null after starting object[], got '$next' in $lineNumber:$lineIndex")
                        }
                    })
                    obj.setProperty(name, elements)
                } else {
                    when (val next = skipSpace()) {
                        'n' -> obj.setProperty(name, readNull())
                        '{' -> obj.setProperty(name, readObjectAndRegister(type))
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
                                    obj.setProperty(name, child)
                                }
                            }
                        }
                        else -> error("Missing { or ptr or null after starting object of class $type, got '$next' in $lineNumber:$lineIndex")
                    }
                }
            }
        }
    }

    private fun readPtr(next: Char): Saveable? {
        tmpChar = next.code
        return getByPointer(readInt(), warnIfMissing = true)
    }

    private fun readNull(): Nothing? {
        assertEquals(next(), 'u', "Reading null")
        assertEquals(next(), 'l', "Reading null")
        assertEquals(next(), 'l', "Reading null")
        return null
    }

    fun register(value: Saveable) = register(value, getUnusedPointer())

    private fun readObjectAndRegister(type: String): Saveable {
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
                if (isPtrProperty(property0)) {
                    val ptr = readNumber().toIntOrNull()
                        ?: throw InvalidFormatException("Invalid pointer")
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

    private inline fun <V> readWithBrackets(name: String, run: () -> V): V {
        assertEquals(skipSpace(), '[', name)
        val value = run()
        assertEquals(skipSpace(), ']', name)
        return value
    }

    private fun splitTypeName(typeName: String): Pair<String, String> {
        val index = typeName.indexOf(':')
        if (index < 0) error("Invalid Type:Name '$typeName' in $lineNumber:$lineIndex")
        val type = typeName.substring(0, index)
        val name = typeName.substring(index + 1)
        return type to name
    }

    companion object {

        private val readers = HashMap<String, JsonReaderBase.(obj: Saveable, name: String) -> Unit>(64)

        fun isPtrProperty(name: String): Boolean {
            return name == "i:*ptr" || name == "*ptr" || name == "ptr"
        }

        private fun <V> register1(
            type: SimpleType, v0: V,
            reader: JsonReaderBase.() -> V
        ) {
            readers[type.scalar] = { obj, name -> obj.setProperty(name, reader()) }
            readers[type.array] = { obj, name -> obj.setProperty(name, readArray(type, v0) { reader() }) }
            readers[type.array2d] = { obj, name -> obj.setProperty(name, readArray2D(type, v0) { reader() }) }
        }

        private fun <V, W> register2(
            type: SimpleType, v0: W,
            reader1: JsonReaderBase.() -> V,
            readerN: JsonReaderBase.() -> W
        ) {
            readers[type.scalar] = { obj, name -> obj.setProperty(name, reader1()) }
            readers[type.array] = { obj, name -> obj.setProperty(name, readerN()) }
            readers[type.array2d] = { obj, name -> obj.setProperty(name, readArray(type.array2d, v0) { readerN() }) }
        }

        init {
            register2(SimpleType.BYTE, ByteArray(0), JsonReaderBase::readByte, JsonReaderBase::readByteArray)
            register2(SimpleType.SHORT, ShortArray(0), JsonReaderBase::readShort, JsonReaderBase::readShortArray)
            register2(SimpleType.INT, IntArray(0), JsonReaderBase::readInt, JsonReaderBase::readIntArray)
            register2(SimpleType.LONG, LongArray(0), JsonReaderBase::readLong, JsonReaderBase::readLongArray)
            register2(SimpleType.FLOAT, FloatArray(0), JsonReaderBase::readFloat, JsonReaderBase::readFloatArray)
            register2(SimpleType.DOUBLE, DoubleArray(0), JsonReaderBase::readDouble, JsonReaderBase::readDoubleArray)
            register2(SimpleType.BOOLEAN, BooleanArray(0), JsonReaderBase::readBool, JsonReaderBase::readBoolArray)
            register2(SimpleType.CHAR, CharArray(0), JsonReaderBase::readChar, JsonReaderBase::readCharArray)
            register2(SimpleType.COLOR, IntArray(0), JsonReaderBase::readInt, JsonReaderBase::readIntArray)
            register1(SimpleType.STRING, "", JsonReaderBase::readStringValue)
            register1(SimpleType.REFERENCE, InvalidRef, JsonReaderBase::readFile)
            register1(SimpleType.VECTOR2F, Vector2f(), JsonReaderBase::readVector2f)
            register1(SimpleType.VECTOR3F, Vector3f(), JsonReaderBase::readVector3f)
            register1(SimpleType.VECTOR4F, Vector4f(), JsonReaderBase::readVector4f)
            register1(SimpleType.VECTOR2D, Vector2d(), JsonReaderBase::readVector2d)
            register1(SimpleType.VECTOR3D, Vector3d(), JsonReaderBase::readVector3d)
            register1(SimpleType.VECTOR4D, Vector4d(), JsonReaderBase::readVector4d)
            register1(SimpleType.VECTOR2I, Vector2i(), JsonReaderBase::readVector2i)
            register1(SimpleType.VECTOR3I, Vector3i(), JsonReaderBase::readVector3i)
            register1(SimpleType.VECTOR4I, Vector4i(), JsonReaderBase::readVector4i)
            register1(SimpleType.QUATERNIONF, Quaternionf(), JsonReaderBase::readQuaternionf)
            register1(SimpleType.QUATERNIOND, Quaterniond(), JsonReaderBase::readQuaterniond)
            register1(SimpleType.AABBF, AABBf(), JsonReaderBase::readAABBf)
            register1(SimpleType.AABBD, AABBd(), JsonReaderBase::readAABBd)
            register1(SimpleType.PLANEF, Planef(), JsonReaderBase::readPlanef)
            register1(SimpleType.PLANED, Planed(), JsonReaderBase::readPlaned)
            register1(SimpleType.MATRIX2X2F, Matrix2f(), JsonReaderBase::readMatrix2x2)
            register1(SimpleType.MATRIX3X2F, Matrix3x2f(), JsonReaderBase::readMatrix3x2)
            register1(SimpleType.MATRIX3X3F, Matrix3f(), JsonReaderBase::readMatrix3x3)
            register1(SimpleType.MATRIX4X3F, Matrix4x3f(), JsonReaderBase::readMatrix4x3)
            register1(SimpleType.MATRIX4X4F, Matrix4f(), JsonReaderBase::readMatrix4x4)
            register1(SimpleType.MATRIX2X2D, Matrix2d(), JsonReaderBase::readMatrix2x2d)
            register1(SimpleType.MATRIX3X2D, Matrix3x2d(), JsonReaderBase::readMatrix3x2d)
            register1(SimpleType.MATRIX3X3D, Matrix3d(), JsonReaderBase::readMatrix3x3d)
            register1(SimpleType.MATRIX4X3D, Matrix4x3d(), JsonReaderBase::readMatrix4x3d)
            register1(SimpleType.MATRIX4X4D, Matrix4d(), JsonReaderBase::readMatrix4x4d)
        }

        private const val BLACK = 0xff000000L
        private val LOGGER = LogManager.getLogger(JsonReaderBase::class)
    }
}