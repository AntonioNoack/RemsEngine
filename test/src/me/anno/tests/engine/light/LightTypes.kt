package me.anno.tests.engine.light

import me.anno.ecs.Entity
import me.anno.ecs.components.light.CircleLight
import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.light.PointLight
import me.anno.ecs.components.light.RectangleLight
import me.anno.ecs.components.light.SpotLight
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.RenderDoc.forceLoadRenderDoc
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.mix
import me.anno.mesh.Shapes.flatCube
import me.anno.sdf.shapes.SDFSphere
import me.anno.utils.OS.downloads
import org.joml.Vector3f

/**
 * Showcase of all light types
 *
 * except for EnvironmentMap, which is kind of a light, too
 * */
fun main() {
    forceLoadRenderDoc()
    OfficialExtensions.initForTests()
    testSceneWithUI("Light Types", createLightTypesScene())
}

fun createLightTypesScene(): Entity {
    val scene = Entity("Scene")
    // scene.add(SkyboxBase().apply { skyColor.set(0f) })

    val metallic = Material()
    metallic.metallicMinMax.set(0.9f)
    metallic.roughnessMinMax.set(0.1f)

    // if your eyes hurt too much, reduce this number
    val numStripes = 150
    val floorHalfSize = Vector3f(8f, 0.1f, 3f)
    val floor = Entity("Floor", scene)
    fun placeFloor(z: Double, r: Float) {
        val floorMat = Material().apply {
            roughnessMinMax.set(mix(0.1f, 1f, r))
            metallicMinMax.set(1f - r)
        }
        Entity("Floor", floor)
            .add(MeshComponent(flatCube.front).apply {
                isInstanced = true
                materials = listOf(floorMat.ref)
            })
            .setPosition(0.0, -floorHalfSize.y.toDouble(), z)
            .setScale(floorHalfSize.x, floorHalfSize.y, floorHalfSize.z / numStripes)
    }
    for (i in 0 until numStripes) {
        placeFloor(3.0 * ((i + 0.5) * 2.0 / numStripes - 1.0), i.and(1).toFloat())
    }

    fun placeTruck(e: Entity) {
        Entity("${e.name} Truck", scene)
            .add(MeshComponent(downloads.getChild("MagicaVoxel/vox/truck.vox"), metallic))
            .setPosition(e.transform.localPosition.x, 0.0, 0.0)
            .setScale(1f / 64f)
        // add an SDF sphere to test shadows there, too
        scene.add(SDFSphere().apply {
            position.set(e.transform.localPosition.x.toFloat() - 0.35f, 0.1f, -0.2f)
            scale = 0.08f
        })
    }

    // sunlight
    // todo bug: SDF shapes throw shadows only in a small range (seemingly scaled on z-axis, probably forgetting a factor)
    val sun = Entity("Directional", scene)
        .add(DirectionalLight().apply {
            shadowMapCascades = 2
            cutoff = 1e-3f
            color.mul(10f)
        })
        .setPosition(-5.0, 0.0, 0.0)
        .setRotation(-PIf * 0.5f, 0f, 0f)
        .setScale(4f, 4f, 8f)
    placeTruck(sun)

    // local lights
    val point = Entity("Point", scene)
        .add(PointLight().apply { color.set(10f); shadowMapCascades = 1 })
        .setPosition(-3.0, 0.5, 0.0)
    placeTruck(point)

    // todo bug: spot-light shadows are broken
    val spot = Entity("Spot", scene)
        .add(SpotLight().apply { color.set(10f); shadowMapCascades = 1 })
        .setPosition(-1.0, 0.5, 0.5)
        .setRotation(-PIf * 0.3f, 0f, 0f)
        .setScale(5f)
    placeTruck(spot)

    Entity("Tube", scene)
        .add(RectangleLight().apply { color.set(10f); height = 0f })
        .setPosition(1.0, 0.005, 0.0)
        .setRotation(PIf * 0.5f, 0f, 0f)

    Entity("Circle", scene)
        .add(CircleLight().apply { color.set(10f) })
        .setPosition(3.0, 0.005, 0.0)
        .setRotation(PIf * 0.5f, 0f, 0f)

    Entity("Rectangle", scene)
        .add(RectangleLight().apply { color.set(10f) })
        .setPosition(5.0, 0.005, 0.0)
        .setRotation(PIf * 0.5f, 0f, 0f)

    return scene
}