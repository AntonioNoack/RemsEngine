package me.anno.ecs.components.light

import me.anno.ecs.Entity
import me.anno.ecs.annotations.Type
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.pipeline.Pipeline
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Vector3f

class AmbientLight : LightComponentBase() {

    @Type("Color3HDR")
    var color = Vector3f(0.5f)

    override fun fill(
        pipeline: Pipeline,
        entity: Entity,
        clickId: Int
    ): Int {
        pipeline.ambient.add(color)
        return super.fill(pipeline, entity, clickId)
    }

    override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean {
        aabb.all()
        return true
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as AmbientLight
        dst.color = color
    }

    override val className: String get() = "AmbientLight"

}