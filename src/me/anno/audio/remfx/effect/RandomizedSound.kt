package me.anno.audio.remfx.effect

import me.anno.maths.Maths

abstract class RandomizedSound : Sound() {
    var randomState = Maths.randomInt()
    fun nextRandomFloat(): Float {
        // Deterministic, inexpensive pseudo-random value in [0, 1).
        randomState = randomState * 1664525 + 1013904223
        return (randomState ushr 8) / 16777216f
    }
}
