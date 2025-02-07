package me.anno.ecs.systems

import me.anno.ecs.Component
import me.anno.ecs.System
import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.interfaces.Renderable
import me.anno.gpu.pipeline.Pipeline
import me.anno.utils.structures.Collections.setContains

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

    override fun setContains(component: Component, contains: Boolean) {
        when (component) {
            is MeshComponent -> meshes.setContains(component, contains)
            is MeshComponentBase -> others.setContains(component, contains)
        }
    }

    override fun fill(pipeline: Pipeline, transform: Transform) {

        for (c in meshes) {
            val e = c.entity ?: continue
            if (pipeline.frustum.isVisible(e.getGlobalBounds())) {
                val mesh = c.getMesh() ?: continue
                c.clickId = pipeline.getClickId(e.getGlobalBounds())
                pipeline.addMesh(mesh, c, e.transform)
            }
        }
        for (c in others) {
            val e = c.entity ?: continue
            if (pipeline.frustum.isVisible(e.getGlobalBounds())) {
                 c.fill(pipeline, transform)
            }
        }
    }

    override fun clear() {
        meshes.clear()
        others.clear()
    }
}