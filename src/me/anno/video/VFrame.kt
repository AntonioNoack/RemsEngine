package me.anno.video

import me.anno.gpu.GFX.glThread
import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.GPUFiltering
import me.anno.utils.Sleep.sleepShortly
import java.io.InputStream
import java.lang.RuntimeException

abstract class VFrame(var w: Int, var h: Int, val code: Int){
    open val isCreated = false
    abstract fun get3DShader(): Shader
    abstract fun bind(offset: Int, nearestFiltering: GPUFiltering, clamping: Clamping)
    fun bind(offset: Int, filtering: Filtering, clamping: Clamping){
        bind(offset, if(filtering.baseIsNearest) GPUFiltering.NEAREST else GPUFiltering.LINEAR, clamping)
    }
    open fun destroy(){}
    abstract fun load(input: InputStream)
    fun waitToLoad(){
        if(Thread.currentThread() == glThread) throw RuntimeException("Cannot wait on main thread")
        while (true) {
            if (isCreated) break
            sleepShortly()
        }
    }
}