package com.bulletphysics.softbody

import com.bulletphysics.collision.dispatch.CollisionWorld
import com.bulletphysics.dynamics.ActionInterface
import com.bulletphysics.linearmath.IDebugDraw
import com.bulletphysics.softbody.CellPose.updateCellTransforms
import me.anno.ecs.components.mesh.Mesh
import org.joml.Matrix4x3f
import org.joml.Vector3f
import org.joml.Vector3i

// todo define a soft body...
//  GhostObject, which applies forces
//  verlet integration
//  pressure to avoid collapse
//  rest position / rest length?
//  separate on too much force
//  plastic deformation on too much force (change rest positions)

class SoftBody(
    val numVertices: Int, val indices: IntArray,
    val cellStructure: CellStructure
) : ActionInterface {

    val positions = FloatArray(numVertices * 3)
    val prevPositions = FloatArray(numVertices * 3) // replacement for velocity

    val forces = FloatArray(numVertices * 3)
    val masses = FloatArray(numVertices)

    val numCells = cellStructure.numCells.product()
    val cellTransforms = FloatArray(numCells * 12)

    fun cellIndex(cx: Int, cy: Int, cz: Int): Int {
        val numCells = cellStructure.numCells
        return cx + numCells.x * (cy + numCells.y * cz)
    }

    init {
        // initialize cell transforms
        val unit4x3 = Matrix4x3f()
        for (i in 0 until numCells) {
            unit4x3.get(cellTransforms, i * 12)
        }
    }


    // todo for each cell, recalculate the orientation, and then apply back-to-rest-position-forces

    fun clearForces() {
        forces.fill(0f)
    }

    fun applyGravity(gravity: Vector3f) {
        val masses = masses
        val forces = forces
        for (i in masses.indices) {
            val mass = masses[i]
            val i3 = i * 3
            forces[i3] += gravity.x * mass
            forces[i3 + 1] += gravity.y * mass
            forces[i3 + 2] += gravity.z * mass
        }
    }

    fun relax() {
        updateCellTransforms(this)

        // todo apply forces from cells to its vertices based on stiffness and its ideal shape...
    }

    override fun updateAction(collisionWorld: CollisionWorld, deltaTimeStep: Float) {
        // todo find all collisions with static geometry and other rigid bodies in scene...
        //  but for each vertex...
    }

    override fun debugDraw(debugDrawer: IDebugDraw) {
        // todo debug draw all vertices and lines??? might be a good idea :)

    }

    companion object {

        fun cuboid(
            size: Vector3f, numVertices: Vector3i, mesh: Mesh,
            density: Float
        ): SoftBody {
            val total = numVertices.x * numVertices.y * numVertices.z
            val nx = numVertices.x - 1
            val ny = numVertices.y - 1
            val nz = numVertices.z - 1

            val s = CuboidCellStructure(
                Vector3f(
                    size.x / nx, size.y / ny, size.z / nz
                ), numVertices
            )

            val indices = IntArray(12 * (nx * ny + ny * nz + nx * nz))

            var k = 0

            fun addQuadFace(a: Int, b: Int, c: Int, d: Int) {
                indices[k++] = a
                indices[k++] = b
                indices[k++] = d
                indices[k++] = d
                indices[k++] = b
                indices[k++] = c
            }

            for (xi in 0 until nx) {
                for (yi in 0 until ny) {
                    addQuadFace(
                        s.idx(xi + 0, yi + 0, 0),
                        s.idx(xi + 0, yi + 1, 0),
                        s.idx(xi + 1, yi + 1, 0),
                        s.idx(xi + 1, yi + 0, 0),
                    )
                    addQuadFace(
                        s.idx(xi + 1, yi + 0, nz),
                        s.idx(xi + 1, yi + 1, nz),
                        s.idx(xi + 0, yi + 1, nz),
                        s.idx(xi + 0, yi + 0, nz),
                    )
                }
                for (zi in 0 until nz) {
                    addQuadFace(
                        s.idx(xi + 1, 0, zi + 0),
                        s.idx(xi + 1, 0, zi + 1),
                        s.idx(xi + 0, 0, zi + 1),
                        s.idx(xi + 0, 0, zi + 0),
                    )
                    addQuadFace(
                        s.idx(xi + 0, ny, zi + 0),
                        s.idx(xi + 0, ny, zi + 1),
                        s.idx(xi + 1, ny, zi + 1),
                        s.idx(xi + 1, ny, zi + 0),
                    )
                }
            }
            for (yi in 0 until ny) {
                for (zi in 0 until nz) {
                    addQuadFace(
                        s.idx(0, yi + 0, zi + 0),
                        s.idx(0, yi + 0, zi + 1),
                        s.idx(0, yi + 1, zi + 1),
                        s.idx(0, yi + 1, zi + 0),
                    )
                    addQuadFace(
                        s.idx(nx, yi + 1, zi + 0),
                        s.idx(nx, yi + 1, zi + 1),
                        s.idx(nx, yi + 0, zi + 1),
                        s.idx(nx, yi + 0, zi + 0),
                    )
                }
            }

            val softMesh = SoftBody(total, indices, s)
            var vi = 0
            for (zi in 0 until numVertices.z) {
                val z = size.z * (zi / (numVertices.z - 1f) * 2f - 1f)
                for (yi in 0 until numVertices.y) {
                    val y = size.y * (yi / (numVertices.y - 1f) * 2f - 1f)
                    for (xi in 0 until numVertices.x) {
                        val x = size.x * (xi / (numVertices.x - 1f) * 2f - 1f)
                        softMesh.positions[vi++] = x
                        softMesh.positions[vi++] = y
                        softMesh.positions[vi++] = z
                    }
                }
            }

            val totalMass = size.product() * density
            softMesh.positions.copyInto(softMesh.prevPositions)
            softMesh.masses.fill(totalMass / softMesh.numVertices)

            mesh.positions = softMesh.positions
            mesh.indices = indices
            mesh.invalidateGeometry()
            return softMesh
        }
    }
}

