package me.anno.ecs.systems

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.System
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.interfaces.Renderable
import me.anno.gpu.pipeline.Pipeline

/**
 * todo like for lights: spatial acceleration structure
 * todo instanced/non-instanced meshes
 * todo static meshes -> static mesh manager, by area for LODs/skipping small things (?)
 * todo dynamic LODs for the static mesh manager???
 *
 * todo use this system for testing on the school scene with 20kE+8kC
 * */
class MeshRenderSystem : System(), Renderable {

    val meshes = HashSet<MeshComponent>(2048)
    val others = HashSet<MeshComponentBase>(64)

    override fun onEnable(component: Component) {
        when (component) {
            is MeshComponent -> meshes.add(component)
            is MeshComponentBase -> others.add(component)
        }
    }

    override fun onEnable(childSystem: System) {
        if (childSystem is MeshRenderSystem) {
            meshes.addAll(childSystem.meshes)
            others.addAll(childSystem.others)
        }
    }

    override fun onDisable(childSystem: System) {
        if (childSystem is MeshRenderSystem) {
            meshes.removeAll(childSystem.meshes)
            others.removeAll(childSystem.others)
        }
    }

    override fun fill(pipeline: Pipeline, entity: Entity, clickId: Int): Int {
        var clickIdI = 0
        for (c in meshes) {
            val e = c.entity ?: continue
            if (pipeline.frustum.isVisible(e.aabb)) {
                val mesh = c.getMesh() ?: continue
                c.clickId = clickIdI++
                pipeline.addMesh(mesh, c, e)
            }
        }
        for (c in others) {
            val e = c.entity ?: continue
            if (pipeline.frustum.isVisible(e.aabb)) {
                clickIdI = c.fill(pipeline, entity, clickIdI)
            }
        }
        return clickIdI
    }
}