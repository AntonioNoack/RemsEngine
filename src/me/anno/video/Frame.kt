package me.anno.video

import me.anno.gpu.shader.ShaderPlus
import me.anno.gpu.texture.ClampMode
import me.anno.gpu.texture.FilteringMode
import java.io.InputStream

abstract class Frame(var w: Int, var h: Int){
    var isLoaded = false
    abstract fun get3DShader(): ShaderPlus
    abstract fun bind(offset: Int, nearestFiltering: Boolean, clampMode: ClampMode)
    fun bind(offset: Int, filtering: FilteringMode, clampMode: ClampMode) = bind(offset, filtering.baseIsNearest, clampMode)
    abstract fun destroy()
    abstract fun load(input: InputStream)
}