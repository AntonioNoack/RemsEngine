package me.anno.video

import me.anno.gpu.shader.ShaderPlus
import me.anno.gpu.texture.FilteringMode
import java.io.InputStream

abstract class Frame(var w: Int, var h: Int){
    var isLoaded = false
    abstract fun get3DShader(): ShaderPlus
    abstract fun bind(offset: Int, nearestFiltering: Boolean)
    fun bind(offset: Int, filtering: FilteringMode) = bind(offset, filtering.baseIsNearest)
    abstract fun destroy()
    abstract fun load(input: InputStream)
}