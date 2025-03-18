package me.anno.tests.utils

import me.anno.config.DefaultConfig
import me.anno.ecs.Entity
import me.anno.ecs.prefab.PrefabInspector
import me.anno.ecs.prefab.change.Path
import me.anno.engine.ECSRegistry
import me.anno.engine.WindowRenderFlags
import me.anno.engine.ui.EditorState
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.ui.debug.TestEngine.Companion.testUI
import me.anno.ui.editor.PropertyInspector

fun main() {

    ECSRegistry.init()

    disableRenderDoc()

    val sample = Entity()

    // broken text input
    // testSceneWithUI(sample)
    testUI("PrefabTest") {
        WindowRenderFlags.enableVSync = true
        sample.prefabPath = Path.ROOT_PATH
        EditorState.prefabSource = sample.ref
        PrefabInspector.currentInspector = PrefabInspector(sample.ref)
        EditorState.select(sample)
        PropertyInspector({ EditorState.selection }, DefaultConfig.style)
    }
    /*testUI { // works
        val list = PanelListY(style)
        sample.prefabPath = Path.ROOT_PATH
        PrefabInspector(sample.ref).inspect(sample, list, style)
        list
    }*/
}
