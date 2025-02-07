package me.anno.ecs.systems

import me.anno.ecs.Component
import me.anno.ecs.System
import me.anno.ecs.Transform
import me.anno.ecs.interfaces.Renderable
import me.anno.gpu.pipeline.Pipeline
import me.anno.input.Input
import me.anno.utils.structures.sets.FastIteratorSet

/**
 * todo like for lights: spatial acceleration structure
 * todo static meshes -> static mesh manager, by area for LODs/skipping small things (?)
 * todo dynamic LODs for the static mesh manager???
 *
 * use this system for testing on the school scene with 20kE+8kC ->
 * I tested it with an easier scene (disable world.fill() in Pipeline!), and it is slightly faster with many meshes on screen,
 * but also slower when lots is in the scene, but little is visible (a typical scenario for open world)
 * */
object MeshRenderSystem : System(), Renderable {

    val renderables = FastIteratorSet<Renderable>()

    override fun setContains(component: Component, contains: Boolean) {
        if (component !is Renderable) return
        renderables.setContains(component, contains)
    }

    override fun fill(pipeline: Pipeline, transform: Transform) {

        if (Input.isShiftDown) {
            (Systems.world as? Renderable)?.fill(pipeline, transform)
            return
        }

        // sort renderables by class?
        val renderables = renderables.asList()
        for (i in renderables.indices) {
            val renderable = renderables[i]
            val transform1 = (renderable as? Component)?.transform ?: continue
            val bounds = renderable.getGlobalBounds()
            if (bounds == null || pipeline.frustum.isVisible(bounds)) {
                renderable.fill(pipeline, transform1)
            }
        }
    }

    override fun clear() {
        renderables.clear()
    }
}