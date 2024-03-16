package me.anno.io.xml.saveable

import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.SimpleType
import me.anno.io.json.saveable.SimpleType.AABBD
import me.anno.io.json.saveable.SimpleType.AABBF
import me.anno.io.json.saveable.SimpleType.BOOLEAN
import me.anno.io.json.saveable.SimpleType.BYTE
import me.anno.io.json.saveable.SimpleType.CHAR
import me.anno.io.json.saveable.SimpleType.COLOR
import me.anno.io.json.saveable.SimpleType.DOUBLE
import me.anno.io.json.saveable.SimpleType.FLOAT
import me.anno.io.json.saveable.SimpleType.INT
import me.anno.io.json.saveable.SimpleType.LONG
import me.anno.io.json.saveable.SimpleType.MATRIX2X2F
import me.anno.io.json.saveable.SimpleType.MATRIX3X2F
import me.anno.io.json.saveable.SimpleType.MATRIX3X3F
import me.anno.io.json.saveable.SimpleType.MATRIX4X3F
import me.anno.io.json.saveable.SimpleType.MATRIX4X4F
import me.anno.io.json.saveable.SimpleType.PLANED
import me.anno.io.json.saveable.SimpleType.PLANEF
import me.anno.io.json.saveable.SimpleType.QUATERNIOND
import me.anno.io.json.saveable.SimpleType.QUATERNIONF
import me.anno.io.json.saveable.SimpleType.REFERENCE
import me.anno.io.json.saveable.SimpleType.SHORT
import me.anno.io.json.saveable.SimpleType.STRING
import me.anno.io.json.saveable.SimpleType.VECTOR2D
import me.anno.io.json.saveable.SimpleType.VECTOR2F
import me.anno.io.json.saveable.SimpleType.VECTOR2I
import me.anno.io.json.saveable.SimpleType.VECTOR3D
import me.anno.io.json.saveable.SimpleType.VECTOR3F
import me.anno.io.json.saveable.SimpleType.VECTOR3I
import me.anno.io.json.saveable.SimpleType.VECTOR4D
import me.anno.io.json.saveable.SimpleType.VECTOR4F
import me.anno.io.json.saveable.SimpleType.VECTOR4I
import me.anno.utils.Color.toHexColor
import me.anno.utils.types.Strings
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

// todo instead of this complex logic, could we convert JSON to XML?
abstract class XMLWriterBase(val workspace: FileReference) : BaseWriter(true) {

    abstract fun append(v: String)

    val simpleObjects = ArrayList<String>()
    val objects = ArrayList<Saveable>()

    private fun escape(value: String): String {
        return if (value.all { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' }) value
        else {
            val tmp = StringBuilder()
            Strings.writeEscaped(value, tmp)
            tmp.toString()
        }
    }

    private fun attr(type: SimpleType, name: String, value: String) {
        attr(type.scalar, name, value)
    }

    private fun attr(type: String, name: String, value: String) {
        append(" $name=\"$type:${escape(value)}\"")
    }

    private fun array(type: SimpleType, name: String, value: String) {
        attr(type.array, name, value)
    }

    private fun array(type: SimpleType, name: String, size: Int, lastIndex: Int, formatValue: (Int) -> String) {
        array(type, name, "$size,${(0 .. lastIndex).joinToString(",") { formatValue(it) }}")
    }

    private fun simpleObject(type: SimpleType, name: String, attributes: String) {
        simpleObjects.add("<${type.scalar} name=\"${escape(name)}\" $attributes/>")
    }

    override fun writeBoolean(name: String, value: Boolean, force: Boolean) {
        attr(BOOLEAN, name, if (value) "true" else "false")
    }

    override fun writeBooleanArray(name: String, values: BooleanArray, force: Boolean) {
        attr(BOOLEAN.array, name, values.joinToString("") { if (it) "1" else "0" })
    }

    override fun writeBooleanArray2D(name: String, values: Array<BooleanArray>, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeChar(name: String, value: Char, force: Boolean) {
        if (force || value.code != 0) attr(CHAR, name, "${value.code}")
    }

    override fun writeCharArray(name: String, values: CharArray, force: Boolean) {
        array(CHAR, name, values.size, values.indexOfLast { it.code != 0 }) { values[it].code.toString() }
    }

    override fun writeCharArray2D(name: String, values: Array<CharArray>, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeByte(name: String, value: Byte, force: Boolean) {
        if (force || value.toInt() != 0) attr(BYTE, name, value.toString())
    }

    override fun writeByteArray(name: String, values: ByteArray, force: Boolean) {
        array(BYTE, name, values.size, values.indexOfLast { it.toInt() != 0 }) { values[it].toString() }
    }

    override fun writeByteArray2D(name: String, values: Array<ByteArray>, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeShort(name: String, value: Short, force: Boolean) {
        if (force || value.toInt() != 0) attr(SHORT, name, value.toString())
    }

    override fun writeShortArray(name: String, values: ShortArray, force: Boolean) {
        array(SHORT, name, values.size, values.indexOfLast { it.toInt() != 0 }) { values[it].toString() }
    }

    override fun writeShortArray2D(name: String, values: Array<ShortArray>, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeInt(name: String, value: Int, force: Boolean) {
        if (force || value != 0) attr(INT, name, value.toString())
    }

    override fun writeIntArray(name: String, values: IntArray, force: Boolean) {
        array(INT, name, values.size, values.indexOfLast { it != 0 }) { values[it].toString() }
    }

    override fun writeIntArray2D(name: String, values: Array<IntArray>, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeColor(name: String, value: Int, force: Boolean) {
        if (force || value != 0) attr(COLOR, name, value.toHexColor())
    }

    override fun writeColorArray(name: String, values: IntArray, force: Boolean) {
        array(COLOR, name, values.size, values.indexOfLast { it != 0 }) { values[it].toHexColor() }
    }

    override fun writeColorArray2D(name: String, values: Array<IntArray>, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeLong(name: String, value: Long, force: Boolean) {
        if (force || value != 0L) attr(LONG, name, value.toString())
    }

    override fun writeLongArray(name: String, values: LongArray, force: Boolean) {
        array(LONG, name, values.size, values.indexOfLast { it != 0L }) { values[it].toString() }
    }

    override fun writeLongArray2D(name: String, values: Array<LongArray>, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeFloat(name: String, value: Float, force: Boolean) {
        if (force || value != 0f) attr(FLOAT, name, value.toString())
    }

    override fun writeFloatArray(name: String, values: FloatArray, force: Boolean) {
        array(FLOAT, name, values.size, values.indexOfLast { it != 0f }) { values[it].toString() }
    }

    override fun writeFloatArray2D(name: String, values: Array<FloatArray>, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeDouble(name: String, value: Double, force: Boolean) {
        if (force || value != 0.0) attr(DOUBLE, name, value.toString())
    }

    override fun writeDoubleArray(name: String, values: DoubleArray, force: Boolean) {
        array(DOUBLE, name, values.size, values.indexOfLast { it != 0.0 }) { values[it].toString() }
    }

    override fun writeDoubleArray2D(name: String, values: Array<DoubleArray>, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeString(name: String, value: String, force: Boolean) {
        if (force || value != "") attr(STRING, name, value)
    }

    override fun writeStringArray(name: String, values: Array<String>, force: Boolean) {
        array(STRING, name, values.size, values.indexOfLast { it != "" }) { "\"${escape(values[it])}\"" }
    }

    override fun writeStringArray2D(name: String, values: Array<Array<String>>, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeVector2f(name: String, value: Vector2f, force: Boolean) {
        simpleObject(VECTOR2F, name, "x=${value.x} y=${value.y}")
    }

    override fun writeVector3f(name: String, value: Vector3f, force: Boolean) {
        simpleObject(VECTOR3F, name, "x=${value.x} y=${value.y} z=${value.z}")
    }

    override fun writeVector4f(name: String, value: Vector4f, force: Boolean) {
        simpleObject(VECTOR4F, name, "x=${value.x} y=${value.y} z=${value.z} w=${value.w}")
    }

    override fun writeVector2fArray(name: String, values: Array<Vector2f>, force: Boolean) {
        array(VECTOR2F, name, values.joinToString(",") { "(${it.x},${it.y})" })
    }

    override fun writeVector3fArray(name: String, values: Array<Vector3f>, force: Boolean) {
        array(VECTOR2F, name, values.joinToString(",") { "(${it.x},${it.y},${it.z})" })
    }

    override fun writeVector4fArray(name: String, values: Array<Vector4f>, force: Boolean) {
        array(VECTOR2F, name, values.joinToString(",") { "(${it.x},${it.y},${it.z},${it.w})" })
    }

    override fun writeVector2fArray2D(name: String, values: Array<Array<Vector2f>>, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeVector3fArray2D(name: String, values: Array<Array<Vector3f>>, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeVector4fArray2D(name: String, values: Array<Array<Vector4f>>, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeVector2d(name: String, value: Vector2d, force: Boolean) {
        simpleObject(VECTOR2D, name, "x=\"${value.x}\" y=\"${value.y}\"")
    }

    override fun writeVector3d(name: String, value: Vector3d, force: Boolean) {
        simpleObject(VECTOR3D, name, "x=\"${value.x}\" y=\"${value.y}\" z=\"${value.z}\"")
    }

    override fun writeVector4d(name: String, value: Vector4d, force: Boolean) {
        simpleObject(VECTOR4D, name, "x=\"${value.x}\" y=\"${value.y}\" z=\"${value.z}\" w=\"${value.w}\"")
    }

    override fun writeVector2dArray(name: String, values: Array<Vector2d>, force: Boolean) {
        array(VECTOR2D, name, values.joinToString(",") { "(${it.x},${it.y})" })
    }

    override fun writeVector3dArray(name: String, values: Array<Vector3d>, force: Boolean) {
        array(VECTOR3D, name, values.joinToString(",") { "(${it.x},${it.y},${it.z})" })
    }

    override fun writeVector4dArray(name: String, values: Array<Vector4d>, force: Boolean) {
        array(VECTOR4D, name, values.joinToString(",") { "(${it.x},${it.y},${it.z},${it.w})" })
    }

    override fun writeVector2dArray2D(name: String, values: Array<Array<Vector2d>>, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeVector3dArray2D(name: String, values: Array<Array<Vector3d>>, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeVector4dArray2D(name: String, values: Array<Array<Vector4d>>, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeVector2i(name: String, value: Vector2i, force: Boolean) {
        simpleObject(VECTOR2I, name, "x=\"${value.x}\" y=\"${value.y}\"")
    }

    override fun writeVector3i(name: String, value: Vector3i, force: Boolean) {
        simpleObject(VECTOR3I, name, "x=\"${value.x}\" y=\"${value.y}\" z=\"${value.z}\"")
    }

    override fun writeVector4i(name: String, value: Vector4i, force: Boolean) {
        simpleObject(VECTOR4I, name, "x=\"${value.x}\" y=\"${value.y}\" z=\"${value.z}\" w=\"${value.w}\"")
    }

    override fun writeVector2iArray(name: String, values: Array<Vector2i>, force: Boolean) {
        array(VECTOR2I, name, values.joinToString(",") { "(${it.x},${it.y})" })
    }

    override fun writeVector3iArray(name: String, values: Array<Vector3i>, force: Boolean) {
        array(VECTOR3I, name, values.joinToString(",") { "(${it.x},${it.y},${it.z})" })
    }

    override fun writeVector4iArray(name: String, values: Array<Vector4i>, force: Boolean) {
        array(VECTOR4I, name, values.joinToString(",") { "(${it.x},${it.y},${it.z},${it.w})" })
    }

    override fun writeVector2iArray2D(name: String, values: Array<Array<Vector2i>>, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeVector3iArray2D(name: String, values: Array<Array<Vector3i>>, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeVector4iArray2D(name: String, values: Array<Array<Vector4i>>, force: Boolean) {
        TODO("Not yet implemented")
    }

    private fun m22(value: Matrix2f): String {
        return "[${value.m00},${value.m01},${value.m10},${value.m11}]"
    }

    private fun m32(value: Matrix3x2f): String {
        return "[${value.m00},${value.m01},${value.m10},${value.m11},${value.m20},${value.m21}]"
    }

    private fun m33(value: Matrix3f): String {
        return "[${value.m00},${value.m01},${value.m02}," +
                "${value.m10},${value.m11},${value.m12}," +
                "${value.m20},${value.m21},${value.m22}]"
    }

    private fun m43(value: Matrix4x3f): String {
        return "[${value.m00},${value.m01},${value.m02}," +
                "${value.m10},${value.m11},${value.m12}," +
                "${value.m20},${value.m21},${value.m22}," +
                "${value.m30},${value.m31},${value.m32}]"
    }

    private fun m44(value: Matrix4f): String {
        return "[${value.m00},${value.m01},${value.m02},${value.m03}," +
                "${value.m10},${value.m11},${value.m12},${value.m13}," +
                "${value.m20},${value.m21},${value.m22},${value.m23}," +
                "${value.m30},${value.m31},${value.m32},${value.m33}]"
    }

    override fun writeMatrix2x2f(name: String, value: Matrix2f, force: Boolean) {
        simpleObject(MATRIX2X2F, name, "v=\"${m22(value)}\"")
    }

    override fun writeMatrix3x2f(name: String, value: Matrix3x2f, force: Boolean) {
        simpleObject(MATRIX3X2F, name, "v=\"${m32(value)}\"")
    }

    override fun writeMatrix3x3f(name: String, value: Matrix3f, force: Boolean) {
        simpleObject(MATRIX3X3F, name, "v=\"${m33(value)}\"")
    }

    override fun writeMatrix4x3f(name: String, value: Matrix4x3f, force: Boolean) {
        simpleObject(MATRIX4X3F, name, "v=\"${m43(value)}\"")
    }

    override fun writeMatrix4x4f(name: String, value: Matrix4f, force: Boolean) {
        simpleObject(MATRIX4X4F, name, "v=\"${m44(value)}\"")
    }

    override fun writeMatrix2x2fArray(name: String, values: Array<Matrix2f>, force: Boolean) {
        array(MATRIX2X2F, name, values.joinToString(",") { m22(it) })
    }

    override fun writeMatrix3x2fArray(name: String, values: Array<Matrix3x2f>, force: Boolean) {
        array(MATRIX3X2F, name, values.joinToString(",") { m32(it) })
    }

    override fun writeMatrix3x3fArray(name: String, values: Array<Matrix3f>, force: Boolean) {
        array(MATRIX3X3F, name, values.joinToString(",") { m33(it) })
    }

    override fun writeMatrix4x3fArray(name: String, values: Array<Matrix4x3f>, force: Boolean) {
        array(MATRIX4X3F, name, values.joinToString(",") { m43(it) })
    }

    override fun writeMatrix4x4fArray(name: String, values: Array<Matrix4f>, force: Boolean) {
        array(MATRIX4X4F, name, values.joinToString(",") { m44(it) })
    }

    override fun writeMatrix2x2fArray2D(name: String, values: Array<Array<Matrix2f>>, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeMatrix3x2fArray2D(name: String, values: Array<Array<Matrix3x2f>>, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeMatrix3x3fArray2D(name: String, values: Array<Array<Matrix3f>>, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeMatrix4x3fArray2D(name: String, values: Array<Array<Matrix4x3f>>, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeMatrix4x4fArray2D(name: String, values: Array<Array<Matrix4f>>, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeMatrix2x2d(name: String, value: Matrix2d, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeMatrix3x2d(name: String, value: Matrix3x2d, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeMatrix3x3d(name: String, value: Matrix3d, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeMatrix4x3d(name: String, value: Matrix4x3d, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeMatrix4x4d(name: String, value: Matrix4d, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeMatrix2x2dArray(name: String, values: Array<Matrix2d>, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeMatrix3x2dArray(name: String, values: Array<Matrix3x2d>, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeMatrix3x3dArray(name: String, values: Array<Matrix3d>, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeMatrix4x3dArray(name: String, values: Array<Matrix4x3d>, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeMatrix4x4dArray(name: String, values: Array<Matrix4d>, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeMatrix2x2dArray2D(name: String, values: Array<Array<Matrix2d>>, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeMatrix3x2dArray2D(name: String, values: Array<Array<Matrix3x2d>>, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeMatrix3x3dArray2D(name: String, values: Array<Array<Matrix3d>>, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeMatrix4x3dArray2D(name: String, values: Array<Array<Matrix4x3d>>, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeMatrix4x4dArray2D(name: String, values: Array<Array<Matrix4d>>, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeQuaternionf(name: String, value: Quaternionf, force: Boolean) {
        simpleObject(QUATERNIONF, name, "x=\"${value.x}\" y=\"${value.y}\" z=\"${value.z}\" w=\"${value.w}\"")
    }

    override fun writeQuaterniond(name: String, value: Quaterniond, force: Boolean) {
        simpleObject(QUATERNIOND, name, "x=\"${value.x}\" y=\"${value.y}\" z=\"${value.z}\" w=\"${value.w}\"")
    }

    override fun writeQuaternionfArray(name: String, values: Array<Quaternionf>, force: Boolean) {
        array(QUATERNIONF, name, values.joinToString(",") { "(${it.x},${it.y},${it.z},${it.w})" })
    }

    override fun writeQuaterniondArray(name: String, values: Array<Quaterniond>, force: Boolean) {
        array(QUATERNIOND, name, values.joinToString(",") { "(${it.x},${it.y},${it.z},${it.w})" })
    }

    override fun writeQuaternionfArray2D(name: String, values: Array<Array<Quaternionf>>, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeQuaterniondArray2D(name: String, values: Array<Array<Quaterniond>>, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeAABBf(name: String, value: AABBf, force: Boolean) {
        simpleObject(
            AABBF, name, "minX=\"${value.minX}\" minY=\"${value.minY}\" minZ=\"${value.minZ}\"" +
                    " maxX=\"${value.maxX}\" maxY=\"${value.maxY}\" maxZ=\"${value.maxZ}\""
        )
    }

    override fun writeAABBd(name: String, value: AABBd, force: Boolean) {
        simpleObject(
            AABBD, name, "minX=\"${value.minX}\" minY=\"${value.minY}\" minZ=\"${value.minZ}\"" +
                    " maxX=\"${value.maxX}\" maxY=\"${value.maxY}\" maxZ=\"${value.maxZ}\""
        )
    }

    override fun writeAABBfArray(name: String, values: Array<AABBf>, force: Boolean) {
        array(AABBF, name, values.joinToString(",") {
            "(${it.minX},${it.minY},${it.minZ},${it.maxX},${it.maxY},${it.maxZ})"
        })
    }

    override fun writeAABBdArray(name: String, values: Array<AABBd>, force: Boolean) {
        array(AABBD, name, values.joinToString(",") {
            "(${it.minX},${it.minY},${it.minZ},${it.maxX},${it.maxY},${it.maxZ})"
        })
    }

    override fun writeAABBfArray2D(name: String, values: Array<Array<AABBf>>, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeAABBdArray2D(name: String, values: Array<Array<AABBd>>, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writePlanef(name: String, value: Planef, force: Boolean) {
        val attr = "x=\"${value.dirX}\" y=\"${value.dirY}\" z=\"${value.dirZ}\" d=\"${value.distance}\""
        simpleObject(PLANEF, name, attr)
    }

    override fun writePlaned(name: String, value: Planed, force: Boolean) {
        val attr = "x=\"${value.dirX}\" y=\"${value.dirY}\" z=\"${value.dirZ}\" d=\"${value.distance}\""
        simpleObject(PLANED, name, attr)
    }

    override fun writePlanefArray(name: String, values: Array<Planef>, force: Boolean) {
        array(PLANEF, name, values.joinToString(",") { "(${it.dirX},${it.dirY},${it.dirZ},${it.distance})" })
    }

    override fun writePlanedArray(name: String, values: Array<Planed>, force: Boolean) {
        array(PLANED, name, values.joinToString(",") { "(${it.dirX},${it.dirY},${it.dirZ},${it.distance})" })
    }

    override fun writePlanefArray2D(name: String, values: Array<Array<Planef>>, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writePlanedArray2D(name: String, values: Array<Array<Planed>>, force: Boolean) {
        TODO("Not yet implemented")
    }

    private fun formatFile(value: FileReference?, workspace: FileReference): String {
        return (value ?: InvalidRef).toLocalPath(workspace.ifUndefined(this.workspace))
    }

    override fun writeFile(name: String, value: FileReference, force: Boolean, workspace: FileReference) {
        if (force || value != InvalidRef) attr(REFERENCE, name, formatFile(value, workspace))
    }

    override fun writeFileArray(name: String, values: Array<FileReference>, force: Boolean, workspace: FileReference) {
        if (force || values.isNotEmpty()) {
            array(REFERENCE, name, values.size, values.indexOfLast { it != InvalidRef }) {
                "\"${escape(formatFile(values[it], workspace))}\""
            }
        }
    }

    override fun writeFileArray2D(
        name: String,
        values: Array<Array<FileReference>>,
        force: Boolean,
        workspace: FileReference
    ) {
        TODO("Not yet implemented")
    }

    override fun writeNull(name: String?) {
        if (name != null) {
            attr("?", name, "null")
        } else {
            append("<null/>")
        }
    }

    override fun writePointer(name: String?, className: String, ptr: Int, value: Saveable) {
        append("<pointer")
        if (name != null) {
            append(" name=\"${escape(name)}\"")
        }
        append(" class=\"${escape(className)}\"")
        append(" addr=\"$ptr\"")
        append("/>")
    }

    override fun writeObjectImpl(name: String?, value: Saveable) {
        append("<${value.className}")
        val size0 = objects.size
        value.save(this)
        if (simpleObjects.isEmpty() && objects.size == size0) {
            append("/>") // easy end
        } else {
            append(">")
            for (obj in simpleObjects) {
                append(obj) // easy
            }
            for (i in size0 until objects.size) {
                writeObject(value, null, objects[i], true)
            }
            objects.subList(size0, objects.size).clear()
            simpleObjects.clear()
            append("</${value.className}>")
        }
    }

    override fun <V : Saveable?> writeNullableObjectArray(
        self: Saveable?, name: String, values: Array<V>?, force: Boolean
    ) {
        if (force || values?.isNotEmpty() == true) {
            if (!values.isNullOrEmpty()) {
                append("<$name>")
                for (value in values) {
                    writeObject(self, null, value)
                }
                append("</$name>")
            } else {
                append("<$name/>")
            }
        }
    }

    override fun <V : Saveable> writeObjectArray(self: Saveable?, name: String, values: Array<V>?, force: Boolean) {
        writeNullableObjectArray(self, name, values, force)
    }

    override fun <V : Saveable> writeObjectArray2D(
        self: Saveable?, name: String, values: Array<Array<V>>, force: Boolean
    ) {
        TODO("Not yet implemented")
    }

    override fun <V : Saveable?> writeHomogenousObjectArray(
        self: Saveable?, name: String, values: Array<V>, force: Boolean
    ) = writeNullableObjectArray(self, name, values, force)

    override fun writeListStart() {
        append("<thisIsXML>")
    }

    override fun writeListEnd() {
        append("</thisIsXML>")
    }

    override fun writeListSeparator() {
        // the best we could do would be autoformatting
    }
}