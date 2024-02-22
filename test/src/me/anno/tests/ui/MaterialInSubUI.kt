package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ECSRegistry
import me.anno.engine.OfficialExtensions
import me.anno.engine.projects.GameEngineProject
import me.anno.engine.projects.GameEngineProject.Companion.currentProject
import me.anno.engine.ui.EditorState
import me.anno.extensions.ExtensionLoader
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.mesh.Shapes.flatCube
import me.anno.ui.UIColors.fireBrick
import me.anno.ui.UIColors.mediumAquamarine
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.editor.PropertyInspector
import me.anno.utils.OS.documents

fun main() {

    // todo UI is invisible.. why???

    OfficialExtensions.register()
    ExtensionLoader.load()
    ECSRegistry.init()

    // index sample project
    val folder = documents.getChild("RemsEngine/YandereSim")
    val project = GameEngineProject.readOrCreate(folder)!!
    currentProject = project
    project.init()

    // a few temporary files; cannot be GCed, because scope isn't left at runtime
    val green = Material.diffuse(mediumAquamarine).ref
    val darkRed = Material.diffuse(fireBrick).ref
    val tested = MeshComponent(flatCube.front, Material())
    EditorState.select(tested)
    disableRenderDoc()
    testUI3("Easy Material Editing", PropertyInspector({ EditorState.selection }, style))
}