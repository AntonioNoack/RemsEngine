package me.anno.mesh.assimp

import me.anno.ecs.Entity
import me.anno.ecs.components.anim.AnimRenderer
import me.anno.ecs.components.anim.AnimTexture.Companion.useAnimTextures
import me.anno.ecs.components.anim.SkeletonCache
import me.anno.ecs.components.mesh.MaterialCache
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.components.mesh.MeshSpawner
import me.anno.engine.ui.render.ECSShaderLib
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.drawing.GFXx3D
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.io.files.thumbs.ThumbsExt
import me.anno.maths.Maths
import me.anno.mesh.MeshData
import me.anno.mesh.MeshUtils
import me.anno.utils.pooling.JomlPools
import org.apache.logging.log4j.LogManager
import org.joml.*
import org.lwjgl.system.MemoryUtil
import java.nio.FloatBuffer
import kotlin.math.max
import kotlin.math.min

class AnimGameItem(
    val hierarchy: Entity,
    val animations: Map<String, me.anno.ecs.components.anim.Animation>
) {

    constructor(hierarchy: Entity) : this(hierarchy, emptyMap())

    val staticAABB = lazy {
        val rootEntity = hierarchy
        updateTransforms(rootEntity)
        // calculate/get the size
        calculateAABB(rootEntity)
    }

    /**
     * calculates the bounds of the mesh
     * not fast, but the gpu will take just as long -> doesn't matter
     *
     * the goal is to be accurate
     * */
    fun getBounds(transform: Matrix4f): AABBf {
        val rootEntity = hierarchy
        updateTransforms(rootEntity)
        val vf = Vector3f()
        val aabb = AABBf()
        val testAABB = AABBf()
        val jointMatrix = Matrix4f()
        rootEntity.simpleTraversal(false) { entity ->
            entity as Entity
            val global = entity.transform.globalTransform
            entity.anyComponent(MeshComponentBase::class, false) { comp ->
                val mesh = comp.getMesh()
                if (mesh != null) {
                    mesh.getBounds()

                    // join the matrices for 2x better performance than without
                    jointMatrix.set(transform).mul(global)

                    // if aabb u transform(mesh.aabb) == aabb, then skip this sub-mesh
                    mesh.aabb.transformProjectUnion(jointMatrix, testAABB.set(aabb))
                    if (testAABB != aabb) {
                        mesh.forEachPoint(false) { x, y, z ->
                            aabb.union(jointMatrix.transformProject(vf.set(x, y, z)))
                        }
                    }
                }
                false
            }
        }
        return aabb
    }

    fun uploadJointMatrices(
        shader: Shader,
        animation: me.anno.ecs.components.anim.Animation,
        time: Double
    ): Array<Matrix4x3f>? {
        val location = shader.getUniformLocation("jointTransforms")
        if (location < 0) return null
        val skeleton = SkeletonCache[animation.skeleton] ?: return null
        val boneCount = min(skeleton.bones.size, maxBones)
        val matrices = tmpMatrices
        animation.getMatrices(time.toFloat(), matrices)
        shader.use()
        matrixBuffer.limit(matrixSize * boneCount)
        for (index in 0 until boneCount) {
            val offset = index * matrixSize
            matrixBuffer.position(offset)
            get(matrices[index], matrixBuffer)
        }
        matrixBuffer.position(0)
        shader.m4x3Array(location, matrixBuffer)
        return matrices
    }

    fun findModelMatrix(
        cameraMatrix: Matrix4f,
        modelMatrix: Matrix4x3f,
        centerMesh: Boolean,
        normalizeScale: Boolean
    ): Matrix4x3fArrayList {
        val modelMatrices = Matrix4x3fArrayList()
        modelMatrices.set(modelMatrix)
        if (normalizeScale) modelMatrices.scale(getScaleFromAABB(staticAABB.value))
        if (centerMesh) MeshUtils.centerMesh(null, cameraMatrix, modelMatrices, this)
        return modelMatrices
    }

    fun drawAssimp(
        useECSShader: Boolean,
        cameraMatrix: Matrix4f,
        localTransform: Matrix4x3fArrayList,
        time: Double,
        color: Vector4f,
        animationName: String,
        useMaterials: Boolean,
        drawSkeletons: Boolean
    ) {

        RenderView.currentInstance = null

        val animation = animations[animationName]
        GFXState.animated.use(animation != null) {

            val baseShader = if (useECSShader) ECSShaderLib.pbrModelShader else ShaderLib.shaderAssimp
            val shader = baseShader.value
            shader.use()
            GFXx3D.shader3DUniforms(shader, cameraMatrix, color)
            GFXx3D.uploadAttractors0(shader)

            val skinningMatrices = if (animation != null) {
                uploadJointMatrices(shader, animation, time)
            } else null
            shader.v1b("hasAnimation", skinningMatrices != null)

            GFXx3D.transformUniform(shader, cameraMatrix)

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
                hierarchy,
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
        color: Vector4f,
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
                local.m00.toFloat(), local.m01.toFloat(), local.m02.toFloat(),
                local.m10.toFloat(), local.m11.toFloat(), local.m12.toFloat(),
                local.m20.toFloat(), local.m21.toFloat(), local.m22.toFloat(),
                local.m30.toFloat(), local.m31.toFloat(), local.m32.toFloat(),
            )
        )

        if (entity.hasComponent(MeshComponentBase::class)) {

            shader.use()
            shader.m4x3("localTransform", localTransform)

            if (shader["invLocalTransform"] >= 0) {
                val tmp = JomlPools.mat4x3f.borrow()
                tmp.set(localTransform).invert()
                shader.m4x3("invLocalTransform", tmp)
            }

            shader.v1f("worldScale", 1f) // correct?
            GFX.shaderColor(shader, "tint", -1)

            if (useMaterials) {
                entity.anyComponent(MeshComponentBase::class) { comp ->
                    val mesh = comp.getMesh()
                    if (mesh?.positions != null) {
                        mesh.checkCompleteness()
                        mesh.ensureBuffer()
                        val materialOverrides = comp.materials
                        val materials = mesh.materials
                        // LOGGER.info("drawing mesh with material $materialOverrides x $materials")
                        for (index in 0 until mesh.numMaterials) {
                            val m0 = materialOverrides.getOrNull(index)?.nullIfUndefined()
                            val m1 = m0 ?: materials.getOrNull(index)
                            val material = MaterialCache[m1, Mesh.defaultMaterial]
                            shader.v1i("hasVertexColors", if (material.enableVertexColors) mesh.hasVertexColors else 0)
                            material.bind(shader)
                            mesh.draw(shader, index)
                        }
                    } else MeshData.warnMissingMesh(comp, mesh)
                    false
                }
            } else {
                val material = Mesh.defaultMaterial
                material.bind(shader)
                entity.anyComponent(MeshComponentBase::class) { comp ->
                    val mesh = comp.getMesh()
                    if (mesh?.positions != null) {
                        mesh.checkCompleteness()
                        mesh.ensureBuffer()
                        shader.v1i("hasVertexColors", if (material.enableVertexColors) mesh.hasVertexColors else 0)
                        for (i in 0 until mesh.numMaterials) {
                            mesh.draw(shader, i)
                        }
                    } else MeshData.warnMissingMesh(comp, mesh)
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
                            transform.validate()
                            localTransform
                                .set(localTransform0)
                                .mul(transform.getDrawMatrix())
                            shader.m4x3("localTransform", localTransform)
                            if (shader["invLocalTransform"] >= 0) {
                                val tmp = JomlPools.mat4x3f.borrow()
                                tmp.set(localTransform).invert()
                                shader.m4x3("invLocalTransform", tmp)
                            }
                            val materials = mesh.materials
                            for (index in 0 until mesh.numMaterials) {
                                val matI = materials.getOrNull(index)
                                val material1 = material ?: MaterialCache[matI, Mesh.defaultMaterial]
                                val hasVCs = if (material1.enableVertexColors) mesh.hasVertexColors else 0
                                shader.v1i("hasVertexColors", hasVCs)
                                material1.bind(shader)
                                mesh.draw(shader, index)
                            }
                        }
                    }
                    false
                }
            } else {
                val material = Mesh.defaultMaterial
                material.bind(shader)
                entity.anyComponent(MeshSpawner::class) { comp ->
                    comp.forEachMesh { mesh, _, transform ->
                        if (mesh.positions != null) {
                            mesh.checkCompleteness()
                            mesh.ensureBuffer()
                            transform.validate()
                            localTransform
                                .set(localTransform0)
                                .mul(transform.getDrawMatrix())
                            shader.m4x3("localTransform", localTransform)
                            if (shader["invLocalTransform"] >= 0) {
                                val tmp = JomlPools.mat4x3f.borrow()
                                tmp.set(localTransform).invert()
                                shader.m4x3("invLocalTransform", tmp)
                            }
                            shader.v1i("hasVertexColors", mesh.hasVertexColors)
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
                skinningMatrices, color, children[i], useMaterials, drawSkeletons
            )
        }

        localTransform.popMatrix()
    }

    companion object {

        const val matrixSize = 12

        val maxBones = if (useAnimTextures) 256 // limited by indices that can be packed into a byte
        else {
            // limited by number of assignable uniform matrices
            val matrixUniforms = 12
            val maxBonesByComponents = GFX.maxVertexUniformComponents / matrixSize - 40
            val maxBonesByUniforms = GFX.maxUniforms / matrixUniforms - 30
            Maths.clamp(min(maxBonesByComponents, maxBonesByUniforms), 4, 128)
        }

        val matrixBuffer: FloatBuffer = MemoryUtil.memAllocFloat(matrixSize * maxBones)
        val tmpMatrices = Array(maxBones) { Matrix4x3f() }

        @JvmStatic
        private val LOGGER = LogManager.getLogger(AnimGameItem::class)

        init {
            LOGGER.info("Max Bones: $maxBones (by uniforms)")
        }

        @JvmStatic
        fun get(src: Matrix4x3f, dst: FloatBuffer) {
            src.putInto(dst)
        }

        @JvmStatic
        fun get(src: Matrix4f, dst: FloatBuffer) {

            dst.put(src.m00)
            dst.put(src.m01)
            dst.put(src.m02)

            dst.put(src.m10)
            dst.put(src.m11)
            dst.put(src.m12)

            dst.put(src.m20)
            dst.put(src.m21)
            dst.put(src.m22)

            dst.put(src.m30)
            dst.put(src.m31)
            dst.put(src.m32)

        }

        @JvmStatic
        fun getScaleFromAABB(aabb: AABBf): Float {
            // calculate the scale, such that everything can be visible
            val delta = max(aabb.maxX - aabb.minX, max(aabb.maxY - aabb.minY, aabb.maxZ - aabb.minZ))
            return 1f / max(delta, 1e-38f)
        }

        @JvmStatic
        fun getScaleFromAABB(aabb: AABBd): Float {
            // calculate the scale, such that everything can be visible
            val delta = max(aabb.maxX - aabb.minX, max(aabb.maxY - aabb.minY, aabb.maxZ - aabb.minZ))
            return 1f / max(delta.toFloat(), 1e-38f)
        }

        @JvmStatic
        fun centerStackFromAABB(stack: Matrix4x3f, aabb: AABBd) {
            stack.translate(
                -(aabb.minX + aabb.maxX).toFloat() / 2,
                -(aabb.minY + aabb.maxY).toFloat() / 2,
                -(aabb.minZ + aabb.maxZ).toFloat() / 2
            )
        }

        @JvmStatic
        private fun updateTransforms(entity: Entity) {
            entity.validateTransform()
            entity.transform.teleportUpdate(0)
            for (child in entity.children) updateTransforms(child)
        }

        @JvmStatic
        private fun calculateAABB(root: Entity): AABBd {
            val joint = AABBd()
            val local = AABBd()
            val tmpM4 = JomlPools.mat4d.create()
            root.simpleTraversal(true) { entity ->
                entity as Entity
                // todo rendering all points is only a good idea, if there are no meshes
                // todo render all them points, and use them for the bbx calculation (only if no meshes present)
                // because animated clothing may be too small to see
                if (entity.hasComponent(MeshComponentBase::class)) {
                    local.clear()
                    entity.anyComponent(MeshComponentBase::class, false) { comp ->
                        comp.ensureBuffer()
                        val mesh = comp.getMesh()
                        if (mesh != null) {
                            if (mesh.hasBuffer()) {
                                local.union(mesh.aabb.toDouble())
                            } else {
                                mesh.forEachPoint(false) { x, y, z ->
                                    local.union(x.toDouble(), y.toDouble(), z.toDouble())
                                }
                            }
                        }
                        false
                    }
                    if (!local.isEmpty()) {
                        local.transform(tmpM4.set(entity.transform.globalTransform))
                        joint.union(local)
                    }
                }
                false
            }
            JomlPools.mat4d.sub(1)
            return joint
        }

    }

}
