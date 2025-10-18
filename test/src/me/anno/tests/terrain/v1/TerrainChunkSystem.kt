package me.anno.tests.terrain.v1

import me.anno.cache.Promise
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshAttributes.color0
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.ui.render.RenderView
import me.anno.maths.chunks.cartesian.ChunkSystem

class TerrainChunkSystem(val childrenContainer: Entity) :
    ChunkSystem<TerrainChunk, TerrainElement>(xBits, 0, zBits), OnUpdate {

    companion object {
        const val xBits = 5
        const val zBits = 5
        const val sz = 1 shl xBits
        const val sx = 1 shl zBits
    }

    override fun createChunk(chunkX: Int, chunkY: Int, chunkZ: Int, size: Int, result: Promise<TerrainChunk>) {
        result.value = TerrainChunk(chunkX, chunkZ)
    }

    override fun getIndex(localX: Int, localY: Int, localZ: Int): Int {
        return localX + localZ * (sz + 1)
    }

    private fun setElement(container: TerrainChunk, index: Int, value: TerrainElement) {
        val mesh = container.getMeshOrNull()
        mesh.positions!![index * 3 + 1] = value.height
        mesh.color0!![index] = value.color
    }

    override fun setElement(
        container: TerrainChunk, localX: Int, localY: Int, localZ: Int,
        index: Int, element: TerrainElement
    ): Boolean {
        setElement(container, index, element)
        // update neighbor chunks, too
        if (localX == 0) {
            setElement(
                getChunk(container.xi - 1, 0, container.zi, true)!!.waitFor()!!,
                getIndex(sz, 0, localZ), element
            )
        }
        if (localZ == 0) {
            setElement(
                getChunk(container.xi, 0, container.zi - 1, true)!!.waitFor()!!,
                getIndex(localX, 0, sx), element
            )
            if (localX == 0) {
                setElement(
                    getChunk(container.xi - 1, 0, container.zi - 1, true)!!.waitFor()!!,
                    getIndex(sz, 0, sx), element
                )
            }
        }
        return true
    }

    override fun getElement(
        container: TerrainChunk,
        localX: Int, localY: Int, localZ: Int, index: Int
    ): TerrainElement {
        val mesh = container.getMeshOrNull()
        return TerrainElement(mesh.positions!![index * 3 + 1], mesh.color0!![index])
    }

    val visibleChunks = HashSet<TerrainChunk>()

    fun manageChunkLoading() {
        val rv = RenderView.currentInstance ?: return
        val x0 = (rv.orbitCenter.x / sz).toInt()
        val z0 = (rv.orbitCenter.z / sx).toInt()
        for (zi in -10..10) {
            for (xi in -10..10) {
                val visible = xi * xi + zi * zi < 81
                val chunk = getChunk(x0 + xi, 0, z0 + zi, visible)
                    ?.waitFor(!visible)
                if (chunk != null && (chunk in visibleChunks) != visible) {
                    if (visible) {
                        val child = Entity("${chunk.xi}/${chunk.zi}")
                            .setPosition(chunk.dx.toDouble(), 0.0, chunk.dz.toDouble())
                            .add(chunk)
                        childrenContainer.add(child)
                        visibleChunks.add(chunk)
                    } else {
                        childrenContainer.remove(chunk.entity!!)
                        visibleChunks.remove(chunk)
                    }
                }
            }
        }
    }

    override fun onUpdate() {
        manageChunkLoading()
    }
}
