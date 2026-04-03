package me.anno.tests.gfx.matcap

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.material.MatCapMaterial
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.sdf.shapes.SDFBlob
import me.anno.utils.OS.res
import me.anno.utils.types.Floats.toIntOr
import kotlin.math.ceil
import kotlin.math.sqrt

fun main() {
    val matNames = "BlackPlastic,Ceramic,GreenGlass,GreenPlastic,Mud,Water".split(',')
    val scene = Entity("Scene")
    val nx = ceil(sqrt(matNames.size.toFloat())).toIntOr()
    for (i in matNames.indices) {
        val matName = matNames[i]
        val materialI = MatCapMaterial().apply {
            name = matName
            matCapMap = res.getChild("textures/matcap/$matName.webp")
        }
        Entity(matNames[i], scene)
            .setPosition((i % nx) * 3.8, 0.0, (i / nx) * 3.8)
            .add(SDFBlob().apply {
                sdfMaterials = listOf(materialI.ref)
            })
    }

    testSceneWithUI("MatCap-Materials", scene)
}