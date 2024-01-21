package me.anno.gpu.blending

import me.anno.language.translation.NameDesc
import org.lwjgl.opengl.GL46C

// custom blend modes? only for the engine by programmers;
// I don't think people in Rem's Studio or otherwise would/could use them well
class BlendMode(
    val naming: NameDesc,
    val id: String
) {

    var src = GL46C.GL_SRC_ALPHA
    var dst = GL46C.GL_ONE_MINUS_SRC_ALPHA

    var srcAlpha = GL46C.GL_ONE
    var dstAlpha = GL46C.GL_ONE_MINUS_SRC_ALPHA

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
                GL46C.glBlendEquationSeparate(blendFuncModes[func.ordinal], blendFuncModes[funcAlpha.ordinal])
            }
            if (lastMode !== this && (func.hasParams || funcAlpha.hasParams)) {
                GL46C.glBlendFuncSeparate(src, dst, srcAlpha, dstAlpha)
                lastMode = this
            }
        } else throw RuntimeException("UNSPECIFIED can't be applied!")
    }

    fun forceApply() {
        GL46C.glBlendEquationSeparate(blendFuncModes[func.ordinal], blendFuncModes[funcAlpha.ordinal])
        GL46C.glBlendFuncSeparate(src, dst, srcAlpha, dstAlpha)
    }

    fun forceApply(i: Int) {
        GL46C.glBlendEquationSeparatei(i, blendFuncModes[func.ordinal], blendFuncModes[funcAlpha.ordinal])
        GL46C.glBlendFuncSeparatei(i, src, dst, srcAlpha, dstAlpha)
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

        val blendFuncModes = intArrayOf(
            GL46C.GL_FUNC_ADD,
            GL46C.GL_FUNC_SUBTRACT,
            GL46C.GL_FUNC_REVERSE_SUBTRACT,
            GL46C.GL_MIN,
            GL46C.GL_MAX
        )

        /*
        DEFAULT("Default", 0, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA),
        ADD("Add", 1, GL_SRC_ALPHA, GL_ONE),
        SUBTRACT("Subtract", 2, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        */

        val INHERIT = BlendMode(NameDesc("Parent", "", "gpu.blendMode.parent"), "*Inherit")

        /**
         * back to front, non-premultiplied-colors, start/blending transparency being any value between 0 and 1
         * */
        val DEFAULT = BlendMode(NameDesc("Default", "", "gpu.blendMode.default"), "*Blend")

        val ADD = BlendMode(NameDesc("Add", "", "gpu.blendMode.add"), "Add")
            .set(GL46C.GL_SRC_ALPHA, GL46C.GL_ONE)

        val PURE_ADD = BlendMode(NameDesc("Pure Add", "", "gpu.blendMode.addPure"), "Pure Add")
            .set(GL46C.GL_ONE, GL46C.GL_ONE)

        @Suppress("unused")
        val PURE_MUL = BlendMode(NameDesc("Pure Mul", "", "gpu.blendMode.addMul"), "Pure Mul")
            .set(GL46C.GL_DST_COLOR, GL46C.GL_ZERO)

        /*val ADD_MASK = BlendMode("Sub Mask", "Sub Mask")
            .set(GL_ONE, GL_ONE)
            .set(BlendFunc.SUB)*/
        // doesn't work
        @Suppress("unused")
        val SUB_ALPHA = BlendMode(NameDesc("Override Masking", "", "gpu.blendMode.override"), "Override Masking")
            .set(GL46C.GL_ONE, GL46C.GL_ZERO, GL46C.GL_SRC_ALPHA, GL46C.GL_ZERO)
            .set(BlendFunc.ADD)

        @Suppress("unused")
        val SUB = ADD.cloneWithName(NameDesc("Sub", "", "gpu.blendMode.sub"), "Subtract")
            .set(BlendFunc.REV_SUB)

        // a way to remove alpha from an image
        @Suppress("unused")
        val NO_ALPHA = BlendMode(NameDesc("No Alpha", "", "gpu.blendMode.noAlpha"), "No Alpha")
            .set(GL46C.GL_ONE, GL46C.GL_ZERO, GL46C.GL_ONE, GL46C.GL_ZERO)
            .set(BlendFunc.ADD)

        @Suppress("unused")
        val SUB_COLOR = ADD.cloneWithName(NameDesc("Sub Color", "", "gpu.blendMode.subColor"), "Subtract Color")
            .set(GL46C.GL_ONE, GL46C.GL_ONE)
            .set(BlendFunc.REV_SUB, BlendFunc.ADD)

        // a way to remove alpha from an image
        @Suppress("unused")
        val DST_ALPHA = BlendMode(NameDesc("Dst Alpha", "", "gpu.blendMode.dstAlpha"), "Dst Alpha")
            .set(GL46C.GL_ONE, GL46C.GL_ZERO, GL46C.GL_ZERO, GL46C.GL_ONE)
            .set(BlendFunc.ADD)

        operator fun get(code: String) = blendModes[code] ?: INHERIT
    }
}