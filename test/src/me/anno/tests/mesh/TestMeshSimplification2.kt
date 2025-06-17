package me.anno.tests.mesh

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.downloads

fun main() {

    OfficialExtensions.initForTests()

    val mesh0 = MeshCache[downloads.getChild("3d/lucy.obj")] as Mesh
    val mesh1 = simplifyMesh(mesh0, 0.2f, 5)
    val mesh2 = simplifyMesh(mesh1, 0.05f, 5)

    val scene = Entity()
    Entity("1%", scene)
        .add(MeshComponent(mesh2))
        .setPosition(-mesh0.getBounds().deltaX * 1.5, 0.0, 0.0)

    Entity("20%", scene)
        .add(MeshComponent(mesh1))

    Entity("Original", scene)
        .add(MeshComponent(mesh0))
        .setPosition(mesh0.getBounds().deltaX * 1.5, 0.0, 0.0)

    testSceneWithUI("Mesh Simplification2", scene, RenderMode.LINES_MSAA)
}