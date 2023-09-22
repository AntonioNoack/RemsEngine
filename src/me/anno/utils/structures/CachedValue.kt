package me.anno.utils.structures

import me.anno.Time
import kotlin.math.abs

class CachedValue<V>(val getter: () -> V) {

    private var lastUpdate = 0L
    private var internal = getter()

    val value: V
        get() {
            if (abs(lastUpdate - Time.nanoTime) > 1_000_000_000L) {
                internal = getter()
                lastUpdate = Time.nanoTime
            }
            return internal
        }

}