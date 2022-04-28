package me.anno.ecs.components.light

import me.anno.ecs.annotations.Type
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.utils.types.AABBs.all
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Vector3f

class AmbientLight : LightComponentBase() {

    @Type("Color3HDR")
    var color = Vector3f(0.1f)

    override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean {
        aabb.all()
        return true
    }

    override fun clone(): AmbientLight {
        val clone = AmbientLight()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as AmbientLight
        clone.color = color
    }

    override val className: String = "AmbientLight"

}