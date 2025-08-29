package me.anno.ecs.components.mesh.unique

import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.utils.MeshVertexData
import me.anno.engine.ui.render.Frustum
import me.anno.gpu.buffer.CompactAttributeLayout
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.pipeline.Pipeline
import me.anno.utils.assertions.assertEquals
import me.anno.utils.pooling.JomlPools
import me.anno.utils.pooling.Pools
import me.anno.utils.types.Ranges.size
import org.joml.AABBd
import org.joml.AABBf
import org.joml.Matrix4x3

/**
 * renderer for static geometry, that still can be partially loaded/unloaded
 *
 * all instances must use the same material (for now),
 * but we do support fully custom MeshVertexData
 *
 * todo replace this with UniqueMeshRenderer, where little is in common, or the upload can be implemented directly
 *  - e.g. SpriteLayer only needs IntArray, not a StaticBuffer around that
 * todo can we guarantee that we only insert data once? if so, we can delete the mesh immediately :3
 * */
abstract class UniqueMeshRendererImpl<Key, Mesh2>(
    attributes: CompactAttributeLayout, vertexData: MeshVertexData, indexedRendering: Boolean, drawMode: DrawMode
) : UniqueMeshRenderer<Key, MeshEntry<Mesh2>>(attributes, vertexData, indexedRendering, drawMode) {

    /**
     * Implement this method to pack your custom mesh type into a StaticBuffer to be copied around.
     * Returning null will ignore your element.
     * */
    open fun createBuffer(key: Key, mesh: Mesh2): Pair<StaticBuffer, IntArray?>? = null

    /**
     * defines what the world looks like for Raycasting,
     * and for AABBs
     * */
    override fun forEachMesh(pipeline: Pipeline?, callback: (IMesh, Material?, Transform) -> Boolean) {
        var i = 0
        for ((key, entry) in umrVertexData.entries) {
            val transform = getTransform(i++)
            val material = getTransformAndMaterial(key, transform)
            val mesh = entry.mesh as? IMesh ?: continue
            if (callback(mesh, material, transform)) break
        }
    }

    private val boundsF = AABBf()
    override fun getBounds(): AABBf = boundsF

    override fun fillSpace(globalTransform: Matrix4x3, dstUnion: AABBd) {
        // calculate local aabb
        val local = boundsF
        local.clear()
        for (entry in umrVertexData.entries.values) {
            local.union(entry.localBounds)
        }
        localAABB.set(local)

        // calculate global aabb
        val global = globalAABB
        local.transform(globalTransform, global)

        // add the result to the output
        dstUnion.union(global)
    }

    fun add(key: Key, mesh: Mesh2, bounds: AABBd): Boolean {
        if (key in umrVertexData.entries) return false
        val buffer = createBuffer(key, mesh) ?: return false
        val (vertexBuffer, indexBuffer) = buffer
        val entry = MeshEntry(mesh, bounds, vertexBuffer, indexBuffer)
        return add(key, entry)
    }

    override fun shallRenderEntry(frustum: Frustum?, transform: Matrix4x3?, entry: MeshEntry<Mesh2>): Boolean {
        return if (frustum != null) {
            val globalBounds = if (transform != null) {
                val tmpBounds = JomlPools.aabbd.borrow()
                entry.localBounds.transform(transform, tmpBounds)
            } else entry.localBounds
            // frustum culling
            frustum.isVisible(globalBounds)
        } else true
    }

    override fun getVertexRange(mesh: MeshEntry<Mesh2>): IntRange {
        return mesh.vertexRange
    }

    override fun setVertexRange(mesh: MeshEntry<Mesh2>, value: IntRange) {
        mesh.vertexRange = value
    }

    override fun getIndexRange(mesh: MeshEntry<Mesh2>): IntRange {
        return mesh.indexRange
    }

    override fun setIndexRange(mesh: MeshEntry<Mesh2>, value: IntRange) {
        mesh.indexRange = value
    }

    override fun insertVertexData(from: Int, fromData: MeshEntry<Mesh2>, to: IntRange, toData: StaticBuffer) {
        umrVertexData.moveData(from, fromData.vertexBuffer, to, toData)
    }

    override fun insertIndexData(from: Int, fromData: MeshEntry<Mesh2>, to: IntRange, toData: StaticBuffer) {
        assertEquals(0, from)

        // to do if indices are missing, we could fill them procedurally with a compute shader :)
        val offset = fromData.vertexRange.first
        println("Insert indices $offset, $to")
        val fromIndices = fromData.indices
        val movedIndices = Pools.intArrayPool[to.size, false, false]
        if (fromIndices != null) {
            for (i in 0 until to.size) {
                movedIndices[i] = fromIndices[i] + offset
            }
        } else {
            for (i in 0 until to.size) {
                movedIndices[i] = i + offset
            }
        }
        toData.uploadBytesPartially(
            from, movedIndices,
            to.size * 4L, to.start * 4L
        )
        Pools.intArrayPool.returnBuffer(movedIndices)
    }
}