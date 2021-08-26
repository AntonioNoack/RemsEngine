package me.anno.engine.ui.render

import me.anno.ecs.Entity
import me.anno.ecs.components.cache.MeshCache
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.RendererComponent
import me.anno.gpu.ShaderLib
import me.anno.gpu.TextureLib.whiteTexture
import me.anno.gpu.pipeline.M4x3Delta.m4x3delta
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4d

object Outlines {

    private val LOGGER = LogManager.getLogger(Outlines::class)

    private val tmpMat4d = Matrix4d()

    fun drawOutline(entity: Entity, worldScale: Double) {
        whiteTexture.bind(0) // for the albedo
        drawOutlineInternally(entity, worldScale)
    }

    private fun drawOutlineInternally(entity: Entity, worldScale: Double) {
        val children = entity.children
        for (i in children.indices) {
            drawOutlineInternally(children[i], worldScale)
        }
        val renderer = entity.getComponent(RendererComponent::class, false)
        if (renderer != null) {
            val components = entity.components
            for (i in components.indices) {
                val component = components[i]
                if (component is MeshComponent) {
                    val mesh = MeshCache[component.mesh, true] ?: continue
                    drawOutline(renderer, component, mesh, worldScale)
                }
            }
        }
    }

    fun drawOutline(renderer: RendererComponent?, meshComponent: MeshComponent, mesh: Mesh, worldScale: Double) {

        val entity = meshComponent.entity ?: return
        val transform0 = entity.transform

        // important, when the object is off-screen, but the outline is not
        // (can happen, because the outline is slightly larger)
        transform0.drawDrawingLerpFactor()

        val transform = transform0.drawTransform
        val camPosition = RenderView.camPosition

        // scale based on visual scale, or use a geometry shader for that
        val joinedTransform = tmpMat4d.set(RenderView.cameraMatrix)
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
            LOGGER.info("Outlines issue: $scaledMax from ${RenderView.cameraMatrix} * translate($camPosition) * $transform")
        }

        val scale = 1f + 0.01f / ((scaledMax.x + scaledMax.y).toFloat() * 0.5f)

        if (scale < 1e10f) {

            // todo if not outline, respect the materials
            // todo or always respect them, and just override the textures?
            val baseShader = ShaderLib.monochromeModelShader
            val shader = baseShader.value
            shader.use()
            shader.m4x4("transform", RenderView.cameraMatrix)

            shader.m4x3delta("localTransform", transform, camPosition, worldScale, scale)
            shader.v4("tint", -1)

            if (renderer != null) {
                renderer.defineVertexTransform(shader, entity, mesh)
            } else shader.v1("hasAnimation", false)

            mesh.draw(shader, 0)

        }

    }

}