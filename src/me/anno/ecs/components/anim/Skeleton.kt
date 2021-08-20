package me.anno.ecs.components.anim

import me.anno.ecs.annotations.Type
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.io.serialization.SerializedProperty
import me.anno.mesh.assimp.Bone

class Skeleton() : PrefabSaveable() {

    @SerializedProperty
    var bones: Array<Bone>? = null

    @Type("Map<String, Animation>")
    @SerializedProperty
    var animations = HashMap<String, Animation>()

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

    override fun clone(): PrefabSaveable {
        val clone = Skeleton()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        clone as Skeleton
        clone.animations = animations
        clone.bones = bones
    }

}