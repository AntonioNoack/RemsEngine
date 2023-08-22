package me.anno.tests.mesh

import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.query.OcclusionQuery
import me.anno.sdf.shapes.SDFSphere

fun main() {
    val mesh = SDFSphere()
    mesh.occlusionQuery = OcclusionQuery(16, 16)
    testSceneWithUI("Occlusion Query", mesh)
}