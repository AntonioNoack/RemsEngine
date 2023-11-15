package me.anno.tests.mesh

import me.anno.ecs.components.mesh.MeshCache
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.studio.StudioBase
import me.anno.utils.OS.documents

fun main() {
    ECSRegistry.initPrefabs()
    ECSRegistry.initMeshes()
    val workspace = documents.getChild("RemsEngine/YandereSim")
    StudioBase.workspace = workspace
    // the source file isn't a Mesh yet, so it has to be joined
    // todo some transforms are messed up :(, why?
    testSceneWithUI("MeshCache/MeshJoiner", MeshCache[workspace.getChild("School.json")]!!)
}