package me.anno.tests.engine.light

import me.anno.ecs.Entity
import me.anno.ecs.components.light.SpotLight
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.RenderDoc.forceLoadRenderDoc
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.mix
import me.anno.mesh.Shapes.flatCube
import me.anno.sdf.shapes.SDFSphere
import me.anno.utils.OS.downloads
import org.joml.Vector3d

/**
 * Showcase of all light types
 *
 * except for EnvironmentMap, which is kind of a light
 * */
fun main() {
    forceLoadRenderDoc()
    spotLightTest()
}

fun spotLightTest() {

    ECSRegistry.init()

    val scene = Entity("Scene")

    val metallic = Material()
    metallic.metallicMinMax.set(0.9f)
    metallic.roughnessMinMax.set(0.1f)

    // if your eyes hurt too much, reduce this number
    val numStripes = 150
    val floorHalfSize = Vector3d(2.0, 0.1, 3.0)
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
        stripe.setScale(floorHalfSize.x.toFloat(), floorHalfSize.y.toFloat(), floorHalfSize.z.toFloat() / numStripes)
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
        truck.setScale(1f / 64f)
        val sphere = SDFSphere()
        sphere.position.set(e.transform.localPosition.x.toFloat() - 0.35f, 0.1f, -0.2f)
        sphere.scale = 0.08f
        scene.add(sphere)
    }

    val spot = Entity("Spot")
    spot.add(SpotLight().apply { color.set(10f); shadowMapCascades = 1 })
    spot.setPosition(0.0, 0.5, 0.5)
    spot.setRotation(-PIf * 0.3f, 0f, 0f)
    spot.setScale(5f)
    scene.add(spot)
    placeTruck(spot)

    testSceneWithUI("Spot Light", scene)
}