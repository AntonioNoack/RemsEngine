package me.anno.gpu.texture

import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.clamp
import org.lwjgl.opengl.GL46C.*

enum class Clamping(val id: Int, val naming: NameDesc, val mode: Int) {
    CLAMP(0, NameDesc("Clamp"), GL_CLAMP_TO_EDGE) {
        override fun apply(x: Int, w: Int) = clamp(x, 0, w - 1)
    },
    REPEAT(1, NameDesc("Repeat"), GL_REPEAT) {
        override fun apply(x: Int, w: Int): Int {
            var xi = x % w
            if (xi < 0) xi += w
            return xi
        }
    },
    MIRRORED_REPEAT(2, NameDesc("Mirrored"), GL_MIRRORED_REPEAT) {
        override fun apply(x: Int, w: Int): Int {
            val w2 = w + w
            var xi = x % w2
            if (xi < 0) xi += w2
            if (xi >= w) xi = w2 - xi
            return xi
        }
    },
    /**
     * good for text rendering, unfortunately not supported in WebGL :/
     * */
    CLAMP_TO_BORDER(3, NameDesc("ClampToBorder"), GL_CLAMP_TO_BORDER) {
        override fun apply(x: Int, w: Int) = clamp(x, 0, w - 1) // not really supported yet...
    };

    abstract fun apply(x: Int, w: Int): Int
}