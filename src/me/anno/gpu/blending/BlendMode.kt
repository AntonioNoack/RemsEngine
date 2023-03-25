package me.anno.gpu.blending

import me.anno.language.translation.NameDesc
import org.lwjgl.opengl.GL30C.*
import org.lwjgl.opengl.GL40C.glBlendEquationSeparatei
import org.lwjgl.opengl.GL40C.glBlendFuncSeparatei

// custom blend modes? only for the engine by programmers;
// I don't think people in Rem's Studio or otherwise would/could use them well
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

    fun set(src: Int, dst: Int) = set(src, dst, src, dst)
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

    fun forceApply() {
        glBlendEquationSeparate(func.mode, funcAlpha.mode)
        glBlendFuncSeparate(src, dst, srcAlpha, dstAlpha)
    }

    fun forceApply(i: Int) {
        glBlendEquationSeparatei(i, func.mode, funcAlpha.mode)
        glBlendFuncSeparatei(i, src, dst, srcAlpha, dstAlpha)
    }

    fun cloneWithName(displayName: NameDesc, id: String): BlendMode {
        val mode = BlendMode(displayName, id)
        mode.set(src, dst, srcAlpha, dstAlpha)
        mode.set(func, funcAlpha)
        return mode
    }

    override fun toString(): String {
        return naming.name
    }

    companion object {

        val blendModes = HashMap<String, BlendMode>()

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

        val PURE_MUL = BlendMode(NameDesc("Pure Add", "", "gpu.blendMode.addPure"), "Pure Add")
            .set(GL_DST_COLOR, GL_ZERO)

        /*val ADD_MASK = BlendMode("Sub Mask", "Sub Mask")
            .set(GL_ONE, GL_ONE)
            .set(BlendFunc.SUB)*/
        // doesn't work
        @Suppress("unused")
        val SUB_ALPHA = BlendMode(NameDesc("Override Masking", "", "gpu.blendMode.override"), "Override Masking")
            .set(GL_ONE, GL_ZERO, GL_SRC_ALPHA, GL_ZERO)
            .set(BlendFunc.ADD)

        @Suppress("unused")
        val SUB = ADD.cloneWithName(NameDesc("Sub", "", "gpu.blendMode.sub"), "Subtract")
            .set(BlendFunc.REV_SUB)

        // a way to remove alpha from an image
        @Suppress("unused")
        val NO_ALPHA = BlendMode(NameDesc("No Alpha", "", "gpu.blendMode.noAlpha"), "No Alpha")
            .set(GL_ONE, GL_ZERO, GL_ONE, GL_ZERO)
            .set(BlendFunc.ADD)

        @Suppress("unused")
        val SUB_COLOR = ADD.cloneWithName(NameDesc("Sub Color", "", "gpu.blendMode.subColor"), "Subtract Color")
            .set(GL_ONE, GL_ONE)
            .set(BlendFunc.REV_SUB, BlendFunc.ADD)

        // a way to remove alpha from an image
        @Suppress("unused")
        val DST_ALPHA = BlendMode(NameDesc("Dst Alpha", "", "gpu.blendMode.dstAlpha"), "Dst Alpha")
            .set(GL_ONE, GL_ZERO, GL_ZERO, GL_ONE)
            .set(BlendFunc.ADD)

        operator fun get(code: String) = blendModes[code] ?: INHERIT
    }

}