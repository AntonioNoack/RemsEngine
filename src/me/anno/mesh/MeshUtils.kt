package me.anno.mesh

import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.forAllComponents
import me.anno.ecs.EntityQuery.forAllEntitiesInChildren
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.components.mesh.MeshIterators.forEachPoint
import org.joml.AABBd
import org.joml.AABBf
import org.joml.Matrix4f
import org.joml.Matrix4x3
import org.joml.Matrix4x3f
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.max

object MeshUtils {

    fun centerMesh(cameraMatrix: Matrix4f, modelMatrix: Matrix4x3f, mesh: IMesh, targetFrameUsage: Float = 0.95f) {
        if (mesh is Mesh) centerMesh(cameraMatrix, modelMatrix, mesh, targetFrameUsage)
        else centerMesh(
            cameraMatrix,
            modelMatrix,
            AABBd(mesh.getBounds()),
            { transform -> mesh.getBounds().transform(transform) },
            targetFrameUsage
        )
    }

    fun centerMesh(cameraMatrix: Matrix4f, modelMatrix: Matrix4x3f, mesh: Mesh, targetFrameUsage: Float = 0.95f) {
        centerMesh(
            cameraMatrix,
            modelMatrix,
            AABBd(mesh.getBounds()),
            { transform -> mesh.getBounds(transform, false) },
            targetFrameUsage
        )
    }

    fun centerMesh(cameraMatrix: Matrix4f, modelMatrix: Matrix4x3f, mesh: Entity, targetFrameUsage: Float = 0.95f) {
        mesh.getGlobalBounds()
        val aabb = AABBf()
        centerMesh(cameraMatrix, modelMatrix, AABBd(mesh.getGlobalBounds()), {
            aabb.set(mesh.getGlobalBounds())
            aabb.transform(it)
        }, targetFrameUsage)
    }

    fun centerMesh(stack: Matrix4f, localStack: Matrix4x3f, collider: Collider, targetFrameUsage: Float = 0.95f) {
        val aabb = AABBd()
        collider.fillSpace(Matrix4x3(), aabb)
        centerMesh(stack, localStack, aabb, { transform ->
            val aabb2 = AABBd()
            collider.fillSpace(Matrix4x3(), aabb2)
            AABBf(aabb2).transformProject(transform)
        }, targetFrameUsage)
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
            return

        // rough approximation using bounding box
        centerStackFromAABB(modelMatrix, aabb0)

        val modelViewProjectionMatrix = Matrix4f()
        modelViewProjectionMatrix
            .set(cameraMatrix)
            .mul(modelMatrix)

        val m0 = getBounds(modelViewProjectionMatrix)

        val dx = m0.centerX
        val dy = m0.centerY

        val scale = 2f * targetFrameUsage / max(m0.deltaX, m0.deltaY)
        if (abs(dx) + abs(dy) < 5f && scale in 0.1f..10f) {
            cameraMatrix.translateLocal(-dx, -dy, 0f)
            cameraMatrix.scaleLocal(scale, scale, scale)
        }
    }

    /**
     * calculates the bounds of the mesh
     * not fast, but the gpu will take just as long -> doesn't matter
     *
     * the goal is to be accurate
     * */
    fun getBounds(root: Entity, transform: Matrix4f): AABBf {
        root.forAllEntitiesInChildren(false) { entity ->
            entity.validateTransform()
            entity.transform.teleportUpdate()
        }
        val vf = Vector3f()
        val aabb = AABBf()
        val testAABB = AABBf()
        val jointMatrix = Matrix4f()
        root.simpleTraversal(false) { entity ->
            entity as Entity
            val global = entity.transform.globalTransform
            entity.forAllComponents(MeshComponentBase::class) { comp ->
                val mesh = comp.getMesh()
                if (mesh is Mesh) {

                    // join the matrices for 2x better performance than without
                    jointMatrix.set(transform).mul(global)

                    // if aabb u transform(mesh.aabb) == aabb, then skip this sub-mesh
                    mesh.getBounds().transformProject(jointMatrix, testAABB.set(aabb))
                    if (testAABB != aabb) {
                        mesh.forEachPoint(false) { x, y, z ->
                            aabb.union(jointMatrix.transformProject(vf.set(x, y, z)))
                            false
                        }
                    }
                }
            }
            false
        }
        return aabb
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

    fun Mesh.countPrimitives(): Long {
        val indices = indices
        val positions = positions
        val drawMode = drawMode
        val baseLength = if (indices != null) {
            numPrimitivesByType(indices.size * 3, drawMode)
        } else if (positions != null) {
            numPrimitivesByType(positions.size, drawMode)
        } else 0
        val size = proceduralLength
        return if (size <= 0) baseLength.toLong()
        else if (baseLength > 0) baseLength.toLong() * size
        else numPrimitivesByType(size, drawMode).toLong()
    }
}