package me.anno.tests.engine.material

import me.anno.ecs.Entity
import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.light.sky.Skybox
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.material.ParallaxMaterial
import me.anno.ecs.components.mesh.material.shaders.ParallaxShader
import me.anno.engine.DefaultAssets
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.tests.network.rollingshooter.createLighting
import me.anno.utils.OS.pictures
import me.anno.utils.types.Floats.toRadians

fun main() {

    OfficialExtensions.initForTests()
    val folder = pictures.getChild("Textures/marble_cliff_01_4k.blend/textures")

    val baselineMaterial = Material().apply {
        diffuseMap = folder.getChild("marble_cliff_01_diff_4k.jpg")
        normalMap = folder.getChild("marble_cliff_01_nor_gl_4k.exr/bgra.png")
        roughnessMap = folder.getChild("marble_cliff_01_rough_4k.exr")
        roughnessMinMax.set(0f, 1f)
    }

    val parallaxMaterial = ParallaxMaterial().apply {
        parallaxMap = folder.getChild("marble_cliff_01_disp_4k.png")
        parallaxScale = 0.2f
    }
    baselineMaterial.copyInto(parallaxMaterial)
    parallaxMaterial.shader = ParallaxShader

    val mesh = DefaultAssets.flatCube
    val scene = Entity("Scene")
    Entity("Parallax", scene)
        .add(MeshComponent(mesh, parallaxMaterial))
        .setPosition(1.4, 0.0, 0.0)
    Entity("Baseline", scene)
        .add(MeshComponent(mesh, baselineMaterial))
        .setPosition(-1.4, 0.0, 0.0)

    Entity("Floor", scene)
        .add(MeshComponent(DefaultAssets.plane))
        .setPosition(0.0, -1.0, 0.0)
        .setScale(3f)

    // add sun and shadows to the scene
    val sky = Skybox()
    scene.add(sky)
    val sun = DirectionalLight()
    sun.color.set(10f)
    sun.shadowMapCascades = 1
    sun.shadowMapResolution = 1024
    sun.autoUpdate = 1
    val sunE = Entity(scene)
    sunE.setScale(3f) // covering the map
    sunE.setRotation(sky.sunRotation)
    sunE.add(sun)

    testSceneWithUI("Parallax Test", scene)
}
