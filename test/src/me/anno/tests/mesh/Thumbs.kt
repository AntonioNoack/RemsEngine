package me.anno.tests.mesh

import me.anno.config.DefaultConfig.style
import me.anno.engine.ECSRegistry
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.ECSFileExplorer
import me.anno.io.files.Reference.getReference
import me.anno.ui.debug.TestEngine.Companion.testUI3

fun main() {
    OfficialExtensions.initForTests()
    ECSRegistry.initMeshes()
    testUI3("Thumbs", ECSFileExplorer(getReference("res://icon.obj"), style))
}