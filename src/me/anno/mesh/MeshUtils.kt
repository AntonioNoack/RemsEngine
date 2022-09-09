package me.anno.mesh

import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.mesh.Mesh
import me.anno.io.ISaveable
import me.anno.mesh.assimp.AnimGameItem
import org.joml.*
import kotlin.math.abs
import kotlin.math.max

object MeshUtils {

    fun centerMesh(stack: Matrix4f, localStack: Matrix4x3f, mesh: Mesh, targetFrameUsage: Float = 0.95f) {
        mesh.ensureBounds()
        centerMesh(stack, localStack, AABBd().set(mesh.aabb), { mesh.getBounds(it, false) }, targetFrameUsage)
    }

    fun centerMesh(stack: Matrix4f, localStack: Matrix4x3f, collider: Collider, targetFrameUsage: Float = 0.95f) {
        val aabb = AABBd()
        collider.fillSpace(Matrix4x3d(), aabb)
        centerMesh(stack, localStack, aabb, { transform ->
            val aabb2 = AABBd()
            collider.fillSpace(Matrix4x3d(), aabb2)
            AABBf().set(aabb2).transformProject(transform)
        }, targetFrameUsage)
    }

    fun centerMesh(stack: Matrix4f, localStack: Matrix4x3f, model0: AnimGameItem, targetFrameUsage: Float = 0.95f) {
        centerMesh(stack, localStack, model0.staticAABB.value, { model0.getBounds(it) }, targetFrameUsage)
    }

    fun centerMesh(
        transform: ISaveable?,
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
                AnimGameItem.centerStackFromAABB(localStack, staticAABB)
            }
        }
    }

    /**
     * whenever possible, this optimization should be on another thread
     * (because for high-poly meshes, it is expensive)
     * */
    fun centerMesh(
        cameraMatrix: Matrix4f,
        modelMatrix: Matrix4x3f,
        aabb0: AABBd,
        getBounds: (Matrix4f) -> AABBf,
        targetFrameUsage: Float = 0.95f
    ) {

        if (aabb0.isEmpty()) return

        if (modelMatrix.determinant().isNaN())
            throw IllegalArgumentException()

        // rough approximation using bounding box
        AnimGameItem.centerStackFromAABB(modelMatrix, aabb0)

        val modelViewProjectionMatrix = Matrix4f()
        modelViewProjectionMatrix
            .set(cameraMatrix)
            .mul(modelMatrix)

        val m0 = getBounds(modelViewProjectionMatrix)

        val dx = m0.avgX()
        val dy = m0.avgY()

        val scale = 2f * targetFrameUsage / max(m0.deltaX(), m0.deltaY())
        if (abs(dx) + abs(dy) < 5f && scale in 0.1f..10f) {
            cameraMatrix.translateLocal(-dx, -dy, 0f)
            cameraMatrix.scaleLocal(scale, scale, scale)
        }

    }

}