package me.anno.config

import me.anno.io.files.FileReference
import kotlin.reflect.KProperty

/**
 * reference/delegate to default configuration;
 * makes writing settings for UI much more elegant
 * */
class ConfigRef<V>(val key: String, val default: V) {

    operator fun getValue(thisRef: Any?, property: KProperty<*>): V {
        @Suppress("UNCHECKED_CAST")
        return when (default) {
            is Int -> DefaultConfig[key, default]
            is Long -> DefaultConfig[key, default]
            is Float -> DefaultConfig[key, default]
            is Double -> DefaultConfig[key, default]
            is Boolean -> DefaultConfig[key, default]
            is String -> DefaultConfig[key, default]
            is FileReference -> DefaultConfig[key, default]
            else -> DefaultConfig[key, default]
        } as V
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
        DefaultConfig[key] = value
    }
}