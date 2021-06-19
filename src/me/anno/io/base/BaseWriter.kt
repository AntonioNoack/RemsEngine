package me.anno.io.base

import com.sun.javafx.geom.Vec4d
import me.anno.io.FileReference
import me.anno.io.ISaveable
import me.anno.studio.StudioBase
import me.anno.utils.LOGGER
import me.anno.utils.files.LocalFile.toLocalPath
import org.joml.*
import java.io.DataOutputStream
import java.io.File
import java.io.ObjectOutputStream
import java.io.Serializable

abstract class BaseWriter(val respectsDefaultValues: Boolean) {

    val todo = ArrayList<ISaveable>(256)
    private val listed = HashSet<ISaveable>()
    private val pointers = HashMap<ISaveable, Int>()

    /**
     * gets the pointer of a know value
     * */
    fun getPointer(value: ISaveable) = pointers[value]

    abstract fun writeBoolean(name: String, value: Boolean, force: Boolean = true)
    abstract fun writeBooleanArray(name: String, value: BooleanArray, force: Boolean = false)

    abstract fun writeChar(name: String, value: Char, force: Boolean = false)
    abstract fun writeCharArray(name: String, value: CharArray, force: Boolean = false)

    abstract fun writeByte(name: String, value: Byte, force: Boolean = false)
    abstract fun writeByteArray(name: String, value: ByteArray, force: Boolean = false)

    abstract fun writeShort(name: String, value: Short, force: Boolean = false)
    abstract fun writeShortArray(name: String, value: ShortArray, force: Boolean = false)

    abstract fun writeInt(name: String, value: Int, force: Boolean = false)
    abstract fun writeIntArray(name: String, value: IntArray, force: Boolean = false)

    abstract fun writeLong(name: String, value: Long, force: Boolean = false)
    abstract fun writeLongArray(name: String, value: LongArray, force: Boolean = false)

    abstract fun writeFloat(name: String, value: Float, force: Boolean = false)
    abstract fun writeFloatArray(name: String, value: FloatArray, force: Boolean = false)
    abstract fun writeFloatArray2D(name: String, value: Array<FloatArray>, force: Boolean = false)

    abstract fun writeDouble(name: String, value: Double, force: Boolean = false)
    abstract fun writeDoubleArray(name: String, value: DoubleArray, force: Boolean = false)
    abstract fun writeDoubleArray2D(name: String, value: Array<DoubleArray>, force: Boolean = false)

    abstract fun writeString(name: String, value: String?, force: Boolean = false)
    abstract fun writeStringArray(name: String, value: Array<String>, force: Boolean = false)

    abstract fun writeVector2f(name: String, value: Vector2fc, force: Boolean = false)
    abstract fun writeVector2fArray(name: String, values: Array<Vector2fc>, force: Boolean = false)
    abstract fun writeVector3f(name: String, value: Vector3fc, force: Boolean = false)
    abstract fun writeVector3fArray(name: String, values: Array<Vector3fc>, force: Boolean = false)
    abstract fun writeVector4f(name: String, value: Vector4fc, force: Boolean = false)
    abstract fun writeVector4fArray(name: String, values: Array<Vector4fc>, force: Boolean = false)

    abstract fun writeVector2d(name: String, value: Vector2dc, force: Boolean = false)
    abstract fun writeVector2dArray(name: String, values: Array<Vector2dc>, force: Boolean = false)
    abstract fun writeVector3d(name: String, value: Vector3dc, force: Boolean = false)
    abstract fun writeVector3dArray(name: String, values: Array<Vector3dc>, force: Boolean = false)
    abstract fun writeVector4d(name: String, value: Vector4dc, force: Boolean = false)
    abstract fun writeVector4dArray(name: String, values: Array<Vector4dc>, force: Boolean = false)

    // matrices, which are commonly used in game development
    // todo array types, as they could be useful for saving animations maybe
    abstract fun writeMatrix3x3f(name: String, value: Matrix3fc, force: Boolean = false)
    abstract fun writeMatrix4x3f(name: String, value: Matrix4x3fc, force: Boolean = false)
    abstract fun writeMatrix4x4f(name: String, value: Matrix4fc, force: Boolean = false)

    abstract fun writeMatrix3x3d(name: String, value: Matrix3dc, force: Boolean = false)
    abstract fun writeMatrix4x3d(name: String, value: Matrix4x3dc, force: Boolean = false)
    abstract fun writeMatrix4x4d(name: String, value: Matrix4dc, force: Boolean = false)

    fun writeFile(name: String, file: FileReference?, workspace: FileReference? = StudioBase.workspace) {
        writeFile(name, file?.file, workspace)
    }

    fun writeFile(name: String, file: File?, workspace: FileReference? = StudioBase.workspace) {
        if (file != null) {
            writeString(name, file.toLocalPath(workspace))
        } else {
            writeString(name, null)
        }
    }

    private
    fun getOrCreatePtr(value: ISaveable): Int {
        var ptr = pointers[value]
        if (ptr != null) return ptr
        ptr = pointers.size + 1
        pointers[value] = ptr
        return ptr
    }

    fun writeObject(self: ISaveable?, name: String?, value: ISaveable?, force: Boolean = false) {

        if (value == null) {

            if (force) {
                writeNull(name)
            }
            return

        }

        val canDiscard = respectsDefaultValues && value.isDefaultValue()
        if (force || !canDiscard) {

            val ptr = getOrCreatePtr(value)

            if (value in listed) {

                writePointer(name, value.getClassName(), ptr)

            } else {

                listed += value
                val canInclude = self == null || self.getApproxSize() > value.getApproxSize()
                if (canInclude) {

                    writeObjectImpl(name, value)

                } else {

                    todo += value
                    writePointer(name, value.getClassName(), ptr)

                }
            }

        }

    }

    abstract fun writeNull(name: String?)
    abstract fun writePointer(name: String?, className: String, ptr: Int)
    abstract fun writeObjectImpl(name: String?, value: ISaveable)

    open fun <V : ISaveable> writeObjectList(
        self: ISaveable?,
        name: String,
        elements: List<V>,
        force: Boolean = false
    ) {
        if (force || elements.isNotEmpty()) {
            writeObjectArray(self, name, Array<ISaveable>(elements.size) { elements[it] } as Array<V>, force)
        }
    }

    /**
     * saves an array of objects of different classes
     * */
    abstract fun <V : ISaveable> writeObjectArray(
        self: ISaveable?,
        name: String,
        elements: Array<V>,
        force: Boolean = false
    )

    /**
     * saves an array of objects of one single class
     * all elements are guaranteed to be of the exact same getClassName()
     * */
    abstract fun <V : ISaveable> writeHomogenousObjectArray(
        self: ISaveable?,
        name: String,
        elements: Array<V>,
        force: Boolean = false
    )

    fun add(obj: ISaveable) {
        if (obj !in listed) {
            getOrCreatePtr(obj)
            listed += obj
            todo += obj
        }
    }

    abstract fun writeListStart()
    abstract fun writeListEnd()
    abstract fun writeListSeparator()

    open fun writeAllInList() {
        writeListStart()
        if (todo.isNotEmpty()) {
            while (true) {
                writeObjectImpl(null, todo.removeAt(0))
                if (todo.isNotEmpty()) writeListSeparator()
                else break
            }
        }
        writeListEnd()
    }

    fun writeSomething(self: ISaveable?, name: String, value: Any?, forceSaving: Boolean) {
        when (value) {
            is Boolean -> writeBoolean(name, value, forceSaving)
            is Char -> writeChar(name, value, forceSaving)
            is Byte -> writeByte(name, value, forceSaving)
            is Short -> writeShort(name, value, forceSaving)
            is Int -> writeInt(name, value, forceSaving)
            is Long -> writeLong(name, value, forceSaving)
            is Float -> writeFloat(name, value, forceSaving)
            is Double -> writeDouble(name, value, forceSaving)
            is String -> writeString(name, value, forceSaving)
            is ISaveable -> writeObject(self, name, value)
            is List<*> -> {
                // try to save the list
                if (value.isNotEmpty()) {
                    when (val sample = value[0]) {

                        else -> {
                            // todo implement saving a list of $sample
                            LOGGER.warn("Not yet implemented: saving a list of $sample")
                        }
                    }
                } // else if is force saving, then this won't work, because of the weak generics in Java :/
            }
            is Array<*> -> {
                if (value.isNotEmpty()) {
                    when (val sample = value[0]) {
                        is Vector2fc -> writeVector2fArray(name, value as Array<Vector2fc>, forceSaving)
                        is Vector3fc -> writeVector3fArray(name, value as Array<Vector3fc>, forceSaving)
                        is Vector4fc -> writeVector4fArray(name, value as Array<Vector4fc>, forceSaving)
                        is Vector2d -> writeVector2dArray(name, value as Array<Vector2dc>, forceSaving)
                        is Vector3d -> writeVector3dArray(name, value as Array<Vector3dc>, forceSaving)
                        is Vector4d -> writeVector4dArray(name, value as Array<Vector4dc>, forceSaving)
                        is String -> writeStringArray(name, value as Array<String>, forceSaving)
                        is FloatArray -> writeFloatArray2D(name, value as Array<FloatArray>, forceSaving)
                        is DoubleArray -> writeDoubleArray2D(name, value as Array<DoubleArray>, forceSaving)
                        is ISaveable -> writeObjectArray(self, name, value as Array<ISaveable>, forceSaving)
                        else -> {
                            throw RuntimeException("Not yet implemented: saving an array of $sample")
                        }
                    }
                } // else if is force saving, then this won't work, because of the weak generics in Java :/
            }
            is BooleanArray -> writeBooleanArray(name, value, forceSaving)
            is CharArray -> writeCharArray(name, value, forceSaving)
            is ByteArray -> writeByteArray(name, value, forceSaving)
            is ShortArray -> writeShortArray(name, value, forceSaving)
            is IntArray -> writeIntArray(name, value, forceSaving)
            is LongArray -> writeLongArray(name, value, forceSaving)
            is FloatArray -> writeFloatArray(name, value, forceSaving)
            is DoubleArray -> writeDoubleArray(name, value, forceSaving)
            // all vectors and such
            is Vector2fc -> writeVector2f(name, value, forceSaving)
            is Vector3fc -> writeVector3f(name, value, forceSaving)
            is Vector4fc -> writeVector4f(name, value, forceSaving)
            is Vector2dc -> writeVector2d(name, value, forceSaving)
            is Vector3dc -> writeVector3d(name, value, forceSaving)
            is Vector4dc -> writeVector4d(name, value, forceSaving)
            is Matrix3fc -> writeMatrix3x3f(name, value, forceSaving)
            is Matrix4x3fc -> writeMatrix4x3f(name, value, forceSaving)
            is Matrix4fc -> writeMatrix4x4f(name, value, forceSaving)
            is Matrix3dc -> writeMatrix3x3d(name, value, forceSaving)
            is Matrix4x3dc -> writeMatrix4x3d(name, value, forceSaving)
            is Matrix4dc -> writeMatrix4x4d(name, value, forceSaving)
            is File -> writeString(name, value.toString(), forceSaving)
            is FileReference -> writeString(name, value.toString(), forceSaving)
            null -> writeObject(self, name, null, forceSaving)
            is Serializable -> {
                // implement it?...
                throw RuntimeException("Could not serialize field $name with value $value of class ${value::class.java}, Serializable")
            }
            else -> {
                throw RuntimeException("todo implement saving an $value of class ${value::class.java}, maybe it needs to be me.anno.io.[I]Saveable?")
            }
        }
    }

}