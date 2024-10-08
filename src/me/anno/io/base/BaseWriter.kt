package me.anno.io.base

import me.anno.ecs.annotations.ExtendableEnum
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.inspector.CachedReflections.Companion.getEnumId
import me.anno.gpu.texture.ITexture2D
import me.anno.graph.visual.render.Texture
import me.anno.io.files.FileReference
import me.anno.io.saveable.Saveable
import me.anno.io.utils.StringMap
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
import kotlin.reflect.KClass

abstract class BaseWriter(
    val workspace: FileReference,
    val canSkipDefaultValues: Boolean
) {

    var resourceMap: Map<FileReference, FileReference> = emptyMap()
    val todoPointers = ArrayList<Saveable>(256)
    val todoPointersSet = HashSet<Saveable>(256)
    val pointers = HashMap<Saveable, Int>(256)

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

    open fun writeBooleanArray2D(name: String, values: List<BooleanArray>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeChar(name: String, value: Char, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeCharArray(name: String, values: CharArray, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeCharArray2D(name: String, values: List<CharArray>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeByte(name: String, value: Byte, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeByteArray(name: String, values: ByteArray, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeByteArray2D(name: String, values: List<ByteArray>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeShort(name: String, value: Short, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeShortArray(name: String, values: ShortArray, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeShortArray2D(name: String, values: List<ShortArray>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeInt(name: String, value: Int, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeIntArray(name: String, values: IntArray, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeIntArray2D(name: String, values: List<IntArray>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeColor(name: String, value: Int, force: Boolean = true) =
        writeSomething(name, value, force)

    open fun writeColorArray(name: String, values: IntArray, force: Boolean = true) =
        writeSomething(name, values, force)

    open fun writeColorArray2D(name: String, values: List<IntArray>, force: Boolean = true) =
        writeSomething(name, values, force)

    open fun writeLong(name: String, value: Long, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeLongArray(name: String, values: LongArray, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeLongArray2D(name: String, values: List<LongArray>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeFloat(name: String, value: Float, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeFloatArray(name: String, values: FloatArray, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeFloatArray2D(name: String, values: List<FloatArray>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeDouble(name: String, value: Double, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeDoubleArray(name: String, values: DoubleArray, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeDoubleArray2D(name: String, values: List<DoubleArray>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeString(name: String, value: String, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeStringList(name: String, values: List<String>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeStringList2D(name: String, values: List<List<String>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeVector2f(name: String, value: Vector2f, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeVector3f(name: String, value: Vector3f, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeVector4f(name: String, value: Vector4f, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeVector2fList(name: String, values: List<Vector2f>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeVector3fList(name: String, values: List<Vector3f>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeVector4fList(name: String, values: List<Vector4f>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeVector2fList2D(name: String, values: List<List<Vector2f>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeVector3fList2D(name: String, values: List<List<Vector3f>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeVector4fList2D(name: String, values: List<List<Vector4f>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeVector2d(name: String, value: Vector2d, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeVector3d(name: String, value: Vector3d, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeVector4d(name: String, value: Vector4d, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeVector2dList(name: String, values: List<Vector2d>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeVector3dList(name: String, values: List<Vector3d>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeVector4dList(name: String, values: List<Vector4d>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeVector2dList2D(name: String, values: List<List<Vector2d>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeVector3dList2D(name: String, values: List<List<Vector3d>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeVector4dList2D(name: String, values: List<List<Vector4d>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeVector2i(name: String, value: Vector2i, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeVector3i(name: String, value: Vector3i, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeVector4i(name: String, value: Vector4i, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeVector2iList(name: String, values: List<Vector2i>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeVector3iList(name: String, values: List<Vector3i>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeVector4iList(name: String, values: List<Vector4i>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeVector2iList2D(name: String, values: List<List<Vector2i>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeVector3iList2D(name: String, values: List<List<Vector3i>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeVector4iList2D(name: String, values: List<List<Vector4i>>, force: Boolean = false) =
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

    open fun writeMatrix2x2fList(name: String, values: List<Matrix2f>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeMatrix3x2fList(name: String, values: List<Matrix3x2f>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeMatrix3x3fList(name: String, values: List<Matrix3f>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeMatrix4x3fList(name: String, values: List<Matrix4x3f>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeMatrix4x4fList(name: String, values: List<Matrix4f>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeMatrix2x2fList2D(name: String, values: List<List<Matrix2f>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeMatrix3x2fList2D(name: String, values: List<List<Matrix3x2f>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeMatrix3x3fList2D(name: String, values: List<List<Matrix3f>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeMatrix4x3fList2D(name: String, values: List<List<Matrix4x3f>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeMatrix4x4fList2D(name: String, values: List<List<Matrix4f>>, force: Boolean = false) =
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

    open fun writeMatrix2x2dList(name: String, values: List<Matrix2d>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeMatrix3x2dList(name: String, values: List<Matrix3x2d>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeMatrix3x3dList(name: String, values: List<Matrix3d>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeMatrix4x3dList(name: String, values: List<Matrix4x3d>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeMatrix4x4dList(name: String, values: List<Matrix4d>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeMatrix2x2dList2D(name: String, values: List<List<Matrix2d>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeMatrix3x2dList2D(name: String, values: List<List<Matrix3x2d>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeMatrix3x3dList2D(name: String, values: List<List<Matrix3d>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeMatrix4x3dList2D(name: String, values: List<List<Matrix4x3d>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeMatrix4x4dList2D(name: String, values: List<List<Matrix4d>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeQuaternionf(name: String, value: Quaternionf, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeQuaterniond(name: String, value: Quaterniond, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeQuaternionfList(name: String, values: List<Quaternionf>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeQuaterniondList(name: String, values: List<Quaterniond>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeQuaternionfList2D(name: String, values: List<List<Quaternionf>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeQuaterniondList2D(name: String, values: List<List<Quaterniond>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeAABBf(name: String, value: AABBf, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeAABBd(name: String, value: AABBd, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writeAABBfList(name: String, values: List<AABBf>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeAABBdList(name: String, values: List<AABBd>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeAABBfList2D(name: String, values: List<List<AABBf>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeAABBdList2D(name: String, values: List<List<AABBd>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writePlanef(name: String, value: Planef, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writePlaned(name: String, value: Planed, force: Boolean = false) =
        writeSomething(name, value, force)

    open fun writePlanefList(name: String, values: List<Planef>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writePlanedList(name: String, values: List<Planed>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writePlanefList2D(name: String, values: List<List<Planef>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writePlanedList2D(name: String, values: List<List<Planed>>, force: Boolean = false) =
        writeSomething(name, values, force)

    open fun writeFile(
        name: String, value: FileReference, force: Boolean = false
    ) = writeSomething(name, value, force)

    open fun writeFileList(
        name: String, values: List<FileReference>, force: Boolean = false
    ) = writeSomething(name, values, force)

    open fun writeFileList2D(
        name: String, values: List<List<FileReference>>, force: Boolean = false
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

    /**
     * saves a list of objects of different classes
     * */
    abstract fun <V : Saveable?> writeNullableObjectList(
        self: Saveable?, name: String,
        values: List<V>, force: Boolean = false
    )

    /**
     * saves a list of objects of different classes
     * */
    abstract fun <V : Saveable> writeObjectList(
        self: Saveable?, name: String,
        values: List<V>, force: Boolean = false
    )

    /**
     * saves a 2d list of objects of different classes
     * */
    abstract fun <V : Saveable> writeObjectList2D(
        self: Saveable?, name: String,
        values: List<List<V>>, force: Boolean = false
    )

    /**
     * saves a list of objects of one single class
     * all elements are guaranteed to be of the same getClassName()
     * */
    abstract fun <V : Saveable?> writeHomogenousObjectList(
        self: Saveable?, name: String,
        values: List<V>, force: Boolean = false
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
            is String -> writeStringList(name, cast(value), forceSaving)
            is FileReference -> writeFileList(name, cast(value), forceSaving)
            // vectors
            is Vector2f -> writeVector2fList(name, cast(value), forceSaving)
            is Vector3f -> writeVector3fList(name, cast(value), forceSaving)
            is Vector4f -> writeVector4fList(name, cast(value), forceSaving)
            is Vector2d -> writeVector2dList(name, cast(value), forceSaving)
            is Vector3d -> writeVector3dList(name, cast(value), forceSaving)
            is Vector4d -> writeVector4dList(name, cast(value), forceSaving)
            is Vector2i -> writeVector2iList(name, cast(value), forceSaving)
            is Vector3i -> writeVector3iList(name, cast(value), forceSaving)
            is Vector4i -> writeVector4iList(name, cast(value), forceSaving)
            // matrices
            is Matrix2f -> writeMatrix2x2fList(name, cast(value), forceSaving)
            is Matrix3x2f -> writeMatrix3x2fList(name, cast(value), forceSaving)
            is Matrix3f -> writeMatrix3x3fList(name, cast(value), forceSaving)
            is Matrix4x3f -> writeMatrix4x3fList(name, cast(value), forceSaving)
            is Matrix4f -> writeMatrix4x4fList(name, cast(value), forceSaving)
            is Matrix2d -> writeMatrix2x2dList(name, cast(value), forceSaving)
            is Matrix3x2d -> writeMatrix3x2dList(name, cast(value), forceSaving)
            is Matrix3d -> writeMatrix3x3dList(name, cast(value), forceSaving)
            is Matrix4x3d -> writeMatrix4x3dList(name, cast(value), forceSaving)
            is Matrix4d -> writeMatrix4x4dList(name, cast(value), forceSaving)
            // quaternions
            is Quaternionf -> writeQuaternionfList(name, cast(value), forceSaving)
            is Quaterniond -> writeQuaterniondList(name, cast(value), forceSaving)
            // planes
            is Planef -> writePlanefList(name, cast(value), forceSaving)
            is Planed -> writePlanedList(name, cast(value), forceSaving)
            // aabbs
            is AABBf -> writeAABBfList(name, cast(value), forceSaving)
            is AABBd -> writeAABBdList(name, cast(value), forceSaving)

            is BooleanArray -> writeBooleanArray2D(name, cast(value), forceSaving)
            is CharArray -> writeCharArray2D(name, cast(value), forceSaving)
            is ByteArray -> writeByteArray2D(name, cast(value), forceSaving)
            is ShortArray -> writeShortArray2D(name, cast(value), forceSaving)
            is IntArray -> writeIntArray2D(name, cast(value), forceSaving)
            is LongArray -> writeLongArray2D(name, cast(value), forceSaving)
            is FloatArray -> writeFloatArray2D(name, cast(value), forceSaving)
            is DoubleArray -> writeDoubleArray2D(name, cast(value), forceSaving)
            is PrefabSaveable ->
                writeNullableObjectList(self, name, filterII(value, PrefabSaveable::class), forceSaving)
            is Saveable ->
                writeNullableObjectList(self, name, filterII(value, Saveable::class), forceSaving)
            is List<*> -> {
                if (sample.isNotEmpty()) {
                    write2DList(name, cast(value), sample[0], forceSaving)
                } // else ...
            }
            else -> throw RuntimeException(
                "Not yet implemented: saving a list of $sample, " +
                        "${if (sample == null) null else sample::class}"
            )
        }
    }

    private fun write2DList(name: String, value: List<List<*>>, sample1: Any?, forceSaving: Boolean) {
        when (sample1) {
            // vectors
            is Vector2f -> writeVector2fList2D(name, cast(value), forceSaving)
            is Vector3f -> writeVector3fList2D(name, cast(value), forceSaving)
            is Vector4f -> writeVector4fList2D(name, cast(value), forceSaving)
            is Vector2d -> writeVector2dList2D(name, cast(value), forceSaving)
            is Vector3d -> writeVector3dList2D(name, cast(value), forceSaving)
            is Vector4d -> writeVector4dList2D(name, cast(value), forceSaving)
            is Vector2i -> writeVector2iList2D(name, cast(value), forceSaving)
            is Vector3i -> writeVector3iList2D(name, cast(value), forceSaving)
            is Vector4i -> writeVector4iList2D(name, cast(value), forceSaving)
            // matrices
            is Matrix2f -> writeMatrix2x2fList2D(name, cast(value), forceSaving)
            is Matrix2d -> writeMatrix2x2dList2D(name, cast(value), forceSaving)
            is Matrix3x2f -> writeMatrix3x2fList2D(name, cast(value), forceSaving)
            is Matrix3x2d -> writeMatrix3x2dList2D(name, cast(value), forceSaving)
            is Matrix3f -> writeMatrix3x3fList2D(name, cast(value), forceSaving)
            is Matrix3d -> writeMatrix3x3dList2D(name, cast(value), forceSaving)
            is Matrix4x3f -> writeMatrix4x3fList2D(name, cast(value), forceSaving)
            is Matrix4x3d -> writeMatrix4x3dList2D(name, cast(value), forceSaving)
            is Matrix4f -> writeMatrix4x4fList2D(name, cast(value), forceSaving)
            is Matrix4d -> writeMatrix4x4dList2D(name, cast(value), forceSaving)
            // quaternions
            is Quaternionf -> writeQuaternionfList2D(name, cast(value), forceSaving)
            is Quaterniond -> writeQuaterniondList2D(name, cast(value), forceSaving)
            // planes
            is Planef -> writePlanefList2D(name, cast(value), forceSaving)
            is Planed -> writePlanedList2D(name, cast(value), forceSaving)
            // aabbs
            is AABBf -> writeAABBfList2D(name, cast(value), forceSaving)
            is AABBd -> writeAABBdList2D(name, cast(value), forceSaving)
            // other
            is String -> writeStringList2D(name, cast(value), forceSaving)
            is FileReference -> writeFileList2D(name, cast(value), forceSaving)
            is Saveable -> writeObjectList2D<Saveable>(null, name, cast(value), forceSaving)
            else -> LOGGER.warn("Writing 2d array '$name' of type ${if (sample1 != null) sample1::class else null} hasn't been implemented")
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
            // matrices
            is Matrix2f -> writeMatrix2x2f(name, value,forceSaving)
            is Matrix3x2f -> writeMatrix3x2f(name, value,forceSaving)
            is Matrix3f -> writeMatrix3x3f(name, value, forceSaving)
            is Matrix4x3f -> writeMatrix4x3f(name, value, forceSaving)
            is Matrix4f -> writeMatrix4x4f(name, value, forceSaving)
            is Matrix2d -> writeMatrix2x2d(name, value,forceSaving)
            is Matrix3x2d -> writeMatrix3x2d(name, value,forceSaving)
            is Matrix3d -> writeMatrix3x3d(name, value, forceSaving)
            is Matrix4x3d -> writeMatrix4x3d(name, value, forceSaving)
            is Matrix4d -> writeMatrix4x4d(name, value, forceSaving)
            // quaternions
            is Quaternionf -> writeQuaternionf(name, value, forceSaving)
            is Quaterniond -> writeQuaterniond(name, value, forceSaving)
            // planes
            is Planef -> writePlanef(name, value, forceSaving)
            is Planed -> writePlaned(name, value, forceSaving)
            // aabbs
            is AABBf -> writeAABBf(name, value, forceSaving)
            is AABBd -> writeAABBd(name, value, forceSaving)
            // others
            is FileReference -> writeFile(name, value, forceSaving)
            null -> writeObject(self, name, null, forceSaving)
            is Enum<*> -> writeEnum(name, value, forceSaving)
            is ExtendableEnum -> writeInt(name, value.id, forceSaving)
            // java-serializable
            is Serializable -> {
                // implement it?...
                LOGGER.warn("Could not serialize field $name with value $value of class ${value::class}, Serializable")
            }
            is Texture, is ITexture2D -> {}
            else -> LOGGER.warn("Ignored saving $name: $value of class ${value::class}")
        }
    }

    // makes the code a little nicer
    fun <V> cast(input: Any): V {
        @Suppress("unchecked_cast")
        return input as V
    }

    fun <V : Any> filterII(value: List<Any?>, clazz: KClass<V>): List<V> {
        return value.filterIsInstance(clazz.java)
    }

    companion object {
        private val LOGGER = LogManager.getLogger(BaseWriter::class)
    }
}