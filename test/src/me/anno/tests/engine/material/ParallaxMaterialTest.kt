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
import me.anno.utils.OS.pictures

fun main() {

    OfficialExtensions.initForTests()
    val folder = pictures.getChild("Textures/marble_cliff_01_4k.blend/textures")

    val testShadows = true

    val baselineMaterial = Material().apply {
        if (!testShadows) {
            diffuseMap = folder.getChild("marble_cliff_01_diff_4k.jpg")
            normalMap = folder.getChild("marble_cliff_01_nor_gl_4k.exr/bgra.png")
            roughnessMap = folder.getChild("marble_cliff_01_rough_4k.exr")
            roughnessMinMax.set(0f, 1f)
        }
    }

    val parallaxMaterial0 = ParallaxMaterial().apply {
        parallaxMap =
            if (testShadows) pictures.getChild("Textures/Circle.webp")
            else folder.getChild("marble_cliff_01_disp_4k.png")
        parallaxScale = if (testShadows) 0.05f else 0.2f
    }
    baselineMaterial.copyInto(parallaxMaterial0)
    parallaxMaterial0.shader = ParallaxShader

    val parallaxMaterial1 = ParallaxMaterial().apply {
        parallaxMaterial0.copyInto(this)
        parallaxSilhouette = false
    }

    val mesh0 = DefaultAssets.flatCube
    val mesh1 = DefaultAssets.uvSphere

    val scene = Entity("Scene")

    Entity("Parallax0", scene)
        .add(MeshComponent(mesh0, parallaxMaterial0))
        .setPosition(1.4, 0.0, +1.9)
    Entity("Baseline0", scene)
        .add(MeshComponent(mesh0, baselineMaterial))
        .setPosition(-1.4, 0.0, +1.9)

    Entity("Parallax1", scene)
        .add(MeshComponent(mesh1, parallaxMaterial1))
        .setPosition(1.4, 0.0, -1.9)
    Entity("Baseline1", scene)
        .add(MeshComponent(mesh1, baselineMaterial))
        .setPosition(-1.4, 0.0, -1.9)

    Entity("Floor", scene)
        .add(MeshComponent(DefaultAssets.plane))
        .setPosition(0.0, -1.0, 0.0)
        .setScale(4f)

    // add sun and shadows to the scene
    val sky = Skybox()
    scene.add(sky)
    val sun = DirectionalLight()
    sun.color.set(10f)
    sun.shadowMapCascades = 1
    sun.shadowMapResolution = 1024
    sun.autoUpdate = 1
    val sunE = Entity(scene)
    sunE.setScale(4f) // covering the map
    sunE.setRotation(sky.sunRotation)
    sunE.add(sun)

    testSceneWithUI("Parallax Test", scene)
}
