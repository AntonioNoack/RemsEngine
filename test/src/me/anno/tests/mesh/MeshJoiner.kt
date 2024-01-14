package me.anno.tests.mesh

import me.anno.ecs.components.mesh.MeshCache
import me.anno.engine.ECSRegistry
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.extensions.ExtensionLoader
import me.anno.engine.EngineBase
import me.anno.utils.OS.documents

fun main() {
    OfficialExtensions.register()
    ExtensionLoader.load()
    ECSRegistry.init()
    val workspace = documents.getChild("RemsEngine/YandereSim")
    EngineBase.workspace = workspace
    // the source file isn't a Mesh yet, so it has to be joined
    testSceneWithUI("MeshCache/MeshJoiner", MeshCache[workspace.getChild("School.json")]!!)
}