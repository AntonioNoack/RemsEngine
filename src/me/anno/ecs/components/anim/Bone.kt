package me.anno.ecs.components.anim

import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.base.BaseWriter
import org.joml.Matrix4x3f
import org.joml.Vector3f

/**
 * part of a skeleton (for skeletal animations);
 * children are implicit at the moment, via parentId
 * */
class Bone(var id: Int, var parentId: Int, name: String) : PrefabSaveable() {

    constructor() : this(-1, -1, "")

    init {
        this.name = name
    }

    // parent is unknown, maybe be indirect...
    // var parent: Bone? = null

    /**
     * transformation relative to parent
     * */
    @DebugProperty
    val relativeTransform = Matrix4x3f()

    /**
     * bone space to mesh space in bind pose
     * */
    @DebugProperty
    val originalTransform = Matrix4x3f()

    /**
     * offsetMatrix; inverse of bone position + rotation in mesh space
     * */
    @DebugProperty
    val inverseBindPose = Matrix4x3f()

    /**
     * inverseBindPose.m30, inverseBindPose.m31, inverseBindPose.m32
     * */
    @DebugProperty
    val offsetVector = Vector3f()

    /**
     * = inverseBindPose.invert()
     *
     * bone position + rotation in mesh space
     * */
    @DebugProperty
    val bindPose = Matrix4x3f()

    /**
     * bindPose.m30, bindPose.m31, bindPose.m32
     * */
    @DebugProperty
    val bindPosition = Vector3f()

    fun setBindPose(m: Matrix4x3f) {
        bindPose.set(m)
        bindPosition.set(m.m30, m.m31, m.m32)
        m.invert(inverseBindPose)
        offsetVector.set(inverseBindPose.m30, inverseBindPose.m31, inverseBindPose.m32)
    }

    fun hasBoneInHierarchy(name: String, bones: List<Bone>): Boolean {
        if (name == this.name) return true
        val parent = getParent(bones) ?: return false
        return parent.hasBoneInHierarchy(name, bones)
    }

    fun length(bones: List<Bone>): Float {
        val parent = getParent(bones) ?: return 0f
        return bindPosition.distance(parent.bindPosition)
    }

    fun getParent(bones: List<Bone>): Bone? {
        return bones.getOrNull(parentId)
    }

    override val approxSize get() = 1

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeInt("id", id)
        writer.writeInt("parentId", parentId, true)
        writer.writeMatrix4x3f("bindPose", bindPose)
        writer.writeMatrix4x3f("relativeTransform", relativeTransform)
        writer.writeMatrix4x3f("originalTransform", originalTransform)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "id" -> id = value as? Int ?: return
            "parentId" -> parentId = value as? Int ?: return
            "bindPose" -> setBindPose(value as? Matrix4x3f ?: return)
            "relativeTransform" -> relativeTransform.set(value as? Matrix4x3f ?: return)
            "originalTransform" -> originalTransform.set(value as? Matrix4x3f ?: return)
            else -> super.setProperty(name, value)
        }
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is Bone) return
        dst.id = id
        dst.parentId = parentId
        dst.inverseBindPose.set(inverseBindPose)
        dst.relativeTransform.set(relativeTransform)
        dst.originalTransform.set(originalTransform)
    }
}