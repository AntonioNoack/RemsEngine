package me.anno.engine.ui.render

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.gpu.OpenGL
import me.anno.gpu.pipeline.CullMode
import me.anno.gpu.pipeline.M4x3Delta.m4x3delta
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.AABBs.avgX
import me.anno.utils.types.AABBs.avgY
import me.anno.utils.types.AABBs.avgZ
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
        val components = entity.components
        for (i in components.indices) {
            val component = components[i]
            if (component is MeshComponentBase) {
                val mesh = component.getMesh() ?: continue
                drawOutline(component, mesh, worldScale)
            }
        }
    }

    fun drawOutline(meshComponent: MeshComponentBase, mesh: Mesh, worldScale: Double) {

        // todo respect alpha somehow?

        val entity = meshComponent.entity ?: return
        val transform0 = entity.transform

        val transform = transform0.getDrawMatrix()
        val camPosition = RenderView.camPosition

        // scale based on visual scale, or use a geometry shader for that
        val joinedTransform = tmpMat4d.set(RenderView.cameraMatrix)
            .translate(-camPosition.x, -camPosition.y, -camPosition.z)
            .mul(transform)
        // = tmpMat4d.set(viewTransform).mul4x3delta(transform, camPosition, 1.0)
        val aabb = mesh.aabb
        val scaledMin = RenderView.scaledMin.set(+1.0)
        val scaledMax = RenderView.scaledMax.set(-1.0)
        val v = RenderView.tmpVec4d
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
            // LOGGER.warn("Outlines issue: $scaledMax from \n${RenderView.cameraMatrix} * translate($camPosition) * $transform")
            return
        }

        val scaleExtra = 0.01f / ((scaledMax.x + scaledMax.y).toFloat() * 0.5f)
        val scale = 1f + scaleExtra

        // the mesh may not be centered:
        // center the outline-mesh
        val offsetCorrectedTransform = JomlPools.mat4x3d.borrow().set(transform)
        val fac = -scaleExtra.toDouble()
        // translate local or global?
        offsetCorrectedTransform.translate(aabb.avgX() * fac, aabb.avgY() * fac, aabb.avgZ() * fac)

        if (scale < 1e10f) {
            val cullMode = if (mesh.inverseOutline) CullMode.BACK else CullMode.FRONT
            OpenGL.cullMode.use(cullMode) {
                val baseShader = ShaderLib.monochromeModelShader
                val shader = baseShader.value
                shader.use()
                shader.m4x4("transform", RenderView.cameraMatrix)

                shader.m4x3delta("localTransform", offsetCorrectedTransform, camPosition, worldScale, scale)
                shader.v1f("worldScale", worldScale.toFloat())
                shader.v4f("tint", -1)

                meshComponent.defineVertexTransform(shader, entity, mesh)

                mesh.draw(shader, 0)
            }
        }

    }


}