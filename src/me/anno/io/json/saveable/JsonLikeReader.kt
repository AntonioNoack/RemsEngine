package me.anno.io.json.saveable

import me.anno.io.base.BaseReader
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonReaderBase.Companion.isPtrProperty
import me.anno.io.saveable.Saveable
import me.anno.utils.Color.argb
import me.anno.utils.Color.black
import me.anno.utils.assertions.assertFail
import me.anno.utils.assertions.assertTrue
import me.anno.utils.files.LocalFile.toGlobalFile
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.types.AnyToBool.getBool
import me.anno.utils.types.AnyToDouble.getDouble
import me.anno.utils.types.AnyToFloat.getFloat
import me.anno.utils.types.AnyToInt.getInt
import me.anno.utils.types.AnyToLong.getLong
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
import kotlin.math.min

/**
 * reads a JSON-similar format from a mix of List<*> and Map<String,*>
 *
 * todo we need a corresponding writer, and then replace all our readers and writers with that implementation
 *  (we serialize rarely, and reducing code size would be nice)
 * */
abstract class JsonLikeReader(workspace: FileReference) : BaseReader(workspace) {

    private fun getUnusedPointer(): Int = -1
    override fun readObject(): Saveable {
        throw NotImplementedError("Use the method with explicit value argument")
    }

    fun readObject(data: Map<String, *>): Saveable {
        val clazz = data["class"] as String
        return readObjectAndRegister(clazz, data)
    }

    private fun readObjectAndRegister(clazz: String, data: Map<String, Any?>): Saveable {
        val obj = getNewClassInstance(clazz)
        allInstances.add(obj)
        obj.onReadingStarted()

        // name == "i:*ptr" || name == "*ptr" || name == "ptr"
        val ptr = data["i:*ptr"] ?: data["*ptr"] ?: data["ptr"]
        if (ptr != null) {
            register(obj, getInt(ptr))
        } else {
            register(obj)
        }

        for ((key, value) in data) {
            if (key == "class" || isPtrProperty(key)) continue
            readProperty(obj, key, value)
        }

        // cannot be called here, because we might need to wait for not-yet-resolved pointers
        // obj.onReadingEnded()
        return obj
    }

    override fun readAllInList() {
        throw NotImplementedError()
    }

    fun readAllInList(data: List<*>) {
        for (i in data.indices) {
            @Suppress("UNCHECKED_CAST")
            readObject(data[i] as Map<String, Any>)
        }
    }

    private fun <ArrayType> readArray(
        typeName: String, data: List<*>,
        createArray: (arraySize: Int) -> ArrayType,
        putValue: (array: ArrayType, index: Int, value: Any?) -> Unit
    ): ArrayType {
        val rawLength = getInt(data[0])
        assertTrue(rawLength in 0..Int.MAX_VALUE) {
            "Invalid $typeName[] length '$rawLength'"
        }
        val length = rawLength
        val values = createArray(length)
        for (k in 1 until min(data.size, length + 1)) {
            putValue(values, k - 1, data[k])
        }
        if (data.size - 1 > length) LOGGER.warn("$typeName[] contained too many elements!")
        return values
    }

    private fun <Type> readArray(
        typeName: SimpleType, value: List<*>, sampleInstance: Type,
        readValue: (Any?) -> Type,
    ): ArrayList<Type> {
        return readArray(typeName.array, value, sampleInstance, readValue)
    }

    private fun <Type> readArray(
        typeName: String, value: List<*>, sampleInstance: Type,
        readValue: (Any?) -> Type,
    ): ArrayList<Type> {
        return readArray(
            typeName, value,
            { createArrayList(it, sampleInstance) },
            { array, index, value -> array[index] = readValue(value) }
        )
    }

    private fun <Type> readArray2D(
        typeName: SimpleType, value: List<*>, sampleInstance: Type,
        readValue: (Any?) -> Type
    ): ArrayList<List<Type>> {
        return readArray2D(typeName.array2d, value, sampleInstance, readValue)
    }

    private fun <Type> readArray2D(
        typeName: String, value: List<*>,
        sampleInstance: Type, readValue: (Any?) -> Type,
    ): ArrayList<List<Type>> {
        val sampleArray = emptyList<Type>()
        return readArray(
            typeName, value,
            { createArrayList(it, sampleArray) },
            { array, index, value ->
                @Suppress("UNCHECKED_CAST")
                array[index] = readArray(typeName, value as List<*>, sampleInstance, readValue)
            }
        )
    }

    private fun readStringValue(value: Any?): String {
        return value.toString()
    }

    private fun readFile(value: Any?): FileReference {
        return when (value) {
            is FileReference -> value
            else -> value.toString().toGlobalFile(workspace)
        }
    }

    private fun readVector2f(value: Any?): Vector2f {
        value as List<*>
        val rawX = getFloat(value[0])
        val rawY = if (value.size > 1) getFloat(value[1]) else rawX
        return Vector2f(rawX, rawY)
    }

    private fun readVector3f(value: Any?): Vector3f {
        value as List<*>
        val rawX = getFloat(value[0])
        return if (value.size >= 3) {
            val rawY = getFloat(value[1])
            val rawZ = getFloat(value[2])
            Vector3f(rawX, rawY, rawZ)
        } else Vector3f(rawX)
    }

    private fun readVector4f(value: Any?): Vector4f {
        value as List<*>
        val rawX = getFloat(value[0])
        return if (value.size > 1) {
            val rawY = getFloat(value[1])
            if (value.size > 2) {
                val rawZ = getFloat(value[2])
                if (value.size > 3) {
                    val rawW = getFloat(value[3])
                    Vector4f(rawX, rawY, rawZ, rawW)
                } else Vector4f(rawX, rawY, rawZ, 1f) // opaque color
            } else Vector4f(rawX, rawX, rawX, rawY) // white with alpha
        } else Vector4f(rawX) // monotone
    }

    private fun readVector2d(value: Any?): Vector2d {
        value as List<*>
        val rawX = getDouble(value[0])
        val rawY = if (value.size > 1) getDouble(value[1]) else rawX
        return Vector2d(rawX, rawY)
    }

    private fun readVector3d(value: Any?): Vector3d {
        value as List<*>
        val rawX = getDouble(value[0])
        return if (value.size >= 3) {
            val rawY = getDouble(value[1])
            val rawZ = getDouble(value[2])
            Vector3d(rawX, rawY, rawZ)
        } else Vector3d(rawX)
    }

    private fun readVector4d(value: Any?): Vector4d {
        value as List<*>
        val rawX = getDouble(value[0])
        return if (value.size > 1) {
            val rawY = getDouble(value[1])
            if (value.size > 2) {
                val rawZ = getDouble(value[2])
                if (value.size > 3) {
                    val rawW = getDouble(value[3])
                    Vector4d(rawX, rawY, rawZ, rawW)
                } else Vector4d(rawX, rawY, rawZ, 1.0) // opaque color
            } else Vector4d(rawX, rawX, rawX, rawY) // white with alpha
        } else Vector4d(rawX) // monotone
    }

    private fun readAABBf(value: Any?): AABBf {
        value as List<*>
        return AABBf()
            .setMin(readVector3f(value[0]))
            .setMax(readVector3f(value[1]))
    }

    private fun readAABBd(value: Any?): AABBd {
        value as List<*>
        return AABBd()
            .setMin(readVector3d(value[0]))
            .setMax(readVector3d(value[1]))
    }

    private fun readPlanef(value: Any?): Planef {
        value as List<*>
        val rawX = getFloat(value[0])
        val rawY = getFloat(value[1])
        val rawZ = getFloat(value[2])
        val rawW = getFloat(value[3])
        return Planef(rawX, rawY, rawZ, rawW)
    }

    private fun readPlaned(value: Any?): Planed {
        value as List<*>
        val rawX = getDouble(value[0])
        val rawY = getDouble(value[1])
        val rawZ = getDouble(value[2])
        val rawW = getDouble(value[3])
        return Planed(rawX, rawY, rawZ, rawW)
    }

    private fun readQuaternionf(value: Any?): Quaternionf {
        value as List<*>
        val rawX = getFloat(value[0])
        val rawY = getFloat(value[1])
        val rawZ = getFloat(value[2])
        val rawW = getFloat(value[3])
        return Quaternionf(rawX, rawY, rawZ, rawW)
    }

    private fun readQuaterniond(value: Any?): Quaterniond {
        value as List<*>
        val rawX = getDouble(value[0])
        val rawY = getDouble(value[1])
        val rawZ = getDouble(value[2])
        val rawW = getDouble(value[3])
        return Quaterniond(rawX, rawY, rawZ, rawW)
    }

    private fun readVector2i(value: Any?): Vector2i {
        value as List<*>
        val rawX = getInt(value[0])
        val rawY = if (value.size > 1) getInt(value[1]) else rawX
        return Vector2i(rawX, rawY)
    }

    private fun readVector3i(value: Any?): Vector3i {
        value as List<*>
        val rawX = getInt(value[0])
        return if (value.size >= 3) {
            val rawY = getInt(value[1])
            val rawZ = getInt(value[2])
            Vector3i(rawX, rawY, rawZ)
        } else Vector3i(rawX)
    }

    private fun readVector4i(value: Any?): Vector4i {
        value as List<*>
        val rawX = getInt(value[0])
        return if (value.size > 1) {
            val rawY = getInt(value[1])
            if (value.size > 2) {
                val rawZ = getInt(value[2])
                if (value.size > 3) {
                    val rawW = getInt(value[3])
                    Vector4i(rawX, rawY, rawZ, rawW)
                } else Vector4i(rawX, rawY, rawZ, 255) // opaque color
            } else Vector4i(rawX, rawX, rawX, rawY) // white with alpha
        } else Vector4i(rawX) // monotone
    }

    private fun readMatrix2x2(value: Any?): Matrix2f {
        value as List<*>
        return Matrix2f(
            readVector2f(value[0]),
            readVector2f(value[1])
        )
    }

    private fun readMatrix2x2d(value: Any?): Matrix2d {
        value as List<*>
        return Matrix2d(
            readVector2d(value[0]),
            readVector2d(value[1])
        )
    }

    private fun readMatrix3x2(value: Any?): Matrix3x2f {
        value as List<*>
        return Matrix3x2f(
            readVector2f(value[0]),
            readVector2f(value[1]),
            readVector2f(value[2])
        )
    }

    private fun readMatrix3x2d(value: Any?): Matrix3x2d {
        value as List<*>
        return Matrix3x2d(
            readVector2d(value[0]),
            readVector2d(value[1]),
            readVector2d(value[2])
        )
    }

    private fun readMatrix3x3(value: Any?): Matrix3f {
        value as List<*>
        return Matrix3f(
            readVector3f(value[0]),
            readVector3f(value[1]),
            readVector3f(value[2])
        )
    }

    private fun readMatrix3x3d(value: Any?): Matrix3d {
        value as List<*>
        return Matrix3d(
            readVector3d(value[0]),
            readVector3d(value[1]),
            readVector3d(value[2])
        )
    }

    private fun readMatrix4x3(value: Any?): Matrix4x3f {
        value as List<*>
        return Matrix4x3f(
            readVector3f(value[0]),
            readVector3f(value[1]),
            readVector3f(value[2]),
            readVector3f(value[3])
        )
    }

    private fun readMatrix4x3d(value: Any?): Matrix4x3d {
        value as List<*>
        return Matrix4x3d(
            readVector3d(value[0]),
            readVector3d(value[1]),
            readVector3d(value[2]),
            readVector3d(value[3])
        )
    }

    private fun readMatrix4x4(value: Any?): Matrix4f {
        value as List<*>
        return Matrix4f(
            readVector4f(value[0]),
            readVector4f(value[1]),
            readVector4f(value[2]),
            readVector4f(value[3])
        )
    }

    private fun readMatrix4x4d(value: Any?): Matrix4d {
        value as List<*>
        return Matrix4d(
            readVector4d(value[0]),
            readVector4d(value[1]),
            readVector4d(value[2]),
            readVector4d(value[3])
        )
    }

    private fun readByte(value: Any?): Byte = getInt(value).toByte()

    private fun readShort(value: Any?): Short = getInt(value).toShort()

    private fun readChar(value: Any?): Char {
        return when (value) { // correct???
            is String -> value[0]
            else -> getInt(value).toChar()
        }
    }

    private fun readInt(value: Any?): Int = getInt(value)

    private fun readColorArray(value: Any?): IntArray {
        return readArray(
            "color", value as List<*>,
            { IntArray(it) }, { array, index, value -> array[index] = readColor(value) })
    }

    private fun readColor(value: Any?): Int {
        return when (value) {
            is String -> {
                when (value.length) {
                    3 -> { // #rgb
                        val v = value.toInt(16)
                        return argb(
                            255,
                            v.ushr(8).and(15) * 17,
                            v.ushr(4).and(15) * 17,
                            v.and(15) * 17,
                        )
                    }
                    4 -> { // #argb
                        val v = value.toInt(16)
                        return argb(
                            v.ushr(12).and(15) * 17,
                            v.ushr(8).and(15) * 17,
                            v.ushr(4).and(15) * 17,
                            v.and(15) * 17,
                        )
                    }
                    // #rrggbb
                    6 -> return value.toInt(16) or black
                    // ##aarrggbb
                    8 -> return value.toInt(16)
                    else -> assertFail("Unknown color format")
                }
            }
            else -> readInt(value)
        }
    }

    private fun readLong(value: Any?): Long {
        return getLong(value)
    }

    private fun readBool(value: Any?): Boolean = getBool(value)
    private fun readFloat(value: Any?): Float = getFloat(value)
    private fun readDouble(value: Any?): Double = getDouble(value)

    private fun readBoolArray(value: List<*>) = readArray(
        "boolean", value,
        { BooleanArray(it) }, { array, index, value -> array[index] = readBool(value) })

    private fun readCharArray(value: List<*>) = readArray(
        "char", value,
        { CharArray(it) }, { array, index, value -> array[index] = readChar(value) })

    private fun readByteArray(value: List<*>) = readArray(
        "byte", value,
        { ByteArray(it) }, { array, index, value -> array[index] = readByte(value) })

    private fun readShortArray(value: List<*>) = readArray(
        "short", value,
        { ShortArray(it) }, { array, index, value -> array[index] = readShort(value) })

    private fun readIntArray(value: List<*>) = readArray(
        "int", value,
        { IntArray(it) }, { array, index, value -> array[index] = readInt(value) })

    private fun readLongArray(value: List<*>) = readArray(
        "long", value,
        { LongArray(it) }, { array, index, value -> array[index] = readLong(value) })

    private fun readFloatArray(value: List<*>) = readArray(
        "float", value,
        { FloatArray(it) }, { array, index, value -> array[index] = readFloat(value) }
    )

    private fun readDoubleArray(value: List<*>) = readArray(
        "double", value,
        { DoubleArray(it) }, { array, index, value -> array[index] = readDouble(value) }
    )

    private fun readSaveable(type: String?, value: Any?): Saveable? {
        return when (value) {
            null -> null
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                if (type != null) readObjectAndRegister(type, value as Map<String, *>)
                else readObject(value as Map<String, *>)
            }
            is Int, is String -> readPtr(value)
            else -> assertFail("Missing { or ptr or null after starting object[], got '$value'")
        }
    }

    private fun readMixedArray(value: List<*>): ArrayList<Saveable?> {
        return readArray("Any", value, {
            createArrayList(it, null)
        }, { array, index, value ->
            array[index] = readSaveable(null, value)
        })
    }

    private fun readMixedArray2D(value: List<*>): ArrayList<List<Saveable?>> {
        return readArray("Any[]", value, {
            createArrayList(it, emptyList())
        }, { array, index, value ->
            array[index] = readMixedArray(value as List<*>)
        })
    }

    private fun readFixedArray(type: String, value: List<*>): ArrayList<Saveable?> {
        return readArray(type, value, {
            createArrayList(it, null)
        }, { array, index, value ->
            array[index] = readSaveable(type, value)
        })
    }

    private fun readFixedArray2D(type: String, value: List<*>): ArrayList<List<Saveable?>> {
        return readArray(type, value, {
            createArrayList(it, emptyList())
        }, { array, index, value ->
            array[index] = readFixedArray(type, value as List<*>)
        })
    }

    private fun readProperty(obj: Saveable, typeName: String, value: Any?) {
        var (type, name) = splitTypeName(typeName)
        when (type) {
            "*[][]", "[][]" -> {// array of mixed types
                obj.setProperty(name, readMixedArray2D(value as List<*>))
            }
            "*[]", "[]" -> {// array of mixed types
                obj.setProperty(name, readMixedArray(value as List<*>))
            }
            else -> {
                val reader = readers[type]
                if (reader != null) {
                    reader(this, obj, name, value)
                } else if (type.endsWith("[][]")) {// 2d-array, but all elements have the same type
                    type = type.substring(0, type.length - 4)
                    val elements = readFixedArray2D(type, value as List<*>)
                    obj.setProperty(name, elements)
                } else if (type.endsWith("[]")) {// array, but all elements have the same type
                    type = type.substring(0, type.length - 2)
                    val elements = readFixedArray(type, value as List<*>)
                    obj.setProperty(name, elements)
                } else {
                    when (value) {
                        null -> obj.setProperty(name, null)
                        is Map<*, *> -> {
                            @Suppress("UNCHECKED_CAST")
                            obj.setProperty(name, readObjectAndRegister(type, value as Map<String, *>))
                        }
                        is Int, is String -> {
                            val ptr = getInt(value)
                            if (ptr > 0) {
                                val child = getByPointer(ptr, false)
                                if (child == null) {
                                    addMissingReference(obj, name, ptr)
                                } else {
                                    obj.setProperty(name, child)
                                }
                            }
                        }
                        else -> assertFail("Missing { or ptr or null after starting object of class $type, got '$value'")
                    }
                }
            }
        }
    }

    private fun readPtr(value: Any?): Saveable? {
        return getByPointer(readInt(value), warnIfMissing = true)
    }

    fun register(value: Saveable) = register(value, getUnusedPointer())

    private fun splitTypeName(typeName: String): Pair<String, String> {
        val index = typeName.indexOf(':')
        assertTrue(index >= 0, "Invalid Type:Name '$typeName'")
        val type = typeName.substring(0, index)
        val name = typeName.substring(index + 1)
        return type to name
    }

    companion object {

        private val readers = HashMap<String, JsonLikeReader.(obj: Saveable, name: String, value: Any?) -> Unit>(64)

        private fun <V> register1(
            type: SimpleType, v0: V,
            reader: JsonLikeReader.(Any?) -> V
        ) {
            readers[type.scalar] = { obj, name, value -> obj.setProperty(name, reader(value)) }
            readers[type.array] =
                { obj, name, value -> obj.setProperty(name, readArray(type, value as List<*>, v0) { reader(it) }) }
            readers[type.array2d] =
                { obj, name, value -> obj.setProperty(name, readArray2D(type, value as List<*>, v0) { reader(it) }) }
        }

        private fun <V, W> register2(
            type: SimpleType, v0: W,
            reader1: JsonLikeReader.(Any?) -> V,
            readerN: JsonLikeReader.(List<*>) -> W
        ) {
            readers[type.scalar] = { obj, name, value -> obj.setProperty(name, reader1(value)) }
            readers[type.array] = { obj, name, value -> obj.setProperty(name, readerN(value as List<*>)) }
            readers[type.array2d] = { obj, name, value ->
                val array = readArray(type.array2d, value as List<*>, v0) { valueI -> readerN(valueI as List<*>) }
                obj.setProperty(name, array)
            }
        }

        init {
            register2(SimpleType.BYTE, ByteArray(0), JsonLikeReader::readByte, JsonLikeReader::readByteArray)
            register2(SimpleType.SHORT, ShortArray(0), JsonLikeReader::readShort, JsonLikeReader::readShortArray)
            register2(SimpleType.INT, IntArray(0), JsonLikeReader::readInt, JsonLikeReader::readIntArray)
            register2(SimpleType.LONG, LongArray(0), JsonLikeReader::readLong, JsonLikeReader::readLongArray)
            register2(SimpleType.FLOAT, FloatArray(0), JsonLikeReader::readFloat, JsonLikeReader::readFloatArray)
            register2(SimpleType.DOUBLE, DoubleArray(0), JsonLikeReader::readDouble, JsonLikeReader::readDoubleArray)
            register2(SimpleType.BOOLEAN, BooleanArray(0), JsonLikeReader::readBool, JsonLikeReader::readBoolArray)
            register2(SimpleType.CHAR, CharArray(0), JsonLikeReader::readChar, JsonLikeReader::readCharArray)
            register2(SimpleType.COLOR, IntArray(0), JsonLikeReader::readColor, JsonLikeReader::readColorArray)
            register1(SimpleType.STRING, "", JsonLikeReader::readStringValue)
            register1(SimpleType.REFERENCE, InvalidRef, JsonLikeReader::readFile)
            register1(SimpleType.VECTOR2F, Vector2f(), JsonLikeReader::readVector2f)
            register1(SimpleType.VECTOR3F, Vector3f(), JsonLikeReader::readVector3f)
            register1(SimpleType.VECTOR4F, Vector4f(), JsonLikeReader::readVector4f)
            register1(SimpleType.VECTOR2D, Vector2d(), JsonLikeReader::readVector2d)
            register1(SimpleType.VECTOR3D, Vector3d(), JsonLikeReader::readVector3d)
            register1(SimpleType.VECTOR4D, Vector4d(), JsonLikeReader::readVector4d)
            register1(SimpleType.VECTOR2I, Vector2i(), JsonLikeReader::readVector2i)
            register1(SimpleType.VECTOR3I, Vector3i(), JsonLikeReader::readVector3i)
            register1(SimpleType.VECTOR4I, Vector4i(), JsonLikeReader::readVector4i)
            register1(SimpleType.QUATERNIONF, Quaternionf(), JsonLikeReader::readQuaternionf)
            register1(SimpleType.QUATERNIOND, Quaterniond(), JsonLikeReader::readQuaterniond)
            register1(SimpleType.AABBF, AABBf(), JsonLikeReader::readAABBf)
            register1(SimpleType.AABBD, AABBd(), JsonLikeReader::readAABBd)
            register1(SimpleType.PLANEF, Planef(), JsonLikeReader::readPlanef)
            register1(SimpleType.PLANED, Planed(), JsonLikeReader::readPlaned)
            register1(SimpleType.MATRIX2X2F, Matrix2f(), JsonLikeReader::readMatrix2x2)
            register1(SimpleType.MATRIX3X2F, Matrix3x2f(), JsonLikeReader::readMatrix3x2)
            register1(SimpleType.MATRIX3X3F, Matrix3f(), JsonLikeReader::readMatrix3x3)
            register1(SimpleType.MATRIX4X3F, Matrix4x3f(), JsonLikeReader::readMatrix4x3)
            register1(SimpleType.MATRIX4X4F, Matrix4f(), JsonLikeReader::readMatrix4x4)
            register1(SimpleType.MATRIX2X2D, Matrix2d(), JsonLikeReader::readMatrix2x2d)
            register1(SimpleType.MATRIX3X2D, Matrix3x2d(), JsonLikeReader::readMatrix3x2d)
            register1(SimpleType.MATRIX3X3D, Matrix3d(), JsonLikeReader::readMatrix3x3d)
            register1(SimpleType.MATRIX4X3D, Matrix4x3d(), JsonLikeReader::readMatrix4x3d)
            register1(SimpleType.MATRIX4X4D, Matrix4d(), JsonLikeReader::readMatrix4x4d)
        }

        private val LOGGER = LogManager.getLogger(JsonLikeReader::class)
    }
}