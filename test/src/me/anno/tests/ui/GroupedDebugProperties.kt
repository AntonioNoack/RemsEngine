package me.anno.tests.ui

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.Group
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS

class TestClass : Component() {

    @DebugProperty
    val time get() = Time.nanoTime

    @DebugProperty
    @Group("Folders")
    val home get() = OS.home

    @DebugProperty
    @Group("Folders")
    val documents get() = OS.documents
}

fun main() {
    testSceneWithUI("Grouped DebugProperties", TestClass())
}