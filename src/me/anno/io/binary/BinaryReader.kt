package me.anno.io.binary

import me.anno.io.Streams.readBE16
import me.anno.io.Streams.readBE32
import me.anno.io.Streams.readBE32F
import me.anno.io.Streams.readBE64
import me.anno.io.Streams.readBE64F
import me.anno.io.Streams.readNBytes2
import me.anno.io.base.BaseReader
import me.anno.io.binary.BinaryTypes.OBJECTS_HOMOGENOUS_ARRAY
import me.anno.io.binary.BinaryTypes.OBJECT_ARRAY
import me.anno.io.binary.BinaryTypes.OBJECT_ARRAY_2D
import me.anno.io.binary.BinaryTypes.OBJECT_IMPL
import me.anno.io.binary.BinaryTypes.OBJECT_LIST_UNKNOWN_LENGTH
import me.anno.io.binary.BinaryTypes.OBJECT_NULL
import me.anno.io.binary.BinaryTypes.OBJECT_PTR
import me.anno.io.binary.BinaryWriter.Companion.LIST_END
import me.anno.io.binary.BinaryWriter.Companion.LIST_SEPARATOR
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.SimpleType
import me.anno.io.saveable.Saveable
import me.anno.utils.assertions.assertFail
import me.anno.utils.files.LocalFile.toGlobalFile
import me.anno.utils.structures.lists.Lists.createArrayList
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
import java.io.IOException
import java.io.InputStream

/**
 * writing as text is:
 * - easier debuggable
 * - similar speed
 * - similar length when compressed
 * */
class BinaryReader(val input: InputStream) : BaseReader() {

    private val knownNames = ArrayList<String>()

    private val knownNameTypes = HashMap<String, ArrayList<NameType>>()

    private var currentClass = ""
    private var currentNameTypes = knownNameTypes.getOrPut(currentClass, ::ArrayList)

    private inline fun usingType(type: String, run: () -> Unit) {
        val old1 = currentClass
        val old2 = currentNameTypes
        currentClass = type
        currentNameTypes = knownNameTypes.getOrPut(type, ::ArrayList)
        run()
        currentClass = old1
        currentNameTypes = old2
    }

    private fun readEfficientString(): String? {
        val id = input.readBE32()
        return when {
            id == -1 -> null
            id >= 0 -> knownNames[id]
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
            val type = input.read()
            val value = NameType(name, type)
            currentNameTypes.add(value)
            value
        }
    }

    private fun readTypeName(): NameType {
        return readTypeName(input.readBE32())
    }

    override fun readObject(): Saveable {
        val clazz = readTypeString()
        return readObject(clazz, true)
    }

    private fun readBooleanArray() = BooleanArray(input.readBE32()) { input.read() > 0 }
    private fun readCharArray() = CharArray(input.readBE32()) { input.readBE16().toChar() }
    private fun readByteArray() = input.readNBytes2(input.readBE32(), true)
    private fun readShortArray() = ShortArray(input.readBE32()) { input.readBE16().toShort() }
    private fun readIntArray() = IntArray(input.readBE32()) { input.readBE32() }
    private fun readLongArray() = LongArray(input.readBE32()) { input.readBE64() }
    private fun readFloatArray() = FloatArray(input.readBE32()) { input.readBE32F() }
    private fun readDoubleArray() = DoubleArray(input.readBE32()) { input.readBE64F() }

    private fun readObjectOrNull(): Saveable? {
        return when (val subType = input.read()) {
            OBJECT_IMPL -> readObject()
            OBJECT_PTR -> getByPointer(input.readBE32(), true)
            OBJECT_NULL -> null
            else -> throw IOException("Unknown sub-type $subType")
        }
    }

    private fun readHomogeneousList(): List<Saveable?> {
        val type = readTypeString()
        return readList { readObject(type, false) }
    }

    fun readObject(clazz: String, readPointer: Boolean): Saveable {
        val obj = getNewClassInstance(clazz)
        if (readPointer) allInstances.add(obj) // always add this? not really needed for homogenous arrays
        obj.onReadingStarted()
        usingType(clazz) {
            readObjectProperties(obj, readPointer)
        }
        obj.onReadingEnded()
        return obj
    }

    private fun readObjectProperties(obj: Saveable, readPointer: Boolean) {
        val ptr = if (readPointer) input.readBE32() else 0
        // read all properties
        while (true) {
            val typeId = input.readBE32()
            if (typeId < -1) break
            val typeName = readTypeName(typeId)
            val name = typeName.name
            val reader = readers.getOrNull(typeName.type)
            if (reader != null) {
                val value = reader.invoke(this)
                obj.setProperty(name, value)
            } else when (typeName.type) {
                OBJECT_PTR -> {
                    val ptr2 = input.readBE32()
                    val child = getByPointer(ptr2, false)
                    if (child == null) {
                        addMissingReference(obj, name, ptr2)
                    } else {
                        obj.setProperty(name, child)
                    }
                }
                else -> assertFail("Unknown type ${typeName.type}")
            }
        }
        if (readPointer) {
            register(obj, ptr)
        }
    }

    private fun <V> readList(get: () -> V): ArrayList<V> {
        return createArrayList(input.readBE32()) { get() }
    }

    private fun <V> readList2D(get: () -> V): ArrayList<ArrayList<V>> {
        return readList { readList(get) }
    }

    private fun readFile(): FileReference =
        readEfficientString()?.toGlobalFile() ?: InvalidRef

    private fun readVector2f(): Vector2f =
        Vector2f(input.readBE32F(), input.readBE32F())

    private fun readVector3f(): Vector3f =
        Vector3f(input.readBE32F(), input.readBE32F(), input.readBE32F())

    private fun readVector4f(): Vector4f =
        Vector4f(input.readBE32F(), input.readBE32F(), input.readBE32F(), input.readBE32F())

    private fun readPlanef(): Planef =
        Planef(input.readBE32F(), input.readBE32F(), input.readBE32F(), input.readBE32F())

    private fun readQuaternionf(): Quaternionf =
        Quaternionf(input.readBE32F(), input.readBE32F(), input.readBE32F(), input.readBE32F())

    private fun readAABBf(): AABBf = AABBf(
        input.readBE32F(), input.readBE32F(), input.readBE32F(),
        input.readBE32F(), input.readBE32F(), input.readBE32F(),
    )

    private fun readVector2d(): Vector2d =
        Vector2d(input.readBE64F(), input.readBE64F())

    private fun readVector3d(): Vector3d =
        Vector3d(input.readBE64F(), input.readBE64F(), input.readBE64F())

    private fun readVector4d(): Vector4d =
        Vector4d(input.readBE64F(), input.readBE64F(), input.readBE64F(), input.readBE64F())

    private fun readPlaned(): Planed =
        Planed(input.readBE64F(), input.readBE64F(), input.readBE64F(), input.readBE64F())

    private fun readQuaterniond(): Quaterniond =
        Quaterniond(input.readBE64F(), input.readBE64F(), input.readBE64F(), input.readBE64F())

    private fun readAABBd(): AABBd = AABBd(
        input.readBE64F(), input.readBE64F(), input.readBE64F(),
        input.readBE64F(), input.readBE64F(), input.readBE64F(),
    )

    private fun readVector2i(): Vector2i =
        Vector2i(input.readBE32(), input.readBE32())

    private fun readVector3i(): Vector3i =
        Vector3i(input.readBE32(), input.readBE32(), input.readBE32())

    private fun readVector4i(): Vector4i =
        Vector4i(input.readBE32(), input.readBE32(), input.readBE32(), input.readBE32())

    private fun readMatrix2x2f(): Matrix2f = Matrix2f(
        input.readBE32F(), input.readBE32F(),
        input.readBE32F(), input.readBE32F()
    )

    private fun readMatrix3x2f(): Matrix3x2f = Matrix3x2f(
        input.readBE32F(), input.readBE32F(),
        input.readBE32F(), input.readBE32F(),
        input.readBE32F(), input.readBE32F()
    )

    private fun readMatrix3x3f(): Matrix3f = Matrix3f(
        input.readBE32F(), input.readBE32F(), input.readBE32F(),
        input.readBE32F(), input.readBE32F(), input.readBE32F(),
        input.readBE32F(), input.readBE32F(), input.readBE32F()
    )

    private fun readMatrix4x3f(): Matrix4x3f = Matrix4x3f(
        input.readBE32F(), input.readBE32F(), input.readBE32F(),
        input.readBE32F(), input.readBE32F(), input.readBE32F(),
        input.readBE32F(), input.readBE32F(), input.readBE32F(),
        input.readBE32F(), input.readBE32F(), input.readBE32F()
    )

    private fun readMatrix4x4f(): Matrix4f = Matrix4f(
        input.readBE32F(), input.readBE32F(), input.readBE32F(), input.readBE32F(),
        input.readBE32F(), input.readBE32F(), input.readBE32F(), input.readBE32F(),
        input.readBE32F(), input.readBE32F(), input.readBE32F(), input.readBE32F(),
        input.readBE32F(), input.readBE32F(), input.readBE32F(), input.readBE32F()
    )

    private fun readMatrix2x2d(): Matrix2d = Matrix2d(
        input.readBE64F(), input.readBE64F(),
        input.readBE64F(), input.readBE64F()
    )

    private fun readMatrix3x2d(): Matrix3x2d = Matrix3x2d(
        input.readBE64F(), input.readBE64F(),
        input.readBE64F(), input.readBE64F(),
        input.readBE64F(), input.readBE64F()
    )

    private fun readMatrix3x3d(): Matrix3d = Matrix3d(
        input.readBE64F(), input.readBE64F(), input.readBE64F(),
        input.readBE64F(), input.readBE64F(), input.readBE64F(),
        input.readBE64F(), input.readBE64F(), input.readBE64F()
    )

    private fun readMatrix4x3d(): Matrix4x3d = Matrix4x3d(
        input.readBE64F(), input.readBE64F(), input.readBE64F(),
        input.readBE64F(), input.readBE64F(), input.readBE64F(),
        input.readBE64F(), input.readBE64F(), input.readBE64F(),
        input.readBE64F(), input.readBE64F(), input.readBE64F()
    )

    private fun readMatrix4x4d(): Matrix4d = Matrix4d(
        input.readBE64F(), input.readBE64F(), input.readBE64F(), input.readBE64F(),
        input.readBE64F(), input.readBE64F(), input.readBE64F(), input.readBE64F(),
        input.readBE64F(), input.readBE64F(), input.readBE64F(), input.readBE64F(),
        input.readBE64F(), input.readBE64F(), input.readBE64F(), input.readBE64F()
    )

    override fun readAllInList() {
        val nameType = readTypeName()
        assertEquals(nameType.name, "", "Expected object without a name")
        assertEquals(nameType.type, OBJECT_LIST_UNKNOWN_LENGTH, "Expected list of unknown length")
        loop@ while (true) {
            val type = input.read()
            assertEquals(OBJECT_IMPL, type, "Type must be OBJECT_IMPL")
            readObject()
            when (val code = input.read()) {
                LIST_SEPARATOR -> {}
                LIST_END -> break@loop
                else -> assertFail("Invalid Code $code")
            }
        }
    }

    companion object {

        private val readers = arrayOfNulls<BinaryReader.() -> Any?>(128)

        private fun <V> registerReader(
            type: SimpleType, reader: BinaryReader.() -> V
        ) {
            readers[type.scalarId] = { reader() }
            readers[type.scalarId + 1] = { readList { reader() } }
            readers[type.scalarId + 2] = { readList2D { reader() } }
        }

        private fun <V, W> registerReader2(
            type: SimpleType, reader1: BinaryReader.() -> V,
            readerN: BinaryReader.() -> W
        ) {
            readers[type.scalarId] = { reader1() }
            readers[type.scalarId + 1] = { readerN() }
            readers[type.scalarId + 2] = { readList { readerN() } }
        }

        init {
            readers[OBJECT_NULL] = { null }
            readers[OBJECT_IMPL] = { readObject() }
            readers[OBJECT_ARRAY] = { readList { readObjectOrNull() } }
            readers[OBJECT_ARRAY_2D] = { readList2D { readObjectOrNull() } }
            readers[OBJECTS_HOMOGENOUS_ARRAY] = { readHomogeneousList() }
            registerReader2(SimpleType.BYTE, { input.read().toByte() }) { readByteArray() }
            registerReader2(SimpleType.SHORT, { input.readBE16().toShort() }) { readShortArray() }
            registerReader2(SimpleType.INT, { input.readBE32() }) { readIntArray() }
            registerReader2(SimpleType.LONG, { input.readBE64() }) { readLongArray() }
            registerReader2(SimpleType.FLOAT, { input.readBE32F() }) { readFloatArray() }
            registerReader2(SimpleType.DOUBLE, { input.readBE64F() }) { readDoubleArray() }
            registerReader2(SimpleType.BOOLEAN, { input.read() > 0 }) { readBooleanArray() }
            registerReader2(SimpleType.CHAR, { input.readBE16().toChar() }) { readCharArray() }
            registerReader(SimpleType.STRING) { readEfficientString() }
            registerReader(SimpleType.REFERENCE) { readFile() }
            registerReader(SimpleType.VECTOR2F) { readVector2f() }
            registerReader(SimpleType.VECTOR3F) { readVector3f() }
            registerReader(SimpleType.VECTOR4F) { readVector4f() }
            registerReader(SimpleType.VECTOR2D) { readVector2d() }
            registerReader(SimpleType.VECTOR3D) { readVector3d() }
            registerReader(SimpleType.VECTOR4D) { readVector4d() }
            registerReader(SimpleType.VECTOR2I) { readVector2i() }
            registerReader(SimpleType.VECTOR3I) { readVector3i() }
            registerReader(SimpleType.VECTOR4I) { readVector4i() }
            registerReader(SimpleType.QUATERNIONF) { readQuaternionf() }
            registerReader(SimpleType.QUATERNIOND) { readQuaterniond() }
            registerReader(SimpleType.AABBF) { readAABBf() }
            registerReader(SimpleType.AABBD) { readAABBd() }
            registerReader(SimpleType.PLANEF) { readPlanef() }
            registerReader(SimpleType.PLANED) { readPlaned() }
            registerReader(SimpleType.MATRIX2X2F) { readMatrix2x2f() }
            registerReader(SimpleType.MATRIX3X2F) { readMatrix3x2f() }
            registerReader(SimpleType.MATRIX3X3F) { readMatrix3x3f() }
            registerReader(SimpleType.MATRIX4X3F) { readMatrix4x3f() }
            registerReader(SimpleType.MATRIX4X4F) { readMatrix4x4f() }
            registerReader(SimpleType.MATRIX2X2D) { readMatrix2x2d() }
            registerReader(SimpleType.MATRIX3X2D) { readMatrix3x2d() }
            registerReader(SimpleType.MATRIX3X3D) { readMatrix3x3d() }
            registerReader(SimpleType.MATRIX4X3D) { readMatrix4x3d() }
            registerReader(SimpleType.MATRIX4X4D) { readMatrix4x4d() }
        }
    }
}