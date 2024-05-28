package me.anno.tests.mesh.gltf

import me.anno.ecs.Entity
import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.light.LightComponent
import me.anno.ecs.components.light.PointLight
import me.anno.ecs.components.light.SpotLight
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ECSRegistry
import me.anno.mesh.gltf.GLTFWriter
import me.anno.utils.OS.desktop

fun main() {
    // todo test these, Blender loads them :)
    //  directional light seems to be flipped in Blender... why??
    ECSRegistry.init()
    writeLight(DirectionalLight())
    writeLight(SpotLight())
    writeLight(PointLight())
}

fun writeLight(light: LightComponent) {
    val scene = Entity()
    scene.add(light)
    scene.add(MeshComponent(LightComponent.shapeForTesting))
    val dst = desktop.getChild("GLTF-Lights/${
        light.name.ifBlank { light.className }
    }.glb")
    dst.getParent().tryMkdirs()
    GLTFWriter().write(scene, dst)
}