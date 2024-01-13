package me.anno.tests.engine.ui

import me.anno.config.DefaultConfig.style
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderView1
import me.anno.gpu.GFX
import me.anno.mesh.Shapes.flatCube
import me.anno.studio.StudioBase
import me.anno.ui.Window

fun main() {
    // this printed lots of errors and wasn't tonemapping correctly
    object : StudioBase("Crashing", 1, false) {
        override fun createUI() {
            val renderView = RenderView1(PlayMode.PLAYING, flatCube.front, style)
            val windowStack = GFX.someWindow.windowStack
            val window = Window(renderView, false, windowStack)
            window.drawDirectly = true
            windowStack.add(window)
        }
    }.run()
}