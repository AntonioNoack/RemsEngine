package me.anno.experiments.sudoku

object Done : Throwable() {
    private fun readResolve(): Any = Done
}