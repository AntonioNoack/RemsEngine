package me.anno.video

import me.anno.gpu.Shader

abstract class Frame(val w: Int, val h: Int){
    var isLoaded = false
    abstract fun get3DShader(): Shader
    abstract fun bind(offset: Int)
    abstract fun destroy()
}