package me.anno.video

import me.anno.gpu.shader.ShaderPlus
import me.anno.gpu.texture.ClampMode
import me.anno.gpu.texture.FilteringMode
import me.anno.gpu.texture.NearestMode
import java.io.InputStream

abstract class VFrame(var w: Int, var h: Int){
    var isLoaded = false
    abstract fun get3DShader(): ShaderPlus
    abstract fun bind(offset: Int, nearestFiltering: NearestMode, clampMode: ClampMode)
    fun bind(offset: Int, filtering: FilteringMode, clampMode: ClampMode){
        bind(offset, if(filtering.baseIsNearest) NearestMode.NEAREST else NearestMode.LINEAR, clampMode)
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