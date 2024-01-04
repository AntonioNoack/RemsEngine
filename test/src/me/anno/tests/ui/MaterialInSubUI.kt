package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ECSRegistry
import me.anno.engine.GameEngineProject
import me.anno.engine.GameEngineProject.Companion.currentProject
import me.anno.engine.ui.EditorState
import me.anno.ui.UIColors.mediumAquamarine
import me.anno.ui.UIColors.fireBrick
import me.anno.mesh.Shapes.flatCube
import me.anno.ui.debug.TestStudio.Companion.testUI3
import me.anno.ui.editor.PropertyInspector
import me.anno.utils.OS.documents

fun main() {

    ECSRegistry.init()

    // todo index sample project
    val folder = documents.getChild("RemsEngine/YandereSim")
    val project = GameEngineProject.readOrCreate(folder)!!
    currentProject = project
    project.init()

    // a few temporary files; cannot be GCed, because scope isn't left at runtime
    val green = Material.diffuse(mediumAquamarine).ref
    val darkRed = Material.diffuse(fireBrick).ref
    val tested = MeshComponent(flatCube.front, Material())
    EditorState.select(tested)
    testUI3("Easy Material Editing", PropertyInspector({ EditorState.selection }, style))
}