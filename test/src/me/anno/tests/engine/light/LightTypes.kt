package me.anno.tests.engine.light

import me.anno.ecs.Entity
import me.anno.ecs.components.light.CircleLight
import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.light.PointLight
import me.anno.ecs.components.light.RectangleLight
import me.anno.ecs.components.light.SpotLight
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.RenderDoc.forceLoadRenderDoc
import me.anno.maths.Maths.mix
import me.anno.mesh.Shapes.flatCube
import me.anno.sdf.shapes.SDFSphere
import me.anno.utils.OS.downloads
import org.joml.Vector3d
import kotlin.math.PI

/**
 * Showcase of all light types
 *
 * except for EnvironmentMap, which is kind of a light
 * */
fun main() {

    forceLoadRenderDoc()
    ECSRegistry.init()

    val scene = Entity("Scene")
    // scene.add(SkyboxBase().apply { skyColor.set(0f) })

    val metallic = Material()
    metallic.metallicMinMax.set(0.9f)
    metallic.roughnessMinMax.set(0.1f)

    // if your eyes hurt too much, reduce this number
    val numStripes = 150
    val floorHalfSize = Vector3d(8.0, 0.1, 3.0)
    val floor = Entity("Floor", scene)
    fun placeFloor(z: Double, r: Float) {
        val stripe = Entity("Floor", floor)
        val floorMat = Material()
        floorMat.roughnessMinMax.set(mix(0.1f, 1f, r))
        floorMat.metallicMinMax.set(1f - r)
        stripe.add(MeshComponent(flatCube.front).apply {
            isInstanced = true
            materials = listOf(floorMat.ref)
        })
        stripe.setPosition(0.0, -floorHalfSize.y, z)
        stripe.setScale(floorHalfSize.x, floorHalfSize.y, floorHalfSize.z / numStripes)
    }
    for (i in 0 until numStripes) {
        placeFloor(3.0 * ((i + 0.5) * 2.0 / numStripes - 1.0), i.and(1).toFloat())
    }

    fun placeTruck(e: Entity) {
        val truck = Entity("${e.name} Truck", scene)
        val mesh = MeshComponent(downloads.getChild("MagicaVoxel/vox/truck.vox"))
        mesh.materials = listOf(metallic.ref)
        truck.add(mesh)
        truck.setPosition(e.transform.localPosition.x, 0.0, 0.0)
        truck.setScale(1.0 / 64.0)
        // add an SDF sphere to test shadows there, too
        val sphere = SDFSphere()
        sphere.position.set(e.transform.localPosition.x.toFloat() - 0.35f, 0.1f, -0.2f)
        sphere.scale = 0.08f
        scene.add(sphere)
    }

    // sunlight
    val sun = Entity("Directional")
    sun.add(DirectionalLight().apply { shadowMapCascades = 2; cutoff = 1e-3f })
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