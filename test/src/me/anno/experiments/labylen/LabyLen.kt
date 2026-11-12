package me.anno.experiments.labylen

import java.util.BitSet

fun main() {
    // given an n x m labyrinth, starting in the center,
    //  what is the average path-length of the first discovery?
    println(labyLen(2, 2))
    println(labyLen(2, 3))
    println(labyLen(2, 4))
    println(labyLen(3, 3))
    println(labyLen(2, 5))
    println(labyLen(3, 4))
}

// runtime: 3^(n*m) (only usable for small sizes)
fun labyLen(sx: Int, sy: Int): Double {
    val state = BitSet(sx * sy)
    return labyLen(sx, sy, state, sx.shr(1), sy.shr(1))
}

val directions = intArrayOf(
    -1, 0,
    +1, 0,
    0, -1,
    0, +1,
)

val invNumDir = DoubleArray(1 + directions.size.shr(1)) { i ->
    if (i > 0) 1.0 / i else 0.0
}

fun labyLen(
    sx: Int, sy: Int, visited: BitSet,
    cx: Int, cy: Int,
): Double {
    // sum over all directions
    // divide by number of valid directions
    var numDir = 0
    var sum = 0.0
    visited.set(cx + sx * cy)
    for (i in directions.indices step 2) {
        val nx = cx + directions[i]
        val ny = cy + directions[i + 1]
        if (nx in 0 until sx && ny in 0 until sy &&
            !visited[nx + sx * ny]
        ) {
            sum += labyLen(sx, sy, visited, nx, ny)
            numDir++
        }
    }
    visited.clear(cx + sx * cy)
    return 1f + sum * invNumDir[numDir]
}