package me.anno.io.base

import me.anno.io.ISaveable
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4d
import org.joml.Vector4f
import java.io.File
import java.util.*

abstract class BaseWriter(val respectsDefaultValues: Boolean) {

    private val listed = HashSet<ISaveable>()
    private val todo = ArrayList<ISaveable>(256)
    private val pointers = HashMap<ISaveable, Int>()

    /**
     * gets the pointer of a know value
     * */
    fun getPointer(value: ISaveable) = pointers[value]

    // the type is important to notice incompatibilities by changes
    abstract fun writeBool(name: String, value: Boolean, force: Boolean = true)
    abstract fun writeByte(name: String, value: Byte, force: Boolean = false)
    abstract fun writeShort(name: String, value: Short, force: Boolean = false)
    abstract fun writeInt(name: String, value: Int, force: Boolean = false)
    abstract fun writeIntArray(name: String, value: IntArray, force: Boolean = false)
    abstract fun writeFloat(name: String, value: Float, force: Boolean = false)
    abstract fun writeFloatArray(name: String, value: FloatArray, force: Boolean = false)
    abstract fun writeDouble(name: String, value: Double, force: Boolean = false)
    abstract fun writeString(name: String, value: String?, force: Boolean = false)
    abstract fun writeLong(name: String, value: Long, force: Boolean = false)
    abstract fun writeLongArray(name: String, value: LongArray, force: Boolean = false)
    abstract fun writeVector2f(name: String, value: Vector2f, force: Boolean = false)
    abstract fun writeVector3f(name: String, value: Vector3f, force: Boolean = false)
    abstract fun writeVector4f(name: String, value: Vector4f, force: Boolean = false)
    abstract fun writeVector4d(name: String, value: Vector4d, force: Boolean = false)

    fun writeFile(name: String, file: File?) = writeString(name, file?.toString() ?: "")

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

    abstract fun <V : ISaveable> writeList(self: ISaveable?, name: String, elements: List<V>?, force: Boolean = false)
    abstract fun writeListV2(name: String, elements: List<Vector2f>?, force: Boolean = false)
    abstract fun writeListV3(name: String, elements: List<Vector3f>?, force: Boolean = false)
    abstract fun writeListV4(name: String, elements: List<Vector4f>?, force: Boolean = false)

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

    fun writeAllInList() {
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
            is Boolean -> writeBool(name, value, force)
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