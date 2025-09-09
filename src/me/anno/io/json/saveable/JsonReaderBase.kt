package me.anno.io.json.saveable

import me.anno.io.base.BaseReader
import me.anno.io.base.InvalidFormatException
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.saveable.Saveable
import me.anno.utils.Color.argb
import me.anno.utils.Color.black
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFail
import me.anno.utils.assertions.assertTrue
import me.anno.utils.files.LocalFile.toGlobalFile
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.types.Strings.toDouble
import me.anno.utils.types.Strings.toInt
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
        assertEquals("class", firstProperty, "Expected first property to be 'class'")
        assertEquals(':', skipSpace())
        assertEquals('"', skipSpace())
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
        // cannot be called here, because we might need to wait for not-yet-resolved pointers
        // obj.onReadingEnded()
        return obj
    }

    override fun readAllInList() {
        assertEquals(skipSpace(), '[', "Expected JSON to start with an array")
        while (true) {
            when (val next = skipSpace()) {
                ',' -> Unit // nothing to do
                '{' -> readObject()
                ']' -> return
                else -> assertFail("Unexpected char $next, ${next.code}")
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
            else -> assertFail("Expected '\"' or 'n' but got $c for readStringValueOrNull")
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
        return readStringTmp().toString()
    }

    private fun readStringTmp(): StringBuilder {

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
                        else -> assertFail("Unknown escape sequence \\$char2")
                    }
                )
                '"' -> return str
                else -> str.append(char)
            }
        }
    }

    /**
     * reads a number string; may return invalid results
     * */
    private fun readNumber(): StringBuilder {
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
                else -> {
                    tmpChar = next.code
                    return str
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
            else -> assertFail("Expected a comment after '/', but got '$next'")
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
                        else -> assertFail("Expected property or end of object after comma, got '$next2'")
                    }
                }
                '}' -> return
                '/' -> skipComment()
                else -> assertFail("Unexpected char $next in object of class ${obj.className}")
            }
        }
    }

    private fun <ArrayType> readArray(
        typeName: String,
        createArray: (arraySize: Int) -> ArrayType,
        putValue: (array: ArrayType, index: Int) -> Unit
    ): ArrayType {
        assertEquals(skipSpace(), '[')
        val rawLength = readLong()
        assertTrue(rawLength in 0..Int.MAX_VALUE) {
            "Invalid $typeName[] length '$rawLength'"
        }
        var i = 0
        val length = rawLength.toInt()
        val values = createArray(length)
        content@ while (true) {
            when (val next = skipSpace()) {
                ',' -> putValue(values, i++)
                ']' -> break@content
                else -> assertFail("unknown character $next in $typeName[] in $lineNumber:$lineIndex")
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
        return readArray(
            typeName,
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
        return readArray(
            typeName,
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
            '\\' -> {
                assertEquals(next(), '"', "boolean:escaped")
                val value = readBool()
                assertEquals(next(), '\\', "boolean:escaped")
                assertEquals(next(), '"', "boolean:escaped")
                value
            }
            else -> throw InvalidFormatException("Unknown boolean value starting with $firstChar")
        }
    }

    private fun readVector2f(skipComma: Boolean = false): Vector2f {
        if (skipComma) skipComma()
        return readWithBrackets("v2f") {
            val rawX = readFloat()
            val rawY = if (skipNextComma()) readFloat() else rawX
            Vector2f(rawX, rawY)
        }
    }

    private fun readVector3f(skipComma: Boolean = false): Vector3f {
        if (skipComma) skipComma()
        return readWithBrackets("v3f") {
            val rawX = readFloat()
            if (skipNextComma()) {
                val rawY = readFloat()
                skipComma()
                val rawZ = readFloat()
                Vector3f(rawX, rawY, rawZ)
            } else Vector3f(rawX)
        }
    }

    private fun readVector4f(skipComma: Boolean = false): Vector4f {
        if (skipComma) skipComma()
        return readWithBrackets("v4f") {
            val rawX = readFloat()
            if (skipNextComma()) {
                val rawY = readFloat()
                if (skipNextComma()) {
                    val rawZ = readFloat()
                    if (skipNextComma()) {
                        val rawW = readFloat()
                        Vector4f(rawX, rawY, rawZ, rawW)
                    } else Vector4f(rawX, rawY, rawZ, 1f) // opaque color
                } else Vector4f(rawX, rawX, rawX, rawY) // white with alpha
            } else Vector4f(rawX) // monotone
        }
    }

    private fun readVector2d(skipComma: Boolean = false): Vector2d {
        if (skipComma) skipComma()
        return readWithBrackets("v2d") {
            val rawX = readDouble()
            val rawY = if (skipNextComma()) readDouble() else rawX
            Vector2d(rawX, rawY)
        }
    }

    private fun readVector3d(skipComma: Boolean = false): Vector3d {
        if (skipComma) skipComma()
        return readWithBrackets("v3d") {
            val rawX = readDouble()
            if (skipNextComma()) {
                val rawY = readDouble()
                skipComma()
                val rawZ = readDouble()
                Vector3d(rawX, rawY, rawZ)
            } else Vector3d(rawX)
        }
    }

    private fun readVector4d(skipComma: Boolean = false): Vector4d {
        if (skipComma) skipComma()
        return readWithBrackets("v4d") {
            val rawX = readDouble()
            if (skipNextComma()) {
                val rawY = readDouble()
                if (skipNextComma()) {
                    val rawZ = readDouble()
                    if (skipNextComma()) {
                        val rawW = readDouble()
                        Vector4d(rawX, rawY, rawZ, rawW)
                    } else Vector4d(rawX, rawY, rawZ, 1.0) // opaque color
                } else Vector4d(rawX, rawX, rawX, rawY) // white with alpha
            } else Vector4d(rawX) // monotone
        }
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

    private fun skipComma() {
        val sep0 = skipSpace()
        assertEquals(sep0, ',')
    }

    private fun nextFloat(): Float {
        skipComma()
        return readFloat()
    }

    private fun nextDouble(): Double {
        skipComma()
        return readDouble()
    }

    private fun readPlanef(): Planef {
        return readWithBrackets("p4") {
            val rawX = readFloat()
            val rawY = nextFloat()
            val rawZ = nextFloat()
            val rawW = nextFloat()
            Planef(rawX, rawY, rawZ, rawW)
        }
    }

    private fun readPlaned(): Planed {
        return readWithBrackets("p4d") {
            val rawX = readDouble()
            val rawY = nextDouble()
            val rawZ = nextDouble()
            val rawW = nextDouble()
            Planed(rawX, rawY, rawZ, rawW)
        }
    }

    private fun readQuaternionf(): Quaternionf {
        return readWithBrackets("q4") {
            val rawX = readFloat()
            val rawY = nextFloat()
            val rawZ = nextFloat()
            val rawW = nextFloat()
            Quaternionf(rawX, rawY, rawZ, rawW)
        }
    }

    private fun readQuaterniond(): Quaterniond {
        return readWithBrackets("q4d") {
            val rawX = readDouble()
            val rawY = nextDouble()
            val rawZ = nextDouble()
            val rawW = nextDouble()
            Quaterniond(rawX, rawY, rawZ, rawW)
        }
    }

    private fun skipNextComma(): Boolean {
        val c0 = skipSpace()
        if (c0 == ',') return true
        tmpChar = c0.code
        return false
    }

    private fun readVector2i(): Vector2i {
        return readWithBrackets("v2i") {
            val rawX = readInt()
            val rawY = if (skipNextComma()) readInt() else rawX
            Vector2i(rawX, rawY)
        }
    }

    private fun readVector3i(): Vector3i {
        return readWithBrackets("v3i") {
            val rawX = readInt()
            if (skipNextComma()) {
                val rawY = readInt()
                skipComma()
                val rawZ = readInt()
                Vector3i(rawX, rawY, rawZ)
            } else Vector3i(rawX)
        }
    }

    private fun readVector4i(): Vector4i {
        return readWithBrackets("v4i") {
            val rawX = readInt()
            if (skipNextComma()) {
                val rawY = readInt()
                if (skipNextComma()) {
                    val rawZ = readInt()
                    if (skipNextComma()) {
                        val rawW = readInt()
                        Vector4i(rawX, rawY, rawZ, rawW)
                    } else Vector4i(rawX, rawY, rawZ, 255) // opaque color
                } else Vector4i(rawX, rawX, rawX, rawY) // white with alpha
            } else Vector4i(rawX) // monotone
        }
    }

    private fun readMatrix2x2(): Matrix2f {
        return readWithBrackets("mat2") {
            Matrix2f(
                readVector2f(),
                readVector2f(true)
            )
        }
    }

    private fun readMatrix2x2d(): Matrix2d {
        return readWithBrackets("mat2d") {
            Matrix2d(
                readVector2d(),
                readVector2d(true)
            )
        }
    }

    private fun readMatrix3x2(): Matrix3x2f {
        return readWithBrackets("mat3x2") {
            Matrix3x2f(
                readVector2f(),
                readVector2f(true),
                readVector2f(true)
            )
        }
    }

    private fun readMatrix3x2d(): Matrix3x2d {
        return readWithBrackets("mat3x2d") {
            Matrix3x2d(
                readVector2d(),
                readVector2d(true),
                readVector2d(true)
            )
        }
    }

    private fun readMatrix3x3(): Matrix3f {
        return readWithBrackets("mat3") {
            Matrix3f(
                readVector3f(),
                readVector3f(true),
                readVector3f(true)
            )
        }
    }

    private fun readMatrix3x3d(): Matrix3d {
        return readWithBrackets("mat3d") {
            Matrix3d(
                readVector3d(),
                readVector3d(true),
                readVector3d(true)
            )
        }
    }

    private fun readMatrix4x3(): Matrix4x3f {
        return readWithBrackets("mat4x3") {
            Matrix4x3f(
                readVector3f(),
                readVector3f(true),
                readVector3f(true),
                readVector3f(true)
            )
        }
    }

    private fun readMatrix4x3d(): Matrix4x3d {
        return readWithBrackets("mat4x3d") {
            Matrix4x3d(
                readVector3d(),
                readVector3d(true),
                readVector3d(true),
                readVector3d(true)
            )
        }
    }

    private fun readMatrix4x4(): Matrix4f {
        return readWithBrackets("mat4") {
            Matrix4f(
                readVector4f(),
                readVector4f(true),
                readVector4f(true),
                readVector4f(true)
            )
        }
    }

    private fun readMatrix4x4d(): Matrix4d {
        return readWithBrackets("mat4d") {
            Matrix4d(
                readVector4d(),
                readVector4d(true),
                readVector4d(true),
                readVector4d(true)
            )
        }
    }

    fun readProperty(obj: Saveable) {
        assertEquals(skipSpace(), '"')
        val typeName = readString()
        assertEquals(skipSpace(), ':')
        readProperty(obj, typeName)
    }

    private fun readByte(): Byte = readLong().toByte()

    private fun readShort(): Short = readLong().toShort()

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

    private fun readColorArray(): IntArray {
        return readArray("color", { IntArray(it) }, { array, index -> array[index] = readColor() })
    }

    private fun readColor(): Int {
        val first = skipSpace()
        if (first == '"') {
            assertEquals('#', next())
            val str = readStringTmp()
            when (str.length) {
                3 -> { // #rgb
                    val v = str.toInt(16)
                    return argb(
                        255,
                        v.ushr(8).and(15) * 17,
                        v.ushr(4).and(15) * 17,
                        v.and(15) * 17,
                    )
                }
                4 -> { // #argb
                    val v = str.toInt(16)
                    return argb(
                        v.ushr(12).and(15) * 17,
                        v.ushr(8).and(15) * 17,
                        v.ushr(4).and(15) * 17,
                        v.and(15) * 17,
                    )
                }
                // #rrggbb
                6 -> return str.toInt(16) or black
                // ##aarrggbb
                8 -> return str.toInt(16)
                else -> assertFail("Unknown color format")
            }
        } else {
            tmpChar = first.code
            return readInt()
        }
    }

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

    private fun readFloat(): Float = readDouble().toFloat()
    private fun readDouble(): Double = readNumber().toDouble()

    private fun readBoolArray() = readArray(
        "boolean",
        { BooleanArray(it) }, { array, index -> array[index] = readBool() })

    private fun readCharArray() = readArray(
        "char",
        { CharArray(it) }, { array, index -> array[index] = readChar() })

    private fun readByteArray() = readArray(
        "byte",
        { ByteArray(it) }, { array, index -> array[index] = readByte() })

    private fun readShortArray() = readArray(
        "short",
        { ShortArray(it) }, { array, index -> array[index] = readShort() })

    private fun readIntArray() = readArray(
        "int",
        { IntArray(it) }, { array, index -> array[index] = readInt() })

    private fun readLongArray() = readArray(
        "long",
        { LongArray(it) }, { array, index -> array[index] = readLong() })

    private fun readFloatArray() = readArray(
        "float",
        { FloatArray(it) }, { array, index -> array[index] = readFloat() }
    )

    private fun readDoubleArray() = readArray(
        "double",
        { DoubleArray(it) }, { array, index -> array[index] = readDouble() }
    )

    private fun readSaveable(type: String?): Saveable? {
        return when (val next = skipSpace()) {
            'n' -> readNull()
            '{' -> {
                if (type != null) readObjectAndRegister(type)
                else readObject()
            }
            in '0'..'9', '"' -> readPtr(next)
            else -> assertFail("Missing { or ptr or null after starting object[], got '$next' in $lineNumber:$lineIndex")
        }
    }

    private fun readMixedArray(): ArrayList<Saveable?> {
        return readArray("Any", {
            createArrayList(it, null)
        }, { array, index ->
            array[index] = readSaveable(null)
        })
    }

    private fun readMixedArray2D(): ArrayList<List<Saveable?>> {
        return readArray("Any[]", {
            createArrayList(it, emptyList())
        }, { array, index ->
            array[index] = readMixedArray()
        })
    }

    private fun readFixedArray(type: String): ArrayList<Saveable?> {
        return readArray(type, {
            createArrayList(it, null)
        }, { array, index ->
            array[index] = readSaveable(type)
        })
    }

    private fun readFixedArray2D(type: String): ArrayList<List<Saveable?>> {
        return readArray(type, {
            createArrayList(it, emptyList())
        }, { array, index ->
            array[index] = readFixedArray(type)
        })
    }

    private fun readProperty(obj: Saveable, typeName: String) {
        var (type, name) = splitTypeName(typeName)
        when (type) {
            "*[][]", "[][]" -> {// array of mixed types
                obj.setProperty(name, readMixedArray2D())
            }
            "*[]", "[]" -> {// array of mixed types
                obj.setProperty(name, readMixedArray())
            }
            else -> {
                val reader = readers[type]
                if (reader != null) {
                    reader(this, obj, name)
                } else if (type.endsWith("[][]")) {// 2d-array, but all elements have the same type
                    type = type.substring(0, type.length - 4)
                    val elements = readFixedArray2D(type)
                    obj.setProperty(name, elements)
                } else if (type.endsWith("[]")) {// array, but all elements have the same type
                    type = type.substring(0, type.length - 2)
                    val elements = readFixedArray(type)
                    obj.setProperty(name, elements)
                } else {
                    when (val next = skipSpace()) {
                        'n' -> obj.setProperty(name, readNull())
                        '{' -> obj.setProperty(name, readObjectAndRegister(type))
                        in '0'..'9' -> {
                            tmpChar = next.code
                            val ptr = readInt()
                            if (ptr > 0) {
                                val child = getByPointer(ptr, false)
                                if (child == null) {
                                    addMissingReference(obj, name, ptr)
                                } else {
                                    obj.setProperty(name, child)
                                }
                            }
                        }
                        else -> assertFail("Missing { or ptr or null after starting object of class $type, got '$next' in $lineNumber:$lineIndex")
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
                assertEquals(readStringTmp(), type)
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
                    val ptr = readInt()
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

    private inline fun <V> readWithBrackets(name: String, readingFunction: () -> V): V {
        assertEquals(skipSpace(), '[', name)
        val value = readingFunction()
        assertEquals(skipSpace(), ']', name)
        return value
    }

    private fun splitTypeName(typeName: String): Pair<String, String> {
        val index = typeName.indexOf(':')
        assertTrue(index >= 0, "Invalid Type:Name '$typeName' in $lineNumber:$lineIndex")
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
            register2(SimpleType.COLOR, IntArray(0), JsonReaderBase::readColor, JsonReaderBase::readColorArray)
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