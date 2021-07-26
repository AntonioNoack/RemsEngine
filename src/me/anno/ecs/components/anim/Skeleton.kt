package me.anno.ecs.components.anim

import me.anno.io.ISaveable
import me.anno.io.NamedSaveable
import me.anno.io.base.BaseWriter
import me.anno.mesh.assimp.Bone

class Skeleton() : NamedSaveable() {

    var bones: Array<Bone>? = null

    val animations = HashMap<String, Animation>()

    override val className: String = "Skeleton"
    override val approxSize: Int = 10

    override fun readObjectArray(name: String, values: Array<ISaveable?>) {
        when (name) {
            "bones" -> bones = values.filterIsInstance<Bone>().toTypedArray()
            else -> super.readObjectArray(name, values)
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        val bones = bones
        if (bones != null) {
            writer.writeObjectArray(this, "bones", bones)
        }
    }

}