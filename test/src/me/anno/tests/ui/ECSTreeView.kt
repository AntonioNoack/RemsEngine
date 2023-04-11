package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.ECSTreeView
import me.anno.engine.ui.EditorState
import me.anno.gpu.GFXBase
import me.anno.ui.debug.TestStudio.Companion.testUI

fun main() {
    GFXBase.disableRenderDoc()
    ECSRegistry.initMeshes()
    val sample = Entity()
    EditorState.prefabSource = sample.ref
    testUI(ECSTreeView(EditorState, style))
}