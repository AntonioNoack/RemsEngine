package me.anno.tests.mesh

import me.anno.config.DefaultConfig.style
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.ui.Panel
import me.anno.ui.debug.TestStudio.Companion.testUI3

/**
 * load an existing asset, like the arrow, and draw it :)
 *
 * like UIMeshTestCustomizable, just easier (and plus normals-background)
 * */

fun createEasyMeshUI(meshRef: FileReference = getReference("res://mesh/arrowX.obj")): Panel {
    ECSRegistry.initMeshes()
    EditorState.prefabSource = meshRef
    val sceneView = SceneView(PlayMode.PLAYING, style)
    val renderView = sceneView.renderer
    renderView.radius = 1.0
    renderView.editorCamera.fovOrthographic = 1f
    renderView.editorCamera.isPerspective = false
    renderView.renderMode = RenderMode.NORMAL
    sceneView.weight = 1f
    return sceneView
}

fun main() {
    // the main method is extracted, so it can be easily ported to web
    // a better method may come in the future
    testUI3("Easy Mesh UI", ::createEasyMeshUI)
}