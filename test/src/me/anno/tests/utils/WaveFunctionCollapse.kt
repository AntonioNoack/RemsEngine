package me.anno.tests.utils

import me.anno.Time
import me.anno.config.DefaultConfig.style
import me.anno.engine.OfficialExtensions
import me.anno.gpu.drawing.DrawTextures
import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import me.anno.image.ImageCache
import me.anno.image.raw.IntImage
import me.anno.maths.geometry.WaveFunctionCollapse
import me.anno.ui.base.groups.MapPanel
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.utils.Color.a
import me.anno.utils.OS
import me.anno.utils.OS.downloads
import me.anno.utils.structures.lists.Lists.any2
import me.anno.utils.structures.maps.LazyMap
import kotlin.random.Random

fun main() {
    // todo this is broken :(
    OfficialExtensions.initForTests()
    // load a set of images
    val wfc = WaveFunctionCollapse()
    wfc.enableMinimalEntropyHeuristic = true
    var id = 0
    val src = downloads.getChild("2d/tilesetCircuit.png") // 14x14
    // val src = downloads.getChild("2d/caveTilesetOpenGameArt-3.png") // 16x16
    val tileW = 16
    val tileH = 16
    val tileAtlas = ImageCache[src].waitFor()!!
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
    val random = Random(Time.nanoTime)
    val grid = wfc.collapseInit(sizeX, sizeY)
    val texToImage = LazyMap({ key: Image -> Texture2D(key, false) }, wfc.types.size)
    var hasRemaining = true
    testUI3("WaveFunctionCollapse") {
        object : MapPanel(style) {
            init {
                minScale.set(1.0 / 16.0)
                maxScale.set(32.0)
            }

            val xs = IntArray(sizeX + 1)
            val ys = IntArray(sizeY + 1)
            override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
                super.draw(x0, y0, x1, y1)
                val t0 = Time.nanoTime
                while (hasRemaining && Time.nanoTime - t0 < 1e9 / 60)
                    hasRemaining = wfc.collapseStep(sizeX, sizeY, grid, random)
                val x0i = x + width / 2
                val y0i = y + height / 2
                for (x in 0..sizeX) xs[x] = x0i + ((x * tileW - center.x) * scale.x).toInt()
                for (y in 0..sizeY) ys[y] = y0i + ((y * tileH - center.y) * scale.y).toInt()
                for (y in 0 until sizeY) {
                    for (x in 0 until sizeX) {
                        // draw tile onto result
                        val cell = (grid[x + y * sizeX].result as? WaveFunctionCollapse.ImageCellType) ?: continue
                        val tile = cell.image
                        DrawTextures.drawTexture(
                            xs[x], ys[y], xs[x + 1] - xs[x], ys[y + 1] - ys[y],
                            texToImage[tile], false, -1
                        )
                    }
                }
            }
        }
    }
}

fun collapse(wfc: WaveFunctionCollapse, sizeX: Int, sizeY: Int, random: Random, tileW: Int, tileH: Int) {
    val grid = wfc.collapseAll(sizeX, sizeY, random)
    val resultW = sizeX * tileW
    val resultH = sizeY * tileH
    val result = IntArray(resultW * resultH)
    var i = 0
    for (y in 0 until sizeY) {
        for (x in 0 until sizeX) {
            // draw tile onto result
            val cell = (grid[i++].result as? WaveFunctionCollapse.ImageCellType) ?: continue
            val tile = cell.image
            for (yi in 0 until tileH) {
                var ri = x * tileW + (y * tileH + yi) * resultW
                for (xi in 0 until tileW) {
                    result[ri++] = tile.getRGB(xi, yi)
                }
            }
        }
    }
    val hasAlphaChannel = wfc.types.any2 { it is WaveFunctionCollapse.ImageCellType && it.image.hasAlphaChannel }
    val resultImage = IntImage(resultW, resultH, result, hasAlphaChannel)
    resultImage.write(OS.desktop.getChild("WaveFunctionCollapse.png"))
}