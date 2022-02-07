package me.anno.remsstudio.utils

import me.anno.animation.Type
import me.anno.io.utils.StringMap
import me.anno.remsstudio.animation.AnimatedProperty

object Utils {

    fun <V> StringMap.getAnimated(key: String, type: Type): AnimatedProperty<V> {
        val v = when (val value = this[key]) {
            is AnimatedProperty<*> -> {
                return if (value.type == type) {
                    @Suppress("UNCHECKED_CAST")
                    value as AnimatedProperty<V>
                } else {
                    val v2 = AnimatedProperty<V>(type)
                    v2.copyFrom(type)
                    set(key, v2)
                    v2
                }
            }
            is Int -> value
            is Long -> value
            is Float -> value
            is Double -> value
            is String -> value
            null -> type.defaultValue
            else -> value
        }
        val v2 = AnimatedProperty<V>(type)
        @Suppress("UNCHECKED_CAST")
        v2.set(v as V)
        set(key, v2)
        return v2
    }

}