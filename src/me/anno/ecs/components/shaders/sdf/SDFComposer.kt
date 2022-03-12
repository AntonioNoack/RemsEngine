package me.anno.ecs.components.shaders.sdf

import me.anno.config.DefaultConfig.style
import me.anno.gpu.shader.BaseShader
import me.anno.ui.Panel
import me.anno.ui.debug.TestStudio.Companion.testUI

object SDFComposer {

    fun createShader(node: SDFComponent): BaseShader {
        // todo also map the nodes internal values to uniform names or indices
        TODO()
    }

    @JvmStatic
    fun main(args: Array<String>) {
        // todo render test of shapes
        // todo we could try to recreate some basic samples from IQ with our nodes :)
        // this would ideally test our capabilities
        testUI {
            Panel(style)
        }
    }

}