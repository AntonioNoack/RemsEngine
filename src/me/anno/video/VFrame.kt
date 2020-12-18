package me.anno.video

import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderPlus
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.GPUFiltering
import java.io.InputStream

abstract class VFrame(var w: Int, var h: Int){
    var isLoaded = false
    abstract fun get3DShader(): Shader
    abstract fun bind(offset: Int, nearestFiltering: GPUFiltering, clamping: Clamping)
    fun bind(offset: Int, filtering: Filtering, clamping: Clamping){
        bind(offset, if(filtering.baseIsNearest) GPUFiltering.NEAREST else GPUFiltering.LINEAR, clamping)
    }
    abstract fun destroy()
    abstract fun load(input: InputStream)
    fun waitToLoad(){
        while (true) {
            if (isLoaded) break
            Thread.sleep(1)
        }
    }
}