package me.anno.tests.engine.animation

import me.anno.ecs.Entity
import me.anno.ecs.components.anim.AnimMeshComponent
import me.anno.ecs.components.anim.BoneAttachmentComponent
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.shapes.PlaneModel
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.mesh.Shapes.flatCube
import me.anno.utils.Color.black
import me.anno.utils.OS.downloads
import me.anno.utils.types.Floats.toRadians
import org.apache.logging.log4j.LogManager

/**
 * Woman on phone, where we add the phone using our own logic, using AttachToBoneComponent.
 * It copies the skeletal animation being played onto our entity (phone).
 * */
fun main() {

    val scene = Entity("Scene")

    // this mesh, including animation is from Mixamo
    val character = PrefabCache[downloads.getChild("3d/Talking On Phone.fbx")]?.createInstance() as Entity
    scene.add(character)

    // a floor for decoration
    scene.add(
        Entity("Floor", MeshComponent(PlaneModel.createPlane(2, 2)))
            .setScale(2.5)
    )

    // find first AnimMeshComponent, so we can attach the calculation there
    val renderer = character.getComponentInChildren(AnimMeshComponent::class)!!

    val prop = Entity("Phone", scene)
    prop.add(Entity(MeshComponent(flatCube.front, Material.diffuse(black))).apply {
        setPosition(0.01, 0.09, 0.025)
        setRotation((-8.4).toRadians(), (-0.4).toRadians(), (+38.5).toRadians())
        setScale(0.146 * 0.5, 0.072 * 0.5, 0.00765 * 0.5)
    })
    prop.add(BoneAttachmentComponent("RightHand", renderer))

    LogManager.logAll()
    testSceneWithUI("Attach To Bone", scene)
}

