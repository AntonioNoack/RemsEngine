package me.anno.io.base

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.prefab.change.Path
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.saveable.Saveable
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4d
import org.joml.Vector4f

/**
 * typically, it's easier to create elements and scene hierarchies directly rather than creating and designing a prefab;
 *
 * this class cleans up after you: it creates a prefab from your "hacked" instance, so you can be lazy
 * */
class PrefabHelperWriter(val prefab: Prefab) : BaseWriter(false) {

    val doneObjects = HashSet<PrefabSaveable>()
    var currentPath: Path = Path.ROOT_PATH

    fun run(instance: PrefabSaveable) {
        val sample = prefab._sampleInstance
        prefab._sampleInstance = null
        currentPath = instance.prefabPath
        writeObjectImpl(instance)
        prefab._sampleInstance = sample
    }

    fun write(name: String, value: Any?) {
        prefab.sets.setUnsafe(currentPath, name, value)
    }

    override fun writeSomething(name: String, value: Any?, force: Boolean) {
        write(name, value)
    }

    override fun writeBoolean(name: String, value: Boolean, force: Boolean) {
        if (force || value) write(name, value)
    }

    override fun writeByte(name: String, value: Byte, force: Boolean) {
        if (force || value.toInt() != 0) write(name, value)
    }

    override fun writeShort(name: String, value: Short, force: Boolean) {
        if (force || value.toInt() != 0) write(name, value)
    }

    override fun writeInt(name: String, value: Int, force: Boolean) {
        if (force || value != 0) write(name, value)
    }

    override fun writeLong(name: String, value: Long, force: Boolean) {
        if (force || value != 0L) write(name, value)
    }

    override fun writeFloat(name: String, value: Float, force: Boolean) {
        if (force || value != 0f) write(name, value)
    }

    override fun writeString(name: String, value: String, force: Boolean) {
        if (force || value != "") write(name, value)
    }

    override fun writeVector2f(name: String, value: Vector2f, force: Boolean) {
        if (force || value.x != 0f || value.y != 0f) write(name, value)
    }

    override fun writeVector3f(name: String, value: Vector3f, force: Boolean) {
        if (force || value.x != 0f || value.y != 0f || value.z != 0f) write(name, value)
    }

    override fun writeVector4f(name: String, value: Vector4f, force: Boolean) {
        if (force || value.x != 0f || value.y != 0f || value.z != 0f || value.w != 0f) write(name, value)
    }

    override fun writeVector2d(name: String, value: Vector2d, force: Boolean) {
        if (force || value.x != 0.0 || value.y != 0.0) write(name, value)
    }

    override fun writeVector3d(name: String, value: Vector3d, force: Boolean) {
        if (force || value.x != 0.0 || value.y != 0.0 || value.z != 0.0) write(name, value)
    }

    override fun writeVector4d(name: String, value: Vector4d, force: Boolean) {
        if (force || value.x != 0.0 || value.y != 0.0 || value.z != 0.0 || value.w != 0.0) write(name, value)
    }

    override fun writeQuaternionf(name: String, value: Quaternionf, force: Boolean) {
        if (force || value.x != 0f || value.y != 0f || value.z != 0f || value.w != 1f) write(name, value)
    }

    override fun writeQuaternionfList2D(name: String, values: List<List<Quaternionf>>, force: Boolean) =
        write(name, values)

    override fun writeQuaterniond(name: String, value: Quaterniond, force: Boolean) {
        if (force || value.x != 0.0 || value.y != 0.0 || value.z != 0.0 || value.w != 1.0) write(name, value)
    }

    override fun writeFile(name: String, value: FileReference, force: Boolean, workspace: FileReference) {
        if (force || value != InvalidRef) write(name, value)
    }

    override fun writeNull(name: String?) = write(name!!, null)
    override fun writePointer(name: String?, className: String, ptr: Int, value: Saveable) = write(name!!, value)

    override fun writeObjectImpl(name: String?, value: Saveable) {
        if (value is PrefabSaveable) {
            writeObjectImpl(value)
            if (name != null) {
                // assign the value... by saving its path
                write(name, value.prefabPath)
            }
        } else if (name != null) write(name, value)
    }

    fun writeObjectImpl(value: PrefabSaveable) {
        if (doneObjects.add(value)) {
            val lastPath = currentPath
            currentPath = value.prefabPath
            if (!Saveable.isRegistered(value.className)) {
                Saveable.registerCustomClass(value)
            }
            prefab.sets.clear(currentPath)
            value.save(this)
            currentPath = lastPath
        }
    }

    override fun <V : Saveable?> writeNullableObjectList(
        self: Saveable?, name: String,
        values: List<V>, force: Boolean
    ) {
        if (self !is PrefabSaveable || self.listChildTypes().all { self.getChildListByType(it) !== values }) {
            write(name, values)
            for (value in values) {
                if (value is PrefabSaveable) {
                    writeObjectImpl(value)
                }
            }
        }
    }

    override fun <V : Saveable> writeObjectList(self: Saveable?, name: String, values: List<V>, force: Boolean) {
        writeNullableObjectList(self, name, values, force)
    }

    override fun <V : Saveable> writeObjectList2D(
        self: Saveable?, name: String, values: List<List<V>>, force: Boolean
    ) {
        write(name, values)
        for (values1d in values) {
            for (value in values1d) {
                if (value is PrefabSaveable) {
                    writeObjectImpl(value)
                }
            }
        }
    }

    override fun <V : Saveable?> writeHomogenousObjectList(
        self: Saveable?, name: String, values: List<V>, force: Boolean
    ) {
        writeNullableObjectList(self, name, values, force)
    }

    override fun writeListStart() {}
    override fun writeListEnd() {}
    override fun writeListSeparator() {}
}