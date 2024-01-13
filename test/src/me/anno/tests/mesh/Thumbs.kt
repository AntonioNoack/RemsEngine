package me.anno.tests.mesh

import me.anno.config.DefaultConfig.style
import me.anno.engine.ECSRegistry
import me.anno.engine.PluginRegistry
import me.anno.engine.ui.ECSFileExplorer
import me.anno.extensions.ExtensionLoader
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.ui.debug.TestStudio.Companion.testUI3

fun main() {
    PluginRegistry.init()
    ExtensionLoader.load()
    ECSRegistry.initMeshes()
    testUI3("Thumbs", ECSFileExplorer(getReference("res://icon.obj"), style))
}