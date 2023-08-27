package me.anno.tests.ecs

import me.anno.ecs.Entity
import me.anno.ecs.components.light.*
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.GFXBase
import me.anno.mesh.Shapes.flatCube
import me.anno.utils.OS.downloads
import kotlin.math.PI

/**
 * Showcase of all light types
 *
 * except for EnvironmentMap, which is kind of a light
 * */
fun main() {

    GFXBase.forceLoadRenderDoc()
    ECSRegistry.init()

    val scene = Entity("Scene")

    // floor
    val floor = Entity("Floor")
    val floorMat = Material()
    floorMat.roughnessMinMax.set(0.1f)
    floor.add(MeshComponent(flatCube.front).apply { materials = listOf(floorMat.ref) })
    floor.position = floor.position.set(0.0, -1.0, 0.0)
    floor.scale = floor.scale.set(20.0, 1.0, 20.0)
    scene.add(floor)

    fun placeTruck(e: Entity, x: Double, scale: Double = 1.0 / 64.0) {
        val truck = Entity("${e.name} Truck")
        truck.add(MeshComponent(downloads.getChild("MagicaVoxel/vox/truck.vox")))
        truck.position = truck.position.set(x, 0.0, 0.0)
        truck.scale = truck.scale.set(scale)
        scene.add(truck)
    }

    // global lights
    val ambient = AmbientLight()
    ambient.color.set(0.3f)
    scene.add(ambient)

    val sun = Entity("Directional")
    sun.add(DirectionalLight().apply { shadowMapCascades = 1; cutoff = 1e-3f; autoUpdate = false })
    sun.position = sun.position.set(0.0, 0.0, 0.0)
    sun.rotation = sun.rotation.rotateX(-PI * 0.5)
    scene.add(sun)
    placeTruck(sun, 0.0)

    // local lights
    val point = Entity("Point")
    point.add(PointLight().apply { color.set(10f); shadowMapCascades = 1; autoUpdate = false })
    point.position = point.position.set(2.0, 0.5, 0.0)
    scene.add(point)
    placeTruck(point, 2.0)

    val spot = Entity("Spot")
    spot.add(SpotLight().apply { color.set(10f); shadowMapCascades = 1; autoUpdate = false })
    spot.position = spot.position.set(4.0, 0.5, 0.5)
    spot.rotation = spot.rotation.rotateX(-PI * 0.3)
    scene.add(spot)
    placeTruck(spot, 4.0)

    val tube = Entity("Tube")
    tube.add(RectangleLight().apply { color.set(10f); height = 0f })
    tube.position = tube.position.set(6.0, 0.005, 0.0)
    tube.rotation = tube.rotation.rotateX(PI * 0.5)
    scene.add(tube)

    val circle = Entity("Circle")
    circle.add(CircleLight().apply { color.set(10f) })
    circle.position = circle.position.set(8.0, 0.005, 0.0)
    circle.rotation = circle.rotation.rotateX(PI * 0.5)
    scene.add(circle)

    val rect = Entity("Rectangle")
    rect.add(RectangleLight().apply { color.set(10f) })
    rect.position = rect.position.set(10.0, 0.005, 0.0)
    rect.rotation = rect.rotation.rotateX(PI * 0.5)
    scene.add(rect)

    testSceneWithUI("Light Types", scene)
}