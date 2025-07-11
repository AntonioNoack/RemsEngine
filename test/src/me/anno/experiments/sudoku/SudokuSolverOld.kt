package me.anno.experiments.sudoku

import me.anno.Time
import me.anno.utils.structures.lists.Lists.createArrayList
import org.junit.jupiter.api.Assertions.assertEquals

// this is 2s faster (15%), just by removing the surrounding class :/
fun main() {

    val samples = getSamples()
    val t0 = Time.nanoTime

    val n = 9
    val xFull = IntArray(n)
    val yFull = IntArray(n)
    val cFull = IntArray(n)
    val field = IntArray(n * n)

    fun canSet(mask: Int, mask1: Int): Boolean {
        return mask.and(mask1) != mask1
    }

    fun canSet(cell: Cell, value: Int): Boolean {
        val mask = 1 shl value
        return canSet(xFull[cell.xi], mask) &&
                canSet(yFull[cell.yi], mask) &&
                canSet(cFull[cell.ci], mask)
    }

    fun unset(cell: Cell, value: Int) {
        val invMask = (1 shl value).inv()
        xFull[cell.xi] = xFull[cell.xi] and invMask
        yFull[cell.yi] = yFull[cell.yi] and invMask
        cFull[cell.ci] = cFull[cell.ci] and invMask
    }

    fun set(cell: Cell, value: Int) {
        val mask = 1 shl value
        xFull[cell.xi] = xFull[cell.xi] or mask
        yFull[cell.yi] = yFull[cell.yi] or mask
        cFull[cell.ci] = cFull[cell.ci] or mask
        field[cell.index] = value
    }

    val cells = createArrayList(n * n) {
        val xi = it % n
        val yi = it / n
        Cell(xi, yi, (xi / 3) + (yi / 3) * 3, it)
    }

    fun clear() {
        field.fill(0)
        xFull.fill(0)
        yFull.fill(0)
        cFull.fill(0)
    }

    fun fill(sample: String) {
        for (i in 0 until n * n) {
            val si = sample[i]
            if (si != '0') {
                set(cells[i], si.code - 48)
            }
        }
    }

    fun solve(i: Int, emptyCells: List<Cell>) {
        if (i >= emptyCells.size) throw Done
        val cell = emptyCells[i]
        // check all possible values
        for (j in 1..n) {
            if (canSet(cell, j)) {
                set(cell, j)
                solve(i + 1, emptyCells)
                unset(cell, j)
            }
        }
    }

    val emptyCells = ArrayList<Cell>(n * n)
    for (si in samples.indices) {

        val sample = samples[si]

        clear()
        fill(sample)

        // sorting by number of options increased the solving time a lot :(, from 5ms to 36ms
        //   total time increased from 12s to 36s
        // sorting in reverse however increased the solving time massively to 6100ms
        //   total time not tested
        emptyCells.clear()
        for (i in field.indices) {
            if (field[i] == 0) {
                emptyCells.add(cells[i])
            }
        }

        try {
            solve(0, emptyCells)
            println("Unsolvable :(")
        } catch (e: Throwable) {
            if (e == Done) {
                // check solution against ground-truth
                // only use when not benchmarking ^^
                if (false) {
                    assertEquals(sample.substring(n * n + 1), field.joinToString(""))
                }
                // printSolution()
            } else e.printStackTrace()
        }
    }

    val t1 = Time.nanoTime
    println("Done in ${(t1 - t0) / 1e9f}s")
}