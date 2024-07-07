package me.anno.tests.engine

import me.anno.Engine
import me.anno.ecs.Component
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI

fun main() {
    testSceneWithUI("OnUpdate-Test", object : Component(), OnUpdate {
        override fun onUpdate() {
            println("It's working :)")
            Engine.requestShutdown()
        }
    })
}