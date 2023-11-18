package me.anno.utils.structures

import me.anno.Time
import kotlin.math.abs
import kotlin.reflect.KProperty

class CachedValue<V>(val timeoutMillis: Long, val getter: () -> V) {

    private var lastUpdate = 0L
    private var internal = getter()

    operator fun getValue(thisRef: Any?, property: KProperty<*>): V {
        if (abs(lastUpdate - Time.nanoTime) > timeoutMillis * 1000_000L) {
            internal = getter()
            lastUpdate = Time.nanoTime
        }
        return internal
    }
}