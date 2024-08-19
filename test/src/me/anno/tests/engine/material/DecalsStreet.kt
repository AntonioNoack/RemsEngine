package me.anno.tests.engine.material

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.DecalMaterial
import me.anno.ecs.components.mesh.material.Material
import me.anno.engine.DefaultAssets
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.mesh.Shapes.flatCube

/**
 * create a street, and place decal markings onto it
 * todo bug: decals in the distance disappear in VR
 *  making them higher improves things, so probably a depth-calculation issue or sth like that
 * */
fun main() {
    OfficialExtensions.initForTests()
    val decalMaterial = DecalMaterial()
    decalMaterial.emissiveBase.set(3f)
    decalMaterial.writeEmissive = true
    decalMaterial.writeColor = true
    decalMaterial.decalSharpness.set(1000f)

    val scene = Entity("Scene")
    Entity("Grass", scene)
        .add(MeshComponent(DefaultAssets.plane, Material.diffuse(0x66BF67)))
        .setScale(100.0)
    Entity("Street", scene)
        .add(MeshComponent(flatCube.front, Material.diffuse(0x222222)))
        .setScale(4.0, 0.1, 100.0)

    val markings = Entity("Markings", scene)
    fun addMarking(x: Double, dx: Double) {
        Entity("Edge Marking", markings)
            .add(MeshComponent(flatCube.front, decalMaterial))
            .setPosition(x, 0.1, 0.0)
            .setScale(dx, 0.1, 100.0)
    }

    addMarking(-3.8, 0.1)
    addMarking(+3.8, 0.1)

    val middle = Entity("Middle", markings)
    for (i in -20..20) {
        Entity("Middle Marking $i", middle)
            .add(MeshComponent(flatCube.front, decalMaterial))
            .setPosition(0.0, 0.1, i * 100.0 / 20.5)
            .setScale(0.1, 0.1, 1.2)
    }

    testSceneWithUI("Decals", scene)
}