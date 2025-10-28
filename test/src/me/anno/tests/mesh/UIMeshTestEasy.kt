package me.anno.tests.mesh

import me.anno.config.DefaultConfig.style
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.systems.Systems
import me.anno.engine.ECSRegistry
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView
import me.anno.io.files.FileReference
import me.anno.ui.Panel
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.utils.OS.res

/**
 * load an existing asset, like the arrow, and draw it :)
 *
 * like UIMeshTestCustomizable, just easier (and plus normals-background)
 * */

fun createEasyMeshUI(meshRef: FileReference = res.getChild("meshes/arrowX.obj")): Panel {
    ECSRegistry.init()
    Systems.world = MeshCache.getEntry(meshRef).waitFor() as Mesh
    val sceneView = SceneView(PlayMode.PLAYING, style)
    val renderView = sceneView.renderView
    renderView.radius = 1f
    renderView.editorCamera.fovOrthographic = 1f
    renderView.editorCamera.isPerspective = false
    renderView.renderMode = RenderMode.NORMAL
    sceneView.weight = 1f
    return sceneView
}

fun main() {
    OfficialExtensions.initForTests()
    // the main method is extracted, so it can be easily ported to web
    // a better method may come in the future
    testUI3("Easy Mesh UI", ::createEasyMeshUI)
}