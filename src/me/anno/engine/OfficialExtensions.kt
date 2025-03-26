package me.anno.engine

import me.anno.extensions.ExtensionLoader
import me.anno.utils.OS.res

/**
 * tries to find official extensions, which were made into extensions to reduce porting complexity
 * */
object OfficialExtensions {
    fun register() {
        val paths = listOf(
            "jvm-ext.info",
            "pdf-ext.info",
            "box2d-ext.info",
            "bullet-ext.info",
            "recast-ext.info",
            "sdf-ext.info",
            "lua-ext.info",
            "mesh-ext.info",
            "unpack-ext.info",
            "image-ext.info",
            "video-ext.info",
            "export-ext.info",
            "openxr-ext.info",
        )
        for (path in paths) {
            val file = res.getChild(path)
            ExtensionLoader.tryLoadMainInfo(file)
        }
    }

    fun initForTests() {
        register()
        ExtensionLoader.load()
        ECSRegistry.init()
    }
}