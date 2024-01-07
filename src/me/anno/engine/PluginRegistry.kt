package me.anno.engine

import me.anno.extensions.ExtensionLoader

/**
 * tries to find official extensions, which were made into extensions in the first place to save storage space
 * */
object PluginRegistry {
    fun init() {
        val paths = listOf(
            "res://pdf-ext.info",
            "res://box2d-ext.info",
            "res://bullet-ext.info",
            "res://recast-ext.info",
            "res://sdf-ext.info",
        )
        for (path in paths) {
            ExtensionLoader.tryLoadMainInfo(path)
        }
    }
}