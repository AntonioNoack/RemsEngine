package me.anno.tests.engine.animation

import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponentInChildren
import me.anno.ecs.components.anim.AnimMeshComponent
import me.anno.ecs.components.anim.BoneAttachmentComponent
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.DefaultAssets
import me.anno.engine.OfficialExtensions
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
    OfficialExtensions.initForTests()
    val scene = Entity("Scene")

    // this mesh, including animation is from Mixamo
    val character = PrefabCache[downloads.getChild("3d/Talking On Phone.fbx")].waitFor()?.newInstance() as Entity
    scene.add(character)

    // a floor for decoration
    Entity("Floor", scene)
        .add(MeshComponent(DefaultAssets.plane))
        .setScale(2.5f)

    // find first AnimMeshComponent, so we can attach the calculation there
    val renderer = character.getComponentInChildren(AnimMeshComponent::class)!!

    val prop = Entity("Phone", scene)
        .add(BoneAttachmentComponent("RightHand", renderer))

    Entity("Offset for Phone", prop)
        .setPosition(0.01, 0.09, 0.025)
        .setRotation((-8.4f).toRadians(), (-0.4f).toRadians(), (+38.5f).toRadians())
        .setScale(0.146f * 0.5f, 0.072f * 0.5f, 0.00765f * 0.5f)
        .add(MeshComponent(flatCube.front, Material.diffuse(black)))

    LogManager.logAll()
    testSceneWithUI("Attach To Bone", scene)
}

