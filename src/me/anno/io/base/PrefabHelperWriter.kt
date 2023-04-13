package me.anno.io.base

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.Path
import me.anno.io.ISaveable
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.utils.structures.lists.Lists.none2
import me.anno.utils.structures.lists.Lists.pop
import org.joml.*

/**
 * typically, it's easier to create elements and scene hierarchies directly rather than creating and designing a prefab;
 *
 * this class cleans up after you: it creates a prefab from your "hacked" instance, so you can be lazy
 * */
class PrefabHelperWriter(val prefab: Prefab) : BaseWriter(false) {

    val objectStack = ArrayList<Path>()
    val doneObjects = HashSet<PrefabSaveable>()
    var head: Path = Path.ROOT_PATH

    fun run(instance: PrefabSaveable) {
        val sample = prefab._sampleInstance
        prefab._sampleInstance = null
        head = getPath(instance)
        add(instance)
        writeAllInList()
        prefab._sampleInstance = sample
    }

    fun getPath(instance: PrefabSaveable): Path {
        val parent = instance.parent ?: return Path.ROOT_PATH
        val nameId = if (instance.prefabPath != Path.ROOT_PATH) instance.prefabPath.nameId
        else Path.generateRandomId()
        val (index, type) = findType(parent, instance) ?: Pair(-1, ' ')
        return getPath(parent).added(nameId, index, type)
    }

    fun write(name: String, value: Any?) {
        prefab[head, name] = value
        println(" - set $head.$name = $value")
    }

    fun findType(parent: PrefabSaveable, instance: PrefabSaveable): Pair<Int, Char>? {
        for (type in parent.listChildTypes()) {
            val index = parent.getChildListByType(type).indexOf(instance)
            if (index >= 0) return Pair(index, type)
        }
        return null
    }

    override fun writeBoolean(name: String, value: Boolean, force: Boolean) = write(name, value)
    override fun writeBooleanArray(name: String, values: BooleanArray, force: Boolean) = write(name, values)
    override fun writeBooleanArray2D(name: String, values: Array<BooleanArray>, force: Boolean) = write(name, values)

    override fun writeChar(name: String, value: Char, force: Boolean) = write(name, value)
    override fun writeCharArray(name: String, values: CharArray, force: Boolean) = write(name, values)
    override fun writeCharArray2D(name: String, values: Array<CharArray>, force: Boolean) = write(name, values)

    override fun writeByte(name: String, value: Byte, force: Boolean) = write(name, value)
    override fun writeByteArray(name: String, values: ByteArray, force: Boolean) = write(name, values)
    override fun writeByteArray2D(name: String, values: Array<ByteArray>, force: Boolean) = write(name, values)

    override fun writeShort(name: String, value: Short, force: Boolean) = write(name, value)
    override fun writeShortArray(name: String, values: ShortArray, force: Boolean) = write(name, values)
    override fun writeShortArray2D(name: String, values: Array<ShortArray>, force: Boolean) = write(name, values)

    override fun writeInt(name: String, value: Int, force: Boolean) = write(name, value)
    override fun writeIntArray(name: String, values: IntArray, force: Boolean) = write(name, values)
    override fun writeIntArray2D(name: String, values: Array<IntArray>, force: Boolean) = write(name, values)

    override fun writeColor(name: String, value: Int, force: Boolean) = write(name, value)
    override fun writeColorArray(name: String, values: IntArray, force: Boolean) = write(name, values)
    override fun writeColorArray2D(name: String, values: Array<IntArray>, force: Boolean) = write(name, values)

    override fun writeLong(name: String, value: Long, force: Boolean) = write(name, value)
    override fun writeLongArray(name: String, values: LongArray, force: Boolean) = write(name, values)
    override fun writeLongArray2D(name: String, values: Array<LongArray>, force: Boolean) = write(name, values)

    override fun writeFloat(name: String, value: Float, force: Boolean) = write(name, value)
    override fun writeFloatArray(name: String, values: FloatArray?, force: Boolean) = write(name, values)
    override fun writeFloatArray2D(name: String, values: Array<FloatArray>?, force: Boolean) = write(name, values)

    override fun writeDouble(name: String, value: Double, force: Boolean) = write(name, value)
    override fun writeDoubleArray(name: String, values: DoubleArray, force: Boolean) = write(name, values)
    override fun writeDoubleArray2D(name: String, values: Array<DoubleArray>, force: Boolean) = write(name, values)

    override fun writeString(name: String, value: String?, force: Boolean) = write(name, value)
    override fun writeStringArray(name: String, values: Array<String>, force: Boolean) = write(name, values)
    override fun writeStringArray2D(name: String, values: Array<Array<String>>, force: Boolean) = write(name, values)

    override fun writeVector2f(name: String, value: Vector2f, force: Boolean) = write(name, value)
    override fun writeVector3f(name: String, value: Vector3f, force: Boolean) = write(name, value)
    override fun writeVector4f(name: String, value: Vector4f, force: Boolean) = write(name, value)
    override fun writeVector2fArray(name: String, values: Array<Vector2f>, force: Boolean) = write(name, values)
    override fun writeVector3fArray(name: String, values: Array<Vector3f>, force: Boolean) = write(name, values)
    override fun writeVector4fArray(name: String, values: Array<Vector4f>, force: Boolean) = write(name, values)

    override fun writeVector2d(name: String, value: Vector2d, force: Boolean) = write(name, value)
    override fun writeVector3d(name: String, value: Vector3d, force: Boolean) = write(name, value)
    override fun writeVector4d(name: String, value: Vector4d, force: Boolean) = write(name, value)
    override fun writeVector2dArray(name: String, values: Array<Vector2d>, force: Boolean) = write(name, values)
    override fun writeVector3dArray(name: String, values: Array<Vector3d>, force: Boolean) = write(name, values)
    override fun writeVector4dArray(name: String, values: Array<Vector4d>, force: Boolean) = write(name, values)

    override fun writeVector2i(name: String, value: Vector2i, force: Boolean) = write(name, value)
    override fun writeVector3i(name: String, value: Vector3i, force: Boolean) = write(name, value)
    override fun writeVector4i(name: String, value: Vector4i, force: Boolean) = write(name, value)
    override fun writeVector2iArray(name: String, values: Array<Vector2i>, force: Boolean) = write(name, values)
    override fun writeVector3iArray(name: String, values: Array<Vector3i>, force: Boolean) = write(name, values)
    override fun writeVector4iArray(name: String, values: Array<Vector4i>, force: Boolean) = write(name, values)

    override fun writeMatrix2x2f(name: String, value: Matrix2f, force: Boolean) = write(name, value)
    override fun writeMatrix3x2f(name: String, value: Matrix3x2f, force: Boolean) = write(name, value)
    override fun writeMatrix3x3f(name: String, value: Matrix3f, force: Boolean) = write(name, value)
    override fun writeMatrix4x3f(name: String, value: Matrix4x3f, force: Boolean) = write(name, value)
    override fun writeMatrix4x4f(name: String, value: Matrix4f, force: Boolean) = write(name, value)

    override fun writeMatrix2x2fArray(name: String, values: Array<Matrix2f>, force: Boolean) = write(name, values)
    override fun writeMatrix3x2fArray(name: String, values: Array<Matrix3x2f>, force: Boolean) = write(name, values)
    override fun writeMatrix3x3fArray(name: String, values: Array<Matrix3f>, force: Boolean) = write(name, values)
    override fun writeMatrix4x3fArray(name: String, values: Array<Matrix4x3f>, force: Boolean) = write(name, values)
    override fun writeMatrix4x4fArray(name: String, values: Array<Matrix4f>, force: Boolean) = write(name, values)

    override fun writeMatrix2x2fArray2D(name: String, values: Array<Array<Matrix2f>>, force: Boolean) =
        write(name, values)

    override fun writeMatrix3x2fArray2D(name: String, values: Array<Array<Matrix3x2f>>, force: Boolean) =
        write(name, values)

    override fun writeMatrix3x3fArray2D(name: String, values: Array<Array<Matrix3f>>, force: Boolean) =
        write(name, values)

    override fun writeMatrix4x3fArray2D(name: String, values: Array<Array<Matrix4x3f>>, force: Boolean) =
        write(name, values)

    override fun writeMatrix4x4fArray2D(name: String, values: Array<Array<Matrix4f>>, force: Boolean) =
        write(name, values)

    override fun writeMatrix2x2d(name: String, value: Matrix2d, force: Boolean) = write(name, value)
    override fun writeMatrix3x2d(name: String, value: Matrix3x2d, force: Boolean) = write(name, value)
    override fun writeMatrix3x3d(name: String, value: Matrix3d, force: Boolean) = write(name, value)
    override fun writeMatrix4x3d(name: String, value: Matrix4x3d, force: Boolean) = write(name, value)
    override fun writeMatrix4x4d(name: String, value: Matrix4d, force: Boolean) = write(name, value)

    override fun writeMatrix2x2dArray(name: String, values: Array<Matrix2d>, force: Boolean) = write(name, values)
    override fun writeMatrix3x2dArray(name: String, values: Array<Matrix3x2d>, force: Boolean) = write(name, values)
    override fun writeMatrix3x3dArray(name: String, values: Array<Matrix3d>, force: Boolean) = write(name, values)
    override fun writeMatrix4x3dArray(name: String, values: Array<Matrix4x3d>, force: Boolean) = write(name, values)
    override fun writeMatrix4x4dArray(name: String, values: Array<Matrix4d>, force: Boolean) = write(name, values)
    override fun writeMatrix2x2dArray2D(name: String, values: Array<Array<Matrix2d>>, force: Boolean) =
        write(name, values)

    override fun writeMatrix3x2dArray2D(name: String, values: Array<Array<Matrix3x2d>>, force: Boolean) =
        write(name, values)

    override fun writeMatrix3x3dArray2D(name: String, values: Array<Array<Matrix3d>>, force: Boolean) =
        write(name, values)

    override fun writeMatrix4x3dArray2D(name: String, values: Array<Array<Matrix4x3d>>, force: Boolean) =
        write(name, values)

    override fun writeMatrix4x4dArray2D(name: String, values: Array<Array<Matrix4d>>, force: Boolean) =
        write(name, values)

    override fun writeQuaternionf(name: String, value: Quaternionf, force: Boolean) = write(name, value)
    override fun writeQuaterniond(name: String, value: Quaterniond, force: Boolean) = write(name, value)
    override fun writeQuaternionfArray(name: String, values: Array<Quaternionf>, force: Boolean) = write(name, values)
    override fun writeQuaternionfArray2D(name: String, values: Array<Array<Quaternionf>>, force: Boolean) =
        write(name, values)

    override fun writeQuaterniondArray(name: String, values: Array<Quaterniond>, force: Boolean) = write(name, values)
    override fun writeQuaterniondArray2D(name: String, values: Array<Array<Quaterniond>>, force: Boolean) =
        write(name, values)

    override fun writeAABBf(name: String, value: AABBf, force: Boolean) = write(name, value)
    override fun writeAABBd(name: String, value: AABBd, force: Boolean) = write(name, value)
    override fun writePlanef(name: String, value: Planef, force: Boolean) = write(name, value)
    override fun writePlaned(name: String, value: Planed, force: Boolean) = write(name, value)
    override fun writeFile(name: String, value: FileReference?, force: Boolean, workspace: FileReference?) =
        write(name, value)

    override fun writeFileArray(name: String, values: Array<FileReference>, force: Boolean, workspace: FileReference?) =
        write(name, values)

    override fun writeNull(name: String?) = write(name!!, null)
    override fun writePointer(name: String?, className: String, ptr: Int, value: ISaveable) = write(name!!, value)

    override fun writeObjectImpl(name: String?, value: ISaveable) {
        if (value is PrefabSaveable) {
            if (doneObjects.add(value)) {
                // push to stack
                // todo is this good enough? mmh...
                val path = getPath(value)
                val parentsPath = path.parent ?: Path.ROOT_PATH
                val nameId = path.nameId
                if (value.prefabPath != Path.ROOT_PATH && prefab.adds.none2 { it.path == parentsPath && it.nameId == nameId }) {
                    val (_, type) = findType(value.parent!!, value) ?: Pair(-1, ' ')
                    val add = CAdd(
                        parentsPath, type, value.className, nameId,
                        if (value.prefab === prefab) InvalidRef else
                        // is this correct???
                            value.prefab?.source ?: InvalidRef
                    )
                    (prefab.adds as MutableList).add(add)
                    println(" - added $add")
                }
                objectStack.add(parentsPath)
                head = path
                value.save(this)
                head = objectStack.pop()!!
            }
            if (name != null) {
                // assign the value... by saving its path
                write(name, value.prefabPath)
            }
        } else if (name != null) write(name, value)
    }

    override fun <V : ISaveable?> writeNullableObjectArray(
        self: ISaveable?,
        name: String,
        values: Array<V>?,
        force: Boolean
    ) {
        write(name, values)
        for (v in values ?: return) {
            if (v is PrefabSaveable) {
                writeObjectImpl(null, v)
            }
        }
    }

    override fun <V : ISaveable> writeNullableObjectList(
        self: ISaveable?,
        name: String,
        values: List<V?>?,
        force: Boolean
    ) {
        if (self !is PrefabSaveable || self.listChildTypes().all { self.getChildListByType(it) !== values })
            write(name, values)
        for (v in values ?: return) {
            if (v is PrefabSaveable) {
                writeObjectImpl(null, v)
            }
        }
    }

    override fun <V : ISaveable> writeObjectArray(self: ISaveable?, name: String, values: Array<V>?, force: Boolean) {
        writeNullableObjectArray(self, name, values, force)
    }

    override fun <V : ISaveable> writeObjectArray2D(
        self: ISaveable?,
        name: String,
        values: Array<Array<V>>,
        force: Boolean
    ) {
        write(name, values)
        for (w in values) {
            for (v in w) {
                if (v is PrefabSaveable) {
                    writeObjectImpl(null, v)
                }
            }
        }
    }

    override fun <V : ISaveable?> writeHomogenousObjectArray(
        self: ISaveable?,
        name: String,
        values: Array<V>,
        force: Boolean
    ) = writeNullableObjectArray(self, name, values, force)

    override fun writeListStart() {}
    override fun writeListEnd() {}
    override fun writeListSeparator() {}

}