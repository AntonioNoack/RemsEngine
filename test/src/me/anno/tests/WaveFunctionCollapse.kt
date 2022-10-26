package me.anno.tests

import me.anno.Engine
import me.anno.gpu.drawing.DrawTextures
import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import me.anno.image.ImageCPUCache
import me.anno.image.raw.IntImage
import me.anno.maths.Maths
import me.anno.maths.geometry.WaveFunctionCollapse
import me.anno.ui.debug.TestDrawPanel
import me.anno.utils.Color.a
import me.anno.utils.OS
import me.anno.utils.structures.maps.LazyMap
import java.util.*
import kotlin.math.min

fun main() {
    // load a set of images
    val wfc = WaveFunctionCollapse()
    wfc.enableMinimalEntropyHeuristic = true
    var id = 0
    // val src = downloads.getChild("2d/tilesetCircuit.png") // 14x14
    val src = OS.downloads.getChild("2d/caveTilesetOpenGameArt-3.png") // 16x16
    val tileW = 16
    val tileH = 16
    val tileAtlas = ImageCPUCache[src, false]!!
    for (yi in 0 until tileAtlas.height step tileH) {
        for (xi in 0 until tileAtlas.width step tileW) {
            val pixels = IntArray(tileW * tileH)
            var i = 0
            for (y in 0 until tileH) {
                for (x in 0 until tileW) {
                    pixels[i++] = tileAtlas.getRGB(xi + x, yi + y)
                }
            }
            if (pixels.any { it.a() > 64 }) {
                val tile = IntImage(tileW, tileH, pixels, tileAtlas.hasAlphaChannel)
                wfc.types.add(WaveFunctionCollapse.ImageCellType(id++, tile))
            }
        }
    }
    wfc.calculateEdges()
    wfc.addTransformedTypes(allowMirrorX = true, allowMirrorY = true, 4)
    wfc.defineNeighborsByImages(16)
    for (idx in wfc.types.indices) {
        println("$idx -> ${wfc.types[idx].neighbors.joinToString()}")
    }
    println("num types: ${wfc.types.size}")
    wfc.removeInvalidCells()
    if (wfc.types.isEmpty())
        throw IllegalStateException("Cannot connect any tiles")
    val sizeX = 64
    val sizeY = 64
    val random = Random(Engine.nanoTime)
    val grid = wfc.collapseInit(sizeX, sizeY)
    val texToImage = LazyMap({ key: Image -> Texture2D(key, false) }, wfc.types.size)
    var hasRemaining = true
    TestDrawPanel.testDrawing {
        it.clear()
        val t0 = Engine.nanoTime
        while (hasRemaining && Engine.nanoTime - t0 < 1e9 / 60)
            hasRemaining = wfc.collapseStep(sizeX, sizeY, grid, random)
        for (y in 0 until min(sizeY, Maths.ceilDiv(it.h, tileH))) {
            var i = y * sizeX
            for (x in 0 until min(sizeX, Maths.ceilDiv(it.w, tileW))) {
                // draw tile onto result
                val cell = (grid[i++].result as? WaveFunctionCollapse.ImageCellType) ?: continue
                val tile = cell.image
                DrawTextures.drawTexture(x * tileW, y * tileH, tileW, tileH, texToImage[tile]!!, false, -1)
            }
        }
    }
}