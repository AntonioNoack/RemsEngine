package me.anno.gpu.shader

import me.anno.gpu.deferred.DeferredSettingsV2

// todo define the types of flat rendering here? would be a nice idea, so it stays customizable

open class Renderer(

    val isFakeColor: Boolean,
    val drawMode: ShaderPlus.DrawMode,
    // null, if not deferred
    val deferredSettings: DeferredSettingsV2? = null

) {

    // todo the render engine decides how it is rendered...
    // todo this could include multiple passes as well...

    open fun getPostProcessing(): String = ""

    companion object {
        val colorRenderer = Renderer(false, ShaderPlus.DrawMode.COLOR, null)
        val colorSqRenderer = Renderer(false, ShaderPlus.DrawMode.COLOR_SQUARED, null)
        val idRenderer = Renderer(true, ShaderPlus.DrawMode.ID, null)
        val depthRenderer01 = Renderer(true, ShaderPlus.DrawMode.DEPTH_LOG2_01, null)
        val depthRenderer = Renderer(true, ShaderPlus.DrawMode.DEPTH_LOG2, null)
        val copyRenderer = Renderer(false, ShaderPlus.DrawMode.COPY, null)
    }

}