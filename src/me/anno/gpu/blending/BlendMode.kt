package me.anno.gpu.blending

import org.lwjgl.opengl.GL30.*
import java.lang.RuntimeException

// todo custom blend modes? -> maybe... could be customizable...
class BlendMode(
    val displayName: String,
    val id: String
){

    var src = GL_SRC_ALPHA
    var dst = GL_ONE_MINUS_SRC_ALPHA
    var srcAlpha = GL_SRC_ALPHA
    var dstAlpha = GL_ONE_MINUS_SRC_ALPHA
    var func = BlendFunc.ADD
    var funcAlpha = BlendFunc.ADD

    init {
        blendModes[id] = this
    }

    fun set(src: Int, dst: Int) = set(src, dst, srcAlpha, dstAlpha)
    fun set(src: Int, dst: Int, srcAlpha: Int, dstAlpha: Int): BlendMode {
        this.src = src
        this.dst = dst
        this.srcAlpha = srcAlpha
        this.dstAlpha = dstAlpha
        return this
    }

    fun set(func: BlendFunc, funcAlpha: BlendFunc = func): BlendMode {
        this.func = func
        this.funcAlpha = funcAlpha
        return this
    }

    fun apply(){
        if(this != UNSPECIFIED){
            if(lastFunc != func || lastFuncAlpha != funcAlpha){
                lastFunc = func
                lastFuncAlpha = funcAlpha
                glBlendEquationSeparate(func.mode, funcAlpha.mode)
            }
            if(lastMode !== this && (func.hasParams || funcAlpha.hasParams)){
                glBlendFuncSeparate(src, dst, srcAlpha, dstAlpha)
                lastMode = this
            }
        } else throw RuntimeException("UNSPECIFIED can't be applied!")
    }

    fun copy(displayName: String, id: String): BlendMode {
        val mode = BlendMode(displayName, id)
        mode.set(src, dst, srcAlpha, dstAlpha)
        mode.set(func, funcAlpha)
        return mode
    }

    companion object {

        var lastFunc: BlendFunc? = null
        var lastFuncAlpha: BlendFunc? = null
        var lastMode: BlendMode? = null

        /*
        DEFAULT("Default", 0, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA),
        ADD("Add", 1, GL_SRC_ALPHA, GL_ONE),
        SUBTRACT("Subtract", 2, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        */

        val UNSPECIFIED = BlendMode("Parent", "*Inherit")
        val DEFAULT = BlendMode("Default", "*Blend")
        val ADD = BlendMode("Add", "Add")
            .set(GL_SRC_ALPHA, GL_ONE)
        /*val ADD_MASK = BlendMode("Sub Mask", "Sub Mask")
            .set(GL_ONE, GL_ONE)
            .set(BlendFunc.SUB)*/
        // doesn't work
        val SUB_ALPHA = BlendMode("Override Masking", "Override Masking")
            .set(GL_ONE, GL_ZERO, GL_SRC_ALPHA, GL_ZERO)
            .set(BlendFunc.ADD)

        val SUB = ADD.copy("Sub", "Subtract")
            .set(BlendFunc.REV_SUB)

        // a way to remove alpha from an image
        val NO_ALPHA = BlendMode("No Alpha", "No Alpha")
            .set(GL_ONE, GL_ZERO, GL_ONE, GL_ZERO)
            .set(BlendFunc.ADD)

        operator fun get(code: String) = blendModes[code] ?: UNSPECIFIED
    }

}