package me.anno.graph.visual.render.effects

import me.anno.gpu.query.GPUClockNanos
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.TextureLib.blackTexture
import me.anno.graph.visual.actions.ActionNode
import me.anno.graph.visual.node.Node
import me.anno.graph.visual.render.Texture

/**
 * A node with a GPU-timer, so the rendering times for it can be debugged easily.
 * */
abstract class TimedRenderingNode(name: String, inputs: List<String>, outputs: List<String>) :
    ActionNode(name, inputs, outputs) {

    // todo implement reasonable fail() for all rendering nodes

    val timer = GPUClockNanos()

    override fun destroy() {
        super.destroy()
        timer.destroy()
    }

    companion object {
        fun Node.finish(texture: Texture) {
            setOutput(1, texture)
        }

        fun Node.finish(texture: ITexture2D = blackTexture) {
            setOutput(1, Texture(texture))
        }
    }
}