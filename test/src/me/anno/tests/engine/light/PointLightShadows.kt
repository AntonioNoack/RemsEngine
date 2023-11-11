package me.anno.tests.engine.light

import me.anno.ecs.Entity
import me.anno.ecs.components.light.PointLight
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.RenderDoc.forceLoadRenderDoc
import me.anno.mesh.Shapes.flatCube
import java.util.*

/**
 * Test the shadow of a point light properly;
 *
 * go inside the cube, and inspect the shadows
 * */
fun main() {

    forceLoadRenderDoc()

    val scene = Entity("Scene")
    val floor = Entity("Floor", scene)
    floor.add(MeshComponent(flatCube.scaled(0.5f).both))

    val lightE = Entity("Light", scene)
    val light = PointLight()
    light.shadowMapCascades = 1
    light.color.set(10f)
    lightE.add(light)

    // spawn random cubes, so the shadow can fall on sth
    val rnd = Random()
    val cubes = Entity("Cubes")
    for (i in 0 until 20) {
        val cube = Entity("Cube $i")
        cube.setPosition(rnd.nextDouble() - 0.5, rnd.nextDouble() - 0.5, rnd.nextDouble() - 0.5)
        cube.setScale(0.03)
        val comp = MeshComponent(flatCube.front)
        comp.isInstanced = true
        cube.add(comp)
        cubes.add(cube)
    }
    scene.add(cubes)

    testSceneWithUI("Point Light Shadows", scene)
}