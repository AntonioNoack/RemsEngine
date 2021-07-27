package me.anno.mesh.assimp

import me.anno.ecs.Entity
import me.anno.ecs.components.anim.ImportedAnimation
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.prefab.EntityPrefab
import me.anno.gpu.GFX
import me.anno.gpu.shader.Shader
import me.anno.utils.Maths
import org.apache.logging.log4j.LogManager
import org.joml.*
import org.lwjgl.opengl.GL21
import org.lwjgl.system.MemoryUtil
import java.nio.FloatBuffer
import kotlin.math.max
import kotlin.math.min

class AnimGameItem(
    val hierarchy: Entity,
    val hierarchyPrefab: EntityPrefab,
    val meshes: List<Mesh>,
    val bones: List<Bone>,
    val animations: Map<String, me.anno.ecs.components.anim.Animation>
) {

    val staticAABB = lazy {
        val rootEntity = hierarchy
        updateTransforms(rootEntity)
        // calculate/get the size
        calculateAABB(rootEntity)
    }

    fun uploadJointMatrices(shader: Shader, animation: me.anno.ecs.components.anim.Animation, time: Double) {
        val location = shader.getUniformLocation("jointTransforms")
        if (location < 0) return
        // most times the duration is specified in milli seconds
        animation as ImportedAnimation
        val frames = animation.frames
        val frameCount = frames.size
        var frameIndexFloat = ((time * frameCount / animation.duration) % frameCount).toFloat()
        if (frameIndexFloat < 0) frameIndexFloat += frameCount
        val frameIndex0 = frameIndexFloat.toInt() % frameCount
        val frameIndex1 = (frameIndex0 + 1) % frameCount
        val frame0 = frames[frameIndex0]
        val frame1 = frames[frameIndex1]
        val fraction = frameIndexFloat - frameIndex0
        val invFraction = 1f - fraction
        shader.use()
        val boneCount = min(frame0.size, maxBones)
        matrixBuffer.limit(matrixSize * boneCount)
        for (index in 0 until boneCount) {
            val matrix0 = frame0[index]
            val matrix1 = frame1[index]
            tmpBuffer.position(0)
            val offset = index * matrixSize
            matrixBuffer.position(offset)
            get(matrix0, matrixBuffer)
            get(matrix1, tmpBuffer)
            // matrix interpolation
            for (i in 0 until matrixSize) {
                val j = offset + i
                matrixBuffer.put(j, matrixBuffer[j] * invFraction + fraction * tmpBuffer[i])
            }
        }
        matrixBuffer.position(0)
        GL21.glUniformMatrix4x3fv(location, false, matrixBuffer)
    }


    companion object {

        val matrixSize = 12
        val maxBones = Maths.clamp((GFX.maxVertexUniforms - (matrixSize * 3)) / matrixSize, 4, 256)
        val matrixBuffer = MemoryUtil.memAllocFloat(matrixSize * maxBones)
        val tmpBuffer = MemoryUtil.memAllocFloat(matrixSize)

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

        fun getScaleFromAABB(aabb: AABBd): Float {
            // calculate the scale, such that everything can be visible
            val delta = max(aabb.maxX - aabb.minX, max(aabb.maxY - aabb.minY, aabb.maxZ - aabb.minZ))
            return 1f / delta.toFloat()
        }

        fun centerStackFromAABB(stack: Matrix4x3f, aabb: AABBd) {
            stack.translate(
                -(aabb.minX + aabb.maxX).toFloat() / 2,
                -(aabb.minY + aabb.maxY).toFloat() / 2,
                -(aabb.minZ + aabb.maxZ).toFloat() / 2
            )
        }

        private fun updateTransforms(entity: Entity) {
            entity.transform.update(entity.parent?.transform, 0)
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
            root.simpleTraversal(true) { entity ->
                // todo rendering all points is only a good idea, if there are no meshes
                // todo render all them points, and use them for the bbx calculation (only if no meshes present)
                // because animated clothing may be too small to see
                val local = AABBd()
                val meshes = entity.getComponents<MeshComponent>(false).mapNotNull { it.mesh }
                for (mesh in meshes) {
                    mesh.ensureBuffer()
                    local.union(mesh.aabb.toDouble())
                }
                local.transform(Matrix4d(entity.transform.globalTransform))
                joint.union(local)
                false
            }
            return joint
        }

        private val LOGGER = LogManager.getLogger(AnimGameItem::class)

    }

    // todo number input: cannot enter 0.01 from left to right, because the 0 is removed instantly
    // todo cubemap from 6 images...

}
