package me.anno.ecs.components.anim

import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.base.BaseWriter
import org.joml.Matrix4x3f
import org.joml.Vector3f

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

    override val className: String get() = "Bone"
    override val approxSize get() = 1

    override fun readInt(name: String, value: Int) {
        when (name) {
            "id" -> id = value
            "parentId" -> parentId = value
            else -> super.readInt(name, value)
        }
    }

    override fun readMatrix4x3f(name: String, value: Matrix4x3f) {
        when (name) {
            "bindPose" -> setBindPose(value)
            "relativeTransform" -> relativeTransform.set(value)
            "originalTransform" -> originalTransform.set(value)
            else -> super.readMatrix4x3f(name, value)
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeInt("id", id)
        writer.writeInt("parentId", parentId, true)
        writer.writeMatrix4x3f("bindPose", bindPose)
        writer.writeMatrix4x3f("relativeTransform", relativeTransform)
        writer.writeMatrix4x3f("originalTransform", originalTransform)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as Bone
        dst.id = id
        dst.parentId = parentId
        dst.inverseBindPose.set(inverseBindPose)
        dst.relativeTransform.set(relativeTransform)
        dst.originalTransform.set(originalTransform)
    }

}