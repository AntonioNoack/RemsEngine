package me.anno.tests.engine.ui

import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.LineRenderer
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.ecs.components.ui.CanvasComponent
import me.anno.engine.ECSRegistry
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.CullMode
import me.anno.gpu.drawing.DrawRectangles
import me.anno.gpu.pipeline.PipelineStage
import me.anno.language.translation.NameDesc
import me.anno.openxr.ecs.VRHandController
import me.anno.openxr.ecs.VRHandPickup
import me.anno.openxr.ecs.VRSocket
import me.anno.ui.base.buttons.TextButton
import me.anno.utils.OS.documents
import me.anno.utils.OS.res

fun main() {

    // todo ui probably should react, if it is camera-space (only scaled properly in non-editor-mode)
    // todo option for rendering text as Mesh, to make it scale-independent?

    // create UI in 3d
    ECSRegistry.init()
    OfficialExtensions.initForTests()

    val ui = object : TextButton(NameDesc("Test Button"), style) {
        override fun onUpdate() {
            super.onUpdate()
            val ws = windowStack
            text = "[${ws.mouseXi},${ws.mouseYi}]"
        }

        override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
            super.draw(x0, y0, x1, y1)
            // draw cursor position
            val ws = windowStack
            DrawRectangles.drawRect(ws.mouseXi, ws.mouseYi, 1, 1, -1)
        }
    }

    val scene = Entity("Scene")
    scene.add(CanvasComponent().apply {
        add(ui)
        width = 240
        height = 40
    })

    prepareHand(scene, false)
    prepareHand(scene, true)

    Entity("NavMesh", scene)
        .add(MeshComponent(res.getChild("meshes/NavMesh.fbx")))
        .setPosition(-2.0, -1.0, -2.0)
        .setScale(0.25f)

    // add 5 objects, which can be picked up
    val ball = IcosahedronModel.createIcosphere(2, 0.05f)
    for (i in 0 until 5) {
        scene.add(
            Entity("Pickup[$i]", scene)
                .add(MeshComponent(res.getChild("meshes/CuteGhost.fbx")))
                .add(VRHandPickup().apply {
                    // todo test this
                    shouldBeLockedInHand = i == 2
                })
                .setPosition(1.0 + i, 1.0, 0.0)
                .setScale(0.1f)
        )
        // add 5 sockets, where objects can be placed
        scene.add(
            Entity("Socket[$i]")
                .add(MeshComponent(ball))
                .add(VRSocket())
                .setPosition(1.0 + i, 1.0, 0.0)
        )
    }

    testSceneWithUI("UI in 3d", scene)
}

val teleportLineMaterial = Material().apply {
    indexOfRefraction = 1f
    metallicMinMax.set(1f)
    roughnessMinMax.set(0.01f)
    pipelineStage = PipelineStage.TRANSPARENT
}

fun prepareHand(scene: Entity, isRightHand1: Boolean) {
    val hand = Entity(if (isRightHand1) "Right Hand" else "Left Hand", scene)
    if (!isRightHand1) {
        // fix outside-inside
        val material = Material()
        material.cullMode = CullMode.BACK
        Entity(hand)
            .add(MeshComponent(documents.getChild("Blender/VRControllerV0.glb"), material))
            .setScale(-1f, 1f, 1f)
    } else {
        hand.add(MeshComponent(documents.getChild("Blender/VRControllerV0.glb")))
    }
    hand.add(VRHandController().apply {
        isRightHand = isRightHand1
        if (isRightHand) {
            val ref = res.getChild("meshes/TeleportCircle.glb")
            val handCube = Entity().add(MeshComponent(ref, teleportLineMaterial))
            scene.add(handCube)
            teleportCircleMesh = handCube
        }
    })
    hand.add(LineRenderer().apply {
        materials = listOf(teleportLineMaterial.ref)
    })
}
