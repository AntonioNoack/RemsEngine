package me.anno.tests.utils

import me.anno.ecs.components.chunks.PlayerLocation
import me.anno.ecs.components.chunks.cartesian.SingleChunkSystem
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.ui.debug.TestDrawPanel.Companion.testDrawing
import me.anno.utils.Color.white
import me.anno.utils.structures.tuples.IntPair

/**
 * tests ChunkSystem.updateVisibility visually
 * */
fun main() {
    val loadedChunks = HashSet<IntPair>()
    val chunks = object : SingleChunkSystem<IntPair>() {
        override fun createChunk(chunkX: Int, chunkY: Int, chunkZ: Int, size: Int): IntPair {
            return IntPair(chunkX, chunkY)
        }

        override fun onCreateChunk(chunk: IntPair, chunkX: Int, chunkY: Int, chunkZ: Int) {
            loadedChunks.add(chunk)
        }

        override fun onDestroyChunk(chunk: IntPair, chunkX: Int, chunkY: Int, chunkZ: Int) {
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
        for ((x, y) in loadedChunks) {
            drawRect(x * cellSize, y * cellSize, cellSize - 1, cellSize - 1, white)
        }
    }
}