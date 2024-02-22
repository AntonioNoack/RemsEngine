package me.anno.tests.utils

import me.anno.maths.chunks.PlayerLocation
import me.anno.maths.chunks.cartesian.SingleChunkSystem
import me.anno.gpu.GFXState
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.ui.debug.TestDrawPanel.Companion.testDrawing
import me.anno.utils.Color.white
import me.anno.utils.Color.withAlpha
import org.joml.Vector3i

/**
 * tests ChunkSystem.updateVisibility visually
 * */
fun main() {
    val loadedChunks = HashSet<Vector3i>()
    val chunks = object : SingleChunkSystem<Vector3i>() {
        override fun createChunk(chunkX: Int, chunkY: Int, chunkZ: Int, size: Int): Vector3i {
            return Vector3i(chunkX, chunkY, chunkZ)
        }

        override fun onCreateChunk(chunk: Vector3i, chunkX: Int, chunkY: Int, chunkZ: Int) {
            loadedChunks.add(chunk)
        }

        override fun onDestroyChunk(chunk: Vector3i, chunkX: Int, chunkY: Int, chunkZ: Int) {
            loadedChunks.remove(chunk)
        }
    }
    val cellSize = 10
    testDrawing("Chunk System") {
        it.clear()
        val window = it.window!!
        chunks.updateVisibility(
            5.0, 10.0, listOf(
                PlayerLocation(window.mouseX.toDouble() / cellSize, window.mouseY.toDouble() / cellSize, 0.0)
            )
        )
        val color = white.withAlpha(70)
        for (chunk in loadedChunks) {
            drawRect(chunk.x * cellSize, chunk.y * cellSize, cellSize - 1, cellSize - 1, color)
        }
    }
}