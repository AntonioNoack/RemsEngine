package me.anno.maths.geometry

import me.anno.image.Image
import me.anno.image.ImageCPUCache
import me.anno.image.raw.IntImage
import me.anno.utils.Color.a
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.Color.toHexColor
import me.anno.utils.LOGGER
import me.anno.utils.OS.desktop
import me.anno.utils.OS.downloads
import me.anno.utils.structures.lists.Lists.any2
import sun.plugin.dom.exception.InvalidStateException
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class WaveFunctionCollapse {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            // load a set of images
            val wfc = WaveFunctionCollapse()
            var id = 0
            val tileW = 16
            val tileH = 16
            val src = downloads.getChild("2d/caveTilesetOpenGameArt.png")
            val tileAtlas = ImageCPUCache.getImage(src, false)!!
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
                        wfc.types.add(ImageCellType(id++, tile))
                    }
                }
            }
            wfc.calculateEdges()
            wfc.addTransformedTypes(allowMirrorX = true, allowMirrorY = true, 4, id)
            wfc.defineNeighborsByImages(8)
            for (idx in wfc.types.indices) {
                println("$idx, ${wfc.types[idx]} -> ${wfc.types[idx].neighbors.joinToString()}")
            }
            println(wfc.types.size)
            // define neighbors manually for testing
            // correct
            /*val ts = 3
            for (y in 0 until ts) {
                for (x in 0 until ts) {
                    val idx = x + y * ts
                    fun i(xi: Int, yi: Int) = (((xi + ts) % ts) + ((yi + ts) % ts) * ts)
                    wfc.types[idx].neighbors[0].add(wfc.types[i(x, y - 1)])
                    wfc.types[idx].neighbors[1].add(wfc.types[i(x + 1, y)])
                    wfc.types[idx].neighbors[2].add(wfc.types[i(x, y + 1)])
                    wfc.types[idx].neighbors[3].add(wfc.types[i(x - 1, y)])
                    println("$idx, ${wfc.types[idx]} -> ${wfc.types[idx].neighbors.joinToString()}")
                }
            }
            for (i in 0 until 3) {
                wfc.connect(i, i + 6, 0)
                wfc.connect(i, i + 6, 2)
                wfc.connect(i * 3, i * 3 + 2, 1)
                wfc.connect(i * 3, i * 3 + 2, 3)
                wfc.connect(1 + i * 3, 1 + i * 3, 1)
                wfc.connect(1 + i * 3, 1 + i * 3, 3)
                wfc.connect(3 + i, 3 + i, 0)
                wfc.connect(3 + i, 3 + i, 2)
            }*/
            wfc.removeInvalidCells()
            if (wfc.types.isEmpty())
                throw InvalidStateException("Cannot connect any tiles")
            val sizeX = 16
            val sizeY = 16
            val grid = wfc.collapseAll(sizeX, sizeY, Random(1234L))
            val resultW = sizeX * tileW
            val resultH = sizeY * tileH
            val result = IntArray(resultW * resultH)
            var i = 0
            for (y in 0 until sizeY) {
                for (x in 0 until sizeX) {
                    // draw tile onto result
                    val cell = (grid[i++].result as? ImageCellType) ?: continue
                    val tile = cell.image
                    for (yi in 0 until tileH) {
                        var ri = x * tileW + (y * tileH + yi) * resultW
                        for (xi in 0 until tileW) {
                            result[ri++] = tile.getRGB(xi, yi)
                        }
                    }
                }
            }
            val resultImage = IntImage(resultW, resultH, result, tileAtlas.hasAlphaChannel)
            resultImage.write(desktop.getChild("WaveFunctionCollapse.png"))
        }
    }

    fun removeInvalidCells() {
        // such a cell cannot have a neighbor -> is not tileable
        // todo the only exception is that it can be placed on a wall
        types.removeIf { t -> t.neighbors.any { n -> n.isEmpty() } }
    }

    fun connect(a: Int, b: Int, side: Int) {
        val ta = types[a]
        val tb = types[b]
        ta.neighbors[side].add(tb)
        tb.neighbors[(side + 2) and 3].add(ta)
    }

    val types = ArrayList<CellType>()

    open class CellType(val id: Int) {
        val neighbors = Array(4) { HashSet<CellType>() }
        override fun toString() = "$id"
    }

    open class ImageCellType(id: Int, val image: Image) : CellType(id) {
        lateinit var edges: Array<IntArray>
    }

    class DerivedImageCellType(
        id: Int, val base: ImageCellType,
        rotation: Int,
        mirrorX: Boolean,
        edges: Array<IntArray>
    ) : ImageCellType(id, base.image.rotate(rotation).mirrorX(mirrorX)) {

        init {
            this.edges = edges
        }

        companion object {

            fun Image.rotate(r: Int): Image {
                val w = width
                val h = height
                val i = this
                val wm1 = w - 1
                val hm1 = h - 1
                return when (r) {
                    1 -> object : Image(h, w, numChannels, hasAlphaChannel) {
                        override fun getRGB(index: Int): Int {
                            val rx = index % h
                            val ry = index / h
                            return i.getRGB(wm1 - ry, rx)
                        }
                    }
                    2 -> object : Image(h, w, numChannels, hasAlphaChannel) {
                        override fun getRGB(index: Int): Int {
                            val rx = wm1 - index % w
                            val ry = hm1 - index / w
                            return i.getRGB(rx, ry)
                        }
                    }
                    3 -> object : Image(h, w, numChannels, hasAlphaChannel) {
                        override fun getRGB(index: Int): Int {
                            val rx = index % h
                            val ry = index / h
                            return i.getRGB(ry, hm1 - rx)
                        }
                    }
                    else -> this
                }
            }

            fun Image.mirrorX(b: Boolean): Image {
                if (!b) return this
                val i = this
                return object : Image(width, height, numChannels, hasAlphaChannel) {
                    val wm1 = width - 1
                    override fun getRGB(index: Int): Int {
                        val x = index % width
                        val x2 = wm1 - x
                        return i.getRGB(index + x2 - x)
                    }
                }
            }
        }
    }


    fun shuffle(i: Int) =
        (i and 3).shl(6) or
                (i and 12).shl(2) or
                (i and 48).ushr(2) or
                (i and 192).ushr(6)

    fun rot2(i: Int) =
        (i and 3).shl(2) or (i and 12).ushr(2)

    fun rot1(i: Int) =
        (i and 7).shl(1) or (i and 8).ushr(3)

    fun Image.col(x: Int) = IntArray(height) { getRGB(x, it) }
    fun Image.row(y: Int) = IntArray(width) { getRGB(it, y) }

    fun calculateEdges() {
        // calculate edges by images
        for (type in types) {
            type as ImageCellType
            val edges = calculateEdges(type.image)
            for (i in 0 until 4) {
                if (edges[i].last() != edges[(i + 1) % 4][0])
                    throw IllegalStateException()
            }
            type.edges = edges
        }
    }

    fun calculateEdges(image: Image): Array<IntArray> {
        return arrayOf(
            image.row(0), // top
            image.col(image.width - 1), // right
            image.row(image.height - 1).apply { reverse() }, // bottom
            image.col(0).apply { reverse() }, // left
        )
    }

    fun addTransformedTypes(allowMirrorX: Boolean, allowMirrorY: Boolean, allowedRotations: Int, newId: Int): Int {
        var nextId = newId
        if (allowMirrorX || allowMirrorY || allowedRotations > 1) {
            val default = 1
            var allowed = default
            if (allowMirrorX && allowMirrorY && allowedRotations >= 4) {
                allowed = 255
            } else {
                for (i in 0 until 4) {
                    if (allowMirrorX) allowed = allowed or (allowed ushr 4) or (allowed shl 4)
                    if (allowMirrorY) allowed = allowed or shuffle(allowed)
                    if (allowedRotations >= 2) allowed = allowed or rot2(allowed) or rot2(allowed ushr 4).shl(4)
                    if (allowedRotations >= 4) allowed = allowed or rot1(allowed) or rot1(allowed ushr 4).shl(4)
                }
            }
            println("$allowMirrorX,$allowMirrorY,$allowedRotations -> $allowed")
            if (allowed != default) {
                for (typeIndex in types.indices) {
                    val type = types[typeIndex]
                    type as ImageCellType
                    if (type is DerivedImageCellType)
                        throw RuntimeException("Must not execute this function twice")
                    val edges = type.edges
                    val mirrors = Array(edges.size) { edges[it].reversedArray() }
                    var remaining = allowed xor default
                    while (remaining > 0) {
                        var bit = remaining.takeLowestOneBit()
                        remaining = remaining xor bit
                        var bitIndex = 0
                        if (bit >= 16) {
                            bitIndex += 4
                            bit = bit ushr 4
                        }
                        if (bit >= 4) {
                            bitIndex += 2
                            bit = bit ushr 2
                        }
                        if (bit >= 2) {
                            bitIndex++
                        }
                        val rot = bitIndex and 3
                        val mirrorX = bitIndex >= 4
                        if (rot == 0 && !mirrorX) continue
                        // apply mirror and rotation on edges
                        val newEdges = if (mirrorX) {
                            arrayOf(
                                mirrors[rot],
                                mirrors[(rot + 3) and 3],
                                mirrors[(rot + 2) and 3],
                                mirrors[(rot + 1) and 3]
                            )
                        } else Array(4) { edges[(it + rot) and 3] } // just rotate -> easy :)
                        types.add(DerivedImageCellType(nextId++, type, rot, mirrorX, newEdges))
                    }
                }
            }
        }
        return nextId
    }

    /**
     * define all possible neighbors for each type; O(n²)
     * @param colorTolerance how much |r|+|g|+|b| are allowed to differ per pixel
     * @param acceptance how many pixels must match decently; 0.5 = half, 1.0 = all
     * */
    fun defineNeighborsByImages(
        colorTolerance: Int = 32,
        acceptance: Float = 0.5f,
        s0: Int = 0,
        s1: Int = types.size,
        d0: Int = 0,
        d1: Int = types.size
    ) {
        for (ti in s0 until s1) {
            val type = types[ti]
            if (type is ImageCellType) {
                for (side in 0 until 4) {
                    val neighbors = type.neighbors[side]
                    for (tj in max(ti, d0) until d1) {
                        val type2 = types[tj]
                        if (type2 is ImageCellType && isCompatible(type, type2, side, colorTolerance, acceptance)) {
                            neighbors.add(type2)
                            type2.neighbors[(side + 2) and 3].add(type)
                        }
                    }
                }
            }
        }
    }

    class Cell(var length: Int) {
        var types: ArrayList<CellType>? = null
        var result: CellType? = null
    }

    fun collapseInit(sizeX: Int, sizeY: Int): Array<Cell> {
        val size = sizeX * sizeY
        return Array(size) { Cell(types.size) }
    }

    fun collapseAll(sizeX: Int, sizeY: Int, random: Random): Array<Cell> {
        val grid = collapseInit(sizeX, sizeY)
        for (i in grid.indices) {
            if (!collapseStep(sizeX, sizeY, grid, random))
                break
        }
        return grid
    }

    fun collapseStep(sizeX: Int, sizeY: Int, grid: Array<Cell>, random: Random): Boolean {

        // find cell with the lowest number of possibilities
        var bestIndex = -1
        var bestLength = types.size
        var numEqualCells = 0f
        for (index in grid.indices) {
            val cell = grid[index]
            val cellLength = cell.length
            if (cellLength in 2..bestLength) {
                if (cellLength < bestLength) {
                    // 100% accept this cell
                    bestLength = cellLength
                    numEqualCells = 1f
                    bestIndex = index
                } else {
                    // randomly accept this cell
                    numEqualCells++
                    if (random.nextFloat() * numEqualCells < 1f) {
                        bestIndex = index
                    }
                }
            }
        }

        if (bestIndex < 0) {
            println("no valid sample was found")
            return false
        }

        // collapse cell
        // -> choose random bit
        val cell = grid[bestIndex]
        val selectedIndex = random.nextInt(cell.length)

        // update all cells around it
        val x = bestIndex % sizeX
        val y = bestIndex / sizeX

        // collapse cell
        cell.result = (cell.types ?: types)[selectedIndex]
        println("collapsing $x,$y by ${cell.types?.size} types to ${cell.result}")
        cell.types = null
        cell.length = 1

        onChange(sizeX, sizeY, grid, x, y, bestIndex)

        return true
    }

    private fun onChange(sizeX: Int, sizeY: Int, grid: Array<Cell>, x: Int, y: Int, index: Int) {
        if (x + 1 < sizeX) calculatePossibilities(sizeX, sizeY, grid, x + 1, y, index + 1)
        if (x > 0) calculatePossibilities(sizeX, sizeY, grid, x - 1, y, index - 1)
        if (y + 1 < sizeY) calculatePossibilities(sizeX, sizeY, grid, x, y + 1, index + sizeX)
        if (y > 0) calculatePossibilities(sizeX, sizeY, grid, x, y - 1, index - sizeX)
    }

    private fun isCompatible(
        a: ImageCellType, b: ImageCellType, side: Int,
        maxColorDifference: Int,
        acceptance: Float = 0.5f
    ): Boolean {
        if (maxColorDifference >= 255 * 3) return true
        var ai = a.edges[side]
        var bi = b.edges[(side + 2) and 3]
        if (ai.size > bi.size) {
            val tmp = ai
            ai = bi
            bi = tmp
        }
        if (delta(ai.first(), bi.last()) + delta(ai.last(), bi.first()) > 2 * maxColorDifference) {
            return false
        }
        var delta = 0
        val am1 = ai.size - 1
        val maxError = (maxColorDifference / acceptance).toInt()
        val maxTotalError = maxColorDifference * ai.size
        if (ai.size == bi.size) {
            // simple comparison
            for (i in ai.indices) {
                delta += min(delta(ai[am1 - i], bi[i]), maxError)
                if (delta > maxTotalError) return false
            }
        } else {
            // scaled comparison
            val aj = ai.size - 1
            val bj = bi.size - 1
            for (i in ai.indices) {
                val j = i * bj / aj
                delta += min(delta(ai[am1 - i], bi[j]), maxError)
                if (delta > maxTotalError) return false
            }
        }
        return true
    }

    fun delta(a: Int, b: Int): Int {
        return abs(b.a() - a.a()) + abs(b.r() - a.r()) + abs(b.g() - a.g()) + abs(b.b() - a.b())
    }

    val sideDx = intArrayOf(0, +1, 0, -1)
    val sideDy = intArrayOf(-1, 0, +1, 0)

    fun calculatePossibilities(sizeX: Int, sizeY: Int, grid: Array<Cell>, x: Int, y: Int, index: Int) {
        val cell = grid[x + y * sizeX]
        if (cell.length < 2) return
        val baseTypes = if (cell.types == null) ArrayList(types) else cell.types!!
        val changed = baseTypes.retainAll { type ->
            // check if cell type is allowed by all sides
            (0 until 4).all { side ->
                val dx = sideDx[side]
                val dy = sideDy[side]
                if (x + dx in 0 until sizeX && y + dy in 0 until sizeY) {
                    val cell1 = grid[index + dx + dy * sizeX]
                    val result = cell1.result
                    val otherSide = (side + 2) and 3
                    if (result != null) {
                        type in result.neighbors[otherSide]
                    } else {
                        val types = cell1.types
                        types == null || types.any2 { type in it.neighbors[otherSide] }
                    }
                } else true
            }
        }
        if (changed || cell.types == null) {
            if (baseTypes.size == 1) {
                cell.result = baseTypes.first()
                cell.types = null
            } else {
                cell.types = baseTypes
            }
            cell.length = baseTypes.size
            println("$x,$y -> ${cell.length}")
            if (cell.length == 0) {
                LOGGER.warn("No neighbors are possible for $x,$y")
            } else {
                if (changed) onChange(sizeX, sizeY, grid, x, y, index)
            }
        }
    }

}