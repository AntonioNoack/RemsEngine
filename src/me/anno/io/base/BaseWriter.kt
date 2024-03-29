package me.anno.io.base

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.EngineBase
import me.anno.engine.inspector.CachedReflections.Companion.getEnumId
import me.anno.io.Saveable
import me.anno.io.files.FileReference
import me.anno.io.utils.StringMap
import me.anno.utils.structures.maps.BiMap
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
import java.io.Serializable

abstract class BaseWriter(val canSkipDefaultValues: Boolean) {

    val todoPointers = ArrayList<Saveable>(256)
    val todoPointersSet = HashSet<Saveable>(256)
    val pointers = BiMap<Saveable, Int>(256)

    /**
     * gets the pointer of a know value
     * */
    fun getPointer(value: Saveable) = pointers[value]

    open fun writeSomething(name: String, value: Any?, force: Boolean) {
        LOGGER.warn("Unknown class ${if (value != null) value::class else null} for serialization")
    }

    open fun writeBoolean(name: String, value: Boolean, force: Boolean = true) =
        writeSomething(name, value, force)

    open fun writeBooleanArray(name: String, values: BooleanArray, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeBooleanArray2D(name: String, values: Array<BooleanArray>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeChar(name: String, value: Char, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeCharArray(name: String, values: CharArray, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeCharArray2D(name: String, values: Array<CharArray>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeByte(name: String, value: Byte, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeByteArray(name: String, values: ByteArray, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeByteArray2D(name: String, values: Array<ByteArray>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeShort(name: String, value: Short, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeShortArray(name: String, values: ShortArray, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeShortArray2D(name: String, values: Array<ShortArray>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeInt(name: String, value: Int, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeIntArray(name: String, values: IntArray, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeIntArray2D(name: String, values: Array<IntArray>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeColor(name: String, value: Int, force: Boolean = true) =
        writeSomething(name, value, force)

    open fun writeColorArray(name: String, values: IntArray, force: Boolean = true) =
        writeSomething(name, values, force)

    open fun writeColorArray2D(name: String, values: Array<IntArray>, force: Boolean = true) =
        writeSomething(name, values, force)

    open fun writeLong(name: String, value: Long, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeLongArray(name: String, values: LongArray, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeLongArray2D(name: String, values: Array<LongArray>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeFloat(name: String, value: Float, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeFloatArray(name: String, values: FloatArray, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeFloatArray2D(name: String, values: Array<FloatArray>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeDouble(name: String, value: Double, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeDoubleArray(name: String, values: DoubleArray, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeDoubleArray2D(name: String, values: Array<DoubleArray>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeString(name: String, value: String, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeStringArray(name: String, values: Array<String>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeStringArray2D(name: String, values: Array<Array<String>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeVector2f(name: String, value: Vector2f, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeVector3f(name: String, value: Vector3f, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeVector4f(name: String, value: Vector4f, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeVector2fArray(name: String, values: Array<Vector2f>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeVector3fArray(name: String, values: Array<Vector3f>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeVector4fArray(name: String, values: Array<Vector4f>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeVector2fArray2D(name: String, values: Array<Array<Vector2f>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeVector3fArray2D(name: String, values: Array<Array<Vector3f>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeVector4fArray2D(name: String, values: Array<Array<Vector4f>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeVector2d(name: String, value: Vector2d, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeVector3d(name: String, value: Vector3d, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeVector4d(name: String, value: Vector4d, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeVector2dArray(name: String, values: Array<Vector2d>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeVector3dArray(name: String, values: Array<Vector3d>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeVector4dArray(name: String, values: Array<Vector4d>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeVector2dArray2D(name: String, values: Array<Array<Vector2d>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeVector3dArray2D(name: String, values: Array<Array<Vector3d>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeVector4dArray2D(name: String, values: Array<Array<Vector4d>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeVector2i(name: String, value: Vector2i, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeVector3i(name: String, value: Vector3i, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeVector4i(name: String, value: Vector4i, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeVector2iArray(name: String, values: Array<Vector2i>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeVector3iArray(name: String, values: Array<Vector3i>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeVector4iArray(name: String, values: Array<Vector4i>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeVector2iArray2D(name: String, values: Array<Array<Vector2i>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeVector3iArray2D(name: String, values: Array<Array<Vector3i>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeVector4iArray2D(name: String, values: Array<Array<Vector4i>>, force: Boolean = false) =
        writeSomething(name, values, force)

    // matrices, which are commonly used in game development
    open fun writeMatrix2x2f(name: String, value: Matrix2f, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeMatrix3x2f(name: String, value: Matrix3x2f, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeMatrix3x3f(name: String, value: Matrix3f, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeMatrix4x3f(name: String, value: Matrix4x3f, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeMatrix4x4f(name: String, value: Matrix4f, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeMatrix2x2fArray(name: String, values: Array<Matrix2f>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeMatrix3x2fArray(name: String, values: Array<Matrix3x2f>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeMatrix3x3fArray(name: String, values: Array<Matrix3f>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeMatrix4x3fArray(name: String, values: Array<Matrix4x3f>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeMatrix4x4fArray(name: String, values: Array<Matrix4f>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeMatrix2x2fArray2D(name: String, values: Array<Array<Matrix2f>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeMatrix3x2fArray2D(name: String, values: Array<Array<Matrix3x2f>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeMatrix3x3fArray2D(name: String, values: Array<Array<Matrix3f>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeMatrix4x3fArray2D(name: String, values: Array<Array<Matrix4x3f>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeMatrix4x4fArray2D(name: String, values: Array<Array<Matrix4f>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeMatrix2x2d(name: String, value: Matrix2d, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeMatrix3x2d(name: String, value: Matrix3x2d, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeMatrix3x3d(name: String, value: Matrix3d, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeMatrix4x3d(name: String, value: Matrix4x3d, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeMatrix4x4d(name: String, value: Matrix4d, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeMatrix2x2dArray(name: String, values: Array<Matrix2d>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeMatrix3x2dArray(name: String, values: Array<Matrix3x2d>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeMatrix3x3dArray(name: String, values: Array<Matrix3d>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeMatrix4x3dArray(name: String, values: Array<Matrix4x3d>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeMatrix4x4dArray(name: String, values: Array<Matrix4d>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeMatrix2x2dArray2D(name: String, values: Array<Array<Matrix2d>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeMatrix3x2dArray2D(name: String, values: Array<Array<Matrix3x2d>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeMatrix3x3dArray2D(name: String, values: Array<Array<Matrix3d>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeMatrix4x3dArray2D(name: String, values: Array<Array<Matrix4x3d>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeMatrix4x4dArray2D(name: String, values: Array<Array<Matrix4d>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeQuaternionf(name: String, value: Quaternionf, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeQuaterniond(name: String, value: Quaterniond, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeQuaternionfArray(name: String, values: Array<Quaternionf>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeQuaterniondArray(name: String, values: Array<Quaterniond>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeQuaternionfArray2D(name: String, values: Array<Array<Quaternionf>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeQuaterniondArray2D(name: String, values: Array<Array<Quaterniond>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeAABBf(name: String, value: AABBf, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeAABBd(name: String, value: AABBd, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeAABBfArray(name: String, values: Array<AABBf>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeAABBdArray(name: String, values: Array<AABBd>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeAABBfArray2D(name: String, values: Array<Array<AABBf>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeAABBdArray2D(name: String, values: Array<Array<AABBd>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writePlanef(name: String, value: Planef, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writePlaned(name: String, value: Planed, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writePlanefArray(name: String, values: Array<Planef>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writePlanedArray(name: String, values: Array<Planed>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writePlanefArray2D(name: String, values: Array<Array<Planef>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writePlanedArray2D(name: String, values: Array<Array<Planed>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeFile(
        name: String, value: FileReference, force: Boolean = false,
        workspace: FileReference = EngineBase.workspace
    ) = writeSomething(name, value, force)

    open fun writeFileArray(
        name: String, values: Array<FileReference>, force: Boolean = false,
        workspace: FileReference = EngineBase.workspace
    ) = writeSomething(name, values, force)

    open fun writeFileArray2D(
        name: String, values: Array<Array<FileReference>>, force: Boolean = false,
        workspace: FileReference = EngineBase.workspace
    ) = writeSomething(name, values, force)

    fun writeObject(self: Saveable?, name: String?, value: Saveable?, force: Boolean = false) {
        when {
            value == null -> if (force) writeNull(name)
            force || !(canSkipDefaultValues && value.isDefaultValue()) -> {
                val ptr0 = pointers[value]
                if (ptr0 != null) {
                    if (todoPointersSet.remove(value)) {
                        writeObjectImpl(name, value)
                    } else {
                        writePointer(name, value.className, ptr0, value)
                    }
                } else {
                    val canInclude = self == null || self.approxSize > value.approxSize
                    if (canInclude) {
                        generatePointer(value, false)
                        writeObjectImpl(name, value)
                    } else {
                        val ptr = generatePointer(value, true)
                        writePointer(name, value.className, ptr, value)
                    }
                }
            }
        }
    }

    abstract fun writeNull(name: String?)
    abstract fun writePointer(name: String?, className: String, ptr: Int, value: Saveable)
    abstract fun writeObjectImpl(name: String?, value: Saveable)

    open fun <V : Saveable> writeObjectList(
        self: Saveable?,
        name: String,
        values: List<V>?,
        force: Boolean = false
    ) {
        if (force || values?.isNotEmpty() == true) {
            writeNullableObjectList(self, name, values, force)
        }
    }

    open fun <V : Saveable> writeNullableObjectList(
        self: Saveable?,
        name: String,
        values: List<V?>?,
        force: Boolean = false
    ) {
        if (force || values?.isNotEmpty() == true) {
            @Suppress("unchecked_cast")
            writeNullableObjectArray(
                self, name, if (values == null) emptyArray<Any>() as Array<V> else
                    Array<Saveable?>(values.size) { values[it] }, force
            )
        }
    }

    /**
     * saves an array of objects of different classes
     * */
    abstract fun <V : Saveable?> writeNullableObjectArray(
        self: Saveable?,
        name: String,
        values: Array<V>?,
        force: Boolean = false
    )

    /**
     * saves an array of objects of different classes
     * */
    abstract fun <V : Saveable> writeObjectArray(
        self: Saveable?,
        name: String,
        values: Array<V>?,
        force: Boolean = false
    )

    /**
     * saves a 2d array of objects of different classes
     * */
    abstract fun <V : Saveable> writeObjectArray2D(
        self: Saveable?,
        name: String,
        values: Array<Array<V>>,
        force: Boolean = false
    )

    /**
     * saves an array of objects of one single class
     * all elements are guaranteed to be of the same getClassName()
     * */
    abstract fun <V : Saveable?> writeHomogenousObjectArray(
        self: Saveable?,
        name: String,
        values: Array<V>,
        force: Boolean = false
    )

    fun generatePointer(obj: Saveable, addToSorted: Boolean): Int {
        val ptr = pointers.size + 1
        pointers[obj] = ptr
        if (addToSorted) {
            todoPointers += obj
            todoPointersSet += obj
        }
        return ptr
    }

    fun add(obj: Saveable) {
        if (obj !in pointers) {
            generatePointer(obj, true)
        }
    }

    abstract fun writeListStart()
    abstract fun writeListEnd()
    abstract fun writeListSeparator()

    open fun writeAllInList() {
        writeListStart()
        var i = 0
        var hadEntry = false
        while (i < todoPointers.size) {
            val value = todoPointers[i++]
            if (todoPointersSet.remove(value)) {
                if (hadEntry) writeListSeparator()
                writeObjectImpl(null, value)
                hadEntry = true
            }
        }
        writeListEnd()
        flush()
    }

    open fun flush() {}

    open fun close() {}

    fun writeEnum(name: String, value: Enum<*>, forceSaving: Boolean = true) {
        /**
         * if there is an id, use it instead; must be Int
         * alternatively, we could write the enum as a string
         * this would be developer-friendlier :)
         * at the same time, it causes issues, when old save files are read
         * */
        val id = getEnumId(value)
        if (id is Int) {
            writeInt(name, id, forceSaving)
        } else {
            LOGGER.warn("Enum class '${value::class}' is missing property 'id' of type Int for automatic serialization!")
            writeString(name, "${value.ordinal}/${value.name}", forceSaving)
        }
    }

    fun writeSomething(self: Saveable?, type: String, name: String, value: Any?, forceSaving: Boolean) {
        when (type) {
            // todo all types
            // especially of interest: List<List<...>>, Array<Array<...>>, ...
            else -> writeSomething(self, name, value, forceSaving)
        }
    }

    private fun write1DList(self: Saveable?, name: String, value: List<*>, sample: Any?, forceSaving: Boolean) {
        when (sample) {

            is Boolean -> writeBooleanArray(
                name,
                BooleanArray(value.size) { value[it] as Boolean },
                forceSaving
            )
            is Char -> writeCharArray(name, CharArray(value.size) { value[it] as Char }, forceSaving)
            is Byte -> writeByteArray(name, ByteArray(value.size) { value[it] as Byte }, forceSaving)
            is Short -> writeShortArray(name, ShortArray(value.size) { value[it] as Short }, forceSaving)
            is Int -> writeIntArray(name, IntArray(value.size) { value[it] as Int }, forceSaving)
            is Long -> writeLongArray(name, LongArray(value.size) { value[it] as Long }, forceSaving)
            is Float -> writeFloatArray(name, FloatArray(value.size) { value[it] as Float }, forceSaving)
            is Double -> writeDoubleArray(
                name,
                DoubleArray(value.size) { value[it] as Double },
                forceSaving
            )

            is BooleanArray -> writeBooleanArray2D(name, toArray(value), forceSaving)
            is CharArray -> writeCharArray2D(name, toArray(value), forceSaving)
            is ByteArray -> writeByteArray2D(name, toArray(value), forceSaving)
            is ShortArray -> writeShortArray2D(name, toArray(value), forceSaving)
            is IntArray -> writeIntArray2D(name, toArray(value), forceSaving)
            is LongArray -> writeLongArray2D(name, toArray(value), forceSaving)
            is FloatArray -> writeFloatArray2D(name, toArray(value), forceSaving)
            is DoubleArray -> writeDoubleArray2D(name, toArray(value), forceSaving)

            is String -> writeStringArray(name, toArray(value), forceSaving)

            is Vector2f -> writeVector2fArray(name, toArray(value), forceSaving)
            is Vector3f -> writeVector3fArray(name, toArray(value), forceSaving)
            is Vector4f -> writeVector4fArray(name, toArray(value), forceSaving)
            is Vector2d -> writeVector2dArray(name, toArray(value), forceSaving)
            is Vector3d -> writeVector3dArray(name, toArray(value), forceSaving)
            is Vector4d -> writeVector4dArray(name, toArray(value), forceSaving)
            is Vector2i -> writeVector2iArray(name, toArray(value), forceSaving)
            is Vector3i -> writeVector3iArray(name, toArray(value), forceSaving)
            is Vector4i -> writeVector4iArray(name, toArray(value), forceSaving)

            is Matrix2f -> writeMatrix2x2fArray(name, toArray(value), forceSaving)
            is Matrix3x2f -> writeMatrix3x2fArray(name, toArray(value), forceSaving)
            is Matrix3f -> writeMatrix3x3fArray(name, toArray(value), forceSaving)
            is Matrix4x3f -> writeMatrix4x3fArray(name, toArray(value), forceSaving)
            is Matrix4f -> writeMatrix4x4fArray(name, toArray(value), forceSaving)

            is Matrix2d -> writeMatrix2x2dArray(name, toArray(value), forceSaving)
            is Matrix3x2d -> writeMatrix3x2dArray(name, toArray(value), forceSaving)
            is Matrix3d -> writeMatrix3x3dArray(name, toArray(value), forceSaving)
            is Matrix4x3d -> writeMatrix4x3dArray(name, toArray(value), forceSaving)
            is Matrix4d -> writeMatrix4x4dArray(name, toArray(value), forceSaving)

            is Quaternionf -> writeQuaternionfArray(name, toArray(value), forceSaving)
            is Quaterniond -> writeQuaterniondArray(name, toArray(value), forceSaving)

            is Planef -> writePlanefArray(name, toArray(value), forceSaving)
            is Planed -> writePlanedArray(name, toArray(value), forceSaving)

            is AABBf -> writeAABBfArray(name, toArray(value), forceSaving)
            is AABBd -> writeAABBdArray(name, toArray(value), forceSaving)

            // is PrefabSaveable -> writeObjectArray(self, name, toArray(value), forceSaving)
            is Saveable -> writeObjectArray(self, name, toArray(value), forceSaving)
            is FileReference -> writeFileArray(name, toArray(value), forceSaving)

            // todo 2d stuff...
            else -> throw RuntimeException("Not yet implemented: saving a list of '$name' ${if (sample != null) sample::class else null}")
        }
    }

    private fun write1DArray(self: Saveable?, name: String, value: Any, sample: Any?, forceSaving: Boolean) {
        when (sample) {

            is String -> writeStringArray(name, cast(value), forceSaving)

            is Vector2f -> writeVector2fArray(name, cast(value), forceSaving)
            is Vector3f -> writeVector3fArray(name, cast(value), forceSaving)
            is Vector4f -> writeVector4fArray(name, cast(value), forceSaving)
            is Vector2d -> writeVector2dArray(name, cast(value), forceSaving)
            is Vector3d -> writeVector3dArray(name, cast(value), forceSaving)
            is Vector4d -> writeVector4dArray(name, cast(value), forceSaving)
            is Vector2i -> writeVector2iArray(name, cast(value), forceSaving)
            is Vector3i -> writeVector3iArray(name, cast(value), forceSaving)
            is Vector4i -> writeVector4iArray(name, cast(value), forceSaving)

            is Matrix2f -> writeMatrix2x2fArray(name, cast(value), forceSaving)
            is Matrix3x2f -> writeMatrix3x2fArray(name, cast(value), forceSaving)
            is Matrix3f -> writeMatrix3x3fArray(name, cast(value), forceSaving)
            is Matrix4x3f -> writeMatrix4x3fArray(name, cast(value), forceSaving)
            is Matrix4f -> writeMatrix4x4fArray(name, cast(value), forceSaving)

            is Matrix2d -> writeMatrix2x2dArray(name, cast(value), forceSaving)
            is Matrix3x2d -> writeMatrix3x2dArray(name, cast(value), forceSaving)
            is Matrix3d -> writeMatrix3x3dArray(name, cast(value), forceSaving)
            is Matrix4x3d -> writeMatrix4x3dArray(name, cast(value), forceSaving)
            is Matrix4d -> writeMatrix4x4dArray(name, cast(value), forceSaving)

            is Quaternionf -> writeQuaternionfArray(name, cast(value), forceSaving)
            is Quaterniond -> writeQuaterniondArray(name, cast(value), forceSaving)

            is BooleanArray -> writeBooleanArray2D(name, cast(value), forceSaving)
            is CharArray -> writeCharArray2D(name, cast(value), forceSaving)
            is ByteArray -> writeByteArray2D(name, cast(value), forceSaving)
            is ShortArray -> writeShortArray2D(name, cast(value), forceSaving)
            is IntArray -> writeIntArray2D(name, cast(value), forceSaving)
            is LongArray -> writeLongArray2D(name, cast(value), forceSaving)
            is FloatArray -> writeFloatArray2D(name, cast(value), forceSaving)
            is DoubleArray -> writeDoubleArray2D(name, cast(value), forceSaving)
            is PrefabSaveable -> {
                @Suppress("UNCHECKED_CAST")
                writeNullableObjectArray(
                    self, name, value as Array<Saveable?>, forceSaving
                )
            }
            is Saveable -> {
                @Suppress("UNCHECKED_CAST")
                writeNullableObjectArray(self, name, value as Array<Saveable?>, forceSaving)
            }
            is FileReference -> writeFileArray(name, cast(value), forceSaving)
            is Array<*> -> {
                if (sample.isNotEmpty()) {
                    write2DArray(name, value, sample[0], forceSaving)
                } // else ...
            }
            else -> throw RuntimeException("Not yet implemented: saving an array of $sample")
        }
    }

    private fun write2DArray(name: String, value: Any, sample1: Any?, forceSaving: Boolean) {
        when (sample1) {
            // vectors
            is Vector2f -> writeVector2fArray2D(name, cast(value), forceSaving)
            is Vector3f -> writeVector3fArray2D(name, cast(value), forceSaving)
            is Vector4f -> writeVector4fArray2D(name, cast(value), forceSaving)
            is Vector2d -> writeVector2dArray2D(name, cast(value), forceSaving)
            is Vector3d -> writeVector3dArray2D(name, cast(value), forceSaving)
            is Vector4d -> writeVector4dArray2D(name, cast(value), forceSaving)
            is Vector2i -> writeVector2iArray2D(name, cast(value), forceSaving)
            is Vector3i -> writeVector3iArray2D(name, cast(value), forceSaving)
            is Vector4i -> writeVector4iArray2D(name, cast(value), forceSaving)
            // matrices
            is Matrix2f -> writeMatrix2x2fArray2D(name, cast(value), forceSaving)
            is Matrix2d -> writeMatrix2x2dArray2D(name, cast(value), forceSaving)
            is Matrix3x2f -> writeMatrix3x2fArray2D(name, cast(value), forceSaving)
            is Matrix3x2d -> writeMatrix3x2dArray2D(name, cast(value), forceSaving)
            is Matrix3f -> writeMatrix3x3fArray2D(name, cast(value), forceSaving)
            is Matrix3d -> writeMatrix3x3dArray2D(name, cast(value), forceSaving)
            is Matrix4x3f -> writeMatrix4x3fArray2D(name, cast(value), forceSaving)
            is Matrix4x3d -> writeMatrix4x3dArray2D(name, cast(value), forceSaving)
            is Matrix4f -> writeMatrix4x4fArray2D(name, cast(value), forceSaving)
            is Matrix4d -> writeMatrix4x4dArray2D(name, cast(value), forceSaving)
            // quaternions
            is Quaternionf -> writeQuaternionfArray2D(name, cast(value), forceSaving)
            is Quaterniond -> writeQuaterniondArray2D(name, cast(value), forceSaving)
            // planes
            is Planef -> writePlanefArray2D(name, cast(value), forceSaving)
            is Planed -> writePlanedArray2D(name, cast(value), forceSaving)
            // aabbs
            is AABBf -> writeAABBfArray2D(name, cast(value), forceSaving)
            is AABBd -> writeAABBdArray2D(name, cast(value), forceSaving)
            // other
            is String -> writeStringArray2D(name, cast(value), forceSaving)
            is FileReference -> writeFileArray2D(name, cast(value), forceSaving)
            else -> throw NotImplementedError("Writing 2d array of type ${if (sample1 != null) sample1::class else null}, '$name'")
        }
    }

    /**
     * this is a general function to save a value
     * if you know the type, please use one of the other functions,
     * because they may be faster
     * */
    fun writeSomething(self: Saveable?, name: String, value: Any?, forceSaving: Boolean) {
        when (value) {
            // native types
            is Boolean -> writeBoolean(name, value, forceSaving)
            is Char -> writeChar(name, value, forceSaving)
            is Byte -> writeByte(name, value, forceSaving)
            is Short -> writeShort(name, value, forceSaving)
            is Int -> if (name.endsWith("color", true) || name.contains("color.", true))
                writeColor(name, value, forceSaving) else writeInt(name, value, forceSaving)
            is Long -> writeLong(name, value, forceSaving)
            is Float -> writeFloat(name, value, forceSaving)
            is Double -> writeDouble(name, value, forceSaving)
            is String -> writeString(name, value, forceSaving)
            // saveable
            is Saveable -> writeObject(self, name, value)
            // lists & arrays
            is List<*> -> {
                if (value.isNotEmpty()) {
                    write1DList(self, name, value, value[0], forceSaving)
                } // else if is force saving, then this won't work, because of the weak generics in Java :/
            }
            is Array<*> -> {
                if (value.isNotEmpty()) {
                    write1DArray(self, name, value, value[0], forceSaving)
                } // else if is force saving, then this won't work, because of the weak generics in Java :/
            }
            is Map<*, *> -> {// mmh, mediocre solution
                val map = StringMap(value.size)
                for ((k, v) in value) {
                    map[k.toString()] = v
                }
                writeObject(self, name, map, forceSaving)
            }
            // native arrays
            is BooleanArray -> writeBooleanArray(name, value, forceSaving)
            is CharArray -> writeCharArray(name, value, forceSaving)
            is ByteArray -> writeByteArray(name, value, forceSaving)
            is ShortArray -> writeShortArray(name, value, forceSaving)
            is IntArray -> if (name.endsWith("color", true) || name.endsWith("colors", true)) {
                writeColorArray(name, value, forceSaving)
            } else writeIntArray(name, value, forceSaving)
            is LongArray -> writeLongArray(name, value, forceSaving)
            is FloatArray -> writeFloatArray(name, value, forceSaving)
            is DoubleArray -> writeDoubleArray(name, value, forceSaving)
            // all vectors and such
            is Vector2f -> writeVector2f(name, value, forceSaving)
            is Vector3f -> writeVector3f(name, value, forceSaving)
            is Vector4f -> writeVector4f(name, value, forceSaving)
            is Vector2d -> writeVector2d(name, value, forceSaving)
            is Vector3d -> writeVector3d(name, value, forceSaving)
            is Vector4d -> writeVector4d(name, value, forceSaving)
            is Vector2i -> writeVector2i(name, value, forceSaving)
            is Vector3i -> writeVector3i(name, value, forceSaving)
            is Vector4i -> writeVector4i(name, value, forceSaving)
            is Matrix3f -> writeMatrix3x3f(name, value, forceSaving)
            is Matrix4x3f -> writeMatrix4x3f(name, value, forceSaving)
            is Matrix4f -> writeMatrix4x4f(name, value, forceSaving)
            is Matrix3d -> writeMatrix3x3d(name, value, forceSaving)
            is Matrix4x3d -> writeMatrix4x3d(name, value, forceSaving)
            is Matrix4d -> writeMatrix4x4d(name, value, forceSaving)
            is Quaternionf -> writeQuaternionf(name, value, forceSaving)
            is Quaterniond -> writeQuaterniond(name, value, forceSaving)
            is Planef -> writePlanef(name, value, forceSaving)
            is Planed -> writePlaned(name, value, forceSaving)
            is AABBf -> writeAABBf(name, value, forceSaving)
            is AABBd -> writeAABBd(name, value, forceSaving)
            // others
            is FileReference -> writeFile(name, value, forceSaving)
            null -> writeObject(self, name, null, forceSaving)
            is Enum<*> -> writeEnum(name, value, forceSaving)
            // java-serializable
            is Serializable -> {
                // implement it?...
                // if it is an enum, write its value
                // enums always are Serializable
                val clazz = value::class.java
                try {
                    val getId = clazz.getMethod("getId")
                    val id = getId.invoke(value)
                    if (id is Int) {
                        // all good :)
                        writeInt(name, id, forceSaving)
                        return
                    }
                } catch (e: NoSuchMethodException) {
                    // e.printStackTrace()
                }
                LOGGER.warn("Could not serialize field $name with value $value of class ${value.javaClass}, Serializable")
                // todo write lists and maps with our tools
                // todo write default serializables...
                /*val bytes0 = ByteArrayOutputStream()
                val bytes = ObjectOutputStream(bytes0)
                bytes.writeObject(value)
                bytes.close()
                writeByteArray(name, bytes0.toByteArray())*/
            }
            else -> {
                val msg =
                    "saving $name: $value of class ${value.javaClass}, maybe it needs to be me.anno.io.[I]Saveable?"
                if (value !is Function<*>)
                    throw RuntimeException("Todo implement $msg") // functions cannot easily be serialized
                else LOGGER.warn("Ignored $msg")
            }
        }
    }

    // makes the code a little nicer
    fun <V> cast(input: Any): V {
        @Suppress("unchecked_cast")
        return input as V
    }

    inline fun <reified V> toArray(value: List<Any?>): Array<V> {
        return value.filterIsInstance<V>().toTypedArray()
    }

    companion object {
        private val LOGGER = LogManager.getLogger(BaseWriter::class)
    }
}