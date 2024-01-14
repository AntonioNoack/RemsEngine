package me.anno.tests.mesh

import me.anno.config.DefaultConfig.style
import me.anno.engine.ECSRegistry
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.ECSFileExplorer
import me.anno.extensions.ExtensionLoader
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.ui.debug.TestEngine.Companion.testUI3

fun main() {
    OfficialExtensions.register()
    ExtensionLoader.load()
    ECSRegistry.initMeshes()
    testUI3("Thumbs", ECSFileExplorer(getReference("res://icon.obj"), style))
}