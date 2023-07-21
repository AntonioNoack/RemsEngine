package me.anno.tests.gfx.graphs

import me.anno.config.DefaultConfig
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.shaders.SkyBox
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.SceneView
import me.anno.graph.render.RenderGraph
import me.anno.graph.render.RenderGraphEditor
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.utils.OS

fun main() {
    val graph = RenderGraph.combined1
    val scene = Entity()
    scene.add(MeshComponent(OS.documents.getChild("metal-roughness.glb")))
    scene.add(SkyBox())
    testUI("RenderGraph") {

        EditorState.prefabSource = scene.ref

        val sv = SceneView(EditorState, PlayMode.EDITING, DefaultConfig.style)
        val rv = sv.renderer
        rv.position.set(0.0, 0.0, -5.0)
        rv.updateEditorCameraTransform()

        val list = CustomList(false, DefaultConfig.style)
        list.add(sv, 1f)
        list.add(RenderGraphEditor(rv, graph), 1f)
        list
    }
}