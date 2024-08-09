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

    fun setBlock(coords: Vector3i, block: Byte) {
        world.setElementAt(coords.x, coords.y, coords.z, true, block)
        val chunkId = coordsToChunkId(coords)
        invalidateChunkAt(chunkId)
        val localCoords = Vector3i(
            posMod(coords.x, csx),
            posMod(coords.y, csy),
            posMod(coords.z, csz),
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
        saveSystem.get(chunkId, true) { changesInChunk ->
            changesInChunk[localCoords] = block
            saveSystem.put(chunkId, changesInChunk)
        }
    }

    fun coordsToChunkId(coords: Vector3i): Vector3i {
        return Vector3i(
            floorDiv(coords.x, csx),
            floorDiv(coords.y, csy),
            floorDiv(coords.z, csz)
        )
    }

    fun invalidateChunkAt(coords: Vector3i) {
        chunkLoader.worker += {
            chunkLoader.generateChunk(coords)
        }
    }

    override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
        // todo is this still called???
        // find, which block was clicked
        // expensive way, using raycasting:
        val query = RayQuery(
            renderView.cameraPosition,
            renderView.getMouseRayDirection(),
            1e3
        )
        // todo also implement cheaper raytracing (to show how) going block by block
        //  then we can throw away the meshes and save even more memory :3
        val hitSomething = Raycast.raycastClosestHit(scene, query)
        if (hitSomething) {
            when (button) {
                Key.BUTTON_LEFT -> {
                    // remove block
                    val coords = getCoords(query, +1e-3)
                    setBlock(coords, 0)
                }
                Key.BUTTON_RIGHT -> {
                    // add block
                    val coords = getCoords(query, -1e-3)
                    setBlock(coords, inHandBlock)
                }
                Key.BUTTON_MIDDLE -> {
                    // get block
                    val coords = getCoords(query, +1e-3)
                    inHandBlock = world.getElementAt(coords.x, coords.y, coords.z, true) ?: 0
                }
                else -> {}
            }
        }
    }
}