package me.anno.tests.ui.input

import me.anno.ecs.Component
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI

fun main() {
    // check element in property inspector:
    // - show red text that it's not registered
    // - show and edit properties anyway
    testSceneWithUI("PropertyInspector", object : Component() {
        var a = 0
        var b = 0f
        var c = "lol"
    })
}