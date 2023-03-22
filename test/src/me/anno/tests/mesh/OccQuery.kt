package me.anno.tests.mesh

import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.query.OcclusionQuery
import me.anno.utils.OS.documents

fun main() {
    val scene = MeshComponent(documents.getChild("sphere.obj"))
    scene.occlusionQuery = OcclusionQuery()
    testSceneWithUI(scene)
}