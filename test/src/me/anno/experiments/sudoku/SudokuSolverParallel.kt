package me.anno.experiments.sudoku

import me.anno.Engine
import me.anno.Time
import me.anno.utils.hpc.ProcessingGroup

fun main() {

    // just a try on how fast the parallel Sudoku solver would be
    // -> 8x is as expected of my Ryzen 5 2600, always has been 8x
    // unfortunately, moving the solver to a class also made it 15% slower...
    // (maybe because Thread-exclusivity can no longer be guaranteed)
    val samples = getSamples()
    val t0 = Time.nanoTime

    ProcessingGroup("Sudoku", Runtime.getRuntime().availableProcessors())
        .processBalanced(0, samples.size, 512) { i0, i1 ->
            val solver = SudokuSolver()
            val emptyCells = ArrayList<Cell>(81)
            for (si in i0 until i1) {
                solver.solve(samples[si], emptyCells)
            }
        }

    val t1 = Time.nanoTime
    println("Done in ${(t1 - t0) / 1e9f}s")

    Engine.requestShutdown() // shutdown processing group
}