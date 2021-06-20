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

    override fun writeBooleanArray(name: String, values: BooleanArray, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, BOOL_ARRAY)
            output.writeInt(values.size)
            for (v in values) output.write(if (v) 1 else 0)
        }
    }

    override fun writeBooleanArray2D(name: String, values: Array<BooleanArray>, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, BOOL_ARRAY_2D)
            output.writeInt(values.size)
            for (vs in values) {
                output.writeInt(vs.size)
                for (v in vs) output.write(if (v) 1 else 0)
            }
        }
    }

    override fun writeChar(name: String, value: Char, force: Boolean) {
        if (force || value != 0.toChar()) {
            writeAttributeStart(name, CHAR)
            output.writeChar(value.code)
        }
    }

    override fun writeCharArray(name: String, values: CharArray, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, CHAR_ARRAY)
            output.writeInt(values.size)
            for (c in values) output.writeChar(c.code)
        }
    }

    override fun writeCharArray2D(name: String, values: Array<CharArray>, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, CHAR_ARRAY_2D)
            output.writeInt(values.size)
            for (vs in values) {
                output.writeInt(vs.size)
                for (v in vs) output.writeChar(v.code)
            }
        }
    }


    override fun writeByte(name: String, value: Byte, force: Boolean) {
        if (force || value != 0.toByte()) {
            writeAttributeStart(name, BYTE)
            output.writeByte(value.toInt())
        }
    }

    override fun writeByteArray(name: String, values: ByteArray, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, BYTE_ARRAY)
            output.writeInt(values.size)
            output.write(values)
        }
    }

    override fun writeByteArray2D(name: String, values: Array<ByteArray>, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, BYTE_ARRAY_2D)
            output.writeInt(values.size)
            for (vs in values) {
                output.writeInt(vs.size)
                output.write(vs)
            }
        }
    }

    override fun writeShort(name: String, value: Short, force: Boolean) {
        if (force || value != 0.toShort()) {
            writeAttributeStart(name, 's')
            output.writeShort(value.toInt())
        }
    }

    override fun writeShortArray(name: String, values: ShortArray, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, SHORT_ARRAY)
            output.writeInt(values.size)
            for (v in values) output.writeShort(v.toInt())
        }
    }

    override fun writeShortArray2D(name: String, values: Array<ShortArray>, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, SHORT_ARRAY_2D)
            output.writeInt(values.size)
            for (vs in values) {
                output.writeInt(vs.size)
                for (v in vs) output.writeShort(v.toInt())
            }
        }
    }

    override fun writeInt(name: String, value: Int, force: Boolean) {
        if (force || value != 0) {
            writeAttributeStart(name, INT)
            output.writeInt(value)
        }
    }

    override fun writeIntArray(name: String, values: IntArray, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, INT_ARRAY)
            output.writeInt(values.size)
            for (v in values) output.writeInt(v)
        }
    }

    override fun writeIntArray2D(name: String, values: Array<IntArray>, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, INT_ARRAY_2D)
            output.writeInt(values.size)
            for (vs in values) {
                output.writeInt(vs.size)
                for (v in vs) output.writeInt(v)
            }
        }
    }

    override fun writeLong(name: String, value: Long, force: Boolean) {
        if (force || value != 0L) {
            writeAttributeStart(name, LONG)
            output.writeLong(value)
        }
    }

    override fun writeLongArray(name: String, values: LongArray, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, LONG_ARRAY)
            output.writeInt(values.size)
            for (v in values) output.writeLong(v)
        }
    }

    override fun writeLongArray2D(name: String, values: Array<LongArray>, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, LONG_ARRAY_2D)
            output.writeInt(values.size)
            for (vs in values) {
                output.writeInt(vs.size)
                for (v in vs) output.writeLong(v)
            }
        }
    }

    override fun writeFloat(name: String, value: Float, force: Boolean) {
        if (force || value != 0f) {
            writeAttributeStart(name, FLOAT)
            output.writeFloat(value)
        }
    }

    override fun writeFloatArray(name: String, values: FloatArray, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, FLOAT_ARRAY)
            output.writeInt(values.size)
            for (v in values) output.writeFloat(v)
        }
    }

    override fun writeFloatArray2D(name: String, values: Array<FloatArray>, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, FLOAT_ARRAY_2D)
            output.writeInt(values.size)
            for (vs in values) {
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

    override fun writeDoubleArray(name: String, values: DoubleArray, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, DOUBLE_ARRAY)
            output.writeInt(values.size)
            for (v in values) output.writeDouble(v)
        }
    }

    override fun writeDoubleArray2D(name: String, values: Array<DoubleArray>, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, DOUBLE_ARRAY_2D)
            output.writeInt(values.size)
            for (vs in values) {
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

    override fun writeStringArray(name: String, values: Array<String>, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, STRING_ARRAY)
            output.writeInt(values.size)
            for (v in values) writeEfficientString(v)
        }
    }

    override fun writeStringArray2D(name: String, values: Array<Array<String>>, force: Boolean) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, STRING_ARRAY_2D)
            output.writeInt(values.size)
            for (vs in values) {
                output.writeInt(vs.size)
                for (v in vs) writeEfficientString(v)
            }
        }
    }

    override fun writeVector2f(name: String, value: Vector2fc, force: Boolean) {
        if (force || (value.x() != 0f && value.y() != 0f)) {
            writeAttributeStart(name, VECTOR2F)
            output.writeFloat(value.x())
            output.writeFloat(value.y())
        }
    }

    override fun writeVector2fArray(name: String, values: Array<Vector2fc>, force: Boolean) {
        writeGenericArray(name, values, force, VECTOR2F_ARRAY) { v ->
            output.writeFloat(v.x())
            output.writeFloat(v.y())
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

    override fun writeVector3fArray(name: String, values: Array<Vector3fc>, force: Boolean) {
        writeGenericArray(name, values, force, VECTOR3F_ARRAY) { v ->
            output.writeFloat(v.x())
            output.writeFloat(v.y())
            output.writeFloat(v.z())
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

    override fun writeVector4fArray(name: String, values: Array<Vector4fc>, force: Boolean) {
        writeGenericArray(name, values, force, VECTOR4F_ARRAY) { v ->
            output.writeFloat(v.x())
            output.writeFloat(v.y())
            output.writeFloat(v.z())
            output.writeFloat(v.w())
        }
    }

    override fun writeVector2d(name: String, value: Vector2dc, force: Boolean) {
        if (force || (value.x() != 0.0 || value.y() != 0.0)) {
            writeAttributeStart(name, VECTOR2D)
            output.writeDouble(value.x())
            output.writeDouble(value.y())
        }
    }

    override fun writeVector2dArray(name: String, values: Array<Vector2dc>, force: Boolean) {
        writeGenericArray(name, values, force, VECTOR2D_ARRAY) { v ->
            output.writeDouble(v.x())
            output.writeDouble(v.y())
        }
    }

    override fun writeVector3d(name: String, value: Vector3dc, force: Boolean) {
        if (force || (value.x() != 0.0 || value.y() != 0.0 || value.z() != 0.0)) {
            writeAttributeStart(name, VECTOR3D)
            output.writeDouble(value.x())
            output.writeDouble(value.y())
            output.writeDouble(value.z())
        }
    }

    override fun writeVector3dArray(name: String, values: Array<Vector3dc>, force: Boolean) {
        writeGenericArray(name, values, force, VECTOR3D_ARRAY) { v ->
            output.writeDouble(v.x())
            output.writeDouble(v.y())
            output.writeDouble(v.z())
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

    override fun writeVector4dArray(name: String, values: Array<Vector4dc>, force: Boolean) {
        writeGenericArray(name, values, force, VECTOR4D_ARRAY) { v ->
            output.writeDouble(v.x())
            output.writeDouble(v.y())
            output.writeDouble(v.z())
            output.writeDouble(v.w())
        }
    }

    override fun writeMatrix3x3f(name: String, value: Matrix3fc, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeMatrix4x3f(name: String, value: Matrix4x3fc, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeMatrix4x4f(name: String, value: Matrix4fc, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeMatrix3x3d(name: String, value: Matrix3dc, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeMatrix4x3d(name: String, value: Matrix4x3dc, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeMatrix4x4d(name: String, value: Matrix4dc, force: Boolean) {
        TODO("Not yet implemented")
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

    private inline fun <V> writeGenericArray(
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

    private inline fun <V> writeGenericList(
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

    override fun <V : ISaveable> writeObjectArray(self: ISaveable?, name: String, values: Array<V>, force: Boolean) {
        if (force || values.isNotEmpty()) {
            if (values.isNotEmpty()) {
                val firstType = values.first().getClassName()
                val allSameType = values.all { it.getClassName() == firstType }
                if (allSameType) {
                    writeHomogenousObjectArray(self, name, values, force)
                } else {
                    writeGenericArray(name, values, force, OBJECT_ARRAY) {
                        writeObject(null, null, it, true)
                    }
                }
            } else {
                writeAttributeStart(name, OBJECT_ARRAY)
                output.writeInt(0)
            }
        }
    }

    override fun <V : ISaveable> writeObjectArray2D(
        self: ISaveable?,
        name: String,
        values: Array<Array<V>>,
        force: Boolean
    ) {
        TODO("Not yet implemented")
    }

    override fun <V : ISaveable> writeObjectList(self: ISaveable?, name: String, values: List<V>, force: Boolean) {
        writeGenericList(name, values, force) {
            writeObject(null, null, it, true)
        }
    }

    override fun <V : ISaveable> writeHomogenousObjectArray(
        self: ISaveable?,
        name: String,
        values: Array<V>,
        force: Boolean
    ) {
        if (force || values.isNotEmpty()) {
            writeAttributeStart(name, OBJECTS_HOMOGENOUS_ARRAY)
            writeTypeString(values.firstOrNull()?.getClassName() ?: "")
            output.writeInt(values.size)
            for (element in values) {
                element.save(this)
                writeObjectEnd()
            }
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