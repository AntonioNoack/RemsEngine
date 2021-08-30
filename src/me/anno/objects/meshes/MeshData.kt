package me.anno.objects.meshes

import me.anno.cache.data.ICacheData
import me.anno.ecs.Entity
import me.anno.ecs.components.cache.MaterialCache
import me.anno.ecs.components.cache.MeshCache
import me.anno.ecs.components.cache.SkeletonCache
import me.anno.ecs.components.mesh.AnimRenderer
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.Mesh.Companion.defaultMaterial
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.render.ECSShaderLib.pbrModelShader
import me.anno.gpu.GFX
import me.anno.gpu.ShaderLib.shaderAssimp
import me.anno.gpu.drawing.GFXx3D.shader3DUniforms
import me.anno.gpu.drawing.GFXx3D.transformUniform
import me.anno.gpu.shader.Shader
import me.anno.mesh.assimp.AnimGameItem
import me.anno.mesh.assimp.AnimGameItem.Companion.centerStackFromAABB
import me.anno.mesh.assimp.AnimGameItem.Companion.getScaleFromAABB
import me.anno.objects.GFXTransform
import me.anno.objects.GFXTransform.Companion.uploadAttractors
import me.anno.objects.Transform
import me.anno.utils.types.AABBs.avgX
import me.anno.utils.types.AABBs.avgY
import me.anno.utils.types.AABBs.deltaX
import me.anno.utils.types.AABBs.deltaY
import me.anno.utils.types.AABBs.isEmpty
import me.anno.utils.types.AABBs.set
import org.apache.logging.log4j.LogManager
import org.joml.*
import kotlin.math.abs
import kotlin.math.max

open class MeshData : ICacheData {

    var lastWarning: String? = null
    var assimpModel: AnimGameItem? = null

    fun drawAssimp(
        useECSShader: Boolean,
        transform: GFXTransform?,
        stack: Matrix4fArrayList,
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
        shader3DUniforms(shader, stack, color)
        uploadAttractors(transform, shader, time)

        val model0 = assimpModel!!
        val animation = model0.animations[animationName]
        shader.v1("hasAnimation", animation != null)
        val skinningMatrices = if (animation != null) {
            model0.uploadJointMatrices(shader, animation, time, drawSkeletons)
        } else null

        val localStack = Matrix4x3fArrayList()

        if (normalizeScale) {
            val scale = getScaleFromAABB(model0.staticAABB.value)
            localStack.scale(scale)
        }

        if (centerMesh) {
            centerMesh(transform, stack, localStack, model0)
        }

        transformUniform(shader, stack)

        drawHierarchy(shader, localStack, skinningMatrices, color, model0, model0.hierarchy, useMaterials, drawSkeletons)

        // todo line mode: draw every mesh as lines
        // todo draw non-indexed as lines: use an index buffer
        // todo draw indexed as lines: use a geometry shader, which converts 3 vertices into 3 lines

    }

    fun vec3(v: Vector3d): Vector3f = Vector3f(v.x.toFloat(), v.y.toFloat(), v.z.toFloat())
    fun quat(q: Quaterniond): Quaternionf = Quaternionf(q.x.toFloat(), q.y.toFloat(), q.z.toFloat(), q.w.toFloat())

    fun drawHierarchy(
        shader: Shader,
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

        val meshes = entity.getComponents(MeshComponent::class, false)
        if (meshes.isNotEmpty()) {

            shader.m4x3("localTransform", stack)
            GFX.shaderColor(shader, "tint", -1)

            // LOGGER.info("use materials? $useMaterials")

            if (useMaterials) {
                for (comp in meshes) {
                    val mesh = MeshCache[comp.mesh]
                    if (mesh == null) {
                        LOGGER.warn("Mesh ${comp.mesh} is missing")
                        continue
                    }
                    mesh.ensureBuffer()
                    shader.v1("hasVertexColors", mesh.hasVertexColors)
                    val materials = mesh.materials
                    if (materials.isNotEmpty()) {
                        for ((index, mat) in mesh.materials.withIndex()) {
                            val material = MaterialCache[mat, defaultMaterial]
                            material.defineShader(shader)
                            mesh.draw(shader, index)
                        }
                    } else {
                        val material = defaultMaterial
                        material.defineShader(shader)
                        mesh.draw(shader, 0)
                    }
                }
            } else {
                val material = defaultMaterial
                material.defineShader(shader)
                for (comp in meshes) {
                    val mesh = MeshCache[comp.mesh]
                    if (mesh == null) {
                        LOGGER.warn("Mesh ${comp.mesh} is missing")
                        continue
                    }
                    val materialCount = max(1, mesh.materials.size)
                    for (i in 0 until materialCount) {
                        mesh.draw(shader, i)
                    }
                }
            }
        }

        if (drawSkeletons) {
            val animMeshRenderer = entity.getComponent(AnimRenderer::class, false)
            if (animMeshRenderer != null) {
                SkeletonCache[animMeshRenderer.skeleton]?.draw(shader, stack, skinningMatrices)
            }
        }

        val children = entity.children
        for (i in children.indices) {
            drawHierarchy(shader, stack, skinningMatrices, color, model0, children[i], useMaterials, drawSkeletons)
        }

        stack.popMatrix()
    }

    override fun destroy() {
        // destroy assimp data? no, it uses caches and is cleaned automatically
    }

    companion object {

        private val LOGGER = LogManager.getLogger(MeshData::class)

        // todo make target frame usage dependent on size? probably better: ensure we have a ~ 3px padding

        fun centerMesh(stack: Matrix4f, localStack: Matrix4x3f, mesh: Mesh, targetFrameUsage: Float = 0.95f) {
            mesh.ensureBuffer()
            centerMesh(stack, localStack, AABBd().set(mesh.aabb), { mesh.getBounds(it, false) }, targetFrameUsage)
        }

        fun centerMesh(stack: Matrix4f, localStack: Matrix4x3f, model0: AnimGameItem, targetFrameUsage: Float = 0.95f) {
            centerMesh(stack, localStack, model0.staticAABB.value, { model0.getBounds(it) }, targetFrameUsage)
        }

        fun centerMesh(
            transform: Transform?,
            stack: Matrix4f,
            localStack: Matrix4x3f,
            model0: AnimGameItem,
            targetFrameUsage: Float = 0.95f
        ) {
            val staticAABB = model0.staticAABB.value
            if (!staticAABB.isEmpty()) {
                if (transform == null) {
                    centerMesh(stack, localStack, staticAABB, { model0.getBounds(it) }, targetFrameUsage)
                } else {
                    centerStackFromAABB(localStack, staticAABB)
                }
            }
        }

        fun centerMesh(
            stack: Matrix4f,
            localStack: Matrix4x3f,
            aabb0: AABBd,
            getBounds: (Matrix4f) -> AABBf,
            targetFrameUsage: Float = 0.95f
        ) {

            // rough approximation using bounding box
            centerStackFromAABB(localStack, aabb0)

            // todo whenever possible, this optimization should be on another thread

            // Newton iterations to improve the result
            val matrix = Matrix4f()
            fun test(dx: Float, dy: Float): AABBf {
                matrix
                    .set(stack)
                    .translateLocal(dx, dy, 0f)
                    .mul(localStack)
                return getBounds(matrix)
            }

            for (i in 1..5) {

                val m0 = test(0f, 0f)

                val scale = 2f * targetFrameUsage / max(m0.deltaX(), m0.deltaY())
                if (scale !in 0.1f..10f) break // scale is too wrong... mmh...
                val scaleIsGoodEnough = scale in 0.9999f..1.0001f

                val x0 = m0.avgX()
                val y0 = m0.avgY()

                // good enough for pixels
                // exit early to save two evaluations
                if (scaleIsGoodEnough && abs(x0) + abs(y0) < 1e-4f) break

                val epsilon = 0.1f
                val mx = test(epsilon, 0f)
                val my = test(0f, epsilon)

                /*LOGGER.info("--- Iteration $i ---")
                LOGGER.info("m0: ${m0.print()}")
                LOGGER.info("mx: ${mx.print()}")
                LOGGER.info("my: ${my.print()}")*/

                val dx = (mx.avgX() - x0) / epsilon
                val dy = (my.avgY() - y0) / epsilon

                // todo dx and dy seem to always be close to 1.00000,
                // todo what is the actual meaning of them, can we set them to 1.0 without errors?

                // stack.translateLocal(-alpha * x0 / dx, -alpha * y0 / dy, 0f)
                val newtonX = -x0 / dx
                val newtonY = -y0 / dy

                if (abs(newtonX) + abs(newtonY) > 5f) break // translation is too wrong... mmh...

                // good enough for pixels
                if (scaleIsGoodEnough && abs(newtonX) + abs(newtonY) < 1e-4f) break

                stack.translateLocal(newtonX, newtonY, 0f)
                stack.scaleLocal(scale, scale, scale)

                LOGGER.info("Tested[$i]: $epsilon, Used: Newton = (-($x0/$dx), -($y0/$dy)), Scale: $scale")

            }

        }

    }

}