package me.anno.engine

import me.anno.extensions.ExtensionLoader

/**
 * tries to find official extensions, which were made into extensions to reduce porting complexity
 * */
object OfficialExtensions {
    fun register() {
        val paths = listOf(
            "res://jvm-ext.info",
            "res://pdf-ext.info",
            "res://box2d-ext.info",
            "res://bullet-ext.info",
            "res://bullet-jme-ext.info",
            "res://recast-ext.info",
            "res://sdf-ext.info",
            "res://lua-ext.info",
            "res://mesh-ext.info",
            "res://unpack-ext.info",
            "res://image-ext.info",
            "res://video-ext.info",
            "res://export-ext.info",
        )
        for (path in paths) {
            ExtensionLoader.tryLoadMainInfo(path)
        }
    }

    fun initForTests() {
        register()
        ExtensionLoader.load()
        ECSRegistry.init()
    }
}