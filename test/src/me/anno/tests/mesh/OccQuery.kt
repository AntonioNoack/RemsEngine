package me.anno.tests.mesh

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.query.OcclusionQuery
import me.anno.utils.OS.documents

fun main() {
    val scene = Entity()
    val mesh = MeshComponent(documents.getChild("sphere.obj"))
    mesh.occlusionQuery = OcclusionQuery(16, 16)
    scene.add(mesh)
    testSceneWithUI(scene)
}