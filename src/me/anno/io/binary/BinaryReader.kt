package me.anno.io.binary

import me.anno.io.ISaveable
import me.anno.io.base.BaseReader
import me.anno.io.binary.BinaryTypes.*
import me.anno.utils.readNBytes2
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import java.io.DataInputStream

/**
 * writing as text is:
 * - easier debuggable
 * - similar speed
 * - similar length when compressed
 * */
class BinaryReader(val input: DataInputStream) : BaseReader() {

    private val knownNames = ArrayList<String>()

    private val knownNameTypes = HashMap<String, ArrayList<NameType>>()

    private var currentClass = ""
    private var currentNameTypes = knownNameTypes.getOrPut(currentClass) { ArrayList() }

    private fun usingType(type: String, run: () -> Unit) {
        val old1 = currentClass
        val old2 = currentNameTypes
        currentClass = type
        currentNameTypes = knownNameTypes.getOrPut(type) { ArrayList() }
        run()
        currentClass = old1
        currentNameTypes = old2
    }

    private fun readEfficientString(): String? {
        val id = input.readInt()
        return when {
            id == -1 -> null
            id >= +0 -> knownNames[id]
            else -> {
                val length = -id - 2
                val bytes = input.readNBytes2(length)
                val value = String(bytes)
                knownNames += value
                value
            }
        }
    }

    private fun readTypeString(): String {
        return readEfficientString()!!
    }

    private fun readTypeName(id: Int): NameType {
        return if (id >= 0) {
            currentNameTypes[id]
        } else {
            val name = readTypeString()
            val type = input.read().toChar()
            val value = NameType(name, type)
            currentNameTypes.add(value)
            value
        }
    }

    private fun readTypeName(): NameType {
        return readTypeName(input.readInt())
    }

    override fun readObject(): ISaveable {
        val clazz = readTypeString()
        val obj = getNewClassInstance(clazz)
        usingType(clazz) {
            val ptr = input.readInt()
            // real all properties
            while (true) {
                val typeId = input.readInt()
                if (typeId < -1) break
                val typeName = readTypeName(typeId)
                val name = typeName.name
                when (typeName.type) {
                    BOOL -> obj.readBoolean(name, input.readByte() != 0.toByte())
                    BOOL_ARRAY -> obj.readBooleanArray(name, BooleanArray(input.readInt()) { input.readBoolean() })
                    BYTE -> obj.readByte(name, input.readByte())
                    BYTE_ARRAY -> obj.readByteArray(name, ByteArray(input.readInt()) { input.readByte() })
                    SHORT -> obj.readShort(name, input.readShort())
                    SHORT_ARRAY -> obj.readShortArray(name, ShortArray(input.readInt()) { input.readShort() })
                    INT -> obj.readInt(name, input.readInt())
                    INT_ARRAY -> obj.readIntArray(name, IntArray(input.readInt()) { input.readInt() })
                    LONG -> obj.readLong(name, input.readLong())
                    LONG_ARRAY -> obj.readLongArray(name, LongArray(input.readInt()) { input.readLong() })
                    FLOAT -> obj.readFloat(name, input.readFloat())
                    FLOAT_ARRAY -> obj.readFloatArray(name, FloatArray(input.readInt()) { input.readFloat() })
                    DOUBLE -> obj.readDouble(name, input.readDouble())
                    DOUBLE_ARRAY -> obj.readDoubleArray(name, DoubleArray(input.readInt()) { input.readDouble() })
                    STRING -> obj.readString(name, readEfficientString()!!)
                    STRING_ARRAY -> obj.readStringArray(name, Array(input.readInt()) { readEfficientString()!! })
                    OBJECT_IMPL -> obj.readObject(name, readObject())
                    OBJECT_PTR -> {
                        val ptr2 = input.readInt()
                        val child = content[ptr2]
                        if (child == null) {
                            addMissingReference(obj, name, ptr2)
                        } else {
                            obj.readObject(name, child)
                        }
                    }
                    OBJECT_NULL -> obj.readObject(name, null)
                    OBJECT_ARRAY -> {
                        obj.readObjectArray(name, Array(input.readInt()) {
                            when (val subType = input.read().toChar()) {
                                OBJECT_IMPL -> readObject()
                                OBJECT_PTR -> content[input.readInt()]!!
                                else -> throw RuntimeException("Unknown sub-type $subType")
                            }
                        })
                    }
                    VECTOR2F -> obj.readVector2f(name, readVector2f())
                    VECTOR2F_ARRAY -> obj.readVector2fArray(name, Array(input.readInt()){ readVector2f() })
                    VECTOR3F -> obj.readVector3f(name, readVector3f())
                    VECTOR3F_ARRAY -> obj.readVector3fArray(name, Array(input.readInt()){ readVector3f()})
                    VECTOR4F -> obj.readVector4f(name, readVector4f())
                    VECTOR4F_ARRAY -> obj.readVector4fArray(name, Array(input.readInt()){ readVector4f() })
                    else -> throw RuntimeException("Unknown type ${typeName.type}")
                }
            }
            register(obj, ptr)
        }
        return obj
    }

    private fun readVector2f() = Vector2f(input.readFloat(), input.readFloat())
    private fun readVector3f() = Vector3f(input.readFloat(), input.readFloat(), input.readFloat())
    private fun readVector4f() = Vector4f(input.readFloat(), input.readFloat(), input.readFloat(), input.readFloat())

    override fun readAllInList() {
        val nameType = readTypeName()
        assert(nameType.name == "" && nameType.type == OBJECT_LIST_UNKNOWN_LENGTH)
        loop@ while (true) {
            val type = input.read().toChar()
            if (type != OBJECT_IMPL) throw RuntimeException("Type must be OBJECT_IMPL, but got $type != $OBJECT_IMPL")
            readObject()
            when (val code = input.read()) {
                17 -> Unit
                37 -> break@loop
                else -> {
                    throw RuntimeException("Invalid Code $code")
                }
            }
        }
    }

}