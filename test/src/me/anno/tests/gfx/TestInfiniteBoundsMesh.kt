package me.anno.tests.gfx

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.DefaultAssets.flatCube
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import org.joml.Vector3d

/**
 * verify that the mesh is still visible
 * */
fun main() {

    val e = 1e38
    val positions = listOf(
        Vector3d(),
        Vector3d(-e, -e, -e),
        Vector3d(-e, -e, +e),
        Vector3d(-e, +e, -e),
        Vector3d(-e, +e, +e),
        Vector3d(+e, -e, -e),
        Vector3d(+e, -e, +e),
        Vector3d(+e, +e, -e),
        Vector3d(+e, +e, +e),
    )

    val base = flatCube
    val tmpScene = Entity()
    for (pos in positions) {
        Entity(tmpScene)
            .setPosition(pos)
            .setScale(1.0 + pos.length() * 0.2)
            .add(MeshComponent(base))
    }

    // why is it zoomed out soo far???
    //  I'd expect 10^38, not 10^150
    // -> length() used by ECSSceneTab didn't handle 1e38 well

    val hugeMesh = MeshCache[tmpScene.ref]!!
    // testSceneWithUI("HugeMesh", Entity().add(MeshComponent(hugeMesh)))
    testSceneWithUI("HugeMesh", hugeMesh)
}