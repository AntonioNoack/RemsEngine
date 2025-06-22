package me.anno.experiments.convexdecomposition

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.Clock
import me.anno.utils.OS.downloads

fun main() {

    OfficialExtensions.initForTests()
    val clock = Clock("Dragon Decomposition")
    val dragonFile = downloads.getChild("3d/dragon.obj")
    val mesh = MeshCache.getEntry(dragonFile).waitFor() as Mesh
    clock.stop("Loading Mesh")

    // calculate decomposition
    val hulls = ConvexDecomposition().splitMesh(mesh)
    clock.stop("Decomposition")

    visualizeHulls(hulls)

    val scene = Entity("Scene")
        .add(MeshComponent(dragonFile))

    testSceneWithUI("Dragon Decomposition", scene)
}
