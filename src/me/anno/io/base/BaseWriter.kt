package me.anno.io.base

import me.anno.io.ISaveable
import me.anno.studio.StudioBase
import me.anno.utils.files.LocalFile.toLocalPath
import org.joml.*
import java.io.File
import java.util.*

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
    abstract fun writeVector3f(name: String, value: Vector3fc, force: Boolean = false)
    abstract fun writeVector4f(name: String, value: Vector4fc, force: Boolean = false)
    abstract fun writeVector4d(name: String, value: Vector4dc, force: Boolean = false)

    fun writeFile(name: String, file: File?, workspace: File? = StudioBase.workspace) {
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

    abstract fun writeVector2fArray(name: String, elements: Array<Vector2f>, force: Boolean = false)
    abstract fun writeVector3fArray(name: String, elements: Array<Vector3f>, force: Boolean = false)
    abstract fun writeVector4fArray(name: String, elements: Array<Vector4f>, force: Boolean = false)

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

    fun writeSomething(self: ISaveable, name: String, value: Any?, force: Boolean) {
        when (value) {
            is ISaveable -> writeObject(self, name, value, force)
            is Boolean -> writeBoolean(name, value, force)
            is Byte -> writeByte(name, value, force)
            is Short -> writeShort(name, value, force)
            is Int -> writeInt(name, value, force)
            is Long -> writeLong(name, value, force)
            is Float -> writeFloat(name, value, force)
            is Double -> writeDouble(name, value, force)
            is String -> writeString(name, value, force)
            is File -> writeString(name, value.toString(), force)
            is Vector2f -> writeVector2f(name, value, force)
            is Vector3f -> writeVector3f(name, value, force)
            null -> writeObject(self, name, value, force)
            else -> throw RuntimeException("todo implement saving an $value, maybe it needs to be me.anno.io.[I]Saveable?")
        }
    }

}