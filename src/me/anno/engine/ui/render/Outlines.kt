package me.anno.engine.ui.render

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.RendererComponent
import me.anno.gpu.ShaderLib
import me.anno.gpu.pipeline.M4x3Delta.m4x3delta
import org.joml.Matrix4d

object Outlines {

    private val tmpMat4d = Matrix4d()

    fun drawOutline(entity: Entity, worldScale: Double) {
        val children = entity.children
        for (i in children.indices) {
            drawOutline(children[i], worldScale)
        }
        val renderer = entity.getComponent(RendererComponent::class, false)
        if (renderer != null) {
            val components = entity.components
            for (i in components.indices) {
                val component = components[i]
                if (component is MeshComponent) {
                    drawOutline(renderer, component, worldScale)
                }
            }
        }
    }

    fun drawOutline(renderer: RendererComponent?, component: MeshComponent, worldScale: Double) {

        val mesh = component.mesh ?: return
        val entity = component.entity ?: return
        val transform = entity.transform.drawTransform
        val camPosition = RenderView.camPosition

        // scale based on visual scale, or use a geometry shader for that
        val joinedTransform = tmpMat4d.set(RenderView.viewTransform)
            .translate(-camPosition.x, -camPosition.y, -camPosition.z)
            .mul(transform)
        // = tmpMat4d.set(viewTransform).mul4x3delta(transform, camPosition, 1.0)
        val aabb = mesh.aabb
        val scaledMin = RenderView.scaledMin.set(+1.0)
        val scaledMax = RenderView.scaledMax.set(-1.0)
        val v = RenderView.tmpVec4f
        for (i in 0 until 8) {
            v.set(
                (if ((i and 1) != 0) aabb.minX else aabb.maxX).toDouble(),
                (if ((i and 2) != 0) aabb.minY else aabb.maxY).toDouble(),
                (if ((i and 4) != 0) aabb.minZ else aabb.maxZ).toDouble(),
                1.0
            )
            joinedTransform.transform(v)
            v.div(v.w)
            // clamp to screen?
            scaledMax.max(v)
            scaledMin.min(v)
        }

        scaledMax.sub(scaledMin)

        if (scaledMax.x.isNaN()) {
            println("Outlines issue: $scaledMax from ${RenderView.viewTransform} from $joinedTransform")
        }

        val scale = 1f + 0.01f / ((scaledMax.x + scaledMax.y).toFloat() * 0.5f)

        if (scale < 1e10f) {

            // todo if not outline, respect the materials
            // todo or always respect them, and just override the textures?
            val baseShader = ShaderLib.monochromeModelShader
            val shader = baseShader.value
            shader.use()
            shader.m4x4("transform", RenderView.viewTransform)

            shader.m4x3delta("localTransform", transform, camPosition, worldScale, scale)
            shader.v4("tint", -1)
            if (renderer != null) {
                renderer.defineVertexTransform(shader, entity, mesh)
            } else shader.v1("hasAnimation", 0f)
            // todo all other shader properties...
            // todo we should write a function for that generally
            mesh.draw(shader, 0)

        }

    }

}