package me.anno.bugs.done

import me.anno.config.DefaultConfig.style
import me.anno.engine.EngineBase
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderView1
import me.anno.gpu.GFX
import me.anno.language.translation.NameDesc
import me.anno.mesh.Shapes.flatCube

/**
 * this printed lots of errors and wasn't tonemapping correctly
 * */
fun main() {
    object : EngineBase(NameDesc("Crashing"), 1, false) {
        override fun createUI() {
            val renderView = RenderView1(PlayMode.PLAYING, flatCube.front, style)
            GFX.someWindow.windowStack
                .push(renderView)
                .drawDirectly = true
        }
    }.run()
}