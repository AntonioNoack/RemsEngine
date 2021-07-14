package me.anno.mesh.assimp

import me.anno.io.NamedSaveable
import me.anno.io.base.BaseWriter
import org.joml.Matrix4f
import org.joml.Matrix4x3f
import org.joml.Vector3f

// todo assign an entity for components, which can be assigned to that? -> we would need to clone the skeleton and bone...
class Bone(
    var id: Int, name: String,
    val offsetMatrix: Matrix4x3f // inverse bind pose
) : NamedSaveable() {

    constructor() : this(-1, "", Matrix4x3f())

    init {
        this.name = name
    }

    // parent is unknown, maybe be indirect...
    // var parent: Bone? = null

    val offsetVector get() = Vector3f(offsetMatrix.m30(), offsetMatrix.m31(), offsetMatrix.m32())

    val tmpOffset = Matrix4f()
    val tmpTransform = Matrix4f()

    override val className: String = "Bone"
    override val approxSize: Int = 1

    override fun readInt(name: String, value: Int) {
        when (name) {
            "id" -> id = value
            else -> super.readInt(name, value)
        }
    }

    override fun readMatrix4x3f(name: String, value: Matrix4x3f) {
        when (name) {
            "offset" -> offsetMatrix.set(value)
            else -> super.readMatrix4x3f(name, value)
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeInt("id", id)
        writer.writeMatrix4x3f("offset", offsetMatrix)
    }

}
