package me.anno.tests.engine.light

import me.anno.ecs.Entity
import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.light.PointLight
import me.anno.ecs.components.light.SpotLight
import me.anno.ecs.components.light.sky.SkyboxBase
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.DefaultAssets.flatCube
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.RenderDoc.forceLoadRenderDoc
import me.anno.maths.Maths.PIf
import me.anno.sdf.shapes.SDFSphere
import me.anno.utils.OS.downloads

/**
 * Showcase of all light types
 *
 * todo shadows are broken, when clip-control isn't supported
 *    verify all shadows work correctly without reverse depth
 * except for EnvironmentMap, which is kind of a light, too
 * */
fun main() {

    forceLoadRenderDoc()
    OfficialExtensions.initForTests()

    val scene = Entity("Scene")
    scene.add(SkyboxBase().apply { skyColor.set(0f) })

    // if your eyes hurt too much, reduce this number
    Entity("Floor", scene)
        .add(MeshComponent(flatCube))
        .setPosition(0.0, -0.1, -0.5)
        .setScale(3f, 0.1f, 1.5f)

    val truckFile = downloads.getChild("MagicaVoxel/vox/truck.vox")
    fun placeTruck(e: Entity) {
        val pos = e.transform.localPosition
        Entity("${e.name} Truck", scene)
            .add(MeshComponent(truckFile))
            .setPosition(pos.x - 3f / 64f, 0.0, 0.0)
            .setScale(1f / 64f)

        // add an SDF sphere to test shadows there, too
        val sphere = SDFSphere()
        sphere.position.set(pos.x.toFloat() - 0.35f, 0.1f, -0.2f)
        sphere.scale = 0.08f
        scene.add(sphere)

        // place a few sample cubes
        val cubes = Entity("${e.name} Cubes", scene)
            .setPosition(pos.x + 0.35, 1.0 / 64, 0.0)
        for (zi in 0 until 5) {
            for (y in 0 until 2) {
                for (z in 0 until 3) {
                    for (x in 0 until 3) {
                        cubes.add(
                            Entity("Cube[$x,$y,$z+$zi]", cubes)
                                .add(MeshComponent(flatCube))
                                .setPosition(x * 0.1, y * 0.1, (z - zi * 4) * 0.1)
                                .setScale(1f / 64f)
                        )
                    }
                }
            }
        }
    }

    val brightness = 3f

    // sunlight
    val sun = Entity("Directional")
    sun.add(DirectionalLight().apply {
        color.set(brightness)
        shadowMapCascades = 2
        cutoff = 1e-3f
    })
    sun.setPosition(-2.0, 0.0, 0.0)
    sun.setRotation(-PIf * 0.5f, 0f, 0f)
    scene.add(sun)
    placeTruck(sun)

    // point light, e.g., a lamp
    val point = Entity("Point")
    point.add(PointLight().apply {
        color.set(brightness)
        shadowMapCascades = 1
    })
    point.setPosition(0.0, 0.5, 0.0)
    scene.add(point)
    placeTruck(point)

    val spot = Entity("Spot")
    spot.add(SpotLight().apply {
        color.set(brightness)
        shadowMapCascades = 1
    })
    spot.setPosition(2.0, 0.5, 0.5)
    spot.setRotation(-PIf * 0.3f, 0f, 0f)
    spot.setScale(2f)
    scene.add(spot)
    placeTruck(spot)

    testSceneWithUI("Shadow Types", scene)
}