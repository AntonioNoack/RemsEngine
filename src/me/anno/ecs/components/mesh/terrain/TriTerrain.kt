package me.anno.ecs.components.mesh.terrain

import me.anno.ecs.Component
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.OnEdgeCalculator
import me.anno.ecs.interfaces.CustomEditMode
import me.anno.utils.maths.Maths.sq
import me.anno.utils.structures.arrays.ExpandingFloatArray
import org.joml.AABBf
import org.joml.Matrix3f
import org.joml.Vector3f
import kotlin.math.max
import kotlin.math.sqrt

/**
 * a terrain class, that hopefully will be efficient, and which should handle meshes of giant size
 * these meshes then should be editable, and we want to use brushes on them
 * todo expand, when we want to edit a point at the edge
 * todo brushes: thicken, thinnen, make higher, make lower
 * todo quad tree as acceleration structure
 * */
class TriTerrain : Component(), CustomEditMode {

    // could be local, but that would mean a lot more complex calculations
    val positions = ExpandingFloatArray(512)
    private val removedPositions = HashSet<Int>()

    val init = RegularTerrainInit(32, 100f)

    // todo support smooth normals on request

    val maxTriangles = 512

    // todo whenever the data is changed, we need to update the meshes
    // todo what about triangles, which cross multiple sections? should not happen...
    val data = TriangleOctTree(this, Vector3f(Float.NEGATIVE_INFINITY), Vector3f(Float.POSITIVE_INFINITY), maxTriangles)

    fun applyBrush(
        editorPosition: Vector3f, editorMatrix: Matrix3f,
        radius: Float, strength: Float,
        brush: TerrainBrush
    ) {
        // todo apply this brush
        val indices = HashSet<Int>()
        // todo find all indices in this area
        // todo find out, whether they are on the edge
        val min = Vector3f(editorPosition).sub(radius, radius, radius)
        val max = Vector3f(editorPosition).add(radius, radius, radius)
        val bounds = AABBf()
        bounds.union(min)
        bounds.union(max)
        val pos = positions

        init.ensure(editorPosition, radius, this)

        /*val foundEdgeWithIssue = data.iterate(min, max) { partialData ->
            partialData as TriangleOctTree
            partialData.forEachTriangleIndexed { a, b, c, abIsEdge, bcIsEdge, caIsEdge, i ->
                // test whether this triangle is inside the bounds
                val a3 = a * 3
                val b3 = b * 3
                val c3 = c * 3
                val containsA = bounds.testPoint(pos[a3], pos[a3 + 1], pos[a3 + 2])
                val containsB = bounds.testPoint(pos[b3], pos[b3 + 1], pos[b3 + 2])
                val containsC = bounds.testPoint(pos[c3], pos[c3 + 1], pos[c3 + 2])
                when {
                    (containsA || containsB) && abIsEdge -> {
                        expandTerrainAB(a, b, c, partialData, i)
                        true
                    }
                    (containsB || containsC) && bcIsEdge -> {
                        expandTerrainAB(b, c, a, partialData, i)
                        true
                    }
                    (containsC || containsA) && caIsEdge -> {
                        expandTerrainAB(c, a, b, partialData, i)
                        true
                    }
                    else -> {
                        // register these indices
                        if (containsA) indices.add(a)
                        if (containsB) indices.add(b)
                        if (containsC) indices.add(c)
                        false
                    }
                }
            }
            false
        }

        // todo this is (probably) slow -.-, we should expand iteratively, when we find a missing edge
        // also we may need to expand from the center for nicer topology? not really ^^, we should be fine ^^
        // because we grow quickly, before we even could apply large transforms
        if (foundEdgeWithIssue) {
            applyBrush(editorPosition, editorMatrix, radius, strength, brush)
            return
        }*/

        val point = Vector3f()
        for (index in indices) {
            val i3 = index * 3
            point.set(pos[i3], pos[i3 + 1], pos[i3 + 2])
            brush.apply(editorPosition, editorMatrix, radius, strength, point)
            pos[i3] = point.x
            pos[i3 + 1] = point.y
            pos[i3 + 2] = point.z
        }

        // todo find crumbled/broken triangle sections (?)

    }

    fun remesh(
        editorPosition: Vector3f, editorMatrix: Matrix3f,
        radius: Float, resolution: Float
    ) {

        // todo in this section, remesh the area

        // idea 1:
        // - build bvh
        // - apply triangulation of this bvh on the edges
        // - connect the resulting mesh with the original mesh / replace the sections

        // idea 2:
        // split too large triangles
        // remove too small triangles
        //

    }

    fun addPoint(x: Float, y: Float, z: Float): Int {
        return if (removedPositions.isEmpty()) {
            val idx = positions.size / 3
            positions.add(x)
            positions.add(y)
            positions.add(z)
            idx
        } else {
            val idx = removedPositions.first()
            removedPositions.remove(idx) // no longer removed
            var i3 = idx * 3
            positions[i3++] = x
            positions[i3++] = y
            positions[i3] = z
            idx
        }
    }

    fun removePoint(index: Int) {
        removedPositions.add(index)
    }

    fun addTriangle(a: Int, b: Int, c: Int, abIsEdge: Boolean, bcIsEdge: Boolean, caIsEdge: Boolean) {
        val chunk = TriangleOctTree(this, maxTriangles) // mmh... the allocated size should be dynamic
        // e.g. here we only need a single triangle, and the chance is high, that this object is discarded
        val idx = chunk.indices
        idx[0] = a
        idx[1] = b
        idx[2] = c
        val ioe = chunk.isOnEdge
        ioe[0] = abIsEdge
        ioe[1] = bcIsEdge
        ioe[2] = caIsEdge
        chunk.numTriangles = 1
        data.add(chunk)
    }

    fun union(aabb: AABBf, vertexIndex: Int) {
        val i3 = vertexIndex * 3
        aabb.union(positions[i3], positions[i3 + 1], positions[i3 + 2])
    }

    fun expand3(a3: Int, b3: Int, c3: Int): Float {
        val centerX2 = positions[a3] + positions[b3]
        val opposite = positions[c3]
        return centerX2 - opposite
    }

    fun distanceSq3(a3: Int, b3: Int): Float {
        return sq(
            positions[a3] - positions[b3],
            positions[a3 + 1] - positions[b3 + 1],
            positions[a3 + 2] - positions[b3 + 2],
        )
    }

    fun distanceSq3Sum(a3: Int, b3: Int, c3: Int): Float {
        val positions = positions
        val ax = positions[a3]
        val ay = positions[a3 + 1]
        val az = positions[a3 + 2]
        val bx = positions[b3]
        val by = positions[b3 + 1]
        val bz = positions[b3 + 2]
        val cx = positions[c3]
        val cy = positions[c3 + 1]
        val cz = positions[c3 + 2]
        return sq(bx - ax, by - ay, bz - az) +
                sq(cx - bx, cy - by, cz - bz) +
                sq(ax - cx, ay - cy, az - cz)
    }

    // todo alternatively, we could write a standard procedure, which generates triangles
    // todo this probably would be more more optimizable, and faster

    fun expandTerrainAB(a: Int, b: Int, c: Int, triangleOwner: TriangleOctTree, triangleIndex: Int) {
        // todo add a single triangle
        // todo use a point, if there is one nearby and is on the edge as well
        val a3 = a * 3
        val b3 = b * 3
        val c3 = c * 3
        // first define the aabb for the point candidates
        val aabb = AABBf()
        // union(aabb, c)
        val targetX = expand3(a3, b3, c3)
        val targetY = expand3(a3 + 1, b3 + 1, c3 + 1)
        val targetZ = expand3(a3 + 2, b3 + 2, c3 + 2)
        val radius = 0.5f * sqrt(distanceSq3Sum(a3, b3, c3))
        val min = Vector3f(targetX - radius, targetY - radius, targetZ - radius)
        val max = Vector3f(targetX + radius, targetY + radius, targetZ + radius)
        aabb.setMin(min)
        aabb.setMax(max)
        var foundPoint = -1
        data.iterate(min, max) { partialData ->
            partialData as TriangleOctTree
            partialData.forEachEdgeIndexed { ta, tb, tc, tabIsEdge, tbcIsEdge, tcaIsEdge, index ->
                when {
                    (ta == a || ta == b) && (tabIsEdge || tcaIsEdge) -> {// ta is of interest

                    }
                    (tb == a || tb == b) && (tbcIsEdge || tabIsEdge) -> {

                    }
                    (tc == a || tc == b) -> {

                    }
                }
                if (ta == a || tb == a || tc == a || ta == b || tb == b || tc == b) {
                    // there is a neighbor point, and it has an edge somewhere...

                    false
                } else false
            }
            // todo check whether there is a point of interest
            // todo if found, exit early
            false
        }
        TODO()
    }

    // for debugging/initialization purposes
    fun addMesh(mesh: Mesh) {

        val chunk = TriangleOctTree(this, Vector3f(), Vector3f(), max(maxTriangles, mesh.numTriangles))
        val dstIdx = chunk.indices

        val dstPos = positions
        val positions = mesh.positions!!
        val indices = mesh.indices
        val index0 = dstPos.size / 3
        for (element in positions) {
            dstPos.add(element)
        }

        if (indices == null) { // 0 1 2 3 ...
            for (i in 0 until positions.size / 3) {
                dstIdx[i] = index0 + i
            }
            chunk.numTriangles = positions.size / 9
        } else {
            for (i in indices.indices) {
                dstIdx[i] = index0 + indices[i]
            }
            chunk.numTriangles = indices.size / 3
        }

        OnEdgeCalculator.calculateIsOnEdge(mesh, chunk.isOnEdge)

        data.add(chunk)

    }

    /**
     * adds a new, stand-alone triangle
     * this triangle will be treated as if it has no neighbors,
     * because it shares no points with the rest of the mesh
     *
     * for testing, or the start of a new terrain
     * */
    fun addSingleTriangle(a: Vector3f, b: Vector3f, c: Vector3f) {
        val dstPos = positions
        val index0 = dstPos.size / 3
        val chunk = TriangleOctTree(this, Vector3f(), Vector3f(), 1)
        positions.add(a)
        positions.add(b)
        positions.add(c)
        val dstIdx = chunk.indices
        dstIdx[0] = index0
        dstIdx[1] = index0 + 1
        dstIdx[2] = index0 + 2
        chunk.isOnEdge.fill(true)
        data.add(chunk)
    }

    override fun clone(): Component {
        // copy or clone the data?
        TODO("Not yet implemented")
    }

    override val className = "TriTerrain"

}