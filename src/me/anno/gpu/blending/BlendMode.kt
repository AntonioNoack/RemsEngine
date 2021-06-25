package me.anno.gpu.blending

import me.anno.language.translation.NameDesc
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30.*

// todo custom blend modes? -> maybe... could be customizable...
class BlendMode(
    val naming: NameDesc,
    val id: String
) {

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

    fun apply() {
        if (this != INHERIT) {
            if (lastFunc != func || lastFuncAlpha != funcAlpha) {
                lastFunc = func
                lastFuncAlpha = funcAlpha
                glBlendEquationSeparate(func.mode, funcAlpha.mode)
            }
            if (lastMode !== this && (func.hasParams || funcAlpha.hasParams)) {
                glBlendFuncSeparate(src, dst, srcAlpha, dstAlpha)
                lastMode = this
            }
        } else throw RuntimeException("UNSPECIFIED can't be applied!")
    }

    fun forceApply(){
        glBlendEquationSeparate(func.mode, funcAlpha.mode)
        glBlendFuncSeparate(src, dst, srcAlpha, dstAlpha)
    }

    fun copy(displayName: NameDesc, id: String): BlendMode {
        val mode = BlendMode(displayName, id)
        mode.set(src, dst, srcAlpha, dstAlpha)
        mode.set(func, funcAlpha)
        return mode
    }

    override fun toString(): String {
        return naming.name
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

        val INHERIT = BlendMode(NameDesc("Parent", "", "gpu.blendMode.parent"), "*Inherit")
        val DEFAULT = BlendMode(NameDesc("Default", "", "gpu.blendMode.default"), "*Blend")
        val ADD = BlendMode(NameDesc("Add", "", "gpu.blendMode.add"), "Add")
            .set(GL_SRC_ALPHA, GL_ONE)

        val PURE_ADD = BlendMode(NameDesc("Pure Add", "", "gpu.blendMode.addPure"), "Pure Add")
            .set(GL_ONE, GL_ONE)
            .set(BlendFunc.ADD)

        /*val ADD_MASK = BlendMode("Sub Mask", "Sub Mask")
            .set(GL_ONE, GL_ONE)
            .set(BlendFunc.SUB)*/
        // doesn't work
        val SUB_ALPHA = BlendMode(NameDesc("Override Masking", "", "gpu.blendMode.override"), "Override Masking")
            .set(GL_ONE, GL_ZERO, GL_SRC_ALPHA, GL_ZERO)
            .set(BlendFunc.ADD)

        val SUB = ADD.copy(NameDesc("Sub", "", "gpu.blendMode.sub"), "Subtract")
            .set(BlendFunc.REV_SUB)

        // a way to remove alpha from an image
        val NO_ALPHA = BlendMode(NameDesc("No Alpha", "", "gpu.blendMode.noAlpha"), "No Alpha")
            .set(GL_ONE, GL_ZERO, GL_ONE, GL_ZERO)
            .set(BlendFunc.ADD)

        val SUB_COLOR = ADD.copy(NameDesc("Sub Color", "", "gpu.blendMode.subColor"), "Subtract Color")
            .set(GL11.GL_ONE, GL11.GL_ONE)
            .set(BlendFunc.REV_SUB, BlendFunc.ADD)

        // a way to remove alpha from an image
        val DST_ALPHA = BlendMode(NameDesc("Dst Alpha", "", "gpu.blendMode.dstAlpha"), "Dst Alpha")
            .set(GL_ONE, GL_ZERO, GL_ZERO, GL_ONE)
            .set(BlendFunc.ADD)

        operator fun get(code: String) = blendModes[code] ?: INHERIT
    }

}