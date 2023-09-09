package me.anno.tests.ecs

import me.anno.animation.LoopingState
import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.components.anim.AnimRenderer
import me.anno.ecs.components.anim.AnimationState
import me.anno.ecs.components.anim.graph.AnimController
import me.anno.ecs.components.anim.graph.AnimStateNode
import me.anno.ecs.components.anim.graph.AnimStateNode.Companion.FADE
import me.anno.ecs.components.anim.graph.AnimStateNode.Companion.LOOP
import me.anno.ecs.components.anim.graph.AnimStateNode.Companion.SOURCE
import me.anno.ecs.components.anim.graph.AnimStateNode.Companion.SPEED
import me.anno.ecs.components.anim.graph.AnimStateNode.Companion.START
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testScene
import me.anno.graph.types.states.StateMachine
import me.anno.graph.ui.GraphEditor
import me.anno.io.ISaveable.Companion.registerCustomClass
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.utils.OS.downloads

fun main() {

    // todo looks ok, but not perfect yet

    ECSRegistry.init()
    registerCustomClass { StateMachine() }

    val entity = Entity()
    val renderer = AnimRenderer()
    val graph = StateMachine()
    val controller = AnimController()
    controller.graphSource = graph.ref

    // find file with multiple animations -> fox :)
    val folder = downloads.getChild("3d/azeria/scene.gltf")
    renderer.skeleton = folder.getChild("Skeleton.json")
    renderer.meshFile = folder.getChild("meshes/Object_0.json")
    val animations = folder.getChild("animations").listChildren()!!
        .map { it.getChild("Imported.json") }
    renderer.animations = animations
        .mapIndexed { idx, it -> AnimationState(it, if (idx == 0) 1f else 0f, 0f, 1f, LoopingState.PLAY_LOOP) }

    for ((idx, anim) in animations.withIndex()) {
        val node = AnimStateNode()
        node.setInput(SOURCE, anim)
        node.setInput(SPEED, 1f)
        node.setInput(START, 0f)
        node.setInput(LOOP, true) // todo instead of this, specify number of turns; or end time...
        node.setInput(FADE, 0.2f)
        val y = 300.0 * (idx - (animations.size - 1) * 0.5)
        node.position.set(0.0, y, 0.0)
        graph.add(node)
        if (idx == 0) graph.start(node)
    }

    for (idx in animations.indices) {
        graph.nodes[idx].connectTo(graph.nodes[(idx + 1) % graph.nodes.size])
    }

    entity.add(renderer)
    entity.add(controller)

    testUI("Animation Graph") {
        val list = CustomList(false, style)
        list.add(GraphEditor(graph, style))
        list.add(testScene(entity), 2f)
        list
    }
}