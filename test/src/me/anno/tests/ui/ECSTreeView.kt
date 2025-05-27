package me.anno.tests.ui

import me.anno.ecs.Entity
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.RenderDoc.disableRenderDoc

fun main() {
    disableRenderDoc()
    ECSRegistry.init()
    val sample = Entity("Root")
        .add(Entity("Child 1")) // added before taking reference
        .add(Entity("Child 2")) // added before taking reference
        .add(Entity("Child 3")) // added before taking reference
    val ref = sample.ref
    sample
        .add(Entity("Child 4")) // added after taking reference
        .add(Entity("Child 5")) // added after taking reference
        .add(Entity("Child 6")) // added after taking reference
    testSceneWithUI("ECSTreeView", ref)
}