package me.anno.tests.engine.ui

import me.anno.ecs.Component
import me.anno.ecs.annotations.Type
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import org.joml.Vector3f

class ColorEditTest : Component() {
    @Type("Color3HDR")
    var color = Vector3f()
}

fun main() {
    // fixed: when editing the color, and exiting the menu, the value somehow resets...
    val comp = ColorEditTest()
    registerCustomClass(comp)
    testSceneWithUI("Color Edit", comp)
}