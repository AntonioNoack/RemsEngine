package me.anno.engine

import me.anno.extensions.ExtensionLoader

/**
 * tries to find official extensions, which were made into extensions in the first place to save storage space
 * */
object PluginRegistry {

    fun init() {
        ExtensionLoader.tryLoadMainInfo("res://pdf-ext.info")
    }

}