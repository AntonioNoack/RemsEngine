package me.anno.mesh

import me.anno.cache.data.ICacheData
import me.anno.ecs.Entity
import me.anno.ecs.components.anim.AnimRenderer
import me.anno.ecs.components.cache.MaterialCache
import me.anno.ecs.components.cache.SkeletonCache
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.Mesh.Companion.defaultMaterial
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.components.mesh.MeshSpawner
import me.anno.engine.ui.render.ECSShaderLib.pbrModelShader
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.GFX
import me.anno.gpu.OpenGL
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
import me.anno.utils.types.Matrices.mul2
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4f
import org.joml.Matrix4x3f
import org.joml.Matrix4x3fArrayList
import org.joml.Vector4fc

open class MeshData : ICacheData {

    var lastWarning: String? = null
    var assimpModel: AnimGameItem? = null

    fun findModelMatrix(
        cameraMatrix: Matrix4f,
        modelMatrix: Matrix4x3f,
        centerMesh: Boolean,
        normalizeScale: Boolean
    ): Matrix4x3fArrayList {
        val model0 = assimpModel!!
        val modelMatrices = Matrix4x3fArrayList()
        modelMatrices.set(modelMatrix)
        if (normalizeScale) modelMatrices.scale(getScaleFromAABB(model0.staticAABB.value))
        if (centerMesh) centerMesh(null, cameraMatrix, modelMatrices, model0)
        return modelMatrices
    }

    fun drawAssimp(
        useECSShader: Boolean,
        cameraMatrix: Matrix4f,
        localTransform: Matrix4x3fArrayList,
        time: Double,
        color: Vector4fc,
        animationName: String,
        useMaterials: Boolean,
        drawSkeletons: Boolean
    ) {

        RenderView.currentInstance = null

        val model0 = assimpModel!!
        val animation = model0.animations[animationName]
        val hasAnimation = animation != null

        OpenGL.animated.use(hasAnimation) {

            val baseShader = if (useECSShader) pbrModelShader else shaderAssimp
            val shader = baseShader.value
            shader.use()
            shader3DUniforms(shader, cameraMatrix, color)
            uploadAttractors0(shader)

            val skinningMatrices = if (hasAnimation) {
                model0.uploadJointMatrices(shader, animation!!, time)
            } else null
            shader.v1b("hasAnimation", skinningMatrices != null)

            transformUniform(shader, cameraMatrix)

            // for GUI functions that use the camera matrix
            RenderState.worldScale = 1.0
            RenderState.cameraPosition.set(0.0)
            RenderState.cameraDirection.set(0.0, 0.0, -1.0) // not correct, but approx. correct
            RenderState.cameraMatrix.set(cameraMatrix)
            RenderView.currentInstance = null

            val cameraXPreGlobal = Matrix4f()
            cameraXPreGlobal.set(cameraMatrix)
                .mul(localTransform)

            val localTransform0 = Matrix4x3f(localTransform)

            drawHierarchy(
                shader,
                cameraMatrix,
                cameraXPreGlobal,
                localTransform,
                localTransform0,
                skinningMatrices,
                color,
                model0,
                model0.hierarchy,
                useMaterials,
                drawSkeletons
            )

        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun drawHierarchy(
        shader: Shader,
        cameraMatrix: Matrix4f,
        cameraXPreGlobal: Matrix4f,
        localTransform: Matrix4x3fArrayList,
        localTransform0: Matrix4x3f,
        skinningMatrices: Array<Matrix4x3f>?,
        color: Vector4fc,
        model0: AnimGameItem,
        entity: Entity,
        useMaterials: Boolean,
        drawSkeletons: Boolean
    ) {

        localTransform.pushMatrix()

        val transform = entity.transform
        val local = transform.localTransform

        // this moves the engine parts correctly, but ruins the rotation of the ghost
        // and scales it totally incorrectly
        localTransform.mul(
            Matrix4x3f(
                local.m00().toFloat(), local.m01().toFloat(), local.m02().toFloat(),
                local.m10().toFloat(), local.m11().toFloat(), local.m12().toFloat(),
                local.m20().toFloat(), local.m21().toFloat(), local.m22().toFloat(),
                local.m30().toFloat(), local.m31().toFloat(), local.m32().toFloat(),
            )
        )

        if (entity.hasComponent(MeshComponentBase::class)) {

            shader.use()
            shader.m4x3("localTransform", localTransform)
            shader.v1f("worldScale", 1f) // correct?
            GFX.shaderColor(shader, "tint", -1)

            if (useMaterials) {
                entity.anyComponent(MeshComponentBase::class) { comp ->
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
                            material.bind(shader)
                            mesh.draw(shader, index)
                        }
                    } else warnMissingMesh(comp, mesh)
                    false
                }
            } else {
                val material = defaultMaterial
                material.bind(shader)
                entity.anyComponent(MeshComponentBase::class) { comp ->
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

        if (entity.hasComponent(MeshSpawner::class)) {

            shader.use()
            shader.v1f("worldScale", 1f) // correct?
            GFX.shaderColor(shader, "tint", -1)

            localTransform.pushMatrix()
            if (useMaterials) {
                entity.anyComponent(MeshSpawner::class) { comp ->
                    comp.forEachMesh { mesh, material, transform ->
                        if (mesh.positions != null) {
                            mesh.checkCompleteness()
                            mesh.ensureBuffer()
                            localTransform
                                .set(localTransform0)
                                .mul2(transform.getDrawMatrix())
                            shader.m4x3("localTransform", localTransform)
                            shader.v1b("hasVertexColors", mesh.hasVertexColors)
                            val materials = mesh.materials
                            for (index in 0 until mesh.numMaterials) {
                                val material1 = material ?: MaterialCache[materials.getOrNull(index), defaultMaterial]
                                material1.bind(shader)
                                mesh.draw(shader, index)
                            }
                        }
                    }
                    false
                }
            } else {
                val material = defaultMaterial
                material.bind(shader)
                entity.anyComponent(MeshSpawner::class) { comp ->
                    comp.forEachMesh { mesh, _, transform ->
                        if (mesh.positions != null) {
                            mesh.checkCompleteness()
                            mesh.ensureBuffer()
                            localTransform
                                .set(localTransform0)
                                .mul2(transform.getDrawMatrix())
                            shader.m4x3("localTransform", localTransform)
                            shader.v1b("hasVertexColors", mesh.hasVertexColors)
                            for (i in 0 until mesh.numMaterials) {
                                mesh.draw(shader, i)
                            }
                        }
                    }
                    false
                }
            }
            localTransform.popMatrix()
        }

        val components = entity.components
        for (index in components.indices) {
            val component = components[index]
            component.onDrawGUI(true)
        }

        ThumbsExt.finishLines(cameraXPreGlobal)

        if (drawSkeletons) {
            val animMeshRenderer = entity.getComponent(AnimRenderer::class, false)
            if (animMeshRenderer != null) {
                SkeletonCache[animMeshRenderer.skeleton]?.draw(shader, localTransform, skinningMatrices)
            }
        }

        val children = entity.children
        for (i in children.indices) {
            drawHierarchy(
                shader, cameraMatrix, cameraXPreGlobal, localTransform, localTransform0,
                skinningMatrices, color, model0, children[i], useMaterials, drawSkeletons
            )
        }

        localTransform.popMatrix()
    }

    override fun destroy() {
        // destroy assimp data? no, it uses caches and is cleaned automatically
    }

    companion object {
        private val LOGGER = LogManager.getLogger(MeshData::class)
        fun warnMissingMesh(comp: MeshComponentBase, mesh: Mesh?) {
            if (mesh == null) {
                if (comp is MeshComponent) {
                    if (comp.mesh == InvalidRef) {
                        LOGGER.warn("MeshComponent '${comp.name}' is missing path (${comp.mesh})")
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