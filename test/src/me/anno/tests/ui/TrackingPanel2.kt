package me.anno.tests.ui

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.annotations.DebugProperty
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import kotlin.math.sin

class TestComponent : Component() {
    @DebugProperty
    val debugProperty get() = sin(Time.gameTime)

    @DebugProperty
    val cameraPosition get() = RenderState.cameraPosition
}

fun main() {
    registerCustomClass(TestComponent())
    testSceneWithUI("Tracking", TestComponent())
}