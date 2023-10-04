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

    val metallic = Material()
    metallic.metallicMinMax.set(0.9f)
    metallic.roughnessMinMax.set(0.1f)

    val floor = Entity("Floor")
    val floorMat = Material()
    floorMat.roughnessMinMax.set(0.1f)
    floor.add(MeshComponent(flatCube.front).apply { materials = listOf(floorMat.ref) })
    floor.setPosition(0.0, -0.1, 0.0)
    floor.setScale(8.0, 0.1, 3.0)
    scene.add(floor)

    fun placeTruck(e: Entity) {
        val truck = Entity("${e.name} Truck")
        val mesh = MeshComponent(downloads.getChild("MagicaVoxel/vox/truck.vox"))
        mesh.materials = listOf(metallic.ref)
        truck.add(mesh)
        truck.setPosition(e.transform.localPosition.x, 0.0, 0.0)
        truck.setScale(1.0 / 64.0)
        scene.add(truck)
    }

    // global lights
    val ambient = AmbientLight()
    ambient.color.set(0.3f)
    scene.add(ambient)

    val sun = Entity("Directional")
    sun.add(DirectionalLight().apply { shadowMapCascades = 1; cutoff = 1e-3f })
    sun.setPosition(-5.0, 0.0, 0.0)
    sun.setRotation(-PI * 0.5, 0.0, 0.0)
    scene.add(sun)
    placeTruck(sun)

    // local lights
    val point = Entity("Point")
    point.add(PointLight().apply { color.set(10f); shadowMapCascades = 1 })
    point.setPosition(-3.0, 0.5, 0.0)
    scene.add(point)
    placeTruck(point)

    val spot = Entity("Spot")
    spot.add(SpotLight().apply { color.set(10f); shadowMapCascades = 1 })
    spot.setPosition(-1.0, 0.5, 0.5)
    spot.setRotation(-PI * 0.3, 0.0, 0.0)
    spot.setScale(5.0)
    scene.add(spot)
    placeTruck(spot)

    val tube = Entity("Tube")
    tube.add(RectangleLight().apply { color.set(10f); height = 0f })
    tube.setPosition(1.0, 0.005, 0.0)
    tube.setRotation(PI * 0.5, 0.0, 0.0)
    scene.add(tube)

    val circle = Entity("Circle")
    circle.add(CircleLight().apply { color.set(10f) })
    circle.setPosition(3.0, 0.005, 0.0)
    circle.setRotation(PI * 0.5, 0.0, 0.0)
    scene.add(circle)

    val rect = Entity("Rectangle")
    rect.add(RectangleLight().apply { color.set(10f) })
    rect.setPosition(5.0, 0.005, 0.0)
    rect.setRotation(PI * 0.5, 0.0, 0.0)
    scene.add(rect)

    testSceneWithUI("Light Types", scene)
}