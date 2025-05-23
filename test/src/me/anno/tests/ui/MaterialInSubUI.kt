package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.engine.DefaultAssets.flatCube
import me.anno.engine.OfficialExtensions
import me.anno.engine.projects.GameEngineProject
import me.anno.engine.projects.GameEngineProject.Companion.currentProject
import me.anno.engine.ui.EditorState
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.ui.UIColors.fireBrick
import me.anno.ui.UIColors.mediumAquamarine
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.editor.PropertyInspector
import me.anno.utils.OS.documents
import me.anno.utils.async.Callback

/**
 * Check whether all materials get made available properly (library button)
 * */
fun main() {

    OfficialExtensions.initForTests()

    // index sample project
    val folder = documents.getChild("RemsEngine/YandereSim")
    GameEngineProject.readOrCreate(folder, Callback.onSuccess { project ->
        currentProject = project
        project.init()

        // a few temporary files; cannot be GCed, because scope isn't left at runtime
        val green = Material.diffuse(mediumAquamarine).ref
        val darkRed = Material.diffuse(fireBrick).ref
        val tested = MeshComponent(flatCube, Material())
        EditorState.select(tested)
        disableRenderDoc()
        testUI3("Easy Material Editing", PropertyInspector({ EditorState.selection }, style))
    })
}