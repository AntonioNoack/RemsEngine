package me.anno.video

import me.anno.gpu.shader.ShaderPlus
import java.io.InputStream

abstract class Frame(var w: Int, var h: Int){
    var isLoaded = false
    abstract fun get3DShader(): ShaderPlus
    abstract fun bind(offset: Int, nearestFiltering: Boolean)
    abstract fun destroy()
    abstract fun load(input: InputStream)
}