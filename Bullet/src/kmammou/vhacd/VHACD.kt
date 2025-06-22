package kmammou.vhacd

import com.bulletphysics.linearmath.convexhull.HullDesc
import com.bulletphysics.linearmath.convexhull.HullFlags
import com.bulletphysics.linearmath.convexhull.HullLibrary
import com.bulletphysics.linearmath.convexhull.HullResult
import kmammou.vhacd.HullPair.Companion.hullPairQueue
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshIterators.forEachTriangle
import me.anno.maths.Maths.sq
import me.anno.maths.bvh.BLASNode
import me.anno.maths.bvh.BVHBuilder
import me.anno.maths.bvh.SplitMethod
import me.anno.mesh.MeshUtils.countPoints
import me.anno.mesh.MeshUtils.countTriangles
import me.anno.utils.Clock
import me.anno.utils.algorithms.ForLoop.forLoopSafely
import me.anno.utils.assertions.assertTrue
import me.anno.utils.hpc.ProcessingQueue
import me.anno.utils.structures.arrays.IntArrayList
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.max

private val LOGGER = LogManager.getLogger("V-HACD")
private val clock = Clock(LOGGER)

var maxRecursionDepth = 10

val pendingHulls = ArrayList<VoxelHull>()
var isCancelled = false

var scale = 1f
var invScale = 1f
val offset = Vector3f()

fun copyInputMesh(mesh: Mesh) {
    val bounds = mesh.getBounds()
    bounds.getCenter(offset)
    scale = bounds.maxDelta
    invScale = 1f / max(scale, 1e-38f)

    var numTooSmall = 0
    val indices = IntArrayList(mesh.countTriangles().toInt() * 3)
    val vi = VertexIndex(0.001f, offset, invScale, mesh.countPoints())
    mesh.forEachTriangle { a, b, c ->
        val ai = vi.map(a)
        val bi = vi.map(b)
        val ci = vi.map(c)
        if (ai == bi || ai == ci || bi == ci) {
            numTooSmall++
        } else {
            indices.add(ai, bi, ci)
        }
        false
    }
    clock.stop("Indexing triangles")
    if (isCancelled) return

    if (numTooSmall > 0) {
        // or they are just very small... why is this not handling small triangles???
        // do we need to apply mesh simplification first???
        LOGGER.warn("Found $numTooSmall degenerate/too-small triangles")
    }

    val vertices = vi.vertices
    if (vertices.isEmpty()) {
        LOGGER.warn("All triangles are too small")
        return
    }

    val blas = buildBLAS(vertices, indices)
    clock.stop("Building BLAS")

    val voxelize = voxelize(vertices, indices, blas)
    clock.stop("Voxelize")

    val voxelHull = voxelize.toConvexHull()
    pendingHulls.add(voxelHull)
    clock.stop("Initial Convex Hull")
    overallHullVolume = voxelHull.convexHull.volume
}

class Voxelize {
    fun toConvexHull(): VoxelHull {
        TODO()
    }
}

class VoxelHull(val convexHull: ConvexHull) {

    fun raycast(){
        // returns the distance
        TODO()
    }

}
// default fill mode is flood fill
var resolution = 400_000
fun voxelize(vertices: List<Vector3f>, indices: IntArrayList, blas: BLASNode): Voxelize {

    TODO()
}

fun List<Vector3f>.toFloatArray(): FloatArray {
    val dst = FloatArray(size * 3)
    for (i in indices) {
        this[i].get(dst, i * 3)
    }
    return dst
}

fun buildBLAS(vertices: List<Vector3f>, indices: IntArrayList): BLASNode {
    val mesh = Mesh()
    mesh.positions = vertices.toFloatArray()
    mesh.indices = indices.toIntArray()
    return BVHBuilder.buildBLAS(mesh, SplitMethod.MEDIAN_APPROX, 16)!!
}

fun performConvexDecomposition() {
    val maxHulls = 1 shl maxRecursionDepth

    while (pendingHulls.isNotEmpty() && !isCancelled) {
        TODO()
    }

    clock.stop("Convex Decomposition")
    if (isCancelled) return

    // Give each convex hull a unique guid
    meshId = 0
    hulls.clear()

    // Build the convex hull id map
    for (vh in voxelHulls) {
        if (isCancelled) return

        val ch = copyConvexHull(vh.convexHull) // todo why? is this needed???
        ch.meshId = meshId++

        hulls.add(ch)
    }
    clock.stop("ConvexHull initialization")

    // why?
    val hullCount = hulls.size
    if (hullCount > maxConvexHulls) {
        val costMatrixSize = (sq(hullCount) - hullCount) ushr 1
        val tasks = ArrayList<CostTask>(costMatrixSize)

        for (i in 1 until hullCount) {
            if (isCancelled) break
            val hull1 = hulls[i]
            for (j in 0 until i) {
                if (isCancelled) break
                addTask(hull1, hulls[j], tasks)
            }
        }
        clock.stop("Initial Costs")

        if (isCancelled) {
            queue.clear()
            return
        }

        if (parallelize) {
            // todo if cancelled, cancel waiting
            queue.waitUntilDone(true)
        }

        addTasksToQueue(tasks)
        clock.stop("Cost Matrix")

        clock.start()
        val maxMergeCount = hullCount - maxConvexHulls
        val startCount = hullCount // could be an alias...
        while (!isCancelled && hulls.size > maxConvexHulls) {
            if (clock.timeSinceStop > 0.1) {
                clock.stop("Merging ${startCount - hulls.size}/$maxMergeCount")
            }

            val pair = hullPairQueue.poll() ?: break
            val hull1 = hulls[pair.hull1MeshId]
            val hull2 = hulls[pair.hull2MeshId]

            if (hull1.meshId < 0 || hull2.meshId < 0) continue // hulls in pair have become invalid (already merged)

            removeHull(hull1)
            removeHull(hull2)

            val combined = computeCombinedVertexHull(hull1, hull2)
            combined.meshId = meshId++

            // add all tasks for this new hull
            for (i in hulls.indices) {
                val hull = hulls[i]
                if (hull.meshId < 0) continue
                addTask(combined, hull, tasks)
            }

            // finally add this hull, too
            hulls.add(combined)

            if (parallelize) {
                queue.waitUntilDone(true) // todo stop waiting if cancelled
            }

            addTasksToQueue(tasks)
        }
        clock.total("Merging Hulls")
        hulls.removeIf { it.meshId < 0 }
    }

    finalizeResults()
}

fun addTasksToQueue(tasks: ArrayList<CostTask>) {
    for (i in tasks.indices) {
        val task = tasks[i]
        addCostToPriorityQueue(task)
    }
    tasks.clear()
}

fun removeHull(hull: ConvexHull) {
    assertTrue(hull.meshId >= 0)
    hull.meshId = -1
}

fun addTask(hull1: ConvexHull, hull2: ConvexHull, tasks: ArrayList<CostTask>) {
    val task = CostTask(hull1, hull2)
    if (!doFastCost(task)) {
        tasks.add(task)
        if (parallelize) {
            queue += { performMergeCostTask(task) }
        } else {
            performMergeCostTask(task)
        }
    }
}

fun addCostToPriorityQueue(task: CostTask) {
    hullPairQueue.add(HullPair(task.hull1.meshId, task.hull2.meshId, task.concavity))
}

var parallelize = true
val queue = ProcessingQueue("V-HACD")

fun finalizeResults() {
    for (i in hulls.indices) {
        var hull = hulls[i]
        if (hull.vertices.size > maxVerticesPerHull) {
            hull = computeReducedConvexHull(hull, maxVerticesPerHull)
            hulls[i] = hull
        }
        scaleOutputConvexHull(hull)
    }
    clock.stop("Finalize results")
}

fun scaleOutputConvexHull(hull: ConvexHull) {
    val vertices = hull.vertices
    for (i in vertices.indices) {
        val vertex = vertices[i]
        vertex.mul(scale.toDouble()).add(offset)
    }
}

var maxConvexHulls = 7
var maxVerticesPerHull = 40

fun copyConvexHull(hull: ConvexHull): ConvexHull {
    TODO()
}

val voxelHulls = ArrayList<VoxelHull>()
val hulls = ArrayList<ConvexHull>()

class CostTask(val hull1: ConvexHull, val hull2: ConvexHull) {
    var concavity = 0.0
}

var meshId = 0
var overallHullVolume = 0.0

fun computeConvexHullVolume(hull: ConvexHull): Double {
    val center = Vector3d()
    val vertices = hull.vertices
    for (i in vertices.indices) {
        center.add(vertices[i])
    }
    center.div(vertices.size.toDouble())

    var volume = 0.0
    val triangles = hull.triangles
    forLoopSafely(triangles.size, 3) { idx ->
        val a = vertices[triangles.get(idx)]
        val b = vertices[triangles.get(idx + 1)]
        val c = vertices[triangles.get(idx + 2)]
        volume += computeVolume4(a, b, c, center)
    }
    return volume
}

fun computeVolume4(a: Vector3d, b: Vector3d, c: Vector3d, d: Vector3d): Double {
    val ad = a - d
    val bd = b - d
    val cd = c - d
    val bcd = bd.cross(cd)
    return ad.dot(bcd)
}

fun computeConcavity(volumeSeparate: Double, volumeCombined: Double, volumeMesh: Double): Double =
    (volumeSeparate - volumeCombined) / volumeMesh

fun doFastCost(task: CostTask): Boolean {
    val bounds1 = task.hull1.bounds
    val bounds2 = task.hull2.bounds
    val overlap = bounds1.testAABB(bounds2)
    if (!overlap) {
        val joined = bounds1.union(bounds2, AABBd())
        val combinedVolume = joined.volume
        val concavity = computeConcavity(task.hull1.volume + task.hull2.volume, combinedVolume, overallHullVolume)
        hullPairQueue.add(HullPair(task.hull1.meshId, task.hull2.meshId, concavity))
    }
    return !overlap
}

fun performMergeCostTask(task: CostTask) {
    val combined = computeCombinedVertexHull(task.hull1, task.hull2)
    task.concavity = computeConcavity(task.hull1.volume + task.hull2.volume, combined.volume, overallHullVolume)
}

fun computeReducedConvexHull(hull: ConvexHull, maxVertices: Int): ConvexHull {

    if (hull.vertices.size <= maxVertices) {
        // todo do we need a copy here?
        return hull
    }

    val hullDesc = HullDesc()
    hullDesc.flags = HullFlags.TRIANGLES
    hullDesc.vcount = hull.vertices.size
    hullDesc.vertices = hull.vertices
    hullDesc.maxVertices = maxVertices

    val hullLibrary = HullLibrary()
    val hullResult = HullResult()
    assertTrue(hullLibrary.createConvexHull(hullDesc, hullResult))

    val dstVertices = hullResult.outputVertices
    dstVertices.subList(hullResult.numOutputVertices, dstVertices.size).clear()
    return ConvexHull(dstVertices, hullResult.indices)
}

fun computeCombinedVertexHull(
    hull1: ConvexHull,
    hull2: ConvexHull
): ConvexHull {

    val hullDesc = HullDesc()
    hullDesc.flags = HullFlags.TRIANGLES
    hullDesc.vcount = hull1.vertices.size + hull2.vertices.size
    hullDesc.vertices = hull1.vertices + hull2.vertices

    val hullLibrary = HullLibrary()
    val hullResult = HullResult()
    assertTrue(hullLibrary.createConvexHull(hullDesc, hullResult))

    val dstVertices = hullResult.outputVertices
    dstVertices.subList(hullResult.numOutputVertices, dstVertices.size).clear()
    return ConvexHull(dstVertices, hullResult.indices)
}
