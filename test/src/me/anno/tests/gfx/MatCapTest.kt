package me.anno.tests.gfx

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.MatCapMaterial
import me.anno.engine.DefaultAssets
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.res
import me.anno.utils.types.Floats.toIntOr
import kotlin.math.ceil
import kotlin.math.sqrt

// todo apply matcap to SDFs

fun main() {
    OfficialExtensions.initForTests()

    val matNames = "BlackPlastic,Ceramic,GreenGlass,GreenPlastic,Mud,Water".split(',')
    val scene = Entity("Scene")
    val nx = ceil(sqrt(matNames.size.toFloat())).toIntOr()
    for (i in matNames.indices) {
        val matName = matNames[i]
        val material = MatCapMaterial().apply {
            name = matName
            matCapMap = res.getChild("textures/matcap/$matName.webp")
        }
        Entity(matNames[i], scene)
            .add(MeshComponent(DefaultAssets.icoSphere, material))
            .setPosition((i % nx) * 2.1, 0.0, (i / nx) * 2.1)
        break
    }

    testSceneWithUI("MatCap-Materials", scene)
}