package me.anno.tests.engine.ui

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.LineRenderer
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.ui.CanvasComponent
import me.anno.engine.ECSRegistry
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.pipeline.PipelineStage
import me.anno.io.files.Reference.getReference
import me.anno.openxr.ecs.VRHandController
import me.anno.openxr.ecs.VRHandPickup
import me.anno.ui.UIColors
import me.anno.ui.Window
import me.anno.ui.base.buttons.TextButton

fun main() {
    // todo ui probably should react, if it is camera-space (only scaled properly in non-editor-mode)
    // todo option for rendering text as Mesh, to make it scale-independent?
    // create UI in 3d
    ECSRegistry.init()
    OfficialExtensions.initForTests()
    val scene = Entity("Scene")
    scene.add(CanvasComponent().apply {
        val ui = TextButton("Test Button", style)
        windowStack.add(Window(ui, false, windowStack))
        width = 120
        height = 40
    })

    prepareHand(scene, UIColors.axisXColor, false)
    prepareHand(scene, UIColors.axisZColor, true)

    Entity("NavMesh", scene).add(MeshComponent(getReference("res://meshes/NavMesh.fbx"))).setScale(0.25)

    // add 5 objects, which can be picked up
    for (i in 0 until 5) {
        scene.add(
            Entity("Pickup[$i]", scene)
                .add(MeshComponent(getReference("res://meshes/CuteGhost.fbx")))
                .add(VRHandPickup().apply {
                    // todo handle that property
                    shouldBeLockedInHand = i == 2
                })
                .setPosition(1.0 + i, 1.0, 0.0)
                .setScale(0.1)
        )
    }

    // todo add 5 sockets, where objects can be placed

    testSceneWithUI("UI in 3d", scene)
}

val teleportLineMaterial = Material().apply {
    indexOfRefraction = 1f
    metallicMinMax.set(1f)
    roughnessMinMax.set(0.01f)
    pipelineStage = PipelineStage.TRANSPARENT
}

fun prepareHand(scene: Entity, color: Int, isRightHand1: Boolean) {
    val hand = Entity("Left Hand", scene)
    hand.add(VRHandController().apply {
        isRightHand = isRightHand1
        // todo make hit target a circle to indicate teleport opportunities
        if (isRightHand) {
            val handCube = createCube()
            scene.add(handCube)
            teleportCircleMesh = handCube
        }
    })
    // todo add controller mesh
    // hand.add(createCube())
    hand.add(LineRenderer().apply {
        materials = listOf(teleportLineMaterial.ref)
    })
}

fun createCube(): Entity {
    return Entity().add(MeshComponent(getReference("res://meshes/TeleportCircle.glb"), teleportLineMaterial))
}