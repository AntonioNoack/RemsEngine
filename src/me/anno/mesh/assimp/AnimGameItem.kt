package me.anno.mesh.assimp

import me.anno.ecs.Entity
import me.anno.ecs.components.cache.MeshCache
import me.anno.ecs.components.cache.SkeletonCache
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.gpu.GFX
import me.anno.gpu.shader.Shader
import me.anno.utils.maths.Maths
import me.anno.utils.types.AABBs.clear
import me.anno.utils.types.AABBs.set
import me.anno.utils.types.AABBs.transformProjectUnion
import me.anno.utils.types.Matrices.mul2
import org.apache.logging.log4j.LogManager
import org.joml.*
import org.lwjgl.opengl.GL21
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
            entity.anyComponent(MeshComponent::class, false) { comp ->
                val mesh = MeshCache[comp.mesh]
                if (mesh != null) {
                    mesh.ensureBuffer()

                    // join the matrices for 2x better performance than without
                    jointMatrix.set(transform).mul2(global)

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
        animation.getMatrices(null, time.toFloat(), matrices)
        shader.use()
        matrixBuffer.limit(matrixSize * boneCount)
        for (index in 0 until boneCount) {
            val offset = index * matrixSize
            matrixBuffer.position(offset)
            get(matrices[index], matrixBuffer)
        }
        matrixBuffer.position(0)
        GL21.glUniformMatrix4x3fv(location, false, matrixBuffer)
        return matrices
    }

    companion object {

        val matrixSize = 12
        val maxBones = Maths.clamp((GFX.maxVertexUniforms - (matrixSize * 3)) / matrixSize, 4, 256)
        val matrixBuffer = MemoryUtil.memAllocFloat(matrixSize * maxBones)
        val tmpMatrices = Array(maxBones) { Matrix4x3f() }

        fun get(src: Matrix4x3f, dst: FloatBuffer) {
            src.get(dst)
        }

        fun get(src: Matrix4f, dst: FloatBuffer) {

            dst.put(src.m00())
            dst.put(src.m01())
            dst.put(src.m02())

            dst.put(src.m10())
            dst.put(src.m11())
            dst.put(src.m12())

            dst.put(src.m20())
            dst.put(src.m21())
            dst.put(src.m22())

            dst.put(src.m30())
            dst.put(src.m31())
            dst.put(src.m32())

        }

        fun getScaleFromAABB(aabb: AABBf): Float {
            // calculate the scale, such that everything can be visible
            val delta = max(aabb.maxX - aabb.minX, max(aabb.maxY - aabb.minY, aabb.maxZ - aabb.minZ))
            return 1f / delta
        }

        fun getScaleFromAABB(aabb: AABBd): Float {
            // calculate the scale, such that everything can be visible
            val delta = max(aabb.maxX - aabb.minX, max(aabb.maxY - aabb.minY, aabb.maxZ - aabb.minZ))
            return 1f / delta.toFloat()
        }

        fun centerStackFromAABB(stack: Matrix4x3f, aabb: AABBf) {
            stack.translate(
                -(aabb.minX + aabb.maxX) / 2,
                -(aabb.minY + aabb.maxY) / 2,
                -(aabb.minZ + aabb.maxZ) / 2
            )
        }

        fun centerStackFromAABB(stack: Matrix4x3f, aabb: AABBd) {
            stack.translate(
                -(aabb.minX + aabb.maxX).toFloat() / 2,
                -(aabb.minY + aabb.maxY).toFloat() / 2,
                -(aabb.minZ + aabb.maxZ).toFloat() / 2
            )
        }

        private fun updateTransforms(entity: Entity) {
            entity.transform.update((entity.parent as? Entity)?.transform, 0)
            for (child in entity.children) updateTransforms(child)
        }

        private fun AABBf.toDouble(): AABBd {
            return AABBd(
                minX.toDouble(), minY.toDouble(), minZ.toDouble(),
                maxX.toDouble(), maxY.toDouble(), maxZ.toDouble()
            )
        }

        private fun calculateAABB(root: Entity): AABBd {
            val joint = AABBd()
            val local = AABBd()
            root.simpleTraversal(true) { entity ->
                entity as Entity
                // todo rendering all points is only a good idea, if there are no meshes
                // todo render all them points, and use them for the bbx calculation (only if no meshes present)
                // because animated clothing may be too small to see
                if (entity.hasComponent(MeshComponent::class)) {
                    local.clear()
                    entity.anyComponent(MeshComponent::class, false) { comp ->
                        val mesh = MeshCache[comp.mesh]
                        if (mesh != null) {
                            mesh.ensureBuffer()
                            local.union(mesh.aabb.toDouble())
                        }
                        false
                    }
                    local.transform(Matrix4d(entity.transform.globalTransform))
                    joint.union(local)
                }
                false
            }
            return joint
        }

        private val LOGGER = LogManager.getLogger(AnimGameItem::class)

    }

    // todo cubemap from 6 images...

}
