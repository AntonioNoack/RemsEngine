package me.anno.mesh.assimp

import me.anno.io.NamedSaveable
import me.anno.io.base.BaseWriter
import org.joml.Matrix4f
import org.joml.Matrix4x3f
import org.joml.Vector3f

class Bone(var id: Int, var parentId: Int, name: String) : NamedSaveable() {

    constructor() : this(-1, -1, "")

    init {
        this.name = name
    }

    // parent is unknown, maybe be indirect...
    // var parent: Bone? = null

    val relativeTransform = Matrix4x3f()

    val originalTransform = Matrix4x3f()

    val inverseBindPose = Matrix4x3f() // offsetMatrix; inverse of bone position + rotation
    val offsetVector = Vector3f() // inverseBindPose.m30(), inverseBindPose.m31(), inverseBindPose.m32()

    val bindPose = Matrix4x3f() // = inverseBindPose.invert()
    val bindPosition = Vector3f() // bindPose.m30(), bindPose.m31(), bindPose.m32()

    fun setBindPose(m: Matrix4f) {
        bindPose.set(m)
        bindPosition.set(m.m30(), m.m31(), m.m32())
        calculateInverseBindPose()
    }

    fun setBindPose(m: Matrix4x3f) {
        bindPose.set(m)
        bindPosition.set(m.m30(), m.m31(), m.m32())
        calculateInverseBindPose()
    }

    fun setInverseBindPose(m: Matrix4f) {
        inverseBindPose.set(m)
        offsetVector.set(m.m30(), m.m31(), m.m32())
        calculateBindPose()
    }

    fun setInverseBindPose(m: Matrix4x3f) {
        inverseBindPose.set(m)
        offsetVector.set(m.m30(), m.m31(), m.m32())
        calculateBindPose()
    }

    fun calculateInverseBindPose() {
        inverseBindPose.set(bindPose).invert()
        offsetVector.set(inverseBindPose.m30(), inverseBindPose.m31(), inverseBindPose.m32())
    }

    fun calculateBindPose() {
        bindPose.set(inverseBindPose).invert()
        bindPosition.set(bindPose.m30(), bindPose.m31(), bindPose.m32())
    }

    override val className: String = "Bone"
    override val approxSize: Int = 1

    override fun readInt(name: String, value: Int) {
        when (name) {
            "id" -> id = value
            "parentId" -> parentId = value
            else -> super.readInt(name, value)
        }
    }

    override fun readMatrix4x3f(name: String, value: Matrix4x3f) {
        when (name) {
            "offset" -> setInverseBindPose(value)
            "relativeTransform" -> relativeTransform.set(value)
            "originalTransform" -> originalTransform.set(value)
            else -> super.readMatrix4x3f(name, value)
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeInt("id", id)
        writer.writeInt("parentId", parentId, true)
        writer.writeMatrix4x3f("offset", inverseBindPose)
        writer.writeMatrix4x3f("relativeTransform", relativeTransform)
        writer.writeMatrix4x3f("originalTransform", originalTransform)
    }

}
