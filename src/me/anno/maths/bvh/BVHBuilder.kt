package me.anno.maths.bvh

import me.anno.ecs.Component
import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.Mesh
import me.anno.gpu.M4x3Delta.set4x3delta
import me.anno.gpu.pipeline.PipelineStageImpl
import me.anno.gpu.texture.Texture2D
import me.anno.maths.Maths
import me.anno.maths.Maths.max
import me.anno.maths.bvh.SplitMethod.Companion.mid
import me.anno.maths.bvh.SplitMethod.Companion.pivot0
import me.anno.utils.Clock
import me.anno.utils.hpc.ProcessingGroup
import me.anno.utils.pooling.JomlPools
import org.apache.logging.log4j.LogManager
import org.joml.AABBf
import org.joml.Matrix4x3f
import org.joml.Vector3d
import org.joml.Vector3f
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.sqrt

object BVHBuilder {

    private val LOGGER = LogManager.getLogger(BVHBuilder::class)

    fun createTLASLeaf(
        mesh: Mesh, blas: BLASNode, transform: Transform, component: Component?,
        cameraPosition: Vector3d, worldScale: Float
    ): TLASLeaf {
        val drawMatrix = transform.getDrawMatrix()
        val localToWorld = Matrix4x3f().set4x3delta(drawMatrix, cameraPosition, worldScale)
        val worldToLocal = Matrix4x3f()
        localToWorld.invert(worldToLocal)
        val centroid = Vector3f()
        val localBounds = mesh.getBounds()
        centroid.set(localBounds.centerX, localBounds.centerY, localBounds.centerZ)
        localToWorld.transformPosition(centroid)
        val globalBounds = AABBf()
        localBounds.transform(localToWorld, globalBounds)
        return TLASLeaf(centroid, localToWorld, worldToLocal, blas, globalBounds, component)
    }

    fun buildTLAS(
        scene: PipelineStageImpl, // filled with meshes
        cameraPosition: Vector3d, worldScale: Float,
        splitMethod: SplitMethod, maxNodeSize: Int
    ): TLASNode? {
        val clock = Clock(LOGGER)
        val sizeGuess = scene.nextInsertIndex + scene.instanced.data.sumOf { it.size }
        val objects = ArrayList<TLASLeaf>(max(sizeGuess, 16))
        // add non-instanced objects
        val dr = scene.drawRequests

        for (index in 0 until scene.nextInsertIndex) {
            val dri = dr[index]
            // to do theoretically, we'd need to respect the material override as well,
            // but idk how to do materials yet...
            val mesh = dri.mesh as? Mesh ?: continue
            val blas = mesh.raycaster ?: buildBLAS(mesh, splitMethod, maxNodeSize) ?: continue
            mesh.raycaster = blas
            val transform = dri.transform
            objects += createTLASLeaf(mesh, blas, transform, dri.component, cameraPosition, worldScale)
        }
        // add all instanced objects
        scene.instanced.data.forEach { mesh, _, _, stack ->
            if (mesh is Mesh) {
                val blas = mesh.raycaster ?: buildBLAS(mesh, splitMethod, maxNodeSize)
                if (blas != null) {
                    mesh.raycaster = blas
                    for (i in 0 until stack.size) {
                        val transform = stack.transforms[i] as Transform
                        objects += createTLASLeaf(mesh, blas, transform, null, cameraPosition, worldScale)
                    }
                }
            }
        }
        clock.stop("Creating BLASes")
        LOGGER.info("Building TLAS from ${objects.size} objects")
        if (objects.isEmpty()) return null
        val tlas = buildTLAS(objects, splitMethod)
        clock.stop("Creating TLAS")
        return tlas
    }

    fun buildBLAS(mesh: Mesh, splitMethod: SplitMethod, maxNodeSize: Int): BLASNode? {
        val srcPos = mesh.positions ?: return null
        mesh.ensureNorTanUVs()
        val srcNor = mesh.normals!!
        val indices = mesh.indices ?: IntArray(srcPos.size / 3) { it }
        val srcUVs = mesh.uvs
        val geometryData = GeometryData(srcPos, srcNor, srcUVs, indices, mesh.color0)
        return recursiveBuildBLAS(srcPos, indices, 0, indices.size / 3, maxNodeSize, splitMethod, geometryData)
    }

    fun buildTLAS(
        objects: ArrayList<TLASLeaf>,
        splitMethod: SplitMethod,
    ) = buildTLAS(objects, splitMethod, 0, objects.size)

    fun buildTLAS(
        objects: ArrayList<TLASLeaf>, splitMethod: SplitMethod,
        start: Int, end: Int // array indices
    ): TLASNode {
        val count = end - start
        if (count <= 1) {
            // leaf was already created by parent buildTLAS()
            return objects[start]
        } else {

            // bounds of center of primitives for efficient split dimension
            val centroidBounds = JomlPools.aabbf.create()
            for (index in start until end) {
                centroidBounds.union(objects[index].centroid)
            }

            // split dimension
            val dim = centroidBounds.maxDim()
            // partition primitives into two sets & build children
            val mid = if (isIllegalSplit(centroidBounds, dim)) {
                // creating a leaf node here would be illegal, because maxNodesPerPoint would be violated
                // nodes must be split randomly -> just skip all splitting computations
                mid(start, end)
            } else {
                // partition based on split method
                // for the very start, we'll only implement the simplest methods
                val pivot = pivot0(centroidBounds, dim)
                splitMethod.partitionTLASLeaves(objects, start, end, dim, pivot)
            }
            JomlPools.aabbf.sub(1)

            val n0 = buildTLAS(objects, splitMethod, start, mid)
            val n1 = buildTLAS(objects, splitMethod, mid, end)
            return TLASBranch(dim, n0, n1)
        }
    }

    fun isIllegalSplit(centroidBounds: AABBf, dim: Int): Boolean {
        return centroidBounds.getMax(dim) == centroidBounds.getMin(dim)
    }

    // todo parallelize using GPU, if possible
    //  because this can be quite slow, taking 600ms for the dragon with 800k triangles
    // -> improved it a bit, it's now taking 42ms for the dragon :) (still slow, but much better)

    private fun createBLASLeaf(
        positions: FloatArray,
        indices: IntArray,
        start: Int,
        end: Int,
        geometryData: GeometryData
    ): BLASLeaf {
        val bounds = AABBf()
        for (i in start * 3 until end * 3) {
            val ci = indices[i] * 3
            bounds.union(positions[ci], positions[ci + 1], positions[ci + 2])
        }
        val count = end - start
        return BLASLeaf(start, count, geometryData, bounds)
    }

    private fun getCentroidX3(positions: FloatArray, indices: IntArray, triIndex: Int, dst: Vector3f): Vector3f {
        val pointIndex = triIndex * 3
        var ai = indices[pointIndex] * 3
        var bi = indices[pointIndex + 1] * 3
        var ci = indices[pointIndex + 2] * 3
        return dst.set(
            positions[ai++] + positions[bi++] + positions[ci++],
            positions[ai++] + positions[bi++] + positions[ci++],
            positions[ai] + positions[bi] + positions[ci]
        )
    }

    private fun calculateCentroidX3(positions: FloatArray, indices: IntArray, start: Int, end: Int, dst: AABBf) {
        dst.clear()
        val tmp = JomlPools.vec3f.borrow()
        for (triIndex in start until end) {
            dst.union(getCentroidX3(positions, indices, triIndex, tmp))
        }
    }

    val centroidTime = AtomicLong()
    val splitTime = AtomicLong()
    private fun recursiveBuildBLAS(
        positions: FloatArray,
        indices: IntArray,
        start: Int, end: Int, // triangle indices
        maxNodeSize: Int,
        splitMethod: SplitMethod,
        geometryData: GeometryData
    ): BLASNode {

        val count = end - start
        if (count <= maxNodeSize) {
            return createBLASLeaf(positions, indices, start, end, geometryData)
        }

        // bounds of center of primitives for efficient split dimension
        val t0 = System.nanoTime()
        val centroidBoundsX3 = JomlPools.aabbf.create()
        calculateCentroidX3(positions, indices, start, end, centroidBoundsX3)
        val t1 = System.nanoTime()
        centroidTime.addAndGet(t1 - t0)

        // split dimension
        val dim = centroidBoundsX3.maxDim()
        // println("centroid ${centroidBounds.deltaX}, ${centroidBounds.deltaY}, ${centroidBounds.deltaZ} -> $dim")

        // partition primitives into two sets & build children
        val mid = if (isIllegalSplit(centroidBoundsX3, dim)) {
            // creating a leaf node here would be illegal, because maxNodesPerPoint would be violated
            // nodes must be split randomly -> just skip all splitting computations
            mid(start, end)
        } else {
            // partition based on split method
            // for the very start, we'll only implement the simplest methods
            val pivot = pivot0(centroidBoundsX3, dim)
            splitMethod.partitionTriangles(positions, indices, start, end, dim, pivot)
        }
        JomlPools.aabbf.sub(1)

        val t2 = System.nanoTime()
        splitTime.addAndGet(t2 - t1)

        val usingThreading = max(mid - start, mid - end) > 4000
        if (usingThreading) {
            // todo use proper parallel algorithm
            // brings the dragon down from 180ms to 80ms on my Ryzen 9 7950x3d
            // and I think I got similar results on my Ryzen 5 2600
            // -> pretty bad speedup, doesn't scale well at all :(
            // 17% utilisation = 2.7 cores for 2.25x speedup -> ok-ish efficiency
            var n0: BLASNode? = null
            pool += {
                n0 = recursiveBuildBLAS(positions, indices, start, mid, maxNodeSize, splitMethod, geometryData)
            }
            val n1 = recursiveBuildBLAS(positions, indices, mid, end, maxNodeSize, splitMethod, geometryData)
            pool.workUntil { n0 != null }
            return BLASBranch(dim, n0!!, n1)
        } else {
            val n0 = recursiveBuildBLAS(positions, indices, start, mid, maxNodeSize, splitMethod, geometryData)
            val n1 = recursiveBuildBLAS(positions, indices, mid, end, maxNodeSize, splitMethod, geometryData)
            return BLASBranch(dim, n0, n1)
        }
    }

    private val pool = ProcessingGroup("BVHBuilder", 1f)

    fun createTexture(name: String, numElements: Int, pixelsPerElement: Int): Texture2D {
        val requiredPixels = numElements * pixelsPerElement
        val textureWidth = Maths.align(sqrt(requiredPixels.toFloat()).toInt(), pixelsPerElement)
        val textureHeight = Maths.ceilDiv(requiredPixels, textureWidth)
        return Texture2D(name, textureWidth, textureHeight, 1)
    }
}