package me.anno.tests.mesh.unique

import me.anno.ecs.Entity
import me.anno.engine.raycast.RayQuery
import me.anno.engine.raycast.Raycast
import me.anno.engine.ui.control.DraggingControls
import me.anno.engine.ui.render.RenderView
import me.anno.input.Key
import me.anno.maths.Maths.posMod
import me.anno.tests.mesh.unique.ItemPanel.Companion.inHandBlock
import me.anno.tests.utils.TestWorld
import org.joml.Vector3d
import org.joml.Vector3i
import java.lang.Math.floorDiv
import kotlin.math.floor

class CreativeControls(
    renderView: RenderView, val scene: Entity, val world: TestWorld,
    val saveSystem: SaveLoadSystem, val chunkLoader: ChunkLoader
) : DraggingControls(renderView) {

    val csx = world.sizeX
    val csy = world.sizeY
    val csz = world.sizeZ

    fun getCoords(query: RayQuery, delta: Double): Vector3i {
        val pos = query.result.positionWS
        val dir = query.direction
        dir.mulAdd(delta, pos, pos)
        return Vector3i(floor(pos.x).toInt(), floor(pos.y).toInt(), floor(pos.z).toInt())
    }

    fun setBlock(positions: Vector3i, block: Byte) {
        world.setElementAt(positions.x, positions.y, positions.z, true, block)
        val chunkId = coordsToChunkId(positions)
        invalidateChunkAt(chunkId)
        val localCoords = Vector3i(
            posMod(positions.x, csx),
            posMod(positions.y, csy),
            posMod(positions.z, csz),
        )
        // when we're on the edge, and we remove a block (set a transparent one), we need to invalidate our neighbors, too
        if (block == TestWorld.air) {
            if (localCoords.x == 0) invalidateChunkAt(Vector3i(chunkId).sub(1, 0, 0))
            if (localCoords.y == 0) invalidateChunkAt(Vector3i(chunkId).sub(0, 1, 0))
            if (localCoords.z == 0) invalidateChunkAt(Vector3i(chunkId).sub(0, 0, 1))
            if (localCoords.x == csx - 1) invalidateChunkAt(Vector3i(chunkId).add(1, 0, 0))
            if (localCoords.y == csy - 1) invalidateChunkAt(Vector3i(chunkId).add(0, 1, 0))
            if (localCoords.z == csz - 1) invalidateChunkAt(Vector3i(chunkId).add(0, 0, 1))
        }
        saveSystem.get(chunkId) { changesInChunk ->
            changesInChunk[localCoords] = block
            saveSystem.put(chunkId, changesInChunk)
        }
    }

    fun coordsToChunkId(positions: Vector3i): Vector3i {
        return Vector3i(
            floorDiv(positions.x, csx),
            floorDiv(positions.y, csy),
            floorDiv(positions.z, csz)
        )
    }

    fun invalidateChunkAt(positions: Vector3i) {
        chunkLoader.worker += {
            chunkLoader.generateChunk(positions)
        }
    }

    override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
        // todo is this still called???
        // find, which block was clicked
        // expensive way, using raycasting:
        val query = renderView.rayQuery(1e3)
        // todo also implement cheaper raytracing (to show how) going block by block
        //  then we can throw away the meshes and save even more memory :3
        val hitSomething = Raycast.raycast(scene, query)
        if (hitSomething) {
            when (button) {
                Key.BUTTON_LEFT -> {
                    // remove block
                    val positions = getCoords(query, +1e-3)
                    setBlock(positions, 0)
                }
                Key.BUTTON_RIGHT -> {
                    // add block
                    val positions = getCoords(query, -1e-3)
                    setBlock(positions, inHandBlock)
                }
                Key.BUTTON_MIDDLE -> {
                    // get block
                    val positions = getCoords(query, +1e-3)
                    inHandBlock = world.getElementAt(positions.x, positions.y, positions.z, true) ?: 0
                }
                else -> {}
            }
        }
    }
}