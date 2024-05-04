package me.anno.engine

import me.anno.io.SaveableRegistry
import me.anno.io.files.Reference

object ECSRegistry {

    @JvmField
    var hasBeenInited = false

    @JvmStatic
    fun init() {

        if (hasBeenInited) return
        hasBeenInited = true

        Reference.registerStatic(ScenePrefab)
        DefaultAssets.init()
        SaveableRegistry.load()
    }
}