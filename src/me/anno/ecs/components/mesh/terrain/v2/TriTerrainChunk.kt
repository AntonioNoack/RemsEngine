package me.anno.ecs.components.mesh.terrain.v2

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.terrain.RectangleTerrainModel
import me.anno.ecs.components.mesh.terrain.TerrainBrush
import me.anno.ecs.components.mesh.unique.MeshEntry
import me.anno.engine.Events.addEvent
import me.anno.gpu.GFX
import me.anno.graph.octtree.KdTree
import me.anno.graph.octtree.OctTreeF
import me.anno.maths.Maths.length
import me.anno.utils.algorithms.ForLoop.forLoopSafely
import me.anno.utils.callbacks.F2F
import org.joml.AABBf
import org.joml.Matrix4x3f
import org.joml.Vector2i
import org.joml.Vector3f
import kotlin.math.exp
import kotlin.math.max

class TriTerrainChunk(val owner: TriTerrainComponent) : OctTreeF<Mesh>(16) {

    companion object {
        val falloff0 = falloff0(1f)
        fun falloff0(xSq: Float): Float {
            return exp(-5f * xSq)
        }

        fun falloff(v: Vector3f): Float {
            return max(falloff0(v.lengthSquared()) - falloff0, 0f)
        }

        fun falloff(v: Float): Float {
            return max(falloff0(v) - falloff0, 0f)
        }

        /**
         * This function calculates the transform of a normal given dx,dy,dz of when the function was applied.
         * Ignores scale, as it assumes that the result is normalized anyway.
         *
         * Matrix3f().set(dx,dy,dz).invert().transformTranspose(src)
         * */
        fun invert3x3TransformTranspose(dx: Vector3f, dy: Vector3f, dz: Vector3f, src: Vector3f) {
            val m00 = dy.y * dz.z - dz.y * dy.z
            val m01 = dz.y * dx.z - dx.y * dz.z
            val m02 = dx.y * dy.z - dx.z * dy.y
            val m10 = dz.x * dy.z - dy.x * dz.z
            val m11 = dx.x * dz.z - dz.x * dx.z
            val m12 = dx.z * dy.x - dx.x * dy.z
            val m20 = dy.x * dz.y - dz.x * dy.y
            val m21 = dz.x * dx.y - dx.x * dz.y
            val m22 = dx.x * dy.y - dx.y * dy.x
            src.set(
                src.dot(m00, m01, m02),
                src.dot(m10, m11, m12),
                src.dot(m20, m21, m22),
            )// .mul(1f / dz.dot(m02, m12, m22)) -> we normalize anyway, so no need to divide
        }
    }

    override fun createChild(): KdTree<Vector3f, Mesh> {
        return TriTerrainChunk(owner)
    }

    override fun getMin(data: Mesh): Vector3f {
        return data.getBounds().getMin(Vector3f())
    }

    override fun getMax(data: Mesh): Vector3f {
        return data.getBounds().getMax(Vector3f())
    }

    fun createTile(bounds: AABBf, resolution: Vector2i, getHeight: F2F): Mesh {
        val mesh = Mesh()
        RectangleTerrainModel.generateRegularQuadHeightMesh(
            resolution.x, resolution.y, false,
            1f, mesh, false
        )
        val sx = bounds.deltaX / (resolution.x - 1)
        val sy = bounds.deltaZ / (resolution.y - 1)
        val pos = mesh.positions!!
        val nor = mesh.normals!!
        forLoopSafely(pos.size, 3) { i ->
            val px = bounds.minX + sx * pos[i]
            val pz = bounds.minZ + sy * pos[i + 2]
            val py = getHeight.call(px, pz)
            pos[i] = px
            pos[i + 1] = py
            pos[i + 2] = pz
            // calculate normal
            val dx = (getHeight.call(px - sx, pz) - py) / sx
            val dz = (getHeight.call(px, pz - sy) - py) / sy
            val rn = 1f / length(dx, 1f, dz)
            nor[i] = dx * rn
            nor[i + 1] = rn
            nor[i + 2] = dz * rn
        }
        mesh.invalidateGeometry()
        addMesh(mesh, true)
        return mesh
    }

    private fun addMesh(mesh: Mesh, addToTree: Boolean) {
        if (addToTree) add(mesh)
        if (GFX.isGFXThread()) {
            addMeshUnsafe(mesh)
        } else {
            addEvent { addMeshUnsafe(mesh) }
        }
    }

    private fun addMeshUnsafe(mesh: Mesh) {
        owner.add(mesh, MeshEntry(mesh, mesh.getBounds(), owner.getData(mesh, mesh)!!))
    }

    fun applyBrush(brushToPos: Matrix4x3f, brush: TerrainBrush) {
        // find out bounds
        val bounds = AABBf(
            -1f, -1f, -1f,
            +1f, +1f, +1f
        ).transform(brushToPos)
        applyBrush(brushToPos, brush, bounds)
    }

    fun applyBrush(brushToPos: Matrix4x3f, brush: TerrainBrush, bounds: AABBf) {
        val posToBrush = brushToPos.invert(Matrix4x3f())
        var ctr = 0
        query(bounds.getMin(Vector3f()), bounds.getMax(Vector3f())) { mesh ->
            applyBrush(mesh, brush, brushToPos, posToBrush)
            ctr++
            false
        }
    }

    fun applyBrush(mesh: Mesh, brush: TerrainBrush, brushToPos: Matrix4x3f, posToBrush: Matrix4x3f) {
        owner.remove(mesh, false)
        val boundsI = mesh.getBounds()
        val oldMin = boundsI.getMin(Vector3f())
        val oldMax = boundsI.getMax(Vector3f())
        val pos = mesh.positions!!
        val nor = mesh.normals!!
        val posI = Vector3f()
        val norI = Vector3f()
        val dx = Vector3f()
        val dy = Vector3f()
        val dz = Vector3f()
        val eps = mesh.getBounds().maxDelta * 0.001f
        forLoopSafely(pos.size, 3) { i ->
            posToBrush.transformPosition(posI.set(pos, i))
            posToBrush.transformDirection(norI.set(nor, i))

            posI.add(eps, 0f, 0f, dx)
            posI.add(0f, eps, 0f, dy)
            posI.add(0f, 0f, eps, dz)

            // to do this only is really necessary for edge-points;
            //  we could save which are on the edge, and calculate the rest normally
            brush.apply(dx)
            brush.apply(dy)
            brush.apply(dz)
            brush.apply(posI)

            dx.sub(posI)
            dy.sub(posI)
            dz.sub(posI)

            invert3x3TransformTranspose(dx, dy, dz, norI)

            brushToPos.transformPosition(posI)
            brushToPos.transformDirection(norI).normalize()
            posI.get(pos, i)
            norI.get(nor, i)
        }
        mesh.invalidateGeometry()
        addMesh(mesh, false)
        update(mesh, oldMin, oldMax)
    }
}