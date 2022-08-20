package me.anno.ecs.components.light

import me.anno.ecs.Entity
import me.anno.ecs.annotations.Type
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.pipeline.Pipeline
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Vector3d
import org.joml.Vector3f

class AmbientLight : LightComponentBase() {

    @Type("Color3HDR")
    var color = Vector3f(0.1f)

    override fun fill(
        pipeline: Pipeline,
        entity: Entity,
        clickId: Int,
        cameraPosition: Vector3d,
        worldScale: Double
    ): Int {
        pipeline.ambient.add(color)
        return clickId // not itself clickable
    }

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

    override val className = "AmbientLight"

}