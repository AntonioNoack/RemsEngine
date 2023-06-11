package me.anno.tests.mesh

import me.anno.config.DefaultConfig
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.ECSFileExplorer
import me.anno.ui.debug.TestStudio.Companion.testUI3
import me.anno.utils.OS

fun main() {
    testUI3 {
        ECSRegistry.initMeshes()
        val explorer = ECSFileExplorer(OS.documents.getChild("Donut.fbx"), DefaultConfig.style)
        explorer.entrySize = 256f
        explorer.onUpdateEntrySize()
        explorer
    }
}