package me.anno.tests.gfx

import me.anno.ecs.Entity
import me.anno.ecs.components.light.EnvironmentMap
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.shaders.SkyBox
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS
import org.joml.Vector3d

fun main() {

    // rename EnvironmentMap to ReflectionMap or ReflectionCubemap?

    // test environment map
    ECSRegistry.init()
    val scene = Entity()
    scene.add(Entity().apply {
        position = Vector3d(2.8, 0.0, 0.0)
        add(MeshComponent(OS.downloads.getChild("3d/DamagedHelmet.glb")))
        add(EnvironmentMap())
    })
    scene.add(Entity().apply {
        add(MeshComponent(OS.documents.getChild("MetallicSphere.glb")))
        add(EnvironmentMap())
    })
    scene.add(MeshComponent(OS.documents.getChild("metal-roughness.glb")))
    scene.add(SkyBox())
    testSceneWithUI("EnvironmentMap", scene)

}
