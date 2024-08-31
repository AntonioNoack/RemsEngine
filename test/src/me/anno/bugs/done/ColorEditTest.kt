package me.anno.bugs.done

import me.anno.ecs.Component
import me.anno.ecs.annotations.Type
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import org.joml.Vector3f

class ColorEditTest : Component() {
    @Type("Color3HDR")
    var color = Vector3f()
}

/**
 * fixed: when editing the color, and exiting the menu, the value somehow resets...
 * */
fun main() {
    val comp = ColorEditTest()
    registerCustomClass(comp)
    testSceneWithUI("Color Edit", comp) {
        EditorState.select(comp)
    }
}