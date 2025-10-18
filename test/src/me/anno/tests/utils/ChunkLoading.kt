package me.anno.tests.utils

import me.anno.cache.Promise
import me.anno.cache.ICacheData
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.maths.chunks.PlayerLocation
import me.anno.maths.chunks.cartesian.SingleChunkSystem
import me.anno.ui.debug.TestDrawPanel.Companion.testDrawing
import me.anno.utils.Color.white
import me.anno.utils.Color.withAlpha
import org.joml.Vector3i

/**
 * tests ChunkSystem.updateVisibility visually
 * */
fun main() {
    val loadedChunks = HashSet<Vector3i>()

    class Chunk(val coords: Vector3i) : ICacheData {
        override fun destroy() {
            synchronized(loadedChunks) {
                loadedChunks.remove(coords)
            }
        }
    }

    val chunks = object : SingleChunkSystem<Chunk>() {
        override fun createChunk(chunkX: Int, chunkY: Int, chunkZ: Int, size: Int, result: Promise<Chunk>) {
            val chunk = Chunk(Vector3i(chunkX, chunkY, chunkZ))
            result.value = chunk
            synchronized(loadedChunks) {
                loadedChunks.add(chunk.coords)
            }
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
        synchronized(loadedChunks) {
            for (chunk in loadedChunks) {
                drawRect(chunk.x * cellSize, chunk.y * cellSize, cellSize - 1, cellSize - 1, color)
            }
        }
    }
}