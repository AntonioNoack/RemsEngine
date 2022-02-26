package me.anno.utils.structures

import me.anno.Engine
import kotlin.math.abs

class CachedValue<V>(val getter: () -> V) {

    private var lastUpdate = 0L
    private var internal = getter()

    val value: V
        get() {
            if (abs(lastUpdate - Engine.gameTime) > 1_000_000_000L) {
                internal = getter()
                lastUpdate = Engine.gameTime
            }
            return internal
        }

}