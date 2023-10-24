package me.anno.engine.ui.render

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MaterialCache
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.Material.Companion.defaultMaterial
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.engine.ui.render.ECSShaderLib.pbrModelShader
import me.anno.gpu.CullMode
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.M4x3Delta.m4x3delta
import me.anno.gpu.buffer.LineBuffer
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.SimpleRenderer
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.utils.pooling.JomlPools
import org.joml.Matrix4d

object Outlines {

    // private val LOGGER = LogManager.getLogger(Outlines::class)

    val whiteRenderer = SimpleRenderer(
        "white", ShaderStage(
            "white", listOf(
                Variable(GLSLType.V1F, "zDistance"),
                Variable(GLSLType.V1F, "finalAlpha"),
                Variable(GLSLType.V4F, "finalResult", VariableMode.OUT)
            ), "if(finalAlpha<0.01) discard; finalResult = vec4(1.0);\n"
        )
    )

    private val tmpMat4d = Matrix4d()

    fun drawOutline(entity: Entity) {
        whiteTexture.bind(0) // for the albedo
        DrawAABB.drawAABB(entity.aabb, RenderView.aabbColorHovered)
        LineBuffer.finish(RenderState.cameraMatrix)
        drawOutlineInternally(entity)
    }

    private fun drawOutlineInternally(entity: Entity) {
        val children = entity.children
        for (i in children.indices) {
            drawOutlineInternally(children[i])
        }
        val components = entity.components
        for (i in components.indices) {
            val component = components[i]
            if (component is MeshComponentBase) {
                val mesh = component.getMeshOrNull() ?: continue
                drawOutline(component, mesh)
            }
        }
    }

    fun drawOutline(comp: MeshComponentBase, mesh: Mesh) {

        // todo respect alpha somehow?

        val entity = comp.entity ?: return
        val transform0 = entity.transform

        val transform = transform0.getDrawMatrix()
        val camPosition = RenderState.cameraPosition

        // scale based on visual scale, or use a geometry shader for that
        val joinedTransform = tmpMat4d.set(RenderState.cameraMatrix)
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
        offsetCorrectedTransform.translate(aabb.centerX * fac, aabb.centerY * fac, aabb.centerZ * fac)

        if (scale < 1e10f) {
            useFrame(whiteRenderer) {
                val cullMode = if (mesh.inverseOutline) CullMode.BACK else CullMode.FRONT
                GFXState.cullMode.use(cullMode) {
                    val matRef = comp.materials.firstOrNull() ?: mesh.materials.firstOrNull()
                    val material = MaterialCache[matRef, false] ?: defaultMaterial
                    val baseShader = material.shader ?: pbrModelShader
                    val animated = comp.hasAnimation
                    if (!material.isDoubleSided) GFXState.animated.use(animated) {

                        val shader = baseShader.value
                        shader.use()
                        material.bind(shader)

                        shader.m4x4("transform", RenderState.cameraMatrix)
                        shader.m4x4("prevTransform", RenderState.prevCameraMatrix)

                        val worldScale = RenderState.worldScale
                        shader.m4x3delta("localTransform", offsetCorrectedTransform, camPosition, worldScale, scale)
                        // todo inv local transform
                        shader.m4x3delta("prevLocalTransform", offsetCorrectedTransform, camPosition, worldScale, scale)
                        shader.v1f("worldScale", worldScale)
                        shader.v1f("prevWorldScale", RenderState.prevWorldScale)
                        shader.v4f("tint", -1)

                        val hasAnim = animated && comp.defineVertexTransform(shader, entity, mesh)
                        shader.v1b("hasAnimation", hasAnim)

                        mesh.draw(shader, 0)

                    }
                }
            }
        }
    }
}