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
import me.anno.utils.pooling.JomlPools
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
    attributes: CompactAttributeLayout, vertexData: MeshVertexData, drawMode: DrawMode
) : UniqueMeshRenderer<Key, MeshEntry<Mesh2>>(attributes, vertexData, drawMode) {

    /**
     * Implement this method to pack your custom mesh type into a StaticBuffer to be copied around.
     * Returning null will ignore your element.
     * */
    abstract fun createBuffer(key: Key, mesh: Mesh2): StaticBuffer?

    /**
     * defines what the world looks like for Raycasting,
     * and for AABBs
     * */
    override fun forEachMesh(pipeline: Pipeline?, callback: (IMesh, Material?, Transform) -> Boolean) {
        var i = 0
        for ((key, entry) in entries) {
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
        for (i in sortedEntries.indices) {
            val entry = sortedEntries[i]
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
        if (key in entries) return false
        val buffer = createBuffer(key, mesh) ?: return false
        val entry = MeshEntry(mesh, bounds, buffer)
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

    override fun setRange(key: MeshEntry<Mesh2>, value: IntRange) {
        key.range = value
    }

    override fun getRange(key: MeshEntry<Mesh2>): IntRange {
        return key.range
    }

    override fun deallocate(data: StaticBuffer) {
        // we just swap between buffer0 and buffer1, so we must not destroy anything
    }

    override fun insertData(from: Int, fromData: MeshEntry<Mesh2>, to: IntRange, toData: StaticBuffer) {
        moveData(from, fromData.buffer, to, toData)
    }
}