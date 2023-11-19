package me.anno.tests.engine.prefab

import me.anno.ecs.components.anim.graph.AnimStateNode
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.graph.types.flow.control.IfElseNode
import me.anno.graph.types.flow.maths.NotNode
import me.anno.graph.types.flow.maths.ValueNode
import me.anno.graph.types.states.StateMachine
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.utils.OS.desktop
import me.anno.utils.OS.documents

/**
 * on first run, you should see all nodes clobbered together;
 * on reload, same result;
 *
 * then you can reorder them;
 * and on reload, they should have the same positions and connections
 * */
fun main() {

    // todo saving is resetting the scene for unknown reasons
    // todo loading a scene, no connections are set

    ECSRegistry.init()

    val testFile = desktop.getChild("StateMachineTest.json")

    fun createNode(anim: FileReference): AnimStateNode {
        val node = AnimStateNode()
        node.setInput(AnimStateNode.SOURCE, anim)
        node.setInput(AnimStateNode.SPEED, 1f)
        node.setInput(AnimStateNode.START, 0f)
        node.setInput(AnimStateNode.LOOP, true)
        node.setInput(AnimStateNode.FADE, 0.2f)
        return node
    }

    val graphRef = PrefabCache.loadOrInit(testFile, StateMachine::class, InvalidRef) {
        StateMachine().apply {
            // these animations don't need to exist, but they need to be saved properly
            val files = documents.getChild("Characters/anim-files")
            val idle = add(createNode(files.getChild("Idle.fbx")))
            val walking = add(createNode(files.getChild("Walking-inPlace.fbx")))
            val ifWalking = add(IfElseNode())
            idle.connectTo(ifWalking)
            ifWalking.connectTo(walking)
            val walkingCondition = add(ValueNode("Boolean").apply { setInput(0, true) })
            walkingCondition.connectTo(ifWalking, 1)
            val ifIdle = add(IfElseNode())
            walking.connectTo(ifIdle)
            ifIdle.connectTo(idle)
            val notWalkingNode = add(NotNode())
            walkingCondition.connectTo(notWalkingNode)
            notWalkingNode.connectTo(ifIdle, 1)
            start(idle)
        }
    }

    testSceneWithUI("StateMachineUI", graphRef.first)
}