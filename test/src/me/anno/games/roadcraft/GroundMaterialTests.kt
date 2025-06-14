package me.anno.games.roadcraft

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.engine.DefaultAssets
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.Color.toVecRGBA

// sand, gravel, rough tarmac, smooth tarmac, dirt, grass
val sand = Material.diffuse(0xffffcc)
val gravel = Material.diffuse(0x666666)
val rock = Material.diffuse(0x333333)
val roughTarmac = Material.diffuse(0x222222)
val smoothTarmac = diffuse(0x181818, 0.2f)
val dirt = Material.diffuse(0x946649)
val grass = Material.diffuse(0xccff66)

fun diffuse(color: Int, roughness: Float): Material {
    val mat = Material()
    color.toVecRGBA(mat.diffuseBase)
    mat.diffuseBase.w = 1f
    mat.roughnessMinMax.set(roughness)
    return mat
}

fun main() {
    val materials = listOf(
        sand, gravel, rock,
        roughTarmac, smoothTarmac,
        dirt, grass
    )
    val scene = Entity()
    for (i in materials.indices) {
        Entity(scene)
            .setPosition(i * 2.5, 0.0, 0.0)
            .add(MeshComponent(DefaultAssets.icoSphere, materials[i]))
    }
    testSceneWithUI("Materials", scene)
}