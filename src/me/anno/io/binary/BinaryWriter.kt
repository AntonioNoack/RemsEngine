package me.anno.io.binary

import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.io.binary.BinaryTypes.*
import me.anno.utils.types.Booleans.toInt
import org.joml.*
import java.io.DataOutputStream

class BinaryWriter(val output: DataOutputStream) : BaseWriter(true) {

    /**
     * max number of strings? idk...
     * typically we need only a few, but what if we need many?
     * */
    private val knownStrings = HashMap<String, Int>()

    private val knownNameTypes = HashMap<String, HashMap<NameType, Int>>()

    private var currentClass = ""
    private var currentNameTypes = knownNameTypes.getOrPut(currentClass) { HashMap() }

    private fun usingType(type: String, run: () -> Unit) {
        val old1 = currentClass
        val old2 = currentNameTypes
        currentClass = type
        currentNameTypes = knownNameTypes.getOrPut(type) { HashMap() }
        run()
        currentClass = old1
        currentNameTypes = old2
    }

    private fun writeEfficientString(string: String?) {
        if (string == null) {
            output.writeInt(-1)
        } else {
            val known = knownStrings.getOrDefault(string, -1)
            if (known >= 0) {
                output.writeInt(known)
            } else {
                val bytes = string.toByteArray()
                output.writeInt(-2 - bytes.size)
                output.write(bytes)
                knownStrings[string] = knownStrings.size
            }
        }
    }

    private fun writeTypeString(value: String) {
        writeEfficientString(value)
    }

    private fun writeAttributeStart(name: String, type: Char) {
        val nameType = NameType(name, type)
        val id = currentNameTypes.getOrDefault(nameType, -1)
        if (id >= 0) {
            // known -> short cut
            output.writeInt(id)
        } else {
            // not previously known -> create a new one
            output.writeInt(-1)
            val newId = currentNameTypes.size
            currentNameTypes[nameType] = newId
            writeTypeString(name)
            output.writeByte(type.code)
        }
    }

    override fun writeBoolean(name: String, value: Boolean, force: Boolean) {
        if (force || value) {
            writeAttributeStart(name, BOOL)
            output.writeByte(value.toInt())
        }
    }

    override fun writeBooleanArray(name: String, value: BooleanArray, force: Boolean) {
        if (force || value.isNotEmpty()) {
            writeAttributeStart(name, BOOL_ARRAY)
            output.writeInt(value.size)
            for (v in value) output.write(if (v) 1 else 0)
        }
    }

    override fun writeByte(name: String, value: Byte, force: Boolean) {
        if (force || value != 0.toByte()) {
            writeAttributeStart(name, BYTE)
            output.writeByte(value.toInt())
        }
    }

    override fun writeByteArray(name: String, value: ByteArray, force: Boolean) {
        if (force || value.isNotEmpty()) {
            writeAttributeStart(name, BYTE_ARRAY)
            output.writeInt(value.size)
            output.write(value)
        }
    }

    override fun writeShort(name: String, value: Short, force: Boolean) {
        if (force || value != 0.toShort()) {
            writeAttributeStart(name, 's')
            output.writeShort(value.toInt())
        }
    }

    override fun writeShortArray(name: String, value: ShortArray, force: Boolean) {
        if (force || value.isNotEmpty()) {
            writeAttributeStart(name, SHORT_ARRAY)
            output.writeInt(value.size)
            for (v in value) output.writeShort(v.toInt())
        }
    }

    override fun writeInt(name: String, value: Int, force: Boolean) {
        if (force || value != 0) {
            writeAttributeStart(name, INT)
            output.writeInt(value)
        }
    }

    override fun writeIntArray(name: String, value: IntArray, force: Boolean) {
        if (force || value.isNotEmpty()) {
            writeAttributeStart(name, INT_ARRAY)
            output.writeInt(value.size)
            for (v in value) output.writeInt(v)
        }
    }

    override fun writeFloat(name: String, value: Float, force: Boolean) {
        if (force || value != 0f) {
            writeAttributeStart(name, FLOAT)
            output.writeFloat(value)
        }
    }

    override fun writeFloatArray(name: String, value: FloatArray, force: Boolean) {
        if (force || value.isNotEmpty()) {
            writeAttributeStart(name, FLOAT_ARRAY)
            output.writeInt(value.size)
            for (v in value) output.writeFloat(v)
        }
    }

    override fun writeFloatArray2D(name: String, value: Array<FloatArray>, force: Boolean) {
        if (force || value.isNotEmpty()) {
            writeAttributeStart(name, FLOAT_ARRAY_2D)
            output.writeInt(value.size)
            for (vs in value) {
                output.writeInt(vs.size)
                for (v in vs) output.writeFloat(v)
            }
        }
    }

    override fun writeDouble(name: String, value: Double, force: Boolean) {
        if (force || value != 0.0) {
            writeAttributeStart(name, DOUBLE)
            output.writeDouble(value)
        }
    }

    override fun writeDoubleArray(name: String, value: DoubleArray, force: Boolean) {
        if (force || value.isNotEmpty()) {
            writeAttributeStart(name, DOUBLE_ARRAY)
            output.writeInt(value.size)
            for (v in value) output.writeDouble(v)
        }
    }

    override fun writeDoubleArray2D(name: String, value: Array<DoubleArray>, force: Boolean) {
        if (force || value.isNotEmpty()) {
            writeAttributeStart(name, DOUBLE_ARRAY_2D)
            output.writeInt(value.size)
            for (vs in value) {
                output.writeInt(vs.size)
                for (v in vs) output.writeDouble(v)
            }
        }
    }

    override fun writeString(name: String, value: String?, force: Boolean) {
        if (force || value != null) {
            writeAttributeStart(name, STRING)
            writeEfficientString(value)
        }
    }

    override fun writeStringArray(name: String, value: Array<String>, force: Boolean) {
        if (force || value.isNotEmpty()) {
            writeAttributeStart(name, STRING_ARRAY)
            output.writeInt(value.size)
            for (v in value) writeEfficientString(v)
        }
    }

    override fun writeLong(name: String, value: Long, force: Boolean) {
        if (force || value != 0L) {
            writeAttributeStart(name, LONG)
            output.writeLong(value)
        }
    }

    override fun writeLongArray(name: String, value: LongArray, force: Boolean) {
        if (force || value.isNotEmpty()) {
            writeAttributeStart(name, LONG_ARRAY)
            output.writeInt(value.size)
            for (v in value) output.writeLong(v)
        }
    }

    override fun writeVector2f(name: String, value: Vector2fc, force: Boolean) {
        if (force || (value.x() != 0f && value.y() != 0f)) {
            writeAttributeStart(name, VECTOR2F)
            output.writeFloat(value.x())
            output.writeFloat(value.y())
        }
    }

    override fun writeVector3f(name: String, value: Vector3fc, force: Boolean) {
        if (force || (value.x() != 0f || value.y() != 0f || value.z() != 0f)) {
            writeAttributeStart(name, VECTOR3F)
            output.writeFloat(value.x())
            output.writeFloat(value.y())
            output.writeFloat(value.z())
        }
    }

    override fun writeVector4f(name: String, value: Vector4fc, force: Boolean) {
        if (force || (value.x() != 0f || value.y() != 0f || value.z() != 0f || value.w() != 0f)) {
            writeAttributeStart(name, VECTOR4F)
            output.writeFloat(value.x())
            output.writeFloat(value.y())
            output.writeFloat(value.z())
            output.writeFloat(value.w())
        }
    }

    override fun writeVector4d(name: String, value: Vector4dc, force: Boolean) {
        if (force || (value.x() != 0.0 || value.y() != 0.0 || value.z() != 0.0 || value.w() != 0.0)) {
            writeAttributeStart(name, VECTOR4D)
            output.writeDouble(value.x())
            output.writeDouble(value.y())
            output.writeDouble(value.z())
            output.writeDouble(value.w())
        }
    }

    override fun writeNull(name: String?) {
        if (name != null) writeAttributeStart(name, OBJECT_NULL)
        else output.write(OBJECT_NULL.code)
    }

    override fun writePointer(name: String?, className: String, ptr: Int) {
        if (name != null) writeAttributeStart(name, OBJECT_PTR)
        else output.write(OBJECT_PTR.code)
        output.writeInt(ptr)
    }

    private fun writeObjectEnd() {
        output.writeInt(-2)
    }

    override fun writeObjectImpl(name: String?, value: ISaveable) {
        if (name != null) writeAttributeStart(name, OBJECT_IMPL)
        else output.write(OBJECT_IMPL.code)
        usingType(value.getClassName()) {
            writeTypeString(currentClass)
            output.writeInt(getPointer(value)!!)
            value.save(this)
            writeObjectEnd()
        }
    }

    private fun <V> writeGenericArray(
        name: String,
        elements: Array<V>,
        force: Boolean,
        type: Char,
        writeInstance: (V) -> Unit
    ) {
        if (force || elements.isNotEmpty()) {
            writeAttributeStart(name, type)
            output.writeInt(elements.size)
            elements.forEach { element ->
                writeInstance(element)
            }
        }
    }

    private fun <V> writeGenericList(
        name: String,
        elements: List<V>,
        force: Boolean,
        writeInstance: (V) -> Unit
    ) {
        if (force || elements.isNotEmpty()) {
            writeAttributeStart(name, OBJECT_ARRAY)
            output.writeInt(elements.size)
            elements.forEach { element ->
                writeInstance(element)
            }
        }
    }

    override fun <V : ISaveable> writeObjectArray(self: ISaveable?, name: String, elements: Array<V>, force: Boolean) {
        if (force || elements.isNotEmpty()) {
            if (elements.isNotEmpty()) {
                val firstType = elements.first().getClassName()
                val allSameType = elements.all { it.getClassName() == firstType }
                if (allSameType) {
                    writeHomogenousObjectArray(self, name, elements, force)
                } else {
                    writeGenericArray(name, elements, force, OBJECT_ARRAY) {
                        writeObject(null, null, it, true)
                    }
                }
            } else {
                writeAttributeStart(name, OBJECT_ARRAY)
                output.writeInt(0)
            }
        }

    }

    override fun <V : ISaveable> writeObjectList(self: ISaveable?, name: String, elements: List<V>, force: Boolean) {
        writeGenericList(name, elements, force) {
            writeObject(null, null, it, true)
        }
    }

    override fun <V : ISaveable> writeHomogenousObjectArray(
        self: ISaveable?,
        name: String,
        elements: Array<V>,
        force: Boolean
    ) {
        if (force || elements.isNotEmpty()) {
            writeAttributeStart(name, OBJECTS_HOMOGENOUS_ARRAY)
            writeTypeString(elements.firstOrNull()?.getClassName() ?: "")
            output.writeInt(elements.size)
            for (element in elements) {
                element.save(this)
                writeObjectEnd()
            }
        }
    }

    override fun writeVector2fArray(name: String, elements: Array<Vector2f>, force: Boolean) {
        writeGenericArray(name, elements, force, VECTOR2F_ARRAY) {
            output.writeFloat(it.x)
            output.writeFloat(it.y)
        }
    }

    override fun writeVector3fArray(name: String, elements: Array<Vector3f>, force: Boolean) {
        writeGenericArray(name, elements, force, VECTOR3F_ARRAY) {
            output.writeFloat(it.x)
            output.writeFloat(it.y)
            output.writeFloat(it.z)
        }
    }

    override fun writeVector4fArray(name: String, elements: Array<Vector4f>, force: Boolean) {
        writeGenericArray(name, elements, force, VECTOR4F_ARRAY) {
            output.writeFloat(it.x)
            output.writeFloat(it.y)
            output.writeFloat(it.z)
            output.writeFloat(it.w)
        }
    }

    override fun writeListStart() {
        writeAttributeStart("", OBJECT_LIST_UNKNOWN_LENGTH)
    }

    override fun writeListEnd() {
        output.write(37)
    }

    override fun writeListSeparator() {
        output.write(17)
    }
}