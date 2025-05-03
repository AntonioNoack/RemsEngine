package me.anno.maths.geometry

import me.anno.image.Image
import me.anno.utils.Color.a
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.structures.arrays.BooleanArrayList
import me.anno.utils.structures.lists.Lists.createArrayList
import org.apache.logging.log4j.LogManager
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * works ok; but:
 * - only works well with manually set boundaries or lots of compatible tiles -> otherwise it will corrupt, because chosen rules still can contradict each other
 * - is extremely slow for large amounts of tile types (I tested with 2.9k types incl. rotations)
 *
 * I'd assume I implement it correctly, because it works for easier tile sets...
 * */
class WaveFunctionCollapse {

    companion object {

        private val LOGGER = LogManager.getLogger(WaveFunctionCollapse::class)

        fun Image.rotate(r: Int): Image {
            val w = width
            val h = height
            val i = this
            val wm1 = w - 1
            val hm1 = h - 1
            return when (r) {
                1 -> object : Image(h, w, numChannels, hasAlphaChannel) {
                    override fun setRGB(index: Int, value: Int) {}
                    override fun getRGB(index: Int): Int {
                        val rx = index % h
                        val ry = index / h
                        return i.getRGB(wm1 - ry, rx)
                    }
                }
                2 -> object : Image(h, w, numChannels, hasAlphaChannel) {
                    override fun setRGB(index: Int, value: Int) {}
                    override fun getRGB(index: Int): Int {
                        val rx = wm1 - index % w
                        val ry = hm1 - index / w
                        return i.getRGB(rx, ry)
                    }
                }
                3 -> object : Image(h, w, numChannels, hasAlphaChannel) {
                    override fun setRGB(index: Int, value: Int) {}
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
            return mirrorX()
        }

        fun Image.mirrorX(): Image {
            val self = this
            return object : Image(width, height, numChannels, hasAlphaChannel) {
                val wm1 = width - 1
                override fun setRGB(index: Int, value: Int) {}
                override fun getRGB(index: Int): Int {
                    val x = index % width
                    val x2 = wm1 - x
                    return self.getRGB(index + x2 - x)
                }
            }
        }
    }

    fun removeInvalidCells() {
        // such a cell cannot have a neighbor -> is not tileable
        // todo the only exception is that it can be placed on a wall
        types.removeAll { types -> types.neighbors.any { neighbor -> neighbor.isEmpty } }
    }

    fun connect(a: Int, b: Int, side: Int) {
        val ta = types[a]
        val tb = types[b]
        ta.neighbors[side].set(tb.tileIndex)
        tb.neighbors[(side + 2) and 3].set(ta.tileIndex)
    }

    val types = ArrayList<CellType>()

    open class CellType(val tileIndex: Int) {
        val neighbors = createArrayList(4) { BooleanArrayList() }
        override fun toString() = "$tileIndex"
    }

    open class ImageCellType(tileIndex: Int, val image: Image) : CellType(tileIndex) {
        lateinit var edges: List<IntArray>
    }

    class DerivedImageCellType(
        tileIndex: Int, val base: ImageCellType,
        rotation: Int, mirrorX: Boolean,
        edges: List<IntArray>
    ) : ImageCellType(tileIndex, base.image.rotate(rotation).mirrorX(mirrorX)) {
        init {
            this.edges = edges
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

    fun calculateEdges(image: Image): List<IntArray> {
        return listOf(
            image.row(0), // top
            image.col(image.width - 1), // right
            image.row(image.height - 1).apply { reverse() }, // bottom
            image.col(0).apply { reverse() }, // left
        )
    }

    fun addTransformedTypes(allowMirrorX: Boolean, allowMirrorY: Boolean, allowedRotations: Int) {
        var nextId = types.size
        if (allowMirrorX || allowMirrorY || allowedRotations > 1) {
            val default = 1
            var allowed = default
            if (allowMirrorX && allowMirrorY && allowedRotations >= 4) {
                allowed = 255
            } else {
                do {
                    val lastAllowed = allowed
                    if (allowMirrorX) allowed = allowed or (allowed ushr 4) or (allowed shl 4)
                    if (allowMirrorY) allowed = allowed or shuffle(allowed)
                    if (allowedRotations >= 2) allowed = allowed or rot2(allowed) or rot2(allowed ushr 4).shl(4)
                    if (allowedRotations >= 4) allowed = allowed or rot1(allowed) or rot1(allowed ushr 4).shl(4)
                } while (allowed != lastAllowed)
            }
            // LOGGER.debug("$allowMirrorX,$allowMirrorY,$allowedRotations -> $allowed")
            if (allowed != default) {
                for (typeIndex in types.indices) {
                    val type = types[typeIndex]
                    type as ImageCellType
                    if (type is DerivedImageCellType)
                        throw RuntimeException("Must not execute this function twice")
                    val edges = type.edges
                    val mirrors = createArrayList(edges.size) { edges[it].reversedArray() }
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
                            listOf(
                                mirrors[rot],
                                mirrors[(rot + 3) and 3],
                                mirrors[(rot + 2) and 3],
                                mirrors[(rot + 1) and 3]
                            )
                        } else createArrayList(4) { edges[(it + rot) and 3] } // just rotate -> easy :)
                        types.add(DerivedImageCellType(nextId++, type, rot, mirrorX, newEdges))
                    }
                }
            }
        }
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
                            neighbors.set(type2.tileIndex)
                            type2.neighbors[(side + 2) and 3].set(type.tileIndex)
                        }
                    }
                }
            }
        }
    }

    class Cell(var length: Int) {
        var types: ArrayList<CellType>? = null
        val typeNeighbors = createArrayList(4) { BooleanArrayList(length) }
        var result: CellType? = null
    }

    fun collapseInit(sizeX: Int, sizeY: Int) =
        Array(sizeX * sizeY) { Cell(types.size) }

    fun collapseAll(sizeX: Int, sizeY: Int, random: Random): Array<Cell> {
        val grid = collapseInit(sizeX, sizeY)
        for (i in grid.indices) {
            if (!collapseStep(sizeX, sizeY, grid, random))
                break
        }
        return grid
    }

    var enableMinimalEntropyHeuristic = true

    fun collapseStep(sizeX: Int, sizeY: Int, grid: Array<Cell>, random: Random): Boolean {

        // find cell with the lowest number of possibilities
        var bestIndex = -1
        var bestLength = types.size
        var numEqualCells = 0f
        val enableMinimalEntropyHeuristic = enableMinimalEntropyHeuristic
        for (index in grid.indices) {
            val cell = grid[index]
            val cellLength = cell.length
            if (cellLength < 2) continue
            if (enableMinimalEntropyHeuristic) {// less chance of solving, but much more random
                if (cellLength > bestLength) continue
                if (cellLength < bestLength) {
                    // 100% accept this cell + reset weight
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
            } else {
                // randomly accept this cell
                numEqualCells++
                if (random.nextFloat() * numEqualCells < 1f) {
                    bestIndex = index
                }
            }
        }

        if (bestIndex < 0) {
            LOGGER.debug("no valid sample was found")
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
        // LOGGER.debug("collapsing $x,$y by ${cell.types?.size} types to ${cell.result}")
        cell.types = null
        cell.length = 1

        val invalid = BooleanArrayList(sizeX * sizeY)
        onChange(sizeX, sizeY, x, y, bestIndex, invalid)
        while (!invalid.isEmpty) {
            val index2 = invalid.nextSetBit(0)
            invalid.clear(index2)
            val x2 = index2 % sizeX
            val y2 = index2 / sizeX
            calculatePossibilities(sizeX, sizeY, grid, x2, y2, index2, invalid)
        }

        return true
    }

    private fun onChange(sizeX: Int, sizeY: Int, x: Int, y: Int, index: Int, invalid: BooleanArrayList) {
        if (x + 1 < sizeX) invalid.set(index + 1)
        if (x > 0) invalid.set(index - 1)
        if (y + 1 < sizeY) invalid.set(index + sizeX)
        if (y > 0) invalid.set(index - sizeX)
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

    fun isCompatible(sizeX: Int, sizeY: Int, grid: Array<Cell>, x: Int, y: Int, type: CellType, side: Int): Boolean {
        val dx = sideDx[side]
        val dy = sideDy[side]
        return if (x + dx in 0 until sizeX && y + dy in 0 until sizeY) {
            val cell1 = grid[(x + dx) + (y + dy) * sizeX]
            val result = cell1.result
            val otherSide = (side + 2) and 3
            if (result != null) {
                result.neighbors[otherSide][type.tileIndex]
            } else {
                val types = cell1.types
                // types == null || types.any2 { it.neighbors[otherSide][type.id] }
                types == null || cell1.typeNeighbors[otherSide][type.tileIndex] // .any2 { it.neighbors[otherSide][type.id] }
            }
        } else true
    }

    fun recalculateNeighborCache(cache: BooleanArrayList, types: List<CellType>, side: Int) {
        cache.clear()
        var lowestClearBit = 0
        for (type in types) {
            cache.or(type.neighbors[side])
            lowestClearBit = cache.nextClearBit(lowestClearBit)
            if (lowestClearBit >= types.size) {
                // all options are valid -> skip the rest
                break
            }
        }
    }

    fun calculatePossibilities(
        sizeX: Int,
        sizeY: Int,
        grid: Array<Cell>,
        x: Int,
        y: Int,
        index: Int,
        invalid: BooleanArrayList
    ) {
        val cell = grid[x + y * sizeX]
        if (cell.length < 2) return
        val newTypes = if (cell.types == null) ArrayList(types) else cell.types!!
        // val oldSize = newTypes.size
        val changed = newTypes.retainAll { type ->
            // check if cell type is allowed by all sides
            isCompatible(sizeX, sizeY, grid, x, y, type, 0) &&
                    isCompatible(sizeX, sizeY, grid, x, y, type, 1) &&
                    isCompatible(sizeX, sizeY, grid, x, y, type, 2) &&
                    isCompatible(sizeX, sizeY, grid, x, y, type, 3)
        }
        // LOGGER.debug("[$x,$y] $oldSize -> ${cell.length}")
        if (changed || cell.types == null) {
            if (newTypes.size == 1) {
                cell.result = newTypes.first()
                cell.types = null
            } else {
                cell.types = newTypes
            }
            for (i in 0 until 4) {
                recalculateNeighborCache(cell.typeNeighbors[i], newTypes, i)
            }
            cell.length = newTypes.size
            if (cell.length == 0) {
                LOGGER.warn("No neighbors are possible for $x,$y")
            } else {
                if (changed) onChange(sizeX, sizeY, x, y, index, invalid)
            }
        }
    }
}