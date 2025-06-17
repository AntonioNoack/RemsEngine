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

    val mesh = MeshCache[downloads.getChild("3d/lucy.obj")] as Mesh

    val simplified = simplifyMesh(mesh, 0.2f, 8)
    val simplified2 = simplifyMesh(mesh, 0.01f, 8)

    val scene = Entity()
    Entity("1%", scene)
        .add(MeshComponent(simplified2))
        .setPosition(-mesh.getBounds().deltaX * 1.5, 0.0, 0.0)

    Entity("20%", scene)
        .add(MeshComponent(simplified))

    Entity("Original", scene)
        .add(MeshComponent(mesh))
        .setPosition(mesh.getBounds().deltaX * 1.5, 0.0, 0.0)

    testSceneWithUI("Mesh Simplification2", scene, RenderMode.LINES_MSAA)
}