package me.anno.gpu.shader

import me.anno.gpu.deferred.DeferredSettings2

// todo define the types of flat rendering here? would be a nice idea, so it stays customizable

open class Renderer(

    val isFakeColor: Boolean,
    val drawMode: ShaderPlus.DrawMode,
    // null, if not deferred
    val deferredSettings: DeferredSettings2? = null

) {

    companion object {
        val colorRenderer = Renderer(false, ShaderPlus.DrawMode.COLOR, null)
        val colorSqRenderer = Renderer(false, ShaderPlus.DrawMode.COLOR_SQUARED, null)
        val idRenderer = Renderer(true, ShaderPlus.DrawMode.ID, null)
        val depthRenderer = Renderer(true, ShaderPlus.DrawMode.DEPTH, null)
        val copyRenderer = Renderer(false, ShaderPlus.DrawMode.COPY, null)
    }

}