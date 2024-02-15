package me.anno.io.binary

import me.anno.io.Saveable
import me.anno.io.base.BaseReader
import me.anno.io.binary.BinaryTypes.AABB32
import me.anno.io.binary.BinaryTypes.AABB32_ARRAY
import me.anno.io.binary.BinaryTypes.AABB32_ARRAY_2D
import me.anno.io.binary.BinaryTypes.AABB64
import me.anno.io.binary.BinaryTypes.AABB64_ARRAY
import me.anno.io.binary.BinaryTypes.AABB64_ARRAY_2D
import me.anno.io.binary.BinaryTypes.BOOL
import me.anno.io.binary.BinaryTypes.BOOL_ARRAY
import me.anno.io.binary.BinaryTypes.BOOL_ARRAY_2D
import me.anno.io.binary.BinaryTypes.BYTE
import me.anno.io.binary.BinaryTypes.BYTE_ARRAY
import me.anno.io.binary.BinaryTypes.BYTE_ARRAY_2D
import me.anno.io.binary.BinaryTypes.CHAR
import me.anno.io.binary.BinaryTypes.CHAR_ARRAY
import me.anno.io.binary.BinaryTypes.CHAR_ARRAY_2D
import me.anno.io.binary.BinaryTypes.DOUBLE
import me.anno.io.binary.BinaryTypes.DOUBLE_ARRAY
import me.anno.io.binary.BinaryTypes.DOUBLE_ARRAY_2D
import me.anno.io.binary.BinaryTypes.FILE
import me.anno.io.binary.BinaryTypes.FILE_ARRAY
import me.anno.io.binary.BinaryTypes.FILE_ARRAY_2D
import me.anno.io.binary.BinaryTypes.FLOAT
import me.anno.io.binary.BinaryTypes.FLOAT_ARRAY
import me.anno.io.binary.BinaryTypes.FLOAT_ARRAY_2D
import me.anno.io.binary.BinaryTypes.INT
import me.anno.io.binary.BinaryTypes.INT_ARRAY
import me.anno.io.binary.BinaryTypes.INT_ARRAY_2D
import me.anno.io.binary.BinaryTypes.LONG
import me.anno.io.binary.BinaryTypes.LONG_ARRAY
import me.anno.io.binary.BinaryTypes.LONG_ARRAY_2D
import me.anno.io.binary.BinaryTypes.MATRIX2X2D
import me.anno.io.binary.BinaryTypes.MATRIX2X2D_ARRAY
import me.anno.io.binary.BinaryTypes.MATRIX2X2D_ARRAY_2D
import me.anno.io.binary.BinaryTypes.MATRIX2X2F
import me.anno.io.binary.BinaryTypes.MATRIX2X2F_ARRAY
import me.anno.io.binary.BinaryTypes.MATRIX2X2F_ARRAY_2D
import me.anno.io.binary.BinaryTypes.MATRIX3X2D
import me.anno.io.binary.BinaryTypes.MATRIX3X2D_ARRAY
import me.anno.io.binary.BinaryTypes.MATRIX3X2D_ARRAY_2D
import me.anno.io.binary.BinaryTypes.MATRIX3X2F
import me.anno.io.binary.BinaryTypes.MATRIX3X2F_ARRAY
import me.anno.io.binary.BinaryTypes.MATRIX3X2F_ARRAY_2D
import me.anno.io.binary.BinaryTypes.MATRIX3X3D
import me.anno.io.binary.BinaryTypes.MATRIX3X3D_ARRAY
import me.anno.io.binary.BinaryTypes.MATRIX3X3D_ARRAY_2D
import me.anno.io.binary.BinaryTypes.MATRIX3X3F
import me.anno.io.binary.BinaryTypes.MATRIX3X3F_ARRAY
import me.anno.io.binary.BinaryTypes.MATRIX3X3F_ARRAY_2D
import me.anno.io.binary.BinaryTypes.MATRIX4X3D
import me.anno.io.binary.BinaryTypes.MATRIX4X3D_ARRAY
import me.anno.io.binary.BinaryTypes.MATRIX4X3D_ARRAY_2D
import me.anno.io.binary.BinaryTypes.MATRIX4X3F
import me.anno.io.binary.BinaryTypes.MATRIX4X3F_ARRAY
import me.anno.io.binary.BinaryTypes.MATRIX4X3F_ARRAY_2D
import me.anno.io.binary.BinaryTypes.MATRIX4X4D
import me.anno.io.binary.BinaryTypes.MATRIX4X4D_ARRAY
import me.anno.io.binary.BinaryTypes.MATRIX4X4D_ARRAY_2D
import me.anno.io.binary.BinaryTypes.MATRIX4X4F
import me.anno.io.binary.BinaryTypes.MATRIX4X4F_ARRAY
import me.anno.io.binary.BinaryTypes.MATRIX4X4F_ARRAY_2D
import me.anno.io.binary.BinaryTypes.OBJECTS_HOMOGENOUS_ARRAY
import me.anno.io.binary.BinaryTypes.OBJECT_ARRAY
import me.anno.io.binary.BinaryTypes.OBJECT_ARRAY_2D
import me.anno.io.binary.BinaryTypes.OBJECT_IMPL
import me.anno.io.binary.BinaryTypes.OBJECT_LIST_UNKNOWN_LENGTH
import me.anno.io.binary.BinaryTypes.OBJECT_NULL
import me.anno.io.binary.BinaryTypes.OBJECT_PTR
import me.anno.io.binary.BinaryTypes.PLANE32
import me.anno.io.binary.BinaryTypes.PLANE32_ARRAY
import me.anno.io.binary.BinaryTypes.PLANE32_ARRAY_2D
import me.anno.io.binary.BinaryTypes.PLANE64
import me.anno.io.binary.BinaryTypes.PLANE64_ARRAY
import me.anno.io.binary.BinaryTypes.PLANE64_ARRAY_2D
import me.anno.io.binary.BinaryTypes.QUATERNION32
import me.anno.io.binary.BinaryTypes.QUATERNION32_ARRAY
import me.anno.io.binary.BinaryTypes.QUATERNION32_ARRAY_2D
import me.anno.io.binary.BinaryTypes.QUATERNION64
import me.anno.io.binary.BinaryTypes.QUATERNION64_ARRAY
import me.anno.io.binary.BinaryTypes.QUATERNION64_ARRAY_2D
import me.anno.io.binary.BinaryTypes.SHORT
import me.anno.io.binary.BinaryTypes.SHORT_ARRAY
import me.anno.io.binary.BinaryTypes.SHORT_ARRAY_2D
import me.anno.io.binary.BinaryTypes.STRING
import me.anno.io.binary.BinaryTypes.STRING_ARRAY
import me.anno.io.binary.BinaryTypes.STRING_ARRAY_2D
import me.anno.io.binary.BinaryTypes.VECTOR2D
import me.anno.io.binary.BinaryTypes.VECTOR2D_ARRAY
import me.anno.io.binary.BinaryTypes.VECTOR2D_ARRAY_2D
import me.anno.io.binary.BinaryTypes.VECTOR2F
import me.anno.io.binary.BinaryTypes.VECTOR2F_ARRAY
import me.anno.io.binary.BinaryTypes.VECTOR2F_ARRAY_2D
import me.anno.io.binary.BinaryTypes.VECTOR2I
import me.anno.io.binary.BinaryTypes.VECTOR2I_ARRAY
import me.anno.io.binary.BinaryTypes.VECTOR2I_ARRAY_2D
import me.anno.io.binary.BinaryTypes.VECTOR3D
import me.anno.io.binary.BinaryTypes.VECTOR3D_ARRAY
import me.anno.io.binary.BinaryTypes.VECTOR3D_ARRAY_2D
import me.anno.io.binary.BinaryTypes.VECTOR3F
import me.anno.io.binary.BinaryTypes.VECTOR3F_ARRAY
import me.anno.io.binary.BinaryTypes.VECTOR3F_ARRAY_2D
import me.anno.io.binary.BinaryTypes.VECTOR3I
import me.anno.io.binary.BinaryTypes.VECTOR3I_ARRAY
import me.anno.io.binary.BinaryTypes.VECTOR3I_ARRAY_2D
import me.anno.io.binary.BinaryTypes.VECTOR4D
import me.anno.io.binary.BinaryTypes.VECTOR4D_ARRAY
import me.anno.io.binary.BinaryTypes.VECTOR4D_ARRAY_2D
import me.anno.io.binary.BinaryTypes.VECTOR4F
import me.anno.io.binary.BinaryTypes.VECTOR4F_ARRAY
import me.anno.io.binary.BinaryTypes.VECTOR4F_ARRAY_2D
import me.anno.io.binary.BinaryTypes.VECTOR4I
import me.anno.io.binary.BinaryTypes.VECTOR4I_ARRAY
import me.anno.io.binary.BinaryTypes.VECTOR4I_ARRAY_2D
import me.anno.io.files.InvalidRef
import me.anno.utils.files.LocalFile.toGlobalFile
import me.anno.utils.types.InputStreams.readNBytes2
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
import java.io.DataInputStream
import java.io.IOException

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
                val bytes = input.readNBytes2(length, true)
                val value = bytes.decodeToString()
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

    override fun readObject(): Saveable {
        val clazz = readTypeString()
        return readObject(clazz)
    }

    private fun readBooleanArray() = BooleanArray(input.readInt()) { input.readBoolean() }
    private fun readCharArray() = CharArray(input.readInt()) { input.readChar() }
    private fun readByteArray() = ByteArray(input.readInt()) { input.readByte() }
    private fun readShortArray() = ShortArray(input.readInt()) { input.readShort() }
    private fun readIntArray() = IntArray(input.readInt()) { input.readInt() }
    private fun readLongArray() = LongArray(input.readInt()) { input.readLong() }
    private fun readFloatArray() = FloatArray(input.readInt()) { input.readFloat() }
    private fun readDoubleArray() = DoubleArray(input.readInt()) { input.readDouble() }
    private fun readStringArray() = Array(input.readInt()) { readEfficientString()!! }

    private fun readObjectOrNull(): Saveable? {
        return when (val subType = input.read()) {
            OBJECT_IMPL -> readObject()
            OBJECT_PTR -> getByPointer(input.readInt(), true)
            OBJECT_NULL -> null
            else -> throw IOException("Unknown sub-type $subType")
        }
    }

    private fun readHomogeneousObjectArray(type: String): Array<Saveable?> {
        return readArray {
            when (val subType = input.read()) {
                OBJECT_IMPL -> readObject(type)
                OBJECT_PTR -> getByPointer(input.readInt(), true)
                OBJECT_NULL -> null
                else -> throw IOException("Unknown sub-type $subType")
            }
        }
    }

    fun readObject(clazz: String): Saveable {
        val obj = getNewClassInstance(clazz)
        allInstances.add(obj)
        usingType(clazz) {
            val ptr = input.readInt()
            // real all properties
            while (true) {
                val typeId = input.readInt()
                if (typeId < -1) break
                val typeName = readTypeName(typeId)
                val name = typeName.name
                when (typeName.type.code) {

                    BOOL -> obj.setProperty(name, input.readBoolean())
                    BOOL_ARRAY -> obj.setProperty(name, readBooleanArray())
                    BOOL_ARRAY_2D -> obj.setProperty(name, Array(input.readInt()) { readBooleanArray() })

                    CHAR -> obj.setProperty(name, input.readChar())
                    CHAR_ARRAY -> obj.setProperty(name, readCharArray())
                    CHAR_ARRAY_2D -> obj.setProperty(name, Array(input.readInt()) { readCharArray() })

                    BYTE -> obj.setProperty(name, input.readByte())
                    BYTE_ARRAY -> obj.setProperty(name, readByteArray())
                    BYTE_ARRAY_2D -> obj.setProperty(name, Array(input.readInt()) { readByteArray() })

                    SHORT -> obj.setProperty(name, input.readShort())
                    SHORT_ARRAY -> obj.setProperty(name, readShortArray())
                    SHORT_ARRAY_2D -> obj.setProperty(name, Array(input.readInt()) { readShortArray() })

                    INT -> obj.setProperty(name, input.readInt())
                    INT_ARRAY -> obj.setProperty(name, readIntArray())
                    INT_ARRAY_2D -> obj.setProperty(name, Array(input.readInt()) { readIntArray() })

                    LONG -> obj.setProperty(name, input.readLong())
                    LONG_ARRAY -> obj.setProperty(name, readLongArray())
                    LONG_ARRAY_2D -> obj.setProperty(name, Array(input.readInt()) { readLongArray() })

                    FLOAT -> obj.setProperty(name, input.readFloat())
                    FLOAT_ARRAY -> obj.setProperty(name, readFloatArray())
                    FLOAT_ARRAY_2D -> obj.setProperty(name, Array(input.readInt()) { readFloatArray() })

                    DOUBLE -> obj.setProperty(name, input.readDouble())
                    DOUBLE_ARRAY -> obj.setProperty(name, readDoubleArray())
                    DOUBLE_ARRAY_2D -> obj.setProperty(name, Array(input.readInt()) { readDoubleArray() })

                    STRING -> obj.setProperty(name, readEfficientString()!!)
                    STRING_ARRAY -> obj.setProperty(name, readStringArray())
                    STRING_ARRAY_2D -> obj.setProperty(name, Array(input.readInt()) { readStringArray() })

                    FILE -> obj.setProperty(name, readFile())
                    FILE_ARRAY -> obj.setProperty(name, readArray { readFile() })
                    FILE_ARRAY_2D -> obj.setProperty(name, readArray2D { readFile() })

                    OBJECT_IMPL -> obj.setProperty(name, readObject())
                    OBJECT_PTR -> {
                        val ptr2 = input.readInt()
                        val child = getByPointer(ptr2, false)
                        if (child == null) {
                            addMissingReference(obj, name, ptr2)
                        } else {
                            obj.setProperty(name, child)
                        }
                    }

                    OBJECT_NULL -> obj.setProperty(name, null)
                    OBJECT_ARRAY -> obj.setProperty(name, readArray { readObjectOrNull() })
                    OBJECT_ARRAY_2D -> obj.setProperty(name, readArray2D { readObjectOrNull() })

                    OBJECTS_HOMOGENOUS_ARRAY -> obj.setProperty(name, readHomogeneousObjectArray(readTypeString()))

                    VECTOR2F -> obj.setProperty(name, readVector2f())
                    VECTOR2F_ARRAY -> obj.setProperty(name, readArray { readVector2f() })
                    VECTOR2F_ARRAY_2D -> obj.setProperty(name, readArray2D { readVector2f() })

                    VECTOR3F -> obj.setProperty(name, readVector3f())
                    VECTOR3F_ARRAY -> obj.setProperty(name, readArray { readVector3f() })
                    VECTOR3F_ARRAY_2D -> obj.setProperty(name, readArray2D { readVector3f() })

                    VECTOR4F -> obj.setProperty(name, readVector4f())
                    VECTOR4F_ARRAY -> obj.setProperty(name, readArray { readVector4f() })
                    VECTOR4F_ARRAY_2D -> obj.setProperty(name, readArray2D { readVector4f() })

                    VECTOR2D -> obj.setProperty(name, readVector2d())
                    VECTOR2D_ARRAY -> obj.setProperty(name, readArray { readVector2d() })
                    VECTOR2D_ARRAY_2D -> obj.setProperty(name, readArray2D { readVector2d() })

                    VECTOR3D -> obj.setProperty(name, readVector3d())
                    VECTOR3D_ARRAY -> obj.setProperty(name, readArray { readVector3d() })
                    VECTOR3D_ARRAY_2D -> obj.setProperty(name, readArray2D { readVector3d() })

                    VECTOR4D -> obj.setProperty(name, readVector4d())
                    VECTOR4D_ARRAY -> obj.setProperty(name, readArray { readVector4d() })
                    VECTOR4D_ARRAY_2D -> obj.setProperty(name, readArray2D { readVector4d() })

                    VECTOR2I -> obj.setProperty(name, readVector2i())
                    VECTOR2I_ARRAY -> obj.setProperty(name, readArray { readVector2i() })
                    VECTOR2I_ARRAY_2D -> obj.setProperty(name, readArray2D { readVector2i() })

                    VECTOR3I -> obj.setProperty(name, readVector3i())
                    VECTOR3I_ARRAY -> obj.setProperty(name, readArray { readVector3i() })
                    VECTOR3I_ARRAY_2D -> obj.setProperty(name, readArray2D { readVector3i() })

                    VECTOR4I -> obj.setProperty(name, readVector4i())
                    VECTOR4I_ARRAY -> obj.setProperty(name, readArray { readVector4i() })
                    VECTOR4I_ARRAY_2D -> obj.setProperty(name, readArray2D { readVector4i() })

                    QUATERNION32 -> obj.setProperty(name, readQuaternionf())
                    QUATERNION32_ARRAY -> obj.setProperty(name, readArray { readQuaternionf() })
                    QUATERNION32_ARRAY_2D -> obj.setProperty(name, readArray2D { readQuaternionf() })

                    QUATERNION64 -> obj.setProperty(name, readQuaterniond())
                    QUATERNION64_ARRAY -> obj.setProperty(name, readArray { readQuaterniond() })
                    QUATERNION64_ARRAY_2D -> obj.setProperty(name, readArray2D { readQuaterniond() })

                    AABB32 -> obj.setProperty(name, readAABBf())
                    AABB32_ARRAY -> obj.setProperty(name, readArray { readAABBf() })
                    AABB32_ARRAY_2D -> obj.setProperty(name, readArray2D { readAABBf() })

                    AABB64 -> obj.setProperty(name, readAABBd())
                    AABB64_ARRAY -> obj.setProperty(name, readArray { readAABBd() })
                    AABB64_ARRAY_2D -> obj.setProperty(name, readArray2D { readAABBd() })

                    PLANE32 -> obj.setProperty(name, readPlanef())
                    PLANE32_ARRAY -> obj.setProperty(name, readArray { readPlanef() })
                    PLANE32_ARRAY_2D -> obj.setProperty(name, readArray2D { readPlanef() })

                    PLANE64 -> obj.setProperty(name, readPlaned())
                    PLANE64_ARRAY -> obj.setProperty(name, readArray { readPlaned() })
                    PLANE64_ARRAY_2D -> obj.setProperty(name, readArray2D { readPlaned() })

                    MATRIX2X2F -> obj.setProperty(name, readMatrix2x2f())
                    MATRIX3X2F -> obj.setProperty(name, readMatrix3x2f())
                    MATRIX3X3F -> obj.setProperty(name, readMatrix3x3f())
                    MATRIX4X3F -> obj.setProperty(name, readMatrix4x3f())
                    MATRIX4X4F -> obj.setProperty(name, readMatrix4x4f())
                    MATRIX2X2F_ARRAY -> obj.setProperty(name, readArray { readMatrix2x2f() })
                    MATRIX3X2F_ARRAY -> obj.setProperty(name, readArray { readMatrix3x2f() })
                    MATRIX3X3F_ARRAY -> obj.setProperty(name, readArray { readMatrix3x3f() })
                    MATRIX4X3F_ARRAY -> obj.setProperty(name, readArray { readMatrix4x3f() })
                    MATRIX4X4F_ARRAY -> obj.setProperty(name, readArray { readMatrix4x4f() })
                    MATRIX2X2F_ARRAY_2D -> obj.setProperty(name, readArray { readMatrix2x2f() })
                    MATRIX3X2F_ARRAY_2D -> obj.setProperty(name, readArray2D { readMatrix3x2f() })
                    MATRIX3X3F_ARRAY_2D -> obj.setProperty(name, readArray2D { readMatrix3x3f() })
                    MATRIX4X3F_ARRAY_2D -> obj.setProperty(name, readArray2D { readMatrix4x3f() })
                    MATRIX4X4F_ARRAY_2D -> obj.setProperty(name, readArray2D { readMatrix4x4f() })

                    MATRIX2X2D -> obj.setProperty(name, readMatrix2x2d())
                    MATRIX3X2D -> obj.setProperty(name, readMatrix3x2d())
                    MATRIX3X3D -> obj.setProperty(name, readMatrix3x3d())
                    MATRIX4X3D -> obj.setProperty(name, readMatrix4x3d())
                    MATRIX4X4D -> obj.setProperty(name, readMatrix4x4d())
                    MATRIX2X2D_ARRAY -> obj.setProperty(name, readArray { readMatrix2x2d() })
                    MATRIX3X2D_ARRAY -> obj.setProperty(name, readArray { readMatrix3x2d() })
                    MATRIX3X3D_ARRAY -> obj.setProperty(name, readArray { readMatrix3x3d() })
                    MATRIX4X3D_ARRAY -> obj.setProperty(name, readArray { readMatrix4x3d() })
                    MATRIX4X4D_ARRAY -> obj.setProperty(name, readArray { readMatrix4x4d() })
                    MATRIX2X2D_ARRAY_2D -> obj.setProperty(name, readArray2D { readMatrix2x2d() })
                    MATRIX3X2D_ARRAY_2D -> obj.setProperty(name, readArray2D { readMatrix3x2d() })
                    MATRIX3X3D_ARRAY_2D -> obj.setProperty(name, readArray2D { readMatrix3x3d() })
                    MATRIX4X3D_ARRAY_2D -> obj.setProperty(name, readArray2D { readMatrix4x3d() })
                    MATRIX4X4D_ARRAY_2D -> obj.setProperty(name, readArray2D { readMatrix4x4d() })

                    else -> throw IOException("Unknown type ${typeName.type}")
                }
            }
            register(obj, ptr)
        }
        return obj
    }

    private inline fun <reified V> readArray(get: () -> V): Array<V> = Array(input.readInt()) { get() }
    private inline fun <reified V> readArray2D(get: () -> V): Array<Array<V>> {
        return readArray { readArray(get) }
    }

    private fun readFile() = readEfficientString()?.toGlobalFile() ?: InvalidRef
    private fun readVector2f() = Vector2f(input.readFloat(), input.readFloat())
    private fun readVector3f() = Vector3f(input.readFloat(), input.readFloat(), input.readFloat())
    private fun readVector4f() = Vector4f(input.readFloat(), input.readFloat(), input.readFloat(), input.readFloat())
    private fun readPlanef() = Planef(input.readFloat(), input.readFloat(), input.readFloat(), input.readFloat())
    private fun readQuaternionf() =
        Quaternionf(input.readFloat(), input.readFloat(), input.readFloat(), input.readFloat())

    private fun readAABBf() = AABBf(
        input.readFloat(), input.readFloat(), input.readFloat(),
        input.readFloat(), input.readFloat(), input.readFloat(),
    )

    private fun readVector2d() = Vector2d(input.readDouble(), input.readDouble())
    private fun readVector3d() = Vector3d(input.readDouble(), input.readDouble(), input.readDouble())
    private fun readVector4d() =
        Vector4d(input.readDouble(), input.readDouble(), input.readDouble(), input.readDouble())

    private fun readPlaned() = Planed(input.readDouble(), input.readDouble(), input.readDouble(), input.readDouble())
    private fun readQuaterniond() =
        Quaterniond(input.readDouble(), input.readDouble(), input.readDouble(), input.readDouble())

    private fun readAABBd() = AABBd(
        input.readDouble(), input.readDouble(), input.readDouble(),
        input.readDouble(), input.readDouble(), input.readDouble(),
    )

    private fun readVector2i() = Vector2i(input.readInt(), input.readInt())
    private fun readVector3i() = Vector3i(input.readInt(), input.readInt(), input.readInt())
    private fun readVector4i() = Vector4i(input.readInt(), input.readInt(), input.readInt(), input.readInt())

    private fun readMatrix2x2f() = Matrix2f(
        input.readFloat(), input.readFloat(),
        input.readFloat(), input.readFloat()
    )

    private fun readMatrix3x2f() = Matrix3x2f(
        input.readFloat(), input.readFloat(),
        input.readFloat(), input.readFloat(),
        input.readFloat(), input.readFloat()
    )

    private fun readMatrix3x3f() = Matrix3f(
        input.readFloat(), input.readFloat(), input.readFloat(),
        input.readFloat(), input.readFloat(), input.readFloat(),
        input.readFloat(), input.readFloat(), input.readFloat()
    )

    private fun readMatrix4x3f() = Matrix4x3f(
        input.readFloat(), input.readFloat(), input.readFloat(),
        input.readFloat(), input.readFloat(), input.readFloat(),
        input.readFloat(), input.readFloat(), input.readFloat(),
        input.readFloat(), input.readFloat(), input.readFloat()
    )

    private fun readMatrix4x4f() = Matrix4f(
        input.readFloat(), input.readFloat(), input.readFloat(), input.readFloat(),
        input.readFloat(), input.readFloat(), input.readFloat(), input.readFloat(),
        input.readFloat(), input.readFloat(), input.readFloat(), input.readFloat(),
        input.readFloat(), input.readFloat(), input.readFloat(), input.readFloat()
    )

    private fun readMatrix2x2d() = Matrix2d(
        input.readDouble(), input.readDouble(),
        input.readDouble(), input.readDouble()
    )

    private fun readMatrix3x2d() = Matrix3x2d(
        input.readDouble(), input.readDouble(),
        input.readDouble(), input.readDouble(),
        input.readDouble(), input.readDouble()
    )

    private fun readMatrix3x3d() = Matrix3d(
        input.readDouble(), input.readDouble(), input.readDouble(),
        input.readDouble(), input.readDouble(), input.readDouble(),
        input.readDouble(), input.readDouble(), input.readDouble()
    )

    private fun readMatrix4x3d() = Matrix4x3d(
        input.readDouble(), input.readDouble(), input.readDouble(),
        input.readDouble(), input.readDouble(), input.readDouble(),
        input.readDouble(), input.readDouble(), input.readDouble(),
        input.readDouble(), input.readDouble(), input.readDouble()
    )

    private fun readMatrix4x4d() = Matrix4d(
        input.readDouble(), input.readDouble(), input.readDouble(), input.readDouble(),
        input.readDouble(), input.readDouble(), input.readDouble(), input.readDouble(),
        input.readDouble(), input.readDouble(), input.readDouble(), input.readDouble(),
        input.readDouble(), input.readDouble(), input.readDouble(), input.readDouble()
    )

    override fun readAllInList() {
        val nameType = readTypeName()
        assertEquals(nameType.name, "", "Expected object without a name")
        assertEquals(nameType.type.code, OBJECT_LIST_UNKNOWN_LENGTH, "Expected list of unknown length")
        loop@ while (true) {
            val type = input.read()
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

    private fun <V> assertEquals(a: V, b: V, msg: String) {
        if (a != b) throw IOException("$msg, $a != $b")
    }
}