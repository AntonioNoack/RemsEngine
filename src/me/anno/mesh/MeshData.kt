package me.anno.mesh

import me.anno.cache.data.ICacheData
import me.anno.ecs.Entity
import me.anno.ecs.components.cache.MaterialCache
import me.anno.ecs.components.cache.SkeletonCache
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.mesh.AnimRenderer
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.Mesh.Companion.defaultMaterial
import me.anno.ecs.components.mesh.MeshBaseComponent
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.render.ECSShaderLib.pbrModelShader
import me.anno.gpu.GFX
import me.anno.gpu.drawing.GFXx3D.shader3DUniforms
import me.anno.gpu.drawing.GFXx3D.transformUniform
import me.anno.gpu.drawing.GFXx3D.uploadAttractors0
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.shaderAssimp
import me.anno.io.files.InvalidRef
import me.anno.io.files.thumbs.ThumbsExt
import me.anno.mesh.MeshUtils.centerMesh
import me.anno.mesh.assimp.AnimGameItem
import me.anno.mesh.assimp.AnimGameItem.Companion.getScaleFromAABB
import org.apache.logging.log4j.LogManager
import org.joml.*

open class MeshData : ICacheData {

    var lastWarning: String? = null
    var assimpModel: AnimGameItem? = null

    fun drawAssimp(
        useECSShader: Boolean,
        cameraMatrix: Matrix4fArrayList,
        time: Double,
        color: Vector4fc,
        animationName: String,
        useMaterials: Boolean,
        centerMesh: Boolean,
        normalizeScale: Boolean,
        drawSkeletons: Boolean
    ) {

        val baseShader = if (useECSShader) pbrModelShader else shaderAssimp
        val shader = baseShader.value
        shader.use()
        shader3DUniforms(shader, cameraMatrix, color)
        uploadAttractors0(shader)

        val model0 = assimpModel!!
        val animation = model0.animations[animationName]
        val skinningMatrices = if (animation != null) {
            model0.uploadJointMatrices(shader, animation, time)
        } else null
        shader.v1b("hasAnimation", skinningMatrices != null)

        val localStack = Matrix4x3fArrayList()

        if (normalizeScale) {
            val scale = getScaleFromAABB(model0.staticAABB.value)
            localStack.scale(scale)
        }

        if (centerMesh) {
            centerMesh(null, cameraMatrix, localStack, model0)
        }

        transformUniform(shader, cameraMatrix)

        drawHierarchy(
            shader,
            cameraMatrix,
            localStack,
            skinningMatrices,
            color,
            model0,
            model0.hierarchy,
            useMaterials,
            drawSkeletons
        )

        // todo line mode: draw every mesh as lines
        // todo draw non-indexed as lines: use an index buffer
        // todo draw indexed as lines: use a geometry shader, which converts 3 vertices into 3 lines

    }

    fun vec3(v: Vector3d): Vector3f = Vector3f(v.x.toFloat(), v.y.toFloat(), v.z.toFloat())
    fun quat(q: Quaterniond): Quaternionf = Quaternionf(q.x.toFloat(), q.y.toFloat(), q.z.toFloat(), q.w.toFloat())

    fun drawHierarchy(
        shader: Shader,
        cameraMatrix: Matrix4f,
        stack: Matrix4x3fArrayList,
        skinningMatrices: Array<Matrix4x3f>?,
        color: Vector4fc,
        model0: AnimGameItem,
        entity: Entity,
        useMaterials: Boolean,
        drawSkeletons: Boolean
    ) {

        stack.pushMatrix()

        val transform = entity.transform
        val local = transform.localTransform

        // this moves the engine parts correctly, but ruins the rotation of the ghost
        // and scales it totally incorrectly
        stack.mul(
            Matrix4x3f(
                local.m00().toFloat(), local.m01().toFloat(), local.m02().toFloat(),
                local.m10().toFloat(), local.m11().toFloat(), local.m12().toFloat(),
                local.m20().toFloat(), local.m21().toFloat(), local.m22().toFloat(),
                local.m30().toFloat(), local.m31().toFloat(), local.m32().toFloat(),
            )
        )

        if (entity.hasComponent(MeshBaseComponent::class)) {

            shader.m4x3("localTransform", stack)
            GFX.shaderColor(shader, "tint", -1)

            if (useMaterials) {
                entity.anyComponent(MeshBaseComponent::class) { comp ->
                    val mesh = comp.getMesh()
                    if (mesh?.positions != null) {
                        mesh.checkCompleteness()
                        mesh.ensureBuffer()
                        shader.v1b("hasVertexColors", mesh.hasVertexColors)
                        val materialOverrides = comp.materials
                        val materials = mesh.materials
                        // LOGGER.info("drawing mesh with material $materialOverrides x $materials")
                        for (index in 0 until mesh.numMaterials) {
                            val m0 = materialOverrides.getOrNull(index)?.nullIfUndefined()
                            val m1 = m0 ?: materials.getOrNull(index)
                            val material = MaterialCache[m1, defaultMaterial]
                            material.defineShader(shader)
                            mesh.draw(shader, index)
                        }
                    } else warnMissingMesh(comp, mesh)
                    false
                }
            } else {
                val material = defaultMaterial
                material.defineShader(shader)
                entity.anyComponent(MeshBaseComponent::class) { comp ->
                    val mesh = comp.getMesh()
                    if (mesh?.positions != null) {
                        mesh.checkCompleteness()
                        mesh.ensureBuffer()
                        shader.v1b("hasVertexColors", mesh.hasVertexColors)
                        for (i in 0 until mesh.numMaterials) {
                            mesh.draw(shader, i)
                        }
                    } else warnMissingMesh(comp, mesh)
                    false
                }
            }
        }

        if (entity.sumComponents(Collider::class) { collider ->
                collider.drawShape(); 1
            } > 0) {
            ThumbsExt.finishLines(cameraMatrix, stack)
            // switch back to default shader
            shader.use()
        }

        if (drawSkeletons) {
            val animMeshRenderer = entity.getComponent(AnimRenderer::class, false)
            if (animMeshRenderer != null) {
                SkeletonCache[animMeshRenderer.skeleton]?.draw(shader, stack, skinningMatrices)
            }
        }

        val children = entity.children
        for (i in children.indices) {
            drawHierarchy(
                shader, cameraMatrix, stack, skinningMatrices,
                color, model0, children[i], useMaterials, drawSkeletons
            )
        }

        stack.popMatrix()
    }

    override fun destroy() {
        // destroy assimp data? no, it uses caches and is cleaned automatically
    }

    companion object {
        private val LOGGER = LogManager.getLogger(MeshData::class)
        fun warnMissingMesh(comp: MeshBaseComponent, mesh: Mesh?) {
            if(mesh == null){
                if (comp is MeshComponent) {
                    if(comp.mesh == InvalidRef){
                        LOGGER.warn("MeshComponent '${comp.name} is missing path")
                    } else {
                        LOGGER.warn("Mesh '${comp.name}'/'${comp.mesh}' is missing from MeshComponent")
                    }
                } else {
                    LOGGER.warn("Missing mesh $comp, ${comp::class.simpleName}")
                }
            } else {
                LOGGER.warn("Missing positions ${comp.getMesh()}")
            }
        }
    }

}