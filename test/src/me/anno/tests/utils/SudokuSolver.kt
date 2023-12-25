package me.anno.tests.utils

import me.anno.Time
import me.anno.utils.OS.downloads
import org.junit.jupiter.api.Assertions.assertEquals

class Cell(val xi: Int, val yi: Int, val ci: Int, val index: Int)

class SudokuSolver {

    val n = 9
    private val xFull = IntArray(n)
    private val yFull = IntArray(n)
    private val cFull = IntArray(n)
    private val field = IntArray(n * n)

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

    private val cells = Array(n * n) {
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

    @Suppress("unused")
    fun printSolution() {
        fun line() {
            println("-------------------------")
        }
        for (y in 0 until n) {
            if (y % 3 == 0) line()
            for (x in 0 until n) {
                if (x % 3 == 0) print("| ")
                print("${field[x + y * n]} ")
            }
            println("|")
        }
        line()
    }

    fun solve(i: Int, emptyCells: List<Cell>) {
        if (i >= emptyCells.size) throw done
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

    fun solve(sample: String, emptyCells: ArrayList<Cell>) {
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
            if (e == done) {
                // check solution against ground-truth
                // only use when not benchmarking ^^
                if (false) {
                    assertEquals(sample.substring(n * n + 1), field.joinToString(""))
                }
                // printSolution()
            } else e.printStackTrace()
        }
    }
}

val done = Throwable()

fun getSamples(): List<String> {
    return downloads.getChild("sudoku.zip/sudoku.csv")
        .readLinesSync(81 * 2 + 1) // task comma solution
        .apply { next() } // skip header
        .run { (0 until 1000_000).map { next() } }
}

fun main() {

    // for challenge on Cherno server,
    //  implement fast sudoku solver

    val samples = getSamples()
    val solver = SudokuSolver()
    val emptyCells = ArrayList<Cell>(81)
    val t0 = Time.nanoTime

    for (si in samples.indices) {
        solver.solve(samples[si], emptyCells)
    }

    val t1 = Time.nanoTime
    println("Done in ${(t1 - t0) / 1e9f}s")
}