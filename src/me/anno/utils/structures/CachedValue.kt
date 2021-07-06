package me.anno.utils.structures

import me.anno.gpu.GFX
import kotlin.math.abs

class CachedValue<V>(val getter: () -> V) {

    private var lastUpdate = 0L
    private var internal = getter()

    val value: V
        get() {
            if (abs(lastUpdate - GFX.gameTime) > 1_000_000_000) {
                internal = getter()
                lastUpdate = GFX.gameTime
            }
            return internal
        }

}